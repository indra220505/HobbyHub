package com.hobbyhub.config

import com.hobbyhub.websocket.ChatHandler
import com.hobbyhub.websocket.SignalingHandler
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

@Configuration
@EnableWebSocket
class WebSocketConfig(
    private val signalingHandler: SignalingHandler,
    private val chatHandler: ChatHandler
) : WebSocketConfigurer {

    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(signalingHandler, "/signaling")
            .setAllowedOrigins("*")
        registry.addHandler(chatHandler, "/chat")
            .setAllowedOrigins("*")
    }
}
