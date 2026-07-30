package com.hobbyhub.auth

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

data class AuthResponse(
    val token: String,
    val refreshToken: String,
    val user: UserDto
)

data class UserDto(
    val id: String,
    val email: String,
    val username: String,
    val displayName: String,
    val isVerified: Boolean
)
