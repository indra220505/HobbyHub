package com.hobbyhub.auth

data class RegisterRequest(
    val email: String,
    val passwordHash: String, // Note: Android already hashes using PBKDF2. We'll store this directly or re-hash via BCrypt. It's better if Android sends raw password over HTTPS, but to support the legacy "offline" mode transition, Android will send PBKDF2 hash. For this backend, we will re-hash with BCrypt to be safe.
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
