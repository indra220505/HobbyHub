package com.hobbyhub.domain.post

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "post_votes", uniqueConstraints = [UniqueConstraint(columnNames = ["post_id", "user_id"])])
data class PostVoteEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    val id: UUID? = null,

    @Column(name = "post_id", nullable = false, columnDefinition = "uuid")
    val postId: UUID,

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    val userId: UUID,

    @Column(name = "vote_type", nullable = false)
    var voteType: String, // "UP" or "DOWN"

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)

@Entity
@Table(name = "comment_votes", uniqueConstraints = [UniqueConstraint(columnNames = ["comment_id", "user_id"])])
data class CommentVoteEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    val id: UUID? = null,

    @Column(name = "comment_id", nullable = false, columnDefinition = "uuid")
    val commentId: UUID,

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    val userId: UUID,

    @Column(name = "vote_type", nullable = false)
    var voteType: String, // "UP" or "DOWN"

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
