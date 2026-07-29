package com.hobbyhub.config

import com.hobbyhub.websocket.SignalingHandler
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

@Configuration
@EnableWebSocket
class WebSocketConfig(
    private val signalingHandler: SignalingHandler
) : WebSocketConfigurer {

    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(signalingHandler, "/signaling")
            .setAllowedOrigins("*") // Allow all origins for local physical testing
    }
}
