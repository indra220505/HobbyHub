package com.hobbyhub.data.local

import android.content.Context
import android.content.SharedPreferences
import com.hobbyhub.model.ChatMessage
import com.hobbyhub.model.RoleBadge
import com.hobbyhub.repository.MockDataRepository
import org.json.JSONArray
import org.json.JSONObject

class ChatLocalDatabaseManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("hobbyhub_chat_db", Context.MODE_PRIVATE)

    fun getMessagesForChannel(channelName: String): List<ChatMessage> {
        val jsonString = prefs.getString("chat_channel_$channelName", null)
        if (jsonString == null) {
            val initialMessages = MockDataRepository.sampleChatMessages
            saveMessagesForChannel(channelName, initialMessages)
            return initialMessages
        }
        val messages = mutableListOf<ChatMessage>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val badgeObj = obj.optJSONObject("badge")
                val badge = if (badgeObj != null) {
                    RoleBadge(badgeObj.getString("name"), badgeObj.getString("color"))
                } else null

                messages.add(
                    ChatMessage(
                        id = obj.getString("id"),
                        senderName = obj.getString("senderName"),
                        senderAvatar = obj.optString("senderAvatar", "U"),
                        senderBadge = badge,
                        content = obj.getString("content"),
                        codeSnippet = obj.optString("codeSnippet", null).takeIf { !it.isNull_or_empty() },
                        timestamp = obj.getString("timestamp"),
                        isPinned = obj.optBoolean("isPinned", false),
                        reactionsCount = obj.optInt("reactionsCount", 0)
                    )
                )
            }
        } catch (e: Exception) {
            return MockDataRepository.sampleChatMessages
        }
        return messages
    }

    fun saveMessageToChannel(channelName: String, newMessage: ChatMessage) {
        val currentMessages = getMessagesForChannel(channelName).toMutableList()
        if (currentMessages.none { it.id == newMessage.id }) {
            currentMessages.add(newMessage)
            saveMessagesForChannel(channelName, currentMessages)
        }
    }

    fun deleteMessageFromChannel(channelName: String, messageId: String) {
        val currentMessages = getMessagesForChannel(channelName).toMutableList()
        val removed = currentMessages.removeAll { it.id == messageId }
        if (removed) {
            saveMessagesForChannel(channelName, currentMessages)
        }
    }

    private fun saveMessagesForChannel(channelName: String, messages: List<ChatMessage>) {
        val array = JSONArray()
        for (msg in messages) {
            val obj = JSONObject().apply {
                put("id", msg.id)
                put("senderName", msg.senderName)
                put("senderAvatar", msg.senderAvatar)
                put("content", msg.content)
                put("codeSnippet", msg.codeSnippet ?: "")
                put("timestamp", msg.timestamp)
                put("isPinned", msg.isPinned)
                put("reactionsCount", msg.reactionsCount)
                msg.senderBadge?.let { badge ->
                    val bObj = JSONObject().apply {
                        put("name", badge.name)
                        put("color", badge.colorHex)
                    }
                    put("badge", bObj)
                }
            }
            array.put(obj)
        }
        prefs.edit().putString("chat_channel_$channelName", array.toString()).apply()
    }

    fun resetAllChatData() {
        prefs.edit().clear().apply()
    }

    private fun String?.isNull_or_empty(): Boolean = this == null || this.trim().isEmpty()
}
