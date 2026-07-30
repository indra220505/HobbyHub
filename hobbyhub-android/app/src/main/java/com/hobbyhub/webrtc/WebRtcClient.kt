package com.hobbyhub.webrtc

import android.content.Context
import android.media.AudioManager
import android.util.Log
import org.webrtc.*

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
    private var localAudioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null
    private var rootEglBase: EglBase? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    // Map of remoteUserId -> PeerConnection
    private val peerConnections = mutableMapOf<String, PeerConnection>()

    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun3.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun4.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun.services.mozilla.com").createIceServer()
    )

    init {
        setupAudioManager()
        initWebRtc()
    }

    private fun setupAudioManager() {
        try {
            audioManager?.let { am ->
                am.mode = AudioManager.MODE_IN_COMMUNICATION
                am.isSpeakerphoneOn = true
                am.isMicrophoneMute = false
                Log.d(TAG, "AudioManager configured: MODE_IN_COMMUNICATION, Speakerphone ON")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up AudioManager", e)
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

            peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(options)
                .createPeerConnectionFactory()

            createLocalAudioTrack()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing WebRTC PeerConnectionFactory", e)
        }
    }

    private fun createLocalAudioTrack() {
        try {
            val factory = peerConnectionFactory ?: return
            
            // Audio constraints for crystal clear voice chat
            val audioConstraints = MediaConstraints()
            audioConstraints.mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
            audioConstraints.mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
            audioConstraints.mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
            audioConstraints.mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))

            localAudioSource = factory.createAudioSource(audioConstraints)
            localAudioTrack = factory.createAudioTrack("local_audio_track_$userId", localAudioSource)
            localAudioTrack?.setEnabled(true)
            localAudioTrack?.setVolume(1.0)
            Log.d(TAG, "Local Audio Track created successfully for user $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating Local Audio Track (Check Mic Permission)", e)
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
                    listener.onRemoteAudioTrackAdded(targetUserId, track)
                }
            }

            override fun onRemoveStream(stream: MediaStream) {
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

    fun handleUserJoined(targetUserId: String) {
        // PERFECT NEGOTIATOR / POLITENESS PATTERN:
        // Avoid WebRTC Glare by having ONLY the peer with lexicographically smaller ID create the OFFER!
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
                    peerConnection.setLocalDescription(SdpObserverAdapter(), it)
                    signalingClient.sendOffer(targetUserId, it.description)
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
                val constraints = MediaConstraints()
                constraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))

                peerConnection.createAnswer(object : SdpObserverAdapter() {
                    override fun onCreateSuccess(answerSdp: SessionDescription?) {
                        answerSdp?.let {
                            peerConnection.setLocalDescription(SdpObserverAdapter(), it)
                            signalingClient.sendAnswer(senderId, it.description)
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
        peerConnection.setRemoteDescription(SdpObserverAdapter(), sessionDescription)
    }

    fun handleIceCandidateReceived(senderId: String, sdp: String, sdpMid: String, sdpMLineIndex: Int) {
        val peerConnection = peerConnections[senderId] ?: return
        val candidate = IceCandidate(sdpMid, sdpMLineIndex, sdp)
        peerConnection.addIceCandidate(candidate)
    }

    fun handleUserLeft(senderId: String) {
        removePeerConnection(senderId)
    }

    private fun removePeerConnection(userId: String) {
        val pc = peerConnections.remove(userId)
        pc?.close()
        listener.onRemoteAudioTrackRemoved(userId)
    }

    fun disconnect() {
        try {
            audioManager?.let { am ->
                am.mode = AudioManager.MODE_NORMAL
                am.isSpeakerphoneOn = false
            }
        } catch (_: Exception) {}

        peerConnections.forEach { (_, pc) -> 
            try { pc.close() } catch (_: Exception) {}
        }
        peerConnections.clear()
        
        try { localAudioSource?.dispose() } catch (_: Exception) {}
        localAudioSource = null
        
        try { localAudioTrack?.dispose() } catch (_: Exception) {}
        localAudioTrack = null
        
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
