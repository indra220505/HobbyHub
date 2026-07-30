package com.hobbyhub.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

data class CheckAvailabilityResponse(
    val available: Boolean,
    val message: String
)

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

data class ResendOtpRequest(
    val email: String
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

    @GET("/api/v1/auth/check-email")
    suspend fun checkEmail(@Query("email") email: String): Response<CheckAvailabilityResponse>

    @GET("/api/v1/auth/check-username")
    suspend fun checkUsername(@Query("username") username: String): Response<CheckAvailabilityResponse>

    @POST("/api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("/api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("/api/v1/auth/verify-email")
    suspend fun verifyEmail(@Body request: VerifyEmailRequest): Response<AuthResponse>

    @POST("/api/v1/auth/verify-otp")
    suspend fun verifyOtp(@Body request: VerifyEmailRequest): Response<AuthResponse>

    @POST("/api/v1/auth/resend-otp")
    suspend fun resendOtp(@Body request: ResendOtpRequest): Response<AuthResponse>
}
