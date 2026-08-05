package com.hobbyhub.domain.post

import com.hobbyhub.domain.user.UserRepository
import com.hobbyhub.domain.user.User
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID

// ── Request / Response DTOs ──────────────────────────────────

data class CreatePostRequest(
    val communityId: String,
    val title: String,
    val content: String? = null,
    val type: String = "TEXT",
    val mediaUrls: List<String> = emptyList()
)

data class CreateCommentRequest(
    val content: String,
    val parentCommentId: String? = null
)

data class PostDTO(
    val id: String,
    val communityId: String,
    val authorId: String,
    val authorUsername: String,
    val authorDisplayName: String,
    val authorAvatarUrl: String?,
    val title: String,
    val content: String?,
    val type: String,
    val mediaUrls: String,
    val upvoteCount: Int,
    val downvoteCount: Int,
    val commentCount: Int,
    val isPinned: Boolean,
    val createdAt: String,
    val timeAgo: String,
    val communityName: String,
    val communityEmoji: String,
    val userVote: String? = null // "UP", "DOWN", or null
)

data class CommentDTO(
    val id: String,
    val postId: String,
    val parentCommentId: String?,
    val authorId: String,
    val authorUsername: String,
    val authorDisplayName: String,
    val authorAvatarUrl: String?,
    val content: String,
    val upvoteCount: Int,
    val createdAt: String,
    val timeAgo: String
)

data class VoteResponse(
    val postId: String,
    val upvoteCount: Int,
    val downvoteCount: Int,
    val userVote: String?
)

data class PostPageResponse(
    val content: List<PostDTO>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val isLast: Boolean
)

// ── Controller ───────────────────────────────────────────────

