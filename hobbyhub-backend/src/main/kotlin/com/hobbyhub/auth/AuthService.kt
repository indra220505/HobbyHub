package com.hobbyhub.auth

import com.hobbyhub.domain.user.User
import com.hobbyhub.domain.user.UserRepository
import com.hobbyhub.security.JwtTokenProvider
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.LocalDateTime

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider
) {

    @Transactional
    fun register(request: RegisterRequest): AuthResponse {
        if (userRepository.existsByEmail(request.email)) {
            throw IllegalArgumentException("Email already registered")
        }
        if (userRepository.existsByUsername(request.username)) {
            throw IllegalArgumentException("Username already taken")
        }

        val verificationCode = generateVerificationCode()
        
        val user = User(
            email = request.email,
            passwordHash = passwordEncoder.encode(request.passwordHash),
            username = request.username,
            displayName = request.displayName,
            verificationCode = verificationCode,
            verificationExpiry = LocalDateTime.now().plusMinutes(10)
        )

        val savedUser = userRepository.save(user)
        
        // Simulating email send by printing to console for local dev
        println("==================================================")
        println("EMAIL SIMULATION:")
        println("To: ${savedUser.email}")
        println("Subject: Your HobbyHub Verification Code")
        println("Code: $verificationCode")
        println("==================================================")

        val token = jwtTokenProvider.generateAccessToken(savedUser.id!!, savedUser.email, savedUser.username)
        val refreshToken = jwtTokenProvider.generateRefreshToken(savedUser.id)

        return AuthResponse(token, refreshToken, toUserDto(savedUser))
    }

    @Transactional
    fun login(request: LoginRequest): AuthResponse {
        val user = userRepository.findByEmail(request.email)
            ?: throw IllegalArgumentException("Invalid credentials")

        if (!passwordEncoder.matches(request.passwordHash, user.passwordHash)) {
            throw IllegalArgumentException("Invalid credentials")
        }

        val token = jwtTokenProvider.generateAccessToken(user.id!!, user.email, user.username)
        val refreshToken = jwtTokenProvider.generateRefreshToken(user.id)

        return AuthResponse(token, refreshToken, toUserDto(user))
    }

    @Transactional
    fun verifyEmail(request: VerifyEmailRequest): AuthResponse {
        val user = userRepository.findByEmail(request.email)
            ?: throw IllegalArgumentException("User not found")

        if (user.isVerified) {
            throw IllegalArgumentException("Email already verified")
        }

        if (user.verificationCode != request.code || user.verificationExpiry?.isBefore(LocalDateTime.now()) == true) {
            throw IllegalArgumentException("Invalid or expired verification code")
        }

        user.isVerified = true
        user.verificationCode = null
        user.verificationExpiry = null
        val savedUser = userRepository.save(user)

        val token = jwtTokenProvider.generateAccessToken(savedUser.id!!, savedUser.email, savedUser.username)
        val refreshToken = jwtTokenProvider.generateRefreshToken(savedUser.id)

        return AuthResponse(token, refreshToken, toUserDto(savedUser))
    }

    private fun generateVerificationCode(): String {
        val random = SecureRandom()
        return (100000 + random.nextInt(900000)).toString() // 6 digits
    }

    private fun toUserDto(user: User): UserDto {
        return UserDto(
            id = user.id!!,
            email = user.email,
            username = user.username,
            displayName = user.displayName,
            isVerified = user.isVerified
        )
    }
}
