package com.hobbyhub.domain.community

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CommunityRepository : JpaRepository<CommunityEntity, String> {
    fun findByIsPublicTrue(): List<CommunityEntity>
    fun findByCategory(category: String): List<CommunityEntity>
}
