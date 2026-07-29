package com.hobbyhub.repository

import com.hobbyhub.model.*

object MockDataRepository {

    val currentUser = User(
        id = "usr_42",
        username = "alex_dev",
        displayName = "Alex Prasetyo",
        avatarUrl = "https://cdn.hobbyhub.app/avatars/alex.png",
        bio = "Mobile Software Architect & AI Enthusiast | Kotlin & Jetpack Compose Lover",
        level = 42,
        currentXp = 8450,
        maxXp = 10000,
        reputation = 2450,
        roleBadge = RoleBadge("Verified Expert", "#00CEC9"),
        badges = listOf(
            Badge("b1", "Early Supporter", "Bergabung saat tahap Alpha", "🚀", "SYSTEM"),
            Badge("b2", "Top Mentor", "Membantu 100+ member menjawab pertanyaan", "🎓", "REPUTATION"),
            Badge("b3", "Code Ninja", "Menyelesaikan 50+ tantangan koding", "⚡", "SKILL"),
            Badge("b4", "OG Member", "Member generasi pertama", "👑", "SPECIAL")
        )
    )

    // Chat dimulai dalam keadaan 100% Kosong (Clean Initial State)
    val sampleChatMessages = emptyList<ChatMessage>()

    val sampleQuests = listOf(
        Quest("q1", "Kirim 1 pesan pertama di channel komunitasmu", 150, 0.0f, false),
        Quest("q2", "Buat atau ikuti 1 Komunitas Hobi favoritmu", 200, 0.5f, false),
        Quest("q3", "Pilih 3+ minat hobi di profil akunmu", 100, 1.0f, true)
    )
}
