package com.hobbyhub.data.remote

import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

data class CreateCommunityPayload(
    val name: String,
    val category: String,
    val description: String,
    val iconEmoji: String = "🌐",
    val isPublic: Boolean = true
)

data class CommunityRemote(
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

interface CommunityApi {
    @GET("api/v1/communities")
    suspend fun getAllCommunities(): Response<List<CommunityRemote>>

    @POST("api/v1/communities")
    suspend fun createCommunity(@retrofit2.http.Body req: CreateCommunityPayload): Response<CommunityRemote>

    @POST("api/v1/communities/{id}/join")
    suspend fun joinCommunity(@Path("id") id: String): Response<Void>

    @DELETE("api/v1/communities/{id}/leave")
    suspend fun leaveCommunity(@Path("id") id: String): Response<Void>

    @GET("api/v1/communities/joined")
    suspend fun getJoinedCommunities(): Response<Set<String>>
}
