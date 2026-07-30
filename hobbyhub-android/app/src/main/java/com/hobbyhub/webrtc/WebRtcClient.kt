package com.hobbyhub.webrtc

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.util.Log
import org.webrtc.*
import org.webrtc.audio.JavaAudioDeviceModule

interface WebRtcListener {
    fun onRemoteAudioTrackAdded(userId: String, track: AudioTrack)
    fun onRemoteAudioTrackRemoved(userId: String)
    fun onError(error: String)
}

class WebRtcClient(
    private val context: Context,
    private val userId: String,
    private val signalingClient: SignalingClient,
    private val listener: WebRtcListener
) {
    private val TAG = "WebRtcClient"
    
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var audioDeviceModule: JavaAudioDeviceModule? = null
    private var localAudioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null
    private var rootEglBase: EglBase? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    // Map of remoteUserId -> PeerConnection
    private val peerConnections = mutableMapOf<String, PeerConnection>()

    // Map of remoteUserId -> AudioTrack (to prevent garbage collection)
    private val remoteAudioTracks = mutableMapOf<String, AudioTrack>()

    // Queue for ICE candidates arriving before remote description is set
    private val pendingIceCandidates = mutableMapOf<String, MutableList<IceCandidate>>()

    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun3.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun4.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:global.stun.twilio.com:3478").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun.services.mozilla.com").createIceServer(),
        PeerConnection.IceServer.builder("turn:openrelay.metered.ca:80")
            .setUsername("openrelay")
            .setPassword("openrelay")
            .createIceServer(),
        PeerConnection.IceServer.builder("turn:openrelay.metered.ca:443")
            .setUsername("openrelay")
            .setPassword("openrelay")
            .createIceServer(),
        PeerConnection.IceServer.builder("turn:openrelay.metered.ca:443?transport=tcp")
            .setUsername("openrelay")
            .setPassword("openrelay")
            .createIceServer()
    )

    init {
        setupAudioManager()
        initWebRtc()
    }

    private fun setupAudioManager() {
        try {
            audioManager?.let { am ->
                am.mode = AudioManager.MODE_IN_COMMUNICATION
                am.isMicrophoneMute = false
                
                // Crucial Fix for Android 12+ (API 31, 34, 35 - Pixel 8 Pro):
                // Modern Android OS deprecates isSpeakerphoneOn and requires setCommunicationDevice
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val devices = am.availableCommunicationDevices
                    val speakerDevice = devices.find { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                    if (speakerDevice != null) {
                        am.setCommunicationDevice(speakerDevice)
                        Log.d(TAG, "Android 12+ Communication Device set to Built-in Speaker")
                    } else {
                        @Suppress("DEPRECATION")
                        am.isSpeakerphoneOn = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    am.isSpeakerphoneOn = true
                }

                Log.d(TAG, "AudioManager configured: MODE_IN_COMMUNICATION, Speakerphone ON")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up AudioManager", e)
        }
    }

    fun setSpeakerphoneOn(speakerOn: Boolean) {
        try {
            audioManager?.let { am ->
                am.mode = AudioManager.MODE_IN_COMMUNICATION
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (speakerOn) {
                        val speakerDevice = am.availableCommunicationDevices.find { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                        if (speakerDevice != null) {
                            am.setCommunicationDevice(speakerDevice)
                        }
                    } else {
                        am.clearCommunicationDevice()
                    }
                } else {
                    @Suppress("DEPRECATION")
                    am.isSpeakerphoneOn = speakerOn
                }
                Log.d(TAG, "Speakerphone set to: $speakerOn")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting speakerphone", e)
        }
    }

    fun setCallVolume(volumeRatio: Float) {
        try {
            val clampedRatio = volumeRatio.coerceIn(0.0f, 1.0f)
            audioManager?.let { am ->
                val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
                val targetVol = (maxVol * clampedRatio).toInt()
                am.setStreamVolume(AudioManager.STREAM_VOICE_CALL, targetVol, 0)
            }
            remoteAudioTracks.values.forEach { track ->
                try {
                    track.setVolume(clampedRatio.toDouble())
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting call volume", e)
        }
    }

    private fun initWebRtc() {
        try {
            PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(context)
                    .setEnableInternalTracer(false)
                    .createInitializationOptions()
            )

            rootEglBase = EglBase.create()
            val options = PeerConnectionFactory.Options()

            // Safe JavaAudioDeviceModule initialization checking hardware support (Crucial for Emulator stability!)
            audioDeviceModule = try {
                val useHardwareAec = try { JavaAudioDeviceModule.isBuiltInAcousticEchoCancelerSupported() } catch (_: Throwable) { false }
                val useHardwareNs = try { JavaAudioDeviceModule.isBuiltInNoiseSuppressorSupported() } catch (_: Throwable) { false }
                
                Log.d(TAG, "Hardware Audio HAL Support - AEC: $useHardwareAec, NS: $useHardwareNs")

                JavaAudioDeviceModule.builder(context)
                    .setUseHardwareAcousticEchoCanceler(useHardwareAec)
                    .setUseHardwareNoiseSuppressor(useHardwareNs)
                    .setAudioRecordErrorCallback(object : JavaAudioDeviceModule.AudioRecordErrorCallback {
                        override fun onWebRtcAudioRecordInitError(errorMessage: String?) {
                            Log.e(TAG, "AudioRecordInitError: $errorMessage")
                        }
                        override fun onWebRtcAudioRecordStartError(errorCode: JavaAudioDeviceModule.AudioRecordStartErrorCode?, errorMessage: String?) {
                            Log.e(TAG, "AudioRecordStartError: $errorMessage")
                        }
                        override fun onWebRtcAudioRecordError(errorMessage: String?) {
                            Log.e(TAG, "AudioRecordError: $errorMessage")
                        }
                    })
                    .setAudioTrackErrorCallback(object : JavaAudioDeviceModule.AudioTrackErrorCallback {
                        override fun onWebRtcAudioTrackInitError(errorMessage: String?) {
                            Log.e(TAG, "AudioTrackInitError: $errorMessage")
                        }
                        override fun onWebRtcAudioTrackStartError(errorCode: JavaAudioDeviceModule.AudioTrackStartErrorCode?, errorMessage: String?) {
                            Log.e(TAG, "AudioTrackStartError: $errorMessage")
                        }
                        override fun onWebRtcAudioTrackError(errorMessage: String?) {
                            Log.e(TAG, "AudioTrackError: $errorMessage")
                        }
                    })
                    .createAudioDeviceModule()
            } catch (e: Throwable) {
                Log.w(TAG, "Fallback: Unable to initialize hardware JavaAudioDeviceModule: ${e.message}")
                null
            }

            val factoryBuilder = PeerConnectionFactory.builder()
                .setOptions(options)
            audioDeviceModule?.let { factoryBuilder.setAudioDeviceModule(it) }

            peerConnectionFactory = factoryBuilder.createPeerConnectionFactory()

            createLocalAudioTrack()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing WebRTC PeerConnectionFactory", e)
        }
    }

    private fun createLocalAudioTrack() {
        try {
            val factory = peerConnectionFactory ?: return
            
            // Audio constraints (Optional for maximum device & emulator compatibility)
            val audioConstraints = MediaConstraints()
            audioConstraints.optional.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
            audioConstraints.optional.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
            audioConstraints.optional.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
            audioConstraints.optional.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))

            localAudioSource = factory.createAudioSource(audioConstraints)
            localAudioTrack = factory.createAudioTrack("local_audio_track_$userId", localAudioSource)
            localAudioTrack?.setEnabled(true)
            localAudioTrack?.setVolume(1.0)
            Log.d(TAG, "Local Audio Track created successfully for user $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating Local Audio Track (Check Mic Permission / Audio HAL)", e)
        }
    }

    fun setMicrophoneMute(mute: Boolean) {
        try {
            localAudioTrack?.setEnabled(!mute)
            audioManager?.isMicrophoneMute = mute
        } catch (e: Exception) {
            Log.e(TAG, "Error setting mic mute", e)
        }
    }

    private fun getOrCreatePeerConnection(targetUserId: String): PeerConnection? {
        if (peerConnections.containsKey(targetUserId)) {
            return peerConnections[targetUserId]
        }

        val factory = peerConnectionFactory ?: return null
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers)
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        rtcConfig.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY

        val pcObserver = object : PeerConnection.Observer {
            override fun onSignalingChange(p0: PeerConnection.SignalingState?) {}

            override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
                Log.d(TAG, "ICE Connection State for $targetUserId: $newState")
                if (newState == PeerConnection.IceConnectionState.DISCONNECTED ||
                    newState == PeerConnection.IceConnectionState.FAILED || 
                    newState == PeerConnection.IceConnectionState.CLOSED) {
                    removePeerConnection(targetUserId)
                }
            }

            override fun onIceConnectionReceivingChange(p0: Boolean) {}
            override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}

            override fun onIceCandidate(candidate: IceCandidate) {
                signalingClient.sendIceCandidate(
                    targetId = targetUserId,
                    sdp = candidate.sdp,
                    sdpMid = candidate.sdpMid,
                    sdpMLineIndex = candidate.sdpMLineIndex
                )
            }

            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}

            override fun onAddStream(stream: MediaStream) {
                if (stream.audioTracks.isNotEmpty()) {
                    val track = stream.audioTracks[0]
                    track.setEnabled(true)
                    track.setVolume(1.0)
                    remoteAudioTracks[targetUserId] = track
                    listener.onRemoteAudioTrackAdded(targetUserId, track)
                }
            }

            override fun onRemoveStream(stream: MediaStream) {
                remoteAudioTracks.remove(targetUserId)
                listener.onRemoteAudioTrackRemoved(targetUserId)
            }

            override fun onDataChannel(p0: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            
            override fun onAddTrack(receiver: RtpReceiver, mediaStreams: Array<out MediaStream>) {
                val track = receiver.track()
                if (track is AudioTrack) {
                    Log.d(TAG, "Remote Audio Track added from $targetUserId")
                    track.setEnabled(true)
                    track.setVolume(1.0)
                    remoteAudioTracks[targetUserId] = track
                    listener.onRemoteAudioTrackAdded(targetUserId, track)
                }
            }
        }

        val peerConnection = factory.createPeerConnection(rtcConfig, pcObserver)
        
        // Add local audio track to this peer connection
        localAudioTrack?.let { track ->
            peerConnection?.addTrack(track, listOf("audio_stream"))
        }

        if (peerConnection != null) {
            peerConnections[targetUserId] = peerConnection
        }
        return peerConnection
    }

    private fun drainPendingIceCandidates(targetUserId: String, peerConnection: PeerConnection) {
        val candidates = pendingIceCandidates.remove(targetUserId) ?: return
        Log.d(TAG, "Draining ${candidates.size} queued ICE candidates for $targetUserId")
        for (candidate in candidates) {
            peerConnection.addIceCandidate(candidate)
        }
    }

    fun handleUserJoined(targetUserId: String) {
        val isOfferer = userId < targetUserId
        if (!isOfferer) {
            Log.d(TAG, "Polite peer ($userId > $targetUserId): Waiting for offer from $targetUserId")
            return
        }

        Log.d(TAG, "Impolite peer ($userId < $targetUserId): Creating offer for $targetUserId")
        val peerConnection = getOrCreatePeerConnection(targetUserId) ?: return

        val constraints = MediaConstraints()
        constraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))

        peerConnection.createOffer(object : SdpObserverAdapter() {
            override fun onCreateSuccess(sessionDescription: SessionDescription?) {
                sessionDescription?.let {
                    peerConnection.setLocalDescription(object : SdpObserverAdapter() {
                        override fun onSetSuccess() {
                            signalingClient.sendOffer(targetUserId, it.description)
                        }
                    }, it)
                }
            }
        }, constraints)
    }

    fun handleOfferReceived(senderId: String, sdp: String) {
        Log.d(TAG, "Handling Offer from $senderId")
        val peerConnection = getOrCreatePeerConnection(senderId) ?: return
        val sessionDescription = SessionDescription(SessionDescription.Type.OFFER, sdp)
        
        peerConnection.setRemoteDescription(object : SdpObserverAdapter() {
            override fun onSetSuccess() {
                drainPendingIceCandidates(senderId, peerConnection)

                val constraints = MediaConstraints()
                constraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))

                peerConnection.createAnswer(object : SdpObserverAdapter() {
                    override fun onCreateSuccess(answerSdp: SessionDescription?) {
                        answerSdp?.let {
                            peerConnection.setLocalDescription(object : SdpObserverAdapter() {
                                override fun onSetSuccess() {
                                    signalingClient.sendAnswer(senderId, it.description)
                                }
                            }, it)
                        }
                    }
                }, constraints)
            }
        }, sessionDescription)
    }

    fun handleAnswerReceived(senderId: String, sdp: String) {
        Log.d(TAG, "Handling Answer from $senderId")
        val peerConnection = peerConnections[senderId] ?: return
        val sessionDescription = SessionDescription(SessionDescription.Type.ANSWER, sdp)
        peerConnection.setRemoteDescription(object : SdpObserverAdapter() {
            override fun onSetSuccess() {
                drainPendingIceCandidates(senderId, peerConnection)
            }
        }, sessionDescription)
    }

    fun handleIceCandidateReceived(senderId: String, sdp: String, sdpMid: String, sdpMLineIndex: Int) {
        val candidate = IceCandidate(sdpMid, sdpMLineIndex, sdp)
        val peerConnection = peerConnections[senderId]
        if (peerConnection != null && peerConnection.remoteDescription != null) {
            peerConnection.addIceCandidate(candidate)
        } else {
            Log.d(TAG, "Queueing ICE candidate for $senderId before remote description is set")
            pendingIceCandidates.getOrPut(senderId) { mutableListOf() }.add(candidate)
        }
    }

    fun handleUserLeft(senderId: String) {
        removePeerConnection(senderId)
    }

    private fun removePeerConnection(userId: String) {
        val pc = peerConnections.remove(userId)
        try { pc?.close() } catch (_: Exception) {}
        val track = remoteAudioTracks.remove(userId)
        try { track?.setEnabled(false) } catch (_: Exception) {}
        pendingIceCandidates.remove(userId)
        listener.onRemoteAudioTrackRemoved(userId)
    }

    fun disconnect() {
        try {
            audioManager?.let { am ->
                am.mode = AudioManager.MODE_NORMAL
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    am.clearCommunicationDevice()
                } else {
                    @Suppress("DEPRECATION")
                    am.isSpeakerphoneOn = false
                }
            }
        } catch (_: Exception) {}

        peerConnections.forEach { (_, pc) -> 
            try { pc.close() } catch (_: Exception) {}
        }
        peerConnections.clear()
        
        remoteAudioTracks.forEach { (_, track) ->
            try { track.setEnabled(false) } catch (_: Exception) {}
        }
        remoteAudioTracks.clear()
        pendingIceCandidates.clear()
        
        try { localAudioSource?.dispose() } catch (_: Exception) {}
        localAudioSource = null
        
        try { localAudioTrack?.dispose() } catch (_: Exception) {}
        localAudioTrack = null
        
        try { audioDeviceModule?.release() } catch (_: Exception) {}
        audioDeviceModule = null

        try { peerConnectionFactory?.dispose() } catch (_: Exception) {}
        peerConnectionFactory = null
        
        try { rootEglBase?.release() } catch (_: Exception) {}
        rootEglBase = null
    }

    open class SdpObserverAdapter : SdpObserver {
        override fun onCreateSuccess(p0: SessionDescription?) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(p0: String?) { Log.e("WebRtcClient", "SdpObserver Create Failure: $p0") }
        override fun onSetFailure(p0: String?) { Log.e("WebRtcClient", "SdpObserver Set Failure: $p0") }
    }
}
