package com.hobbyhub.ui.screens

import android.widget.Toast
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hobbyhub.data.local.CommunityRegistryManager
import com.hobbyhub.data.local.UserSessionManager
import com.hobbyhub.model.Channel
import com.hobbyhub.model.ChannelType
import com.hobbyhub.model.Community
import com.hobbyhub.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityDetailScreen(
    community: Community,
    onChannelClick: (Channel) -> Unit,
    onBackClick: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onCommunityUpdated: (Community) -> Unit = {},
    onCommunityDeleted: () -> Unit = {}
) {
    val context = LocalContext.current
    val commDb = remember { CommunityRegistryManager(context) }
    val sessionManager = remember { UserSessionManager(context) }
    val currentUser = remember { sessionManager.getUser() }

    var currentCommunity by remember { mutableStateOf(community) }
    var showAddChannelDialog by remember { mutableStateOf(false) }
    var showLeaveCommunityDialog by remember { mutableStateOf(false) }

    val isOwner = remember(currentUser, currentCommunity) {
        commDb.isOwnerOfCommunity(currentCommunity.id, currentUser.username)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentCommunity.name, color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    if (isOwner) {
                        IconButton(onClick = { showAddChannelDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Tambah Channel", tint = PrimaryViolet)
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Pengaturan Komunitas", tint = TextPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ObsidianBg)
            )
        },
        containerColor = ObsidianBg
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. HEADER KOMUNITAS MODERN (Sesua Desain Screenshot)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Container Logo/Ikon Terpisah Berbentuk Kotak Rounded
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ObsidianBg)
                                    .border(1.dp, BorderDark, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = currentCommunity.iconEmoji, fontSize = 28.sp)
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Details (Nama, Jumlah Member, Role Tag)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentCommunity.name,
                                    color = TextPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${currentCommunity.memberCount} • Owner: @indra_owner",
                                    color = TextMuted,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    color = if (isOwner) PrimaryViolet.copy(alpha = 0.2f) else SecondaryTurquoise.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (isOwner) "Owner 👑" else "Member",
                                        color = if (isOwner) PrimaryViolet else SecondaryTurquoise,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(text = currentCommunity.description, color = TextPrimary, fontSize = 13.sp, lineHeight = 18.sp)
                    }
                }
            }

            // DAFTAR CHANNEL SECTION
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "DAFTAR CHANNEL (${currentCommunity.channels.size})", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    if (isOwner) {
                        TextButton(onClick = { showAddChannelDialog = true }) {
                            Text("+ Tambah Channel", color = PrimaryViolet, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            items(currentCommunity.channels) { channel ->
                ChannelRowItem(
                    channel = channel,
                    isAdmin = isOwner,
                    onClick = { onChannelClick(channel) },
                    onDelete = {
                        if (isOwner) {
                            val updatedList = commDb.deleteChannel(currentCommunity.id, channel.id)
                            val updatedComm = updatedList.find { it.id == currentCommunity.id }
                            if (updatedComm != null) {
                                currentCommunity = updatedComm
                                onCommunityUpdated(updatedComm)
                                Toast.makeText(context, "Channel #${channel.name} berhasil dihapus oleh Owner", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }

            // 2. TOMBOL KELUAR KOMUNITAS (BOTTOM BUTTON Sesua Desain Screenshot)
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (isOwner) {
                            Toast.makeText(context, "Anda adalah Owner komunitas ini. Anda HARUS mentransfer kepemilikan kepada anggota lain atau menghapus komunitas sebelum bisa keluar.", Toast.LENGTH_LONG).show()
                        } else {
                            showLeaveCommunityDialog = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TertiaryCoral.copy(alpha = 0.15f), contentColor = TertiaryCoral),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(TertiaryCoral)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null, tint = TertiaryCoral, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Keluar Komunitas", fontWeight = FontWeight.Bold, color = TertiaryCoral)
                }
            }
        }

        // Add Channel Dialog
        if (showAddChannelDialog) {
            AddChannelDialog(
                onDismiss = { showAddChannelDialog = false },
                onAdd = { name, isVoice ->
                    val type = if (isVoice) ChannelType.VOICE else ChannelType.TEXT_CHAT
                    val updatedList = commDb.addChannelToCommunity(currentCommunity.id, name, type)
                    val updatedComm = updatedList.find { it.id == currentCommunity.id }
                    if (updatedComm != null) {
                        currentCommunity = updatedComm
                        onCommunityUpdated(updatedComm)
                    }
                    showAddChannelDialog = false
                }
            )
        }

        // Leave Community Dialog
        if (showLeaveCommunityDialog) {
            AlertDialog(
                onDismissRequest = { showLeaveCommunityDialog = false },
                containerColor = SurfaceCard,
                icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = TertiaryCoral, modifier = Modifier.size(36.dp)) },
                title = { Text("Konfirmasi Keluar Komunitas", color = TextPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "Apakah kamu yakin ingin keluar dari komunitas \"${currentCommunity.name}\"?",
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            sessionManager.leaveCommunity(currentCommunity.id)
                            showLeaveCommunityDialog = false
                            Toast.makeText(context, "Kamu telah keluar dari komunitas", Toast.LENGTH_SHORT).show()
                            onCommunityDeleted()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TertiaryCoral)
                    ) {
                        Text("Keluar Sekarang", color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLeaveCommunityDialog = false }) { Text("Batal", color = TextMuted) }
                }
            )
        }
    }
}

@Composable
fun ChannelRowItem(
    channel: Channel,
    isAdmin: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = when (channel.type) {
                ChannelType.TEXT_CHAT -> Icons.Default.Tag
                ChannelType.FEED_DISCUSSION -> Icons.Default.Article
                ChannelType.VOICE -> Icons.Default.VolumeUp
                ChannelType.ANNOUNCEMENT -> Icons.Default.Campaign
            }
            val iconTint = if (channel.type == ChannelType.VOICE) SecondaryTurquoise else PrimaryViolet

            Icon(imageVector = icon, contentDescription = null, tint = iconTint)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = channel.name, color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                Text(text = channel.topic, color = TextMuted, fontSize = 12.sp, maxLines = 1)
            }

            if (isAdmin) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Hapus Channel (Owner Only)", tint = TertiaryCoral.copy(alpha = 0.8f))
                }
            } else {
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddChannelDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, isVoice: Boolean) -> Unit
) {
    var channelName by remember { mutableStateOf("") }
    var isVoiceChannel by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        title = {
            Text(text = "Buat Channel Baru (Owner)", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = channelName,
                    onValueChange = { channelName = it },
                    label = { Text("Nama Channel (misal: general-chat)", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryViolet, unfocusedBorderColor = BorderDark, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )

                Text(text = "Tipe Channel:", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = !isVoiceChannel,
                        onClick = { isVoiceChannel = false },
                        colors = RadioButtonDefaults.colors(selectedColor = PrimaryViolet)
                    )
                    Text("💬 Text Chat Channel", color = TextPrimary, fontSize = 13.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = isVoiceChannel,
                        onClick = { isVoiceChannel = true },
                        colors = RadioButtonDefaults.colors(selectedColor = SecondaryTurquoise)
                    )
                    Text("🔊 Voice Chat Lounge", color = TextPrimary, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (channelName.isNotBlank()) {
                        onAdd(channelName, isVoiceChannel)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet)
            ) {
                Text("Buat Channel", color = TextPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = TextMuted)
            }
        }
    )
}
