package com.hobbyhub.auth

import com.hobbyhub.domain.user.User
import com.hobbyhub.domain.user.UserRepository
import com.hobbyhub.security.JwtTokenProvider
import com.hobbyhub.service.EmailService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.LocalDateTime

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,
    private val emailService: EmailService
) {

    @Transactional
    fun register(request: RegisterRequest): AuthResponse {
        val emailClean = request.email.trim().lowercase()
        val usernameClean = request.username.trim().lowercase()

        if (userRepository.existsByEmail(emailClean)) {
            throw IllegalArgumentException("Email sudah terdaftar!")
        }
        if (userRepository.existsByUsername(usernameClean)) {
            throw IllegalArgumentException("Username sudah digunakan!")
        }

        val otpCode = generateVerificationCode()
        val otpExpiry = LocalDateTime.now().plusMinutes(5) // OTP valid for 5 minutes

        val user = User(
            email = emailClean,
            passwordHash = passwordEncoder.encode(request.passwordHash),
            username = usernameClean,
            displayName = request.displayName.trim(),
            isVerified = false, // Must verify OTP to become verified
            verificationCode = otpCode,
            verificationExpiry = otpExpiry
        )

        val savedUser = userRepository.save(user)

        // Send OTP email via SMTP / EmailService
        emailService.sendOtpEmail(savedUser.email, otpCode)

        val token = jwtTokenProvider.generateAccessToken(savedUser.id!!, savedUser.email, savedUser.username)
        val refreshToken = jwtTokenProvider.generateRefreshToken(savedUser.id)

        return AuthResponse(token, refreshToken, toUserDto(savedUser))
    }

    @Transactional
    fun login(request: LoginRequest): AuthResponse {
        val emailClean = request.email.trim().lowercase()
        val user = userRepository.findByEmail(emailClean)
            ?: throw IllegalArgumentException("Kredensial salah atau pengguna tidak ditemukan.")

        if (!passwordEncoder.matches(request.passwordHash, user.passwordHash)) {
            throw IllegalArgumentException("Kredensial salah. Kata sandi Anda tidak cocok.")
        }

        if (!user.isVerified) {
            throw IllegalArgumentException("Email belum diverifikasi. Silakan masukkan kode OTP verifikasi.")
        }

        val token = jwtTokenProvider.generateAccessToken(user.id!!, user.email, user.username)
        val refreshToken = jwtTokenProvider.generateRefreshToken(user.id)

        return AuthResponse(token, refreshToken, toUserDto(user))
    }

    @Transactional
    fun verifyEmail(request: VerifyEmailRequest): AuthResponse {
        val emailClean = request.email.trim().lowercase()
        val user = userRepository.findByEmail(emailClean)
            ?: throw IllegalArgumentException("Pengguna tidak ditemukan.")

        if (user.isVerified) {
            val token = jwtTokenProvider.generateAccessToken(user.id!!, user.email, user.username)
            val refreshToken = jwtTokenProvider.generateRefreshToken(user.id)
            return AuthResponse(token, refreshToken, toUserDto(user))
        }

        if (user.verificationCode == null || user.verificationCode != request.code.trim()) {
            throw IllegalArgumentException("Kode OTP verifikasi salah.")
        }

        if (user.verificationExpiry != null && user.verificationExpiry!!.isBefore(LocalDateTime.now())) {
            throw IllegalArgumentException("Kode OTP sudah kedaluwarsa. Silakan tekan Kirim Ulang OTP.")
        }

        user.isVerified = true
        user.verificationCode = null
        user.verificationExpiry = null
        val savedUser = userRepository.save(user)

        val token = jwtTokenProvider.generateAccessToken(savedUser.id!!, savedUser.email, savedUser.username)
        val refreshToken = jwtTokenProvider.generateRefreshToken(savedUser.id)

        return AuthResponse(token, refreshToken, toUserDto(savedUser))
    }

    @Transactional
    fun resendOtp(request: ResendOtpRequest): AuthResponse {
        val emailClean = request.email.trim().lowercase()
        val user = userRepository.findByEmail(emailClean)
            ?: throw IllegalArgumentException("Pengguna tidak ditemukan.")

        if (user.isVerified) {
            throw IllegalArgumentException("Email sudah terverifikasi sebelumnya.")
        }

        val newOtpCode = generateVerificationCode()
        val newExpiry = LocalDateTime.now().plusMinutes(5) // New 5-minute expiry

        user.verificationCode = newOtpCode
        user.verificationExpiry = newExpiry
        val savedUser = userRepository.save(user)

        // Send new OTP email via SMTP / EmailService
        emailService.sendOtpEmail(savedUser.email, newOtpCode)

        val token = jwtTokenProvider.generateAccessToken(savedUser.id!!, savedUser.email, savedUser.username)
        val refreshToken = jwtTokenProvider.generateRefreshToken(savedUser.id)

        return AuthResponse(token, refreshToken, toUserDto(savedUser))
    }

    private fun generateVerificationCode(): String {
        val random = SecureRandom()
        return (100000 + random.nextInt(900000)).toString() // 6 digits OTP
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
