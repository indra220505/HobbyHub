package com.hobbyhub.domain.chat

import com.hobbyhub.websocket.ChatHandler
import com.hobbyhub.websocket.WsChatMessage
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/chat")
class ChatController(
    private val chatHandler: ChatHandler
) {

    @GetMapping("/history/{channelName}")
    fun getHistory(@PathVariable channelName: String): ResponseEntity<List<WsChatMessage>> {
        val history = chatHandler.getHistory(channelName)
        return ResponseEntity.ok(history)
    }

    @DeleteMapping("/history/{channelName}/{messageId}")
    fun deleteMessage(
        @PathVariable channelName: String,
        @PathVariable messageId: String
    ): ResponseEntity<Map<String, String>> {
        val success = chatHandler.deleteMessage(channelName, messageId)
        return if (success) {
            ResponseEntity.ok(mapOf("status" to "DELETED", "id" to messageId, "channel" to channelName))
        } else {
            ResponseEntity.ok(mapOf("status" to "NOT_FOUND", "id" to messageId))
        }
    }
}
