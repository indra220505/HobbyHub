package com.hobbyhub.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class RegisterRequest(
    val email: String,
    val passwordHash: String,
    val username: String,
    val displayName: String
)

data class LoginRequest(
    val email: String,
    val passwordHash: String
)

data class VerifyEmailRequest(
    val email: String,
    val code: String
)

data class UserDto(
    val id: String,
    val email: String,
    val username: String,
    val displayName: String,
    val isVerified: Boolean
)

data class AuthResponse(
    val token: String,
    val refreshToken: String,
    val user: UserDto
)

interface AuthApi {

    @POST("/api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("/api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("/api/v1/auth/verify-email")
    suspend fun verifyEmail(@Body request: VerifyEmailRequest): Response<AuthResponse>
}