@RestController
@RequestMapping("/api/v1/posts")
class PostController(
    private val postRepository: PostRepository,
    private val commentRepository: CommentRepository,
    private val postVoteRepository: PostVoteRepository,
    private val userRepository: UserRepository,
    private val communityRepository: com.hobbyhub.domain.community.CommunityRepository
) {

    private fun getCurrentUser(): User? {
        val auth = SecurityContextHolder.getContext().authentication ?: return null
        val principal = auth.name ?: return null
        return try {
            val userId = UUID.fromString(principal)
            userRepository.findById(userId).orElse(null)
        } catch (e: Exception) {
            userRepository.findByUsername(principal)
        }
    }

    private fun formatTimeAgo(createdAt: LocalDateTime): String {
        val now = LocalDateTime.now()
        val duration = Duration.between(createdAt, now)
        return when {
            duration.toMinutes() < 1 -> "baru saja"
            duration.toMinutes() < 60 -> "${duration.toMinutes()}m"
            duration.toHours() < 24 -> "${duration.toHours()}h"
            duration.toDays() < 7 -> "${duration.toDays()}d"
            duration.toDays() < 30 -> "${duration.toDays() / 7}w"
            duration.toDays() < 365 -> "${duration.toDays() / 30}mo"
            else -> "${duration.toDays() / 365}y"
        }
    }

    private fun toPostDTO(post: PostEntity, currentUser: User?): PostDTO {
        val author = userRepository.findById(post.authorId).orElse(null)
        val community = communityRepository.findById(post.communityId).orElse(null)

        val userVote = if (currentUser?.id != null) {
            postVoteRepository.findByPostIdAndUserId(post.id!!, currentUser.id!!)?.voteType
        } else null

        return PostDTO(
            id = post.id.toString(),
            communityId = post.communityId,
            authorId = post.authorId.toString(),
            authorUsername = author?.username ?: "unknown",
            authorDisplayName = author?.displayName ?: "Unknown User",
            authorAvatarUrl = author?.avatarUrl,
            title = post.title,
            content = post.content,
            type = post.type,
            mediaUrls = post.mediaUrls,
            upvoteCount = post.upvoteCount,
            downvoteCount = post.downvoteCount,
            commentCount = post.commentCount,
            isPinned = post.isPinned,
            createdAt = post.createdAt.toString(),
            timeAgo = formatTimeAgo(post.createdAt),
            communityName = community?.name ?: "Unknown Community",
            communityEmoji = community?.iconEmoji ?: "🌐",
            userVote = userVote
        )
    }

    // ── GET /api/v1/posts ─────────────────────────────────────
    // Global feed or filtered by communityId
    @GetMapping
    fun getFeed(
        @RequestParam(required = false) communityId: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PostPageResponse> {
        val user = getCurrentUser()
        val pageable = PageRequest.of(page, size)

        val postPage = if (communityId != null) {
            postRepository.findByCommunityIdOrderByCreatedAtDesc(communityId, pageable)
        } else {
            postRepository.findAllByOrderByCreatedAtDesc(pageable)
        }

        val dtos = postPage.content.map { toPostDTO(it, user) }

        return ResponseEntity.ok(
            PostPageResponse(
                content = dtos,
                page = postPage.number,
                size = postPage.size,
                totalElements = postPage.totalElements,
                totalPages = postPage.totalPages,
                isLast = postPage.isLast
            )
        )
    }

    // ── GET /api/v1/posts/{id} ────────────────────────────────
    @GetMapping("/{id}")
    fun getPost(@PathVariable id: String): ResponseEntity<PostDTO> {
        val user = getCurrentUser()
        val postId = try { UUID.fromString(id) } catch (e: Exception) {
            return ResponseEntity.badRequest().build()
        }
        val post = postRepository.findById(postId).orElse(null)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(toPostDTO(post, user))
    }

    // ── POST /api/v1/posts ────────────────────────────────────
    @PostMapping
    fun createPost(@RequestBody req: CreatePostRequest): ResponseEntity<PostDTO> {
        val user = getCurrentUser()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val entity = PostEntity(
            communityId = req.communityId,
            authorId = user.id!!,
            title = req.title,
            content = req.content,
            type = req.type,
            mediaUrls = if (req.mediaUrls.isNotEmpty()) req.mediaUrls.joinToString(",") else "[]"
        )
        val saved = postRepository.save(entity)
        return ResponseEntity.status(HttpStatus.CREATED).body(toPostDTO(saved, user))
    }

    // ── POST /api/v1/posts/{id}/upvote ────────────────────────
    @PostMapping("/{id}/upvote")
    fun upvotePost(@PathVariable id: String): ResponseEntity<VoteResponse> {
        return handleVote(id, "UP")
    }

    // ── POST /api/v1/posts/{id}/downvote ──────────────────────
    @PostMapping("/{id}/downvote")
    fun downvotePost(@PathVariable id: String): ResponseEntity<VoteResponse> {
        return handleVote(id, "DOWN")
    }

    @Synchronized
    private fun handleVote(postIdStr: String, voteType: String): ResponseEntity<VoteResponse> {
        val user = getCurrentUser()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val postId = try { UUID.fromString(postIdStr) } catch (e: Exception) {
            return ResponseEntity.badRequest().build()
        }

        val post = postRepository.findById(postId).orElse(null)
            ?: return ResponseEntity.notFound().build()

        val existingVote = postVoteRepository.findByPostIdAndUserId(postId, user.id!!)

        if (existingVote != null) {
            if (existingVote.voteType == voteType) {
                // Toggle off: remove the vote
                postVoteRepository.delete(existingVote)
                if (voteType == "UP") post.upvoteCount = maxOf(0, post.upvoteCount - 1)
                else post.downvoteCount = maxOf(0, post.downvoteCount - 1)
                postRepository.save(post)
                return ResponseEntity.ok(VoteResponse(postIdStr, post.upvoteCount, post.downvoteCount, null))
            } else {
                // Switch vote
                if (existingVote.voteType == "UP") post.upvoteCount = maxOf(0, post.upvoteCount - 1)
                else post.downvoteCount = maxOf(0, post.downvoteCount - 1)

                existingVote.voteType = voteType
                postVoteRepository.save(existingVote)

                if (voteType == "UP") post.upvoteCount += 1
                else post.downvoteCount += 1
                postRepository.save(post)
                return ResponseEntity.ok(VoteResponse(postIdStr, post.upvoteCount, post.downvoteCount, voteType))
            }
        } else {
            // New vote
            val vote = PostVoteEntity(postId = postId, userId = user.id!!, voteType = voteType)
            postVoteRepository.save(vote)
            if (voteType == "UP") post.upvoteCount += 1
            else post.downvoteCount += 1
            postRepository.save(post)
            return ResponseEntity.ok(VoteResponse(postIdStr, post.upvoteCount, post.downvoteCount, voteType))
        }
    }

    // ── GET /api/v1/posts/{id}/comments ───────────────────────
    @GetMapping("/{id}/comments")
    fun getComments(@PathVariable id: String): ResponseEntity<List<CommentDTO>> {
        val postId = try { UUID.fromString(id) } catch (e: Exception) {
            return ResponseEntity.badRequest().build()
        }
        val comments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId)
        val dtos = comments.map { c ->
            val author = userRepository.findById(c.authorId).orElse(null)
            CommentDTO(
                id = c.id.toString(),
                postId = c.postId.toString(),
                parentCommentId = c.parentCommentId?.toString(),
                authorId = c.authorId.toString(),
                authorUsername = author?.username ?: "unknown",
                authorDisplayName = author?.displayName ?: "Unknown",
                authorAvatarUrl = author?.avatarUrl,
                content = c.content,
                upvoteCount = c.upvoteCount,
                createdAt = c.createdAt.toString(),
                timeAgo = formatTimeAgo(c.createdAt)
            )
        }
        return ResponseEntity.ok(dtos)
    }

    // ── POST /api/v1/posts/{id}/comments ──────────────────────
    @PostMapping("/{id}/comments")
    fun createComment(
        @PathVariable id: String,
        @RequestBody req: CreateCommentRequest
    ): ResponseEntity<CommentDTO> {
        val user = getCurrentUser()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val postId = try { UUID.fromString(id) } catch (e: Exception) {
            return ResponseEntity.badRequest().build()
        }

        val post = postRepository.findById(postId).orElse(null)
            ?: return ResponseEntity.notFound().build()

        val parentId = req.parentCommentId?.let {
            try { UUID.fromString(it) } catch (e: Exception) { null }
        }

        val comment = CommentEntity(
            postId = postId,
            parentCommentId = parentId,
            authorId = user.id!!,
            content = req.content
        )
        val saved = commentRepository.save(comment)

        // Increment comment count on post
        post.commentCount += 1
        postRepository.save(post)

        val dto = CommentDTO(
            id = saved.id.toString(),
            postId = saved.postId.toString(),
            parentCommentId = saved.parentCommentId?.toString(),
            authorId = saved.authorId.toString(),
            authorUsername = user.username,
            authorDisplayName = user.displayName,
            authorAvatarUrl = user.avatarUrl,
            content = saved.content,
            upvoteCount = saved.upvoteCount,
            createdAt = saved.createdAt.toString(),
            timeAgo = formatTimeAgo(saved.createdAt)
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(dto)
    }

    // ── DELETE /api/v1/posts/{id} ─────────────────────────────
    @DeleteMapping("/{id}")
    fun deletePost(@PathVariable id: String): ResponseEntity<Any> {
        val user = getCurrentUser()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val postId = try { UUID.fromString(id) } catch (e: Exception) {
            return ResponseEntity.badRequest().build()
        }

        val post = postRepository.findById(postId).orElse(null)
            ?: return ResponseEntity.notFound().build()

        // Only the author can delete their own post
        if (post.authorId != user.id) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("error" to "Hanya penulis yang bisa menghapus postingan ini"))
        }

        postRepository.delete(post)
        return ResponseEntity.ok(mapOf("status" to "DELETED", "id" to id))
    }
}
