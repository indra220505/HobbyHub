package com.hobbyhub.ui.screens

import android.widget.Toast
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
import com.hobbyhub.model.Channel
import com.hobbyhub.model.ChannelType
import com.hobbyhub.model.Community
import com.hobbyhub.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelManagerScreen(
    community: Community,
    onBackClick: () -> Unit,
    onCommunityUpdated: (Community) -> Unit
) {
    val context = LocalContext.current
    val commDb = remember { CommunityRegistryManager(context) }

    var currentCommunity by remember { mutableStateOf(community) }
    var channels by remember { mutableStateOf(community.channels) }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingChannel by remember { mutableStateOf<Channel?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kelola Channel", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Tambah Channel", tint = PrimaryViolet)
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "DAFTAR CHANNEL (${channels.size})", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { showAddDialog = true }) {
                        Text("+ Buat Channel Baru", color = PrimaryViolet, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            items(channels) { channel ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(10.dp)
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
                            Text(text = "#${channel.name}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(text = channel.topic, color = TextMuted, fontSize = 12.sp, maxLines = 1)
                        }

                        IconButton(onClick = { editingChannel = channel }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Channel", tint = PrimaryViolet)
                        }

                        IconButton(
                            onClick = {
                                if (channels.size > 1) {
                                    val updatedList = commDb.deleteChannel(currentCommunity.id, channel.id)
                                    val updatedComm = updatedList.find { it.id == currentCommunity.id }
                                    if (updatedComm != null) {
                                        currentCommunity = updatedComm
                                        channels = updatedComm.channels
                                        onCommunityUpdated(updatedComm)
                                        Toast.makeText(context, "Channel #${channel.name} berhasil dihapus!", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Komunitas wajib memiliki minimal 1 channel!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Hapus Channel", tint = TertiaryCoral)
                        }
                    }
                }
            }
        }

        // Add Channel Dialog
        if (showAddDialog) {
            AddEditChannelModalDialog(
                title = "Buat Channel Baru",
                initialName = "",
                initialTopic = "",
                initialType = ChannelType.TEXT_CHAT,
                onDismiss = { showAddDialog = false },
                onSave = { name, topic, type ->
                    val updatedList = commDb.addChannelToCommunity(currentCommunity.id, name, type)
                    val updatedComm = updatedList.find { it.id == currentCommunity.id }
                    if (updatedComm != null) {
                        currentCommunity = updatedComm
                        channels = updatedComm.channels
                        onCommunityUpdated(updatedComm)
                        Toast.makeText(context, "Channel #$name berhasil dibuat!", Toast.LENGTH_SHORT).show()
                    }
                    showAddDialog = false
                }
            )
        }

        // Edit Channel Dialog
        if (editingChannel != null) {
            val ch = editingChannel!!
            AddEditChannelModalDialog(
                title = "Edit Channel #${ch.name}",
                initialName = ch.name,
                initialTopic = ch.topic,
                initialType = ch.type,
                onDismiss = { editingChannel = null },
                onSave = { name, topic, type ->
                    val updatedChannels = channels.map {
                        if (it.id == ch.id) it.copy(name = name, topic = topic, type = type) else it
                    }
                    currentCommunity = currentCommunity.copy(channels = updatedChannels)
                    channels = updatedChannels
                    onCommunityUpdated(currentCommunity)
                    Toast.makeText(context, "Channel #$name berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                    editingChannel = null
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditChannelModalDialog(
    title: String,
    initialName: String,
    initialTopic: String,
    initialType: ChannelType,
    onDismiss: () -> Unit,
    onSave: (name: String, topic: String, type: ChannelType) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var topic by remember { mutableStateOf(initialTopic) }
    var selectedType by remember { mutableStateOf(initialType) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        title = { Text(title, color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Channel (misal: general-chat)", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryViolet, unfocusedBorderColor = BorderDark, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )

                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    label = { Text("Topik / Deskripsi Channel", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryViolet, unfocusedBorderColor = BorderDark, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )

                Text("Tipe Channel:", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = selectedType == ChannelType.TEXT_CHAT,
                        onClick = { selectedType = ChannelType.TEXT_CHAT },
                        colors = RadioButtonDefaults.colors(selectedColor = PrimaryViolet)
                    )
                    Text("💬 Text Chat", color = TextPrimary, fontSize = 13.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = selectedType == ChannelType.VOICE,
                        onClick = { selectedType = ChannelType.VOICE },
                        colors = RadioButtonDefaults.colors(selectedColor = SecondaryTurquoise)
                    )
                    Text("🔊 Voice Lounge", color = TextPrimary, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name, topic, selectedType)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet)
            ) {
                Text("Simpan", color = TextPrimary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal", color = TextMuted) }
        }
    )
}
