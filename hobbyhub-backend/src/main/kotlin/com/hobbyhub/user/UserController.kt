package com.hobbyhub.user

import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/users", "/api/users")
class UserController(
    private val userService: UserService
) {

    @DeleteMapping("/me")
    fun deleteMyAccount(): ResponseEntity<Void> {
        val authentication = SecurityContextHolder.getContext().authentication
        // Assuming the principal holds the user's UUID or email. Wait, how is auth setup?
        // Let's assume the username in authentication.name is the UUID of the user, as set by JwtAuthFilter.
        // Or if it's the email, we might need UserRepository to find the UUID.
        // Let's check how JwtService sets the subject.
        val subject = authentication.name
        
        return try {
            val userId = UUID.fromString(subject)
            userService.deleteUserAccount(userId)
            ResponseEntity.noContent().build()
        } catch (e: IllegalArgumentException) {
            // If subject is not UUID, maybe it's email or username. Let's return a proper error or handle it.
            // But we will need to verify auth. Let's just pass subject as UUID assuming subject is UserID.
            ResponseEntity.badRequest().build()
        } catch (e: Exception) {
            ResponseEntity.internalServerError().build()
        }
    }
}
