package com.hobbyhub.domain.post

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "comments")
data class CommentEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    val id: UUID? = null,

    @Column(name = "post_id", nullable = false, columnDefinition = "uuid")
    val postId: UUID,

    @Column(name = "parent_comment_id", columnDefinition = "uuid")
    val parentCommentId: UUID? = null,

    @Column(name = "author_id", nullable = false, columnDefinition = "uuid")
    val authorId: UUID,

    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String,

    @Column(name = "upvote_count", nullable = false)
    var upvoteCount: Int = 0,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
