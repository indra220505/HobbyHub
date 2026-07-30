package com.hobbyhub.domain.community

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "communities")
data class CommunityEntity(
    @Id
    val id: String,

    @Column(nullable = false)
    var name: String,

    @Column(nullable = false)
    var slug: String,

    @Column(nullable = false)
    var category: String,

    @Column(nullable = false)
    var memberCount: Int = 1,

    @Column(columnDefinition = "TEXT")
    var description: String = "",

    var iconEmoji: String = "🌐",

    var iconUrl: String? = null,

    var creatorUsername: String = "",

    @Column(nullable = false)
    var isPublic: Boolean = true,

    @Column(columnDefinition = "TEXT")
    var channelsJson: String = "[]",

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
