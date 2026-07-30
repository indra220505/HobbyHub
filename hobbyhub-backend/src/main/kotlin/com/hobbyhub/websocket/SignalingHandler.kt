package com.hobbyhub.websocket

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.util.concurrent.ConcurrentHashMap

@Component
class SignalingHandler(
    private val objectMapper: ObjectMapper
) : TextWebSocketHandler() {

    private val log = LoggerFactory.getLogger(SignalingHandler::class.java)

    // RoomID -> (UserId -> WebSocketSession)
    private val rooms = ConcurrentHashMap<String, ConcurrentHashMap<String, WebSocketSession>>()

    // SessionID -> Pair<RoomID, UserId> (for easy cleanup on disconnect)
    private val sessionInfo = ConcurrentHashMap<String, Pair<String, String>>()

    override fun afterConnectionEstablished(session: WebSocketSession) {
        log.info("New WebSocket connection established: ${session.id}")
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        try {
            val sigMsg = objectMapper.readValue(message.payload, SignalingMessage::class.java)
            
            when (sigMsg.type) {
                "JOIN" -> handleJoin(session, sigMsg)
                "LEAVE" -> handleLeave(session, sigMsg)
                "OFFER", "ANSWER", "CANDIDATE" -> routeMessage(session, sigMsg)
                else -> log.warn("Unknown signaling message type: ${sigMsg.type}")
            }
        } catch (e: Exception) {
            log.error("Error handling signaling message", e)
        }
    }

    private fun handleJoin(session: WebSocketSession, msg: SignalingMessage) {
        val room = rooms.computeIfAbsent(msg.roomId) { ConcurrentHashMap() }
        
        // 1. Send existing users in the room to the joining user first
        room.forEach { (existingUserId, existingSession) ->
            if (existingUserId != msg.senderId && existingSession.isOpen) {
                val existingUserMsg = SignalingMessage(
                    type = "JOIN",
                    senderId = existingUserId,
                    roomId = msg.roomId
                )
                try {
                    session.sendMessage(TextMessage(objectMapper.writeValueAsString(existingUserMsg)))
                } catch (e: Exception) {
                    log.error("Failed to send existing user $existingUserId to ${msg.senderId}", e)
                }
            }
        }

        room[msg.senderId] = session
        sessionInfo[session.id] = Pair(msg.roomId, msg.senderId)
        
        log.info("User ${msg.senderId} joined room ${msg.roomId}")

        // 2. Broadcast to others that a user joined
        broadcastToRoom(msg.roomId, msg, excludeUserId = msg.senderId)
    }

    private fun handleLeave(session: WebSocketSession, msg: SignalingMessage) {
        val room = rooms[msg.roomId]
        room?.remove(msg.senderId)
        sessionInfo.remove(session.id)
        
        log.info("User ${msg.senderId} left room ${msg.roomId}")

        // Broadcast to others that a user left
        broadcastToRoom(msg.roomId, msg, excludeUserId = msg.senderId)
    }

    private fun routeMessage(session: WebSocketSession, msg: SignalingMessage) {
        val room = rooms[msg.roomId]
        if (room == null) {
            log.warn("Attempt to route message in unknown room: ${msg.roomId}")
            return
        }

        if (msg.targetId != null) {
            // Direct P2P routing (e.g. OFFER to specific user)
            val targetSession = room[msg.targetId]
            if (targetSession != null && targetSession.isOpen) {
                val payloadStr = objectMapper.writeValueAsString(msg)
                targetSession.sendMessage(TextMessage(payloadStr))
            } else {
                log.warn("Target user ${msg.targetId} not found or disconnected in room ${msg.roomId}")
            }
        } else {
            // Broadcast to room
            broadcastToRoom(msg.roomId, msg, excludeUserId = msg.senderId)
        }
    }

    private fun broadcastToRoom(roomId: String, msg: SignalingMessage, excludeUserId: String?) {
        val room = rooms[roomId] ?: return
        val payloadStr = objectMapper.writeValueAsString(msg)
        val textMessage = TextMessage(payloadStr)

        room.forEach { (userId, session) ->
            if (userId != excludeUserId && session.isOpen) {
                try {
                    session.sendMessage(textMessage)
                } catch (e: Exception) {
                    log.error("Failed to broadcast to $userId", e)
                }
            }
        }
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        val info = sessionInfo.remove(session.id)
        if (info != null) {
            val (roomId, userId) = info
            rooms[roomId]?.remove(userId)
            log.info("Connection closed for user $userId in room $roomId. Status: $status")

            // Notify others
            val leaveMsg = SignalingMessage(type = "LEAVE", senderId = userId, roomId = roomId)
            broadcastToRoom(roomId, leaveMsg, excludeUserId = userId)
            
            // Cleanup empty rooms
            if (rooms[roomId]?.isEmpty() == true) {
                rooms.remove(roomId)
            }
        }
    }
}
