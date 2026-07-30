package com.hobbyhub.websocket

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

data class WsChatMessage(
    val id: String = "msg_${System.currentTimeMillis()}",
    val channelName: String = "general",
    val senderName: String = "User",
    val senderAvatar: String = "U",
    val senderBadge: String = "Member",
    val content: String = "",
    val timestamp: String = "Baru saja"
)

@Component
class ChatHandler(
    private val objectMapper: ObjectMapper
) : TextWebSocketHandler() {

    private val log = LoggerFactory.getLogger(ChatHandler::class.java)

    // ChannelName -> List of WebSocketSession
    private val channelSessions = ConcurrentHashMap<String, CopyOnWriteArrayList<WebSocketSession>>()
    
    // SessionID -> ChannelName
    private val sessionChannelMap = ConcurrentHashMap<String, String>()

    // In-Memory Chat History per Channel (up to 100 messages per channel)
    private val channelHistory = ConcurrentHashMap<String, CopyOnWriteArrayList<WsChatMessage>>()

    fun getHistory(channelName: String): List<WsChatMessage> {
        return channelHistory[channelName] ?: emptyList()
    }

    override fun afterConnectionEstablished(session: WebSocketSession) {
        log.info("New Chat WebSocket connection established: ${session.id}")
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        try {
            val payload = message.payload
            val chatMsg = objectMapper.readValue(payload, WsChatMessage::class.java)

            // Register session to channel
            sessionChannelMap[session.id] = chatMsg.channelName
            val sessions = channelSessions.computeIfAbsent(chatMsg.channelName) { CopyOnWriteArrayList() }
            if (!sessions.contains(session)) {
                sessions.add(session)
            }

            // Save to history (max 100)
            val history = channelHistory.computeIfAbsent(chatMsg.channelName) { CopyOnWriteArrayList() }
            history.add(chatMsg)
            if (history.size > 100) {
                history.removeAt(0)
            }

            log.info("Chat message received for #${chatMsg.channelName} from ${chatMsg.senderName}: ${chatMsg.content}")

            // Broadcast to all connected clients in channel
            val jsonText = objectMapper.writeValueAsString(chatMsg)
            val textMsg = TextMessage(jsonText)

            sessions.forEach { s ->
                if (s.isOpen) {
                    try {
                        s.sendMessage(textMsg)
                    } catch (e: Exception) {
                        log.error("Failed to send WebSocket message to ${s.id}", e)
                    }
                }
            }
        } catch (e: Exception) {
            log.error("Error handling Chat WebSocket message", e)
        }
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        val channel = sessionChannelMap.remove(session.id)
        if (channel != null) {
            channelSessions[channel]?.remove(session)
            log.info("Chat WebSocket connection closed for session ${session.id} in channel $channel")
        }
    }
}
