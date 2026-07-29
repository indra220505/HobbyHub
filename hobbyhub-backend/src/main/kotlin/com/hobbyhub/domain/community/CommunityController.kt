package com.hobbyhub.domain.community

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

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
class CommunityController {

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
