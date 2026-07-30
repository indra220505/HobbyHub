package com.hobbyhub.domain.community

import org.springframework.web.bind.annotation.*
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.http.ResponseEntity
import org.springframework.http.HttpStatus
import com.hobbyhub.domain.user.UserRepository
import com.hobbyhub.domain.user.User
import java.util.UUID

data class CommunityRequest(
    val name: String,
    val category: String,
    val description: String,
    val iconEmoji: String = "🌐",
    val isPublic: Boolean = true,
    val iconUrl: String? = null
)

data class CommunityDTO(
    val id: String,
    val name: String,
    val slug: String,
    val category: String,
    val memberCount: Int,
    val description: String,
    val iconEmoji: String,
    val iconUrl: String?,
    val creatorUsername: String,
    val isPublic: Boolean,
    val channelsJson: String
)

@RestController
@RequestMapping("/api/v1/communities")
class CommunityController(
    private val userRepository: UserRepository,
    private val communityRepository: CommunityRepository
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

    @GetMapping
    fun getAllCommunities(): ResponseEntity<List<CommunityDTO>> {
        val list = communityRepository.findAll().map { c ->
            CommunityDTO(
                id = c.id,
                name = c.name,
                slug = c.slug,
                category = c.category,
                memberCount = c.memberCount,
                description = c.description,
                iconEmoji = c.iconEmoji,
                iconUrl = c.iconUrl,
                creatorUsername = c.creatorUsername,
                isPublic = c.isPublic,
                channelsJson = c.channelsJson
            )
        }
        return ResponseEntity.ok(list)
    }

    @PostMapping
    fun createCommunity(@RequestBody req: CommunityRequest): ResponseEntity<CommunityDTO> {
        val user = getCurrentUser() ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        val slug = req.name.lowercase().replace(" ", "-")
        val id = "comm_${user.username}_${System.currentTimeMillis()}"

        val entity = CommunityEntity(
            id = id,
            name = req.name,
            slug = slug,
            category = req.category,
            memberCount = 1,
            description = req.description,
            iconEmoji = req.iconEmoji,
            iconUrl = req.iconUrl,
            creatorUsername = user.username,
            isPublic = req.isPublic,
            channelsJson = "[]"
        )
        val saved = communityRepository.save(entity)

        // Automatically join creator
        user.joinedCommunities.add(saved.id)
        userRepository.save(user)

        val dto = CommunityDTO(
            id = saved.id,
            name = saved.name,
            slug = saved.slug,
            category = saved.category,
            memberCount = saved.memberCount,
            description = saved.description,
            iconEmoji = saved.iconEmoji,
            iconUrl = saved.iconUrl,
            creatorUsername = saved.creatorUsername,
            isPublic = saved.isPublic,
            channelsJson = saved.channelsJson
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(dto)
    }

    @PostMapping("/{id}/join")
    fun joinCommunity(@PathVariable id: String): ResponseEntity<Any> {
        val user = getCurrentUser() ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to "Unauthorized"))
        
        // Ensure joinedCommunities set is initialized
        if (user.joinedCommunities == null) {
            user.joinedCommunities = mutableSetOf()
        }

        if (!user.joinedCommunities.contains(id)) {
            user.joinedCommunities.add(id)
            userRepository.save(user)

            // Update member count in database if present
            communityRepository.findById(id).ifPresent { comm ->
                comm.memberCount += 1
                communityRepository.save(comm)
            }
        }
        return ResponseEntity.ok().build()
    }

    @DeleteMapping("/{id}/leave")
    fun leaveCommunity(@PathVariable id: String): ResponseEntity<Any> {
        val user = getCurrentUser() ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to "Unauthorized"))
        
        if (user.joinedCommunities != null && user.joinedCommunities.contains(id)) {
            user.joinedCommunities.remove(id)
            userRepository.save(user)

            communityRepository.findById(id).ifPresent { comm ->
                if (comm.memberCount > 1) {
                    comm.memberCount -= 1
                    communityRepository.save(comm)
                }
            }
        }
        return ResponseEntity.ok().build()
    }

    @GetMapping("/joined")
    fun getJoinedCommunities(): ResponseEntity<Set<String>> {
        val user = getCurrentUser() ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        return ResponseEntity.ok(user.joinedCommunities ?: emptySet())
    }
}
