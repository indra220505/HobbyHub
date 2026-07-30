package com.hobbyhub.model

data class User(
    val id: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String,
    val bio: String,
    val level: Int,
    val currentXp: Long,
    val maxXp: Long,
    val reputation: Int,
    val roleBadge: RoleBadge?,
    val badges: List<Badge>
)

data class RoleBadge(
    val name: String,
    val colorHex: String
)

data class Badge(
    val id: String,
    val name: String,
    val description: String,
    val iconEmoji: String,
    val category: String
)

data class Community(
    val id: String,
    val name: String,
    val slug: String,
    val category: String,
    val memberCount: String,
    val description: String,
    val iconEmoji: String,
    val channels: List<Channel>
)

data class Channel(
    val id: String,
    val name: String,
    val type: ChannelType, // CHAT, FEED, VOICE, ANNOUNCEMENT
    val topic: String
)

enum class ChannelType {
    TEXT_CHAT, FEED_DISCUSSION, VOICE, ANNOUNCEMENT
}

data class ChatMessage(
    val id: String,
    val senderId: String = "",
    val senderUsername: String = "",
    val senderName: String,
    val senderAvatar: String,
    val senderBadge: RoleBadge?,
    val content: String,
    val codeSnippet: String? = null,
    val timestamp: String,
    val isPinned: Boolean = false,
    val reactionsCount: Int = 0
)

data class Post(
    val id: String,
    val authorName: String,
    val authorBadge: RoleBadge?,
    val communityName: String,
    val title: String,
    val content: String,
    val upvotes: Int,
    val commentsCount: Int,
    val timestamp: String,
    val tags: List<String>
)

data class Quest(
    val id: String,
    val title: String,
    val rewardXp: Int,
    val progress: Float,
    val isCompleted: Boolean
)
