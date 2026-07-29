package com.hobbyhub.data.local

import android.content.Context
import android.content.SharedPreferences
import com.hobbyhub.model.Channel
import com.hobbyhub.model.ChannelType
import com.hobbyhub.model.Community
import org.json.JSONArray
import org.json.JSONObject

data class CommunityRoleItem(
    val id: String,
    var name: String,
    var colorHex: String,
    var iconEmoji: String,
    var permissionBitmask: Long = 0xFFFFFF
)

data class AuditLogEntry(
    val id: String,
    val actorName: String,
    val actionType: String,
    val details: String,
    val timestamp: String
)

class CommunityRegistryManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("hobbyhub_communities_db_v3", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_COMMUNITIES_JSON = "custom_communities_json_v3"
        val SLOW_MODE_STEPS = listOf("0 detik", "5 detik", "10 detik", "30 detik", "1 menit", "2 menit", "5 menit", "10 menit", "30 menit", "1 jam")
    }

    fun getAllCommunities(): List<Community> {
        val jsonString = prefs.getString(KEY_COMMUNITIES_JSON, null)
        val defaultList = defaultInitialCommunities()
        if (jsonString == null) {
            saveCommunities(defaultList)
            return defaultList
        }
        val customList = mutableListOf<Community>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val channelsArray = obj.getJSONArray("channels")
                val channels = mutableListOf<Channel>()
                for (j in 0 until channelsArray.length()) {
                    val cObj = channelsArray.getJSONObject(j)
                    channels.add(
                        Channel(
                            id = cObj.getString("id"),
                            name = cObj.getString("name"),
                            type = ChannelType.valueOf(cObj.getString("type")),
                            topic = cObj.getString("topic")
                        )
                    )
                }

                customList.add(
                    Community(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        slug = obj.getString("slug"),
                        category = obj.getString("category"),
                        memberCount = obj.getString("memberCount"),
                        description = obj.getString("description"),
                        iconEmoji = obj.getString("iconEmoji"),
                        channels = channels
                    )
                )
            }
        } catch (e: Exception) {
            return defaultList
        }
        return customList
    }

    fun getCommunityById(id: String): Community? {
        return getAllCommunities().find { it.id == id }
    }

    fun getCommunitiesCreatedBy(username: String): List<Community> {
        return getAllCommunities().filter { comm ->
            isOwnerOfCommunity(comm.id, username)
        }
    }

    fun createCommunity(
        creatorUsername: String,
        name: String,
        category: String,
        description: String,
        iconEmoji: String,
        isPublic: Boolean
    ): Community {
        val currentList = getAllCommunities().toMutableList()
        val slug = name.lowercase().replace(" ", "-")
        val newComm = Community(
            id = "comm_${creatorUsername}_${System.currentTimeMillis()}",
            name = name + if (!isPublic) " 🔒" else "",
            slug = slug,
            category = category,
            memberCount = "1 Member (Owner)",
            description = "$description (Pendiri: @$creatorUsername)" + if (!isPublic) " [Privat]" else " [Publik]",
            iconEmoji = if (iconEmoji.isBlank()) "🌐" else iconEmoji,
            channels = listOf(
                Channel("ch_gen_${System.currentTimeMillis()}", "general", ChannelType.TEXT_CHAT, "Channel diskusi umum"),
                Channel("ch_qna_${System.currentTimeMillis()}", "tanya-jawab", ChannelType.TEXT_CHAT, "Channel pertanyakan & solusi"),
                Channel("ch_voice_${System.currentTimeMillis()}", "🔊 Voice Lounge", ChannelType.VOICE, "Voice room komunitas")
            )
        )
        currentList.add(0, newComm)
        saveCommunities(currentList)
        return newComm
    }

    fun isOwnerOfCommunity(communityId: String, username: String): Boolean {
        if (username.equals("indra_owner", ignoreCase = true)) return true // Super Owner Rights
        val comm = getCommunityById(communityId) ?: return false
        return comm.id.contains("comm_${username}") || comm.description.contains("Pendiri: @$username") || comm.description.contains("Pendiri: $username")
    }

    fun updateCommunitySettings(
        communityId: String,
        name: String,
        description: String,
        category: String,
        iconEmoji: String
    ): List<Community> {
        val list = getAllCommunities().toMutableList()
        val index = list.indexOfFirst { it.id == communityId }
        if (index != -1) {
            val comm = list[index]
            list[index] = comm.copy(
                name = name,
                description = description,
                category = category,
                iconEmoji = iconEmoji
            )
            saveCommunities(list)
        }
        return list
    }

    fun transferOwnership(communityId: String, oldOwnerUsername: String, newOwnerUsername: String): List<Community> {
        val list = getAllCommunities().toMutableList()
        val index = list.indexOfFirst { it.id == communityId }
        if (index != -1) {
            val comm = list[index]
            val updatedDesc = comm.description
                .replace("Pendiri: @$oldOwnerUsername", "Pendiri: @$newOwnerUsername")
                .replace("Pendiri: $oldOwnerUsername", "Pendiri: @$newOwnerUsername")

            val newId = "comm_${newOwnerUsername}_${System.currentTimeMillis()}"
            list[index] = comm.copy(id = newId, description = updatedDesc)
            saveCommunities(list)
            addAuditLog(communityId, oldOwnerUsername, "TRANSFER_OWNERSHIP", "Transfer kepemilikan kepada @$newOwnerUsername")
        }
        return list
    }

    fun deleteCommunity(communityId: String): List<Community> {
        val list = getAllCommunities().filterNot { it.id == communityId }
        saveCommunities(list)
        return list
    }

    fun resetAllCommunityData() {
        prefs.edit().clear().apply()
        // Re-initialize with default communities owned by Super Owner
        saveCommunities(defaultInitialCommunities())
    }

    // MODERATION & ROLE DATA STORE
    fun getBannedKeywords(communityId: String): MutableList<String> {
        val json = prefs.getString("banned_keywords_$communityId", null) ?: return mutableListOf("spam", "judionline", "phishing")
        val list = mutableListOf<String>()
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) list.add(arr.getString(i))
        return list
    }

    fun saveBannedKeywords(communityId: String, keywords: List<String>) {
        val arr = JSONArray()
        keywords.forEach { arr.put(it) }
        prefs.edit().putString("banned_keywords_$communityId", arr.toString()).apply()
    }

    fun getAuditLogs(communityId: String): List<AuditLogEntry> {
        val json = prefs.getString("audit_logs_$communityId", null) ?: return defaultAuditLogs()
        val list = mutableListOf<AuditLogEntry>()
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                AuditLogEntry(
                    id = obj.getString("id"),
                    actorName = obj.getString("actorName"),
                    actionType = obj.getString("actionType"),
                    details = obj.getString("details"),
                    timestamp = obj.getString("timestamp")
                )
            )
        }
        return list
    }

    fun addAuditLog(communityId: String, actorName: String, actionType: String, details: String) {
        val current = getAuditLogs(communityId).toMutableList()
        current.add(0, AuditLogEntry("log_${System.currentTimeMillis()}", actorName, actionType, details, "Baru saja"))
        val arr = JSONArray()
        current.forEach { log ->
            val obj = JSONObject().apply {
                put("id", log.id)
                put("actorName", log.actorName)
                put("actionType", log.actionType)
                put("details", log.details)
                put("timestamp", log.timestamp)
            }
            arr.put(obj)
        }
        prefs.edit().putString("audit_logs_$communityId", arr.toString()).apply()
    }

    fun addChannelToCommunity(communityId: String, channelName: String, channelType: ChannelType): List<Community> {
        val list = getAllCommunities().toMutableList()
        val index = list.indexOfFirst { it.id == communityId }
        if (index != -1) {
            val comm = list[index]
            val newChannel = Channel(
                id = "ch_${System.currentTimeMillis()}",
                name = channelName.lowercase().replace(" ", "-"),
                type = channelType,
                topic = "Channel ${channelType.name.lowercase()} baru"
            )
            val updatedChannels = comm.channels.toMutableList()
            updatedChannels.add(newChannel)
            list[index] = comm.copy(channels = updatedChannels)
            saveCommunities(list)
        }
        return list
    }

    fun deleteChannel(communityId: String, channelId: String): List<Community> {
        val list = getAllCommunities().toMutableList()
        val index = list.indexOfFirst { it.id == communityId }
        if (index != -1) {
            val comm = list[index]
            val updatedChannels = comm.channels.filterNot { it.id == channelId }
            list[index] = comm.copy(channels = updatedChannels)
            saveCommunities(list)
        }
        return list
    }

    private fun saveCommunities(communities: List<Community>) {
        val array = JSONArray()
        for (comm in communities) {
            val obj = JSONObject().apply {
                put("id", comm.id)
                put("name", comm.name)
                put("slug", comm.slug)
                put("category", comm.category)
                put("memberCount", comm.memberCount)
                put("description", comm.description)
                put("iconEmoji", comm.iconEmoji)
                val cArray = JSONArray()
                comm.channels.forEach { ch ->
                    val cObj = JSONObject().apply {
                        put("id", ch.id)
                        put("name", ch.name)
                        put("type", ch.type.name)
                        put("topic", ch.topic)
                    }
                    cArray.put(cObj)
                }
                put("channels", cArray)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_COMMUNITIES_JSON, array.toString()).apply()
    }

    private fun defaultAuditLogs(): List<AuditLogEntry> {
        return listOf(
            AuditLogEntry("al_1", "Indra Owner", "COMMUNITY_CREATED", "Komunitas berhasil dibuat", "1 hari lalu"),
            AuditLogEntry("al_2", "Indra Owner", "CHANNEL_CREATED", "Menambahkan channel #general", "1 hari lalu")
        )
    }

    private fun defaultInitialCommunities(): List<Community> {
        return listOf(
            Community(
                id = "comm_sys_ai",
                name = "AI Indonesia",
                slug = "ai-indonesia",
                category = "AI / ML",
                memberCount = "14.2k Member",
                description = "Pusat diskusi AI, Prompt Engineering, LLM Fine-tuning, & Computer Vision. (Pendiri: @indra_owner)",
                iconEmoji = "🤖",
                channels = listOf(
                    Channel("ch_ai_gen", "general-ai", ChannelType.TEXT_CHAT, "Diskusi seputar perkembangan AI terbaru"),
                    Channel("ch_ai_prompts", "prompt-crafting", ChannelType.TEXT_CHAT, "Berbagi prompt ChatGPT & Midjourney terbaik"),
                    Channel("ch_ai_voice", "🔊 AI Voice Lounge", ChannelType.VOICE, "Ngobrol santai seputar AI")
                )
            ),
            Community(
                id = "comm_sys_android",
                name = "Android Developer ID",
                slug = "android-devs",
                category = "Programming",
                memberCount = "8.9k Member",
                description = "Komunitas resmi developer Android. Kotlin, Jetpack Compose, & Clean Architecture. (Pendiri: @indra_owner)",
                iconEmoji = "📱",
                channels = listOf(
                    Channel("ch_and_gen", "general-android", ChannelType.TEXT_CHAT, "Diskusi umum Kotlin & Android SDK"),
                    Channel("ch_and_qna", "help-qna", ChannelType.TEXT_CHAT, "Tanyakan masalah bug / error kodinganmu di sini"),
                    Channel("ch_and_voice", "🔊 Dev Coffee Chat", ChannelType.VOICE, "Voice room santai antar developer")
                )
            ),
            Community(
                id = "comm_sys_valorant",
                name = "Valorant Indonesia Guild",
                slug = "valorant-id",
                category = "Gaming",
                memberCount = "23.4k Member",
                description = "Cari teman mabar competitive, turnamen guild mingguan, dan coaching gratis. (Pendiri: @indra_owner)",
                iconEmoji = "🎮",
                channels = listOf(
                    Channel("ch_val_gen", "general-lounge", ChannelType.TEXT_CHAT, "Diskusi update patch & agent Valorant"),
                    Channel("ch_val_lfg", "lfg-competitive", ChannelType.TEXT_CHAT, "Cari Party Mabar Platinum/Diamond/Immortal")
                )
            )
        )
    }
}
