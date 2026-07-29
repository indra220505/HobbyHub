package com.hobbyhub.webrtc

import android.content.Context
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

    // Map of remoteUserId -> PeerConnection
    private val peerConnections = mutableMapOf<String, PeerConnection>()

    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
    )

    init {
        initWebRtc()
    }

    private fun initWebRtc() {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(true)
                .createInitializationOptions()
        )

        rootEglBase = EglBase.create()
        val options = PeerConnectionFactory.Options()

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(options)
            .createPeerConnectionFactory()

        createLocalAudioTrack()
    }

    private fun createLocalAudioTrack() {
        val factory = peerConnectionFactory ?: return
        
        // Audio constraints
        val audioConstraints = MediaConstraints()
        audioConstraints.mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
        audioConstraints.mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
        audioConstraints.mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))

        localAudioSource = factory.createAudioSource(audioConstraints)
        localAudioTrack = factory.createAudioTrack("local_audio_track", localAudioSource)
        localAudioTrack?.setEnabled(true)
    }

    fun setMicrophoneMute(mute: Boolean) {
        localAudioTrack?.setEnabled(!mute)
    }

    private fun getOrCreatePeerConnection(targetUserId: String): PeerConnection? {
        if (peerConnections.containsKey(targetUserId)) {
            return peerConnections[targetUserId]
        }

        val factory = peerConnectionFactory ?: return null
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers)
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN

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
                // Deprecated in Unified Plan, but keeping for safety if fallback happens
                if (stream.audioTracks.isNotEmpty()) {
                    listener.onRemoteAudioTrackAdded(targetUserId, stream.audioTracks[0])
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
        val peerConnection = getOrCreatePeerConnection(targetUserId) ?: return

        // Create Offer
        peerConnection.createOffer(object : SdpObserverAdapter() {
            override fun onCreateSuccess(sessionDescription: SessionDescription?) {
                sessionDescription?.let {
                    peerConnection.setLocalDescription(SdpObserverAdapter(), it)
                    signalingClient.sendOffer(targetUserId, it.description)
                }
            }
        }, MediaConstraints())
    }

    fun handleOfferReceived(senderId: String, sdp: String) {
        val peerConnection = getOrCreatePeerConnection(senderId) ?: return
        val sessionDescription = SessionDescription(SessionDescription.Type.OFFER, sdp)
        
        peerConnection.setRemoteDescription(object : SdpObserverAdapter() {
            override fun onSetSuccess() {
                // Create Answer
                peerConnection.createAnswer(object : SdpObserverAdapter() {
                    override fun onCreateSuccess(answerSdp: SessionDescription?) {
                        answerSdp?.let {
                            peerConnection.setLocalDescription(SdpObserverAdapter(), it)
                            signalingClient.sendAnswer(senderId, it.description)
                        }
                    }
                }, MediaConstraints())
            }
        }, sessionDescription)
    }

    fun handleAnswerReceived(senderId: String, sdp: String) {
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
        peerConnections.forEach { (_, pc) -> pc.close() }
        peerConnections.clear()
        
        localAudioSource?.dispose()
        localAudioSource = null
        
        localAudioTrack?.dispose()
        localAudioTrack = null
        
        peerConnectionFactory?.dispose()
        peerConnectionFactory = null
        
        rootEglBase?.release()
        rootEglBase = null
    }

    // Helper adapter to avoid implementing all methods
    open class SdpObserverAdapter : SdpObserver {
        override fun onCreateSuccess(p0: SessionDescription?) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(p0: String?) { Log.e("WebRtcClient", "SdpObserver Create Failure: $p0") }
        override fun onSetFailure(p0: String?) { Log.e("WebRtcClient", "SdpObserver Set Failure: $p0") }
    }
}
