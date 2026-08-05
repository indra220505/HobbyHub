package com.hobbyhub.domain.post

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "posts")
data class PostEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    val id: UUID? = null,

    @Column(name = "community_id", nullable = false)
    val communityId: String,

    @Column(name = "author_id", nullable = false, columnDefinition = "uuid")
    val authorId: UUID,

    @Column(nullable = false)
    val title: String,

    @Column(columnDefinition = "TEXT")
    val content: String? = null,

    @Column(nullable = false)
    val type: String = "TEXT",

    @Column(name = "media_urls", columnDefinition = "TEXT")
    val mediaUrls: String = "[]",

    @Column(name = "upvote_count", nullable = false)
    var upvoteCount: Int = 0,

    @Column(name = "downvote_count", nullable = false)
    var downvoteCount: Int = 0,

    @Column(name = "comment_count", nullable = false)
    var commentCount: Int = 0,

    @Column(name = "is_pinned", nullable = false)
    var isPinned: Boolean = false,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
