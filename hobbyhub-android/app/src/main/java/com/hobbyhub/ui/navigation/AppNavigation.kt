package com.hobbyhub.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hobbyhub.data.local.CommunityRegistryManager
import com.hobbyhub.data.local.UserSessionManager
import com.hobbyhub.model.ChannelType
import com.hobbyhub.model.Community
import com.hobbyhub.model.RoleBadge
import com.hobbyhub.model.User
import com.hobbyhub.repository.MockDataRepository
import com.hobbyhub.ui.screens.*
import com.hobbyhub.ui.theme.*
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Login : Screen("login", "Login", Icons.Default.Lock)
    object Register : Screen("register", "Register", Icons.Default.PersonAdd)
    object Onboarding : Screen("onboarding", "Onboarding", Icons.Default.Tune)
    object EmailVerification : Screen("email_verification", "Verify Email", Icons.Default.MarkEmailUnread)
    object Explore : Screen("explore", "Explore", Icons.Default.Explore)
    object Guilds : Screen("guilds", "Guilds", Icons.Default.Groups)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
    object CommunityDetail : Screen("community_detail", "Community", Icons.Default.Groups)
    object CommunitySettings : Screen("community_settings", "Settings", Icons.Default.Settings)
    object ChannelManager : Screen("channel_manager", "Channels", Icons.Default.Tag)
    object RoleManager : Screen("role_manager", "Roles", Icons.Default.Shield)
    object ModerationSettings : Screen("moderation_settings", "Moderation", Icons.Default.Gavel)
    object Chat : Screen("chat/{channelName}", "Chat", Icons.Default.Tag)
    object VoiceRoom : Screen("voice/{channelName}", "Voice Room", Icons.Default.VolumeUp)
}

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val sessionManager = remember { UserSessionManager(context) }
    val commDb = remember { CommunityRegistryManager(context) }
    val coroutineScope = rememberCoroutineScope()

    val initialCommunities = remember { commDb.getAllCommunities() }

    var currentScreen by remember {
        mutableStateOf<String>(
            if (sessionManager.isLoggedIn()) {
                // Check if email is verified before allowing access
                if (sessionManager.isEmailVerifiedSession()) "explore" else "email_verification"
            } else "login"
        )
    }

    var currentUser by remember {
        mutableStateOf<User>(
            if (sessionManager.isLoggedIn()) sessionManager.getUser() else MockDataRepository.currentUser
        )
    }

    // Temporary registration data
    var pendingDisplayName by remember { mutableStateOf<String>("") }
    var pendingUsername by remember { mutableStateOf<String>("") }
    var pendingEmail by remember { mutableStateOf<String>("") }
    var pendingPassword by remember { mutableStateOf<String>("") }

    var selectedCommunity by remember { mutableStateOf<Community>(initialCommunities[0]) }
    var selectedChannelName by remember { mutableStateOf<String>("general") }

    Scaffold(
        bottomBar = {
            if (currentScreen in listOf("explore", "guilds", "profile")) {
                NavigationBar(containerColor = SurfaceCard) {
                    NavigationBarItem(
                        selected = currentScreen == "explore",
                        onClick = { currentScreen = "explore" },
                        icon = { Icon(Screen.Explore.icon, contentDescription = Screen.Explore.title) },
                        label = { Text(Screen.Explore.title) }
                    )
                    NavigationBarItem(
                        selected = currentScreen == "guilds",
                        onClick = { currentScreen = "guilds" },
                        icon = { Icon(Screen.Guilds.icon, contentDescription = Screen.Guilds.title) },
                        label = { Text(Screen.Guilds.title) }
                    )
                    NavigationBarItem(
                        selected = currentScreen == "profile",
                        onClick = { currentScreen = "profile" },
                        icon = { Icon(Screen.Profile.icon, contentDescription = Screen.Profile.title) },
                        label = { Text(Screen.Profile.title) }
                    )
                }
            }
        },
        containerColor = ObsidianBg
    ) { innerPadding ->
        Surface(modifier = Modifier.padding(innerPadding)) {
            when (currentScreen) {
                "login" -> {
                    LoginScreen(
                        onLoginSuccess = { authResponse ->
                            val userDto = authResponse.user
                            val isSuperOwner = false // Or derive from token/roles if needed later
                            val userRole = RoleBadge("Member", "#00CEC9")
                            
                            val user = User(
                                id = userDto.id,
                                username = userDto.username,
                                displayName = userDto.displayName,
                                avatarUrl = "",
                                bio = "Pengguna HobbyHub",
                                level = 1,
                                currentXp = 100,
                                maxXp = 100000,
                                reputation = 15,
                                roleBadge = userRole,
                                badges = listOf()
                            )
                            
                            sessionManager.saveTokens(authResponse.token, authResponse.refreshToken)
                            sessionManager.saveUserSession(
                                user = user,
                                email = userDto.email,
                                hobbies = emptySet(),
                                isSuperOwner = isSuperOwner,
                                isDeveloper = false,
                                isEmailVerified = userDto.isVerified
                            )
                            currentUser = user
                            pendingEmail = userDto.email

                            // Check email verification status
                            if (userDto.isVerified) {
                                currentScreen = "explore"
                            } else {
                                currentScreen = "email_verification"
                            }
                        },
                        onNavigateToRegister = { currentScreen = "register" },
                        onNavigateToVerification = { targetEmail ->
                            if (targetEmail.isNotBlank()) {
                                pendingEmail = targetEmail
                            }
                            currentScreen = "email_verification"
                        }
                    )
                }
                "register" -> {
                    RegisterScreen(
                        onRegisterSuccess = { displayName, username, email, password ->
                            pendingDisplayName = displayName
                            pendingUsername = username
                            pendingEmail = email
                            pendingPassword = password
                            currentScreen = "onboarding"
                        },
                        onNavigateToLogin = { currentScreen = "login" }
                    )
                }
                "onboarding" -> {
                    HobbyOnboardingScreen(
                        onCompleteOnboarding = { selectedHobbies ->
                            coroutineScope.launch {
                                try {
                                    val api = com.hobbyhub.data.remote.NetworkModule.getAuthApi(context)
                                    val request = com.hobbyhub.data.remote.RegisterRequest(
                                        email = pendingEmail,
                                        passwordHash = pendingPassword,
                                        username = pendingUsername,
                                        displayName = pendingDisplayName
                                    )
                                    val response = api.register(request)
                                    if (response.isSuccessful && response.body() != null) {
                                        val authResponse = response.body()!!
                                        val userDto = authResponse.user
                                        val newUser = User(
                                            id = userDto.id,
                                            username = userDto.username,
                                            displayName = userDto.displayName,
                                            avatarUrl = "",
                                            bio = "Anggota Komunitas HobbyHub | Hobi: ${selectedHobbies.joinToString()}",
                                            level = 1,
                                            currentXp = 100,
                                            maxXp = 1000,
                                            reputation = 10,
                                            roleBadge = RoleBadge("Member", "#00CEC9"),
                                            badges = emptyList()
                                        )
                                        sessionManager.saveTokens(authResponse.token, authResponse.refreshToken)
                                        sessionManager.saveUserSession(
                                            user = newUser,
                                            email = pendingEmail,
                                            hobbies = selectedHobbies,
                                            isSuperOwner = false,
                                            isDeveloper = false,
                                            isEmailVerified = false
                                        )
                                        currentUser = newUser
                                        currentScreen = "email_verification"
                                    } else {
                                        val errStr = response.errorBody()?.string() ?: ""
                                        val message = try {
                                            org.json.JSONObject(errStr).optString("message", "Gagal mendaftar. Silakan coba lagi.")
                                        } catch (_: Exception) {
                                            "Gagal mendaftar. Silakan periksa koneksi internet Anda."
                                        }
                                        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    android.widget.Toast.makeText(context, "Gagal terhubung ke server: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    )
                }
                "email_verification" -> {
                    val verifyEmail = pendingEmail.ifBlank { sessionManager.getSessionEmail() }
                    EmailVerificationScreen(
                        email = verifyEmail,
                        onVerificationSuccess = {
                            sessionManager.markEmailVerified()
                            currentScreen = "explore"
                        },
                        onBackToLogin = {
                            sessionManager.logout()
                            currentScreen = "login"
                        }
                    )
                }
                "explore" -> {
                    HomeScreen(
                        onCommunityClick = { comm ->
                            val exactComm = commDb.getCommunityById(comm.id) ?: comm
                            selectedCommunity = exactComm
                            currentScreen = "community_detail"
                        }
                    )
                }
                "guilds" -> {
                    // GUILDS / KOMUNITASKU SCREEN
                    val joinedIds = sessionManager.getJoinedCommunityIds()
                    val joinedCommunities = commDb.getAllCommunities().filter { joinedIds.contains(it.id) }
                    var guildSearchQuery by remember { mutableStateOf("") }

                    val filteredJoined = joinedCommunities.filter {
                        guildSearchQuery.isBlank() || it.name.contains(guildSearchQuery, ignoreCase = true) || it.description.contains(guildSearchQuery, ignoreCase = true)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ObsidianBg)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Komunitasku / Guilds (${joinedCommunities.size})",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = guildSearchQuery,
                            onValueChange = { guildSearchQuery = it },
                            placeholder = { Text("Cari di komunitas yang diikuti...", color = TextMuted) },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryViolet) },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryViolet, unfocusedBorderColor = BorderDark, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                            shape = RoundedCornerShape(20.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (joinedCommunities.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(text = "🌐", fontSize = 48.sp)
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "Kamu belum bergabung dengan komunitas mana pun.",
                                            color = TextPrimary,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Tekan tombol + di halaman Jelajah untuk bergabung dengan komunitas yang kamu sukai!",
                                            color = TextMuted,
                                            fontSize = 12.sp,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                            onClick = { currentScreen = "explore" },
                                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet)
                                        ) {
                                            Text("+ Cari & Join Komunitas", color = TextPrimary, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(filteredJoined) { comm ->
                                    val isOwner = commDb.isOwnerOfCommunity(comm.id, currentUser.username)
                                    val userRoleLabel = if (isOwner) "Owner 👑" else "Member 👤"
                                    val roleColor = if (isOwner) PrimaryViolet else SecondaryTurquoise

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedCommunity = comm
                                                currentScreen = "community_detail"
                                            }
                                            .border(1.dp, BorderDark, RoundedCornerShape(12.dp)),
                                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = comm.iconEmoji, fontSize = 36.sp)
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(text = comm.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Surface(color = roleColor.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp)) {
                                                        Text(text = userRoleLabel, color = roleColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                                    }
                                                }
                                                Text(text = "${comm.category} • ${comm.memberCount}", color = SecondaryTurquoise, fontSize = 12.sp)
                                                Text(text = comm.description, color = TextMuted, fontSize = 13.sp, maxLines = 1)
                                            }
                                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                "community_detail" -> {
                    CommunityDetailScreen(
                        community = selectedCommunity,
                        onChannelClick = { ch ->
                            selectedChannelName = ch.name
                            if (ch.type == ChannelType.VOICE) {
                                currentScreen = "voice_room"
                            } else {
                                currentScreen = "chat"
                            }
                        },
                        onBackClick = { currentScreen = "explore" },
                        onNavigateToSettings = { currentScreen = "community_settings" },
                        onCommunityUpdated = { updated ->
                            selectedCommunity = updated
                        },
                        onCommunityDeleted = {
                            currentScreen = "explore"
                        }
                    )
                }
                "community_settings" -> {
                    CommunitySettingsScreen(
                        community = selectedCommunity,
                        onBackClick = { currentScreen = "community_detail" },
                        onNavigateToChannelManager = { currentScreen = "channel_manager" },
                        onNavigateToRoleManager = { currentScreen = "role_manager" },
                        onNavigateToModeration = { currentScreen = "moderation_settings" },
                        onCommunityDeleted = { currentScreen = "explore" },
                        onCommunityUpdated = { updated ->
                            selectedCommunity = updated
                            currentScreen = "community_detail"
                        }
                    )
                }
                "channel_manager" -> {
                    ChannelManagerScreen(
                        community = selectedCommunity,
                        onBackClick = { currentScreen = "community_settings" },
                        onCommunityUpdated = { updated ->
                            selectedCommunity = updated
                        }
                    )
                }
                "role_manager" -> {
                    RoleManagerScreen(
                        communityName = selectedCommunity.name,
                        onBackClick = { currentScreen = "community_settings" }
                    )
                }
                "moderation_settings" -> {
                    ModerationSettingsScreen(
                        communityId = selectedCommunity.id,
                        communityName = selectedCommunity.name,
                        onBackClick = { currentScreen = "community_settings" }
                    )
                }
                "chat" -> {
                    ChatScreen(
                        channelName = selectedChannelName,
                        onBackClick = { currentScreen = "community_detail" }
                    )
                }
                "voice_room" -> {
                    VoiceRoomScreen(
                        channelName = selectedChannelName,
                        currentUser = currentUser,
                        onDisconnect = { currentScreen = "community_detail" }
                    )
                }
                "profile" -> {
                    ProfileScreen(
                        user = currentUser,
                        onCommunityClick = { comm ->
                            val exactComm = commDb.getCommunityById(comm.id) ?: comm
                            selectedCommunity = exactComm
                            currentScreen = "community_detail"
                        },
                        onLogoutClick = {
                            sessionManager.logout()
                            currentScreen = "login"
                        }
                    )
                }
            }
        }
    }
}
