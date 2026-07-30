package com.hobbyhub.auth

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/auth", "/api/auth")
class AuthController(
    private val authService: AuthService
) {

    @GetMapping("/check-email")
    fun checkEmail(@RequestParam email: String): ResponseEntity<CheckAvailabilityResponse> {
        val response = authService.checkEmailAvailability(email)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/check-username")
    fun checkUsername(@RequestParam username: String): ResponseEntity<CheckAvailabilityResponse> {
        val response = authService.checkUsernameAvailability(username)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/register")
    fun register(@RequestBody request: RegisterRequest): ResponseEntity<AuthResponse> {
        val response = authService.register(request)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<AuthResponse> {
        val response = authService.login(request)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/verify-email")
    fun verifyEmail(@RequestBody request: VerifyEmailRequest): ResponseEntity<AuthResponse> {
        val response = authService.verifyEmail(request)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/verify-otp")
    fun verifyOtp(@RequestBody request: VerifyEmailRequest): ResponseEntity<AuthResponse> {
        val response = authService.verifyEmail(request)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/resend-otp")
    fun resendOtp(@RequestBody request: ResendOtpRequest): ResponseEntity<AuthResponse> {
        val response = authService.resendOtp(request)
        return ResponseEntity.ok(response)
    }
}
