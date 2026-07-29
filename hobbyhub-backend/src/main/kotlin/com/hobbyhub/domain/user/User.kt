package com.hobbyhub.domain.user

import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: String? = null,

    @Column(unique = true, nullable = false)
    val email: String,

    @Column(nullable = false)
    var passwordHash: String,

    @Column(unique = true, nullable = false)
    var username: String,

    @Column(nullable = false)
    var displayName: String,

    var bio: String? = null,
    
    var avatarUrl: String? = null,

    @Column(nullable = false)
    var isVerified: Boolean = false,

    var verificationCode: String? = null,
    
    var verificationExpiry: LocalDateTime? = null,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)

interface UserRepository : JpaRepository<User, String> {
    fun findByEmail(email: String): User?
    fun findByUsername(username: String): User?
    fun existsByEmail(email: String): Boolean
    fun existsByUsername(username: String): Boolean
}
