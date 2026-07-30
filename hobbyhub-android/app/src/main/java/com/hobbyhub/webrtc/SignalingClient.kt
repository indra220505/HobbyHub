package com.hobbyhub.webrtc

import android.util.Log
import com.google.gson.Gson
import com.hobbyhub.BuildConfig
import okhttp3.*
import okio.ByteString

data class SignalingMessage(
    val type: String, // "JOIN", "LEAVE", "OFFER", "ANSWER", "CANDIDATE"
    val senderId: String,
    val targetId: String? = null,
    val roomId: String,
    val payload: Any? = null
)

interface SignalingListener {
    fun onConnectionEstablished()
    fun onOfferReceived(senderId: String, sdp: String)
    fun onAnswerReceived(senderId: String, sdp: String)
    fun onIceCandidateReceived(senderId: String, candidate: String, sdpMid: String, sdpMLineIndex: Int)
    fun onUserJoined(senderId: String)
    fun onUserLeft(senderId: String)
}

class SignalingClient(
    private val userId: String,
    private val roomId: String,
    private val listener: SignalingListener,
    private val gson: Gson = Gson()
) {
    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null
    private val TAG = "SignalingClient"

    fun connect() {
        val request = Request.Builder()
            .url(BuildConfig.WS_BASE_URL + "signaling")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket Opened to Signaling server")
                listener.onConnectionEstablished()
                
                // Send JOIN message
                val joinMsg = SignalingMessage(
                    type = "JOIN",
                    senderId = userId,
                    roomId = roomId
                )
                webSocket.send(gson.toJson(joinMsg))
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "Received signaling message: $text")
                try {
                    val msg = gson.fromJson(text, SignalingMessage::class.java)
                    when (msg.type) {
                        "JOIN" -> listener.onUserJoined(msg.senderId)
                        "LEAVE" -> listener.onUserLeft(msg.senderId)
                        "OFFER" -> {
                            val sdp = msg.payload as? String ?: return
                            listener.onOfferReceived(msg.senderId, sdp)
                        }
                        "ANSWER" -> {
                            val sdp = msg.payload as? String ?: return
                            listener.onAnswerReceived(msg.senderId, sdp)
                        }
                        "CANDIDATE" -> {
                            val payloadMap = msg.payload as? Map<*, *> ?: return
                            val sdp = payloadMap["sdp"] as? String ?: return
                            val sdpMid = payloadMap["sdpMid"] as? String ?: ""
                            // CRITICAL FIX: Use Number instead of Double so Int/Long/Double from Gson deserialization work!
                            val sdpMLineIndex = (payloadMap["sdpMLineIndex"] as? Number)?.toInt() ?: 0
                            listener.onIceCandidateReceived(msg.senderId, sdp, sdpMid, sdpMLineIndex)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing signaling message", e)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket Closing: $reason")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket Failure: ${t.message}", t)
            }
        })
    }

    fun sendOffer(targetId: String, sdp: String) {
        val msg = SignalingMessage(
            type = "OFFER",
            senderId = userId,
            targetId = targetId,
            roomId = roomId,
            payload = sdp
        )
        webSocket?.send(gson.toJson(msg))
    }

    fun sendAnswer(targetId: String, sdp: String) {
        val msg = SignalingMessage(
            type = "ANSWER",
            senderId = userId,
            targetId = targetId,
            roomId = roomId,
            payload = sdp
        )
        webSocket?.send(gson.toJson(msg))
    }

    fun sendIceCandidate(targetId: String, sdp: String, sdpMid: String, sdpMLineIndex: Int) {
        val payload = mapOf(
            "sdp" to sdp,
            "sdpMid" to sdpMid,
            "sdpMLineIndex" to sdpMLineIndex
        )
        val msg = SignalingMessage(
            type = "CANDIDATE",
            senderId = userId,
            targetId = targetId,
            roomId = roomId,
            payload = payload
        )
        webSocket?.send(gson.toJson(msg))
    }

    fun disconnect() {
        try {
            val msg = SignalingMessage(
                type = "LEAVE",
                senderId = userId,
                roomId = roomId
            )
            webSocket?.send(gson.toJson(msg))
            webSocket?.close(1000, "User disconnected")
        } catch (_: Exception) {}
        webSocket = null
    }
}
