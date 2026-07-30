package com.hobbyhub.domain.community

import org.springframework.web.bind.annotation.*
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.http.ResponseEntity
import com.hobbyhub.domain.user.UserRepository
import java.util.UUID

data class CommunityResponse(
    val id: String,
    val name: String,
    val slug: String,
    val category: String,
    val memberCount: Int,
    val description: String,
    val iconUrl: String
)

@RestController
@RequestMapping("/api/v1/communities")
class CommunityController(
    private val userRepository: UserRepository
) {

    @PostMapping("/{id}/join")
    fun joinCommunity(@PathVariable id: String): ResponseEntity<Void> {
        val auth = SecurityContextHolder.getContext().authentication
        val userId = UUID.fromString(auth.name)
        val user = userRepository.findById(userId).orElseThrow()
        user.joinedCommunities.add(id)
        userRepository.save(user)
        return ResponseEntity.ok().build()
    }

    @DeleteMapping("/{id}/leave")
    fun leaveCommunity(@PathVariable id: String): ResponseEntity<Void> {
        val auth = SecurityContextHolder.getContext().authentication
        val userId = UUID.fromString(auth.name)
        val user = userRepository.findById(userId).orElseThrow()
        user.joinedCommunities.remove(id)
        userRepository.save(user)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/joined")
    fun getJoinedCommunities(): ResponseEntity<Set<String>> {
        val auth = SecurityContextHolder.getContext().authentication
        val userId = UUID.fromString(auth.name)
        val user = userRepository.findById(userId).orElseThrow()
        return ResponseEntity.ok(user.joinedCommunities)
    }

    @GetMapping
    fun getTrendingCommunities(): List<CommunityResponse> {
        return listOf(
            CommunityResponse(
                id = "comm_01",
                name = "AI & Machine Learning Indonesia",
                slug = "ai-indonesia",
                category = "Programming",
                memberCount = 14250,
                description = "Komunitas riset AI, LLM, Computer Vision, dan Prompt Engineering.",
                iconUrl = "https://cdn.hobbyhub.app/icons/ai.png"
            ),
            CommunityResponse(
                id = "comm_02",
                name = "Android Jetpack Compose Club",
                slug = "android-devs",
                category = "Programming",
                memberCount = 8900,
                description = "Diskusikan Kotlin, Compose, Clean Architecture & Mobile UI.",
                iconUrl = "https://cdn.hobbyhub.app/icons/android.png"
            ),
            CommunityResponse(
                id = "comm_03",
                name = "Valorant Competitive ID",
                slug = "valorant-id",
                category = "Gaming",
                memberCount = 23400,
                description = "Cari Mabar, Turnamen Guild, Lineups, dan Esports Coaching.",
                iconUrl = "https://cdn.hobbyhub.app/icons/valorant.png"
            )
        )
    }
}
