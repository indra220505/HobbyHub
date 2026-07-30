package com.hobbyhub.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hobbyhub.data.local.CommunityRegistryManager
import com.hobbyhub.model.Badge
import com.hobbyhub.model.Community
import com.hobbyhub.model.Quest
import com.hobbyhub.model.User
import com.hobbyhub.repository.MockDataRepository
import com.hobbyhub.ui.theme.*

@Composable
fun ProfileScreen(
    user: User = MockDataRepository.currentUser,
    onCommunityClick: (Community) -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onDeleteAccountClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val commDb = remember { CommunityRegistryManager(context) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Memfilter HANYA komunitas yang DIBUAT (DIMILIKI) oleh user saat ini
    val createdCommunities = remember(user, commDb) {
        commDb.getCommunitiesCreatedBy(user.username)
    }

    val quests = MockDataRepository.sampleQuests

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // User Profile Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Glowing Avatar Level Ring
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .border(3.dp, PrimaryViolet, CircleShape)
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(PrimaryViolet),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = user.displayName.take(1), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = user.displayName, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(text = "@${user.username}", color = TextMuted, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))

                    // Role Badge (Default: Member)
                    Surface(
                        color = SecondaryTurquoise.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = user.roleBadge?.name ?: "Member",
                            color = SecondaryTurquoise,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = user.bio, color = TextMuted, fontSize = 13.sp)

                    Spacer(modifier = Modifier.height(16.dp))

                    // XP Progress Bar
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "LEVEL ${user.level}", color = PrimaryViolet, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(text = "${user.currentXp} / ${user.maxXp} XP", color = TextMuted, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { user.currentXp.toFloat() / user.maxXp.toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = PrimaryViolet,
                            trackColor = ObsidianBg
                        )
                    }
                }
            }
        }

        // Section 3: KOMUNITAS DIBUAT (HANYA KOMUNITAS YANG DIBUAT USER)
        item {
            Text(text = "KOMUNITASKU (DIBUAT)", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        if (createdCommunities.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Groups, contentDescription = null, tint = TextMuted, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Kamu belum membuat komunitas.",
                            color = TextMuted,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        } else {
            items(createdCommunities) { comm ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCommunityClick(comm) },
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = comm.iconEmoji, fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = comm.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(text = "${comm.category} • ${comm.memberCount}", color = SecondaryTurquoise, fontSize = 12.sp)
                        }
                        Surface(color = PrimaryViolet.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp)) {
                            Text("Owner 👑", color = PrimaryViolet, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(6.dp))
                        }
                    }
                }
            }
        }

        // Badges Section
        item {
            Text(text = "BADGE & ACHIEVEMENTS", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    val badgesToShow = if (user.badges.isEmpty()) MockDataRepository.currentUser.badges else user.badges
                    badgesToShow.chunked(2).forEach { rowBadges ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowBadges.forEach { badge ->
                                BadgeItemCard(badge = badge, modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        // Guild Quests Section
        item {
            Text(text = "DAILY GUILD QUESTS", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        items(quests) { quest ->
            QuestRowItem(quest = quest)
        }

        // Logout Button
        item {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onLogoutClick,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Keluar dari Akun")
            }

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { showDeleteDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = TertiaryCoral),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Hapus Akun Permanen", color = Color.White)
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = SurfaceCard,
            title = { Text("Hapus Akun Permanen", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Tindakan ini tidak dapat dibatalkan. Semua data kamu, termasuk pesan dan profil, akan dihapus. Yakin ingin melanjutkan?", color = TextMuted) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteAccountClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TertiaryCoral)
                ) {
                    Text("Hapus", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Batal", color = TextMuted)
                }
            }
        )
    }
}

@Composable
fun BadgeItemCard(badge: Badge, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = ObsidianBg),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = badge.iconEmoji, fontSize = 24.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(text = badge.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(text = badge.description, color = TextMuted, fontSize = 10.sp, maxLines = 1)
            }
        }
    }
}

@Composable
fun QuestRowItem(quest: Quest) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (quest.isCompleted) Icons.Default.CheckCircle else Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = if (quest.isCompleted) SecondaryTurquoise else PrimaryViolet
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = quest.title, color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { quest.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = SecondaryTurquoise,
                    trackColor = ObsidianBg
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = "+${quest.rewardXp} XP", color = PrimaryViolet, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}
