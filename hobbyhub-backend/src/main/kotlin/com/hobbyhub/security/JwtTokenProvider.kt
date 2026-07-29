package com.hobbyhub.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    @Value("\${jwt.secret:defaultSecretKeyWhichShouldBeVeryLongAndSecureForHS256AlgorithmInProductionEnvironments}")
    private val jwtSecret: String,
    @Value("\${jwt.expiration-ms:86400000}") // 1 day default
    private val jwtExpirationMs: Long
) {
    private val key: SecretKey = Keys.hmacShaKeyFor(jwtSecret.toByteArray())

    fun generateAccessToken(userId: String, email: String, username: String): String {
        val now = Date()
        val expiryDate = Date(now.time + jwtExpirationMs)

        return Jwts.builder()
            .subject(userId)
            .claim("email", email)
            .claim("username", username)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(key)
            .compact()
    }
    
    fun generateRefreshToken(userId: String): String {
        val now = Date()
        val expiryDate = Date(now.time + (jwtExpirationMs * 7)) // 7 days

        return Jwts.builder()
            .subject(userId)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(key)
            .compact()
    }

    fun getUserIdFromJWT(token: String): String {
        val claims: Claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
        return claims.subject
    }

    fun validateToken(authToken: String): Boolean {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(authToken)
            return true
        } catch (ex: Exception) {
            // Log exceptions here (e.g., ExpiredJwtException, UnsupportedJwtException, MalformedJwtException)
            println("Invalid JWT Token: ${ex.message}")
        }
        return false
    }

    fun getAuthentication(token: String): Authentication {
        val userId = getUserIdFromJWT(token)
        // Usually you'd fetch the user and their roles here, but we can store roles in claims if needed
        val authorities = listOf(SimpleGrantedAuthority("ROLE_USER"))
        return UsernamePasswordAuthenticationToken(userId, null, authorities)
    }
}
