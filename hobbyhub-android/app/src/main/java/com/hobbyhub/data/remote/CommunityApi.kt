package com.hobbyhub.data.remote

import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface CommunityApi {
    @POST("api/v1/communities/{id}/join")
    suspend fun joinCommunity(@Path("id") id: String): Response<Void>

    @DELETE("api/v1/communities/{id}/leave")
    suspend fun leaveCommunity(@Path("id") id: String): Response<Void>

    @GET("api/v1/communities/joined")
    suspend fun getJoinedCommunities(): Response<Set<String>>
}
