package com.hobbyhub.websocket

/**
 * Represents a signaling message exchanged during WebRTC session establishment.
 * @param type "JOIN", "LEAVE", "OFFER", "ANSWER", "CANDIDATE"
 * @param senderId The ID of the user sending the message
 * @param targetId The ID of the specific user the message is for (e.g., in P2P negotiation), null for broadcasts
 * @param roomId The room/community ID
 * @param payload The SDP offer/answer or ICE candidate string/object (JSON stringified usually)
 * @param senderName The display name of the sender user
 */
data class SignalingMessage(
    val type: String,
    val senderId: String,
    val targetId: String? = null,
    val roomId: String,
    val payload: Any? = null,
    val senderName: String? = null
)
