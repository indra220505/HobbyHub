package com.hobbyhub.domain.post

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface PostRepository : JpaRepository<PostEntity, UUID> {
    fun findByCommunityIdOrderByCreatedAtDesc(communityId: String, pageable: Pageable): Page<PostEntity>
    fun findAllByOrderByCreatedAtDesc(pageable: Pageable): Page<PostEntity>
    fun findByAuthorIdOrderByCreatedAtDesc(authorId: UUID, pageable: Pageable): Page<PostEntity>
}

interface CommentRepository : JpaRepository<CommentEntity, UUID> {
    fun findByPostIdOrderByCreatedAtAsc(postId: UUID): List<CommentEntity>
    fun countByPostId(postId: UUID): Long
}

interface PostVoteRepository : JpaRepository<PostVoteEntity, UUID> {
    fun findByPostIdAndUserId(postId: UUID, userId: UUID): PostVoteEntity?
}

interface CommentVoteRepository : JpaRepository<CommentVoteEntity, UUID> {
    fun findByCommentIdAndUserId(commentId: UUID, userId: UUID): CommentVoteEntity?
}
