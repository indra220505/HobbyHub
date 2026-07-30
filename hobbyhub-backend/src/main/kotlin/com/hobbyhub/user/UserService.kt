package com.hobbyhub.user

import com.hobbyhub.domain.user.UserRepository
import com.hobbyhub.websocket.ChatHandler
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UserService(
    private val userRepository: UserRepository,
    private val chatHandler: ChatHandler
) {

    @Transactional
    fun deleteUserAccount(userId: UUID) {
        // Find the user to ensure they exist
        val user = userRepository.findById(userId).orElseThrow {
            IllegalArgumentException("User not found")
        }
        
        // Wipe out their chat messages from active channels (nullify them)
        chatHandler.deleteUserMessages(userId.toString())
        
        // Remove user from the database
        userRepository.delete(user)
    }
}
