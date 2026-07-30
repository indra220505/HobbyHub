package com.hobbyhub.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.hobbyhub.model.RoleBadge
import com.hobbyhub.model.User

class UserSessionManager(context: Context) {
    private val prefs: SharedPreferences

    init {
        // Use EncryptedSharedPreferences for secure session storage (AES-256-GCM)
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
            
        prefs = try {
            EncryptedSharedPreferences.create(
                context,
                "hobbyhub_secure_session_v4",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback to regular prefs if encryption fails (e.g., rooted device)
            context.getSharedPreferences("hobbyhub_auth_session_v4_fallback", Context.MODE_PRIVATE)
        }
    }

    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_EMAIL = "email"
        private const val KEY_LEVEL = "level"
        private const val KEY_CURRENT_XP = "current_xp"
        private const val KEY_HOBBIES = "selected_hobbies"
        private const val KEY_JOINED_COMMUNITIES = "joined_community_ids"
        private const val KEY_IS_SUPER_OWNER = "is_super_owner"
        private const val KEY_IS_DEVELOPER = "is_developer"
        private const val KEY_IS_EMAIL_VERIFIED = "is_email_verified"
        private const val KEY_JWT_TOKEN = "jwt_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
    }

    fun getJwtToken(): String? = prefs.getString(KEY_JWT_TOKEN, null)
    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)
    
    fun saveTokens(jwt: String, refresh: String) {
        prefs.edit().apply {
            putString(KEY_JWT_TOKEN, jwt)
            putString(KEY_REFRESH_TOKEN, refresh)
            apply()
        }
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    fun saveUserSession(
        user: User,
        email: String,
        hobbies: Set<String> = emptySet(),
        isSuperOwner: Boolean = false,
        isDeveloper: Boolean = false,
        isEmailVerified: Boolean = false
    ) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_ID, user.id)
            putString(KEY_USERNAME, user.username)
            putString(KEY_DISPLAY_NAME, user.displayName)
            putString(KEY_EMAIL, email)
            putInt(KEY_LEVEL, user.level)
            putLong(KEY_CURRENT_XP, user.currentXp)
            putStringSet(KEY_HOBBIES, hobbies)
            putBoolean(KEY_IS_SUPER_OWNER, isSuperOwner)
            putBoolean(KEY_IS_DEVELOPER, isDeveloper)
            putBoolean(KEY_IS_EMAIL_VERIFIED, isEmailVerified)
            // NO AUTO JOIN: Initial joined list is strictly empty for new users
            apply()
        }
    }

    // ===== SESSION ACCESS CONTROL =====

    fun isSuperOwnerSession(): Boolean = prefs.getBoolean(KEY_IS_SUPER_OWNER, false)

    fun isDeveloperSession(): Boolean = prefs.getBoolean(KEY_IS_DEVELOPER, false)

    fun canAccessDeveloperMode(): Boolean = isSuperOwnerSession() || isDeveloperSession()

    fun isEmailVerifiedSession(): Boolean = prefs.getBoolean(KEY_IS_EMAIL_VERIFIED, false)

    fun getSessionEmail(): String = prefs.getString(KEY_EMAIL, "") ?: ""

    fun updateDisplayName(newName: String) {
        prefs.edit().putString(KEY_DISPLAY_NAME, newName).apply()
    }

    fun markEmailVerified() {
        prefs.edit().putBoolean(KEY_IS_EMAIL_VERIFIED, true).apply()
    }

    // ===== COMMUNITY MEMBERSHIP =====

    fun getJoinedCommunityIds(): Set<String> {
        return prefs.getStringSet(KEY_JOINED_COMMUNITIES, emptySet()) ?: emptySet()
    }

    fun setJoinedCommunityIds(ids: Set<String>) {
        prefs.edit().putStringSet(KEY_JOINED_COMMUNITIES, ids).apply()
    }

    fun joinCommunity(communityId: String) {
        val current = getJoinedCommunityIds().toMutableSet()
        current.add(communityId)
        prefs.edit().putStringSet(KEY_JOINED_COMMUNITIES, current).apply()
    }

    fun leaveCommunity(communityId: String) {
        val current = getJoinedCommunityIds().toMutableSet()
        current.remove(communityId)
        prefs.edit().putStringSet(KEY_JOINED_COMMUNITIES, current).apply()
    }

    fun isMemberOfCommunity(communityId: String): Boolean {
        return getJoinedCommunityIds().contains(communityId)
    }

    // ===== USER DATA =====

    fun getUser(): User {
        val username = prefs.getString(KEY_USERNAME, "member") ?: "member"
        val displayName = prefs.getString(KEY_DISPLAY_NAME, "Member") ?: "Member"
        val level = prefs.getInt(KEY_LEVEL, 1)
        val xp = prefs.getLong(KEY_CURRENT_XP, 0L)

        val userId = prefs.getString(KEY_USER_ID, null) ?: run {
            val generated = "usr_${System.currentTimeMillis()}"
            prefs.edit().putString(KEY_USER_ID, generated).apply()
            generated
        }

        val isSuperOwner = isSuperOwnerSession()
        val roleBadge = if (isSuperOwner) RoleBadge("Super Owner 👑", "#FF7675") else RoleBadge("Member", "#00CEC9")

        return User(
            id = userId,
            username = username,
            displayName = displayName,
            avatarUrl = "",
            bio = "Anggota Komunitas HobbyHub | Hobi: ${getSelectedHobbies().joinToString()}",
            level = level,
            currentXp = xp,
            maxXp = 1000,
            reputation = 10,
            roleBadge = roleBadge,
            badges = emptyList()
        )
    }

    fun getSelectedHobbies(): Set<String> {
        return prefs.getStringSet(KEY_HOBBIES, emptySet()) ?: emptySet()
    }

    fun logout() {
        prefs.edit().clear().apply()
    }
}
