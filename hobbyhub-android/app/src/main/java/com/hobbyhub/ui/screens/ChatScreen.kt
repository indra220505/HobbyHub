package com.hobbyhub.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hobbyhub.data.local.ChatLocalDatabaseManager
import com.hobbyhub.data.local.UserSessionManager
import com.hobbyhub.model.ChatMessage
import com.hobbyhub.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    channelName: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val chatDb = remember { ChatLocalDatabaseManager(context) }
    val sessionManager = remember { UserSessionManager(context) }
    val currentUser = remember { sessionManager.getUser() }

    var messageText by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf(*chatDb.getMessagesForChannel(channelName).toTypedArray()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "# $channelName", color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.PushPin, contentDescription = "Pinned", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ObsidianBg)
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceCard)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Add, contentDescription = "Attach", tint = TextMuted)
                }
                TextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    placeholder = { Text("Ketik pesan di #$channelName...", color = TextMuted) },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = ObsidianBg,
                        unfocusedContainerColor = ObsidianBg,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            val newMsg = ChatMessage(
                                id = "msg_${System.currentTimeMillis()}",
                                senderName = currentUser.displayName,
                                senderAvatar = currentUser.displayName.take(1),
                                senderBadge = currentUser.roleBadge,
                                content = messageText,
                                timestamp = "Baru saja"
                            )
                            messages.add(newMsg)
                            chatDb.saveMessageToChannel(channelName, newMsg)
                            messageText = ""
                        }
                    },
                    modifier = Modifier.background(PrimaryViolet, CircleShape)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = TextPrimary)
                }
            }
        },
        containerColor = ObsidianBg
    ) { innerPadding ->
        if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "💬", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Selamat Datang di #$channelName",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Belum ada pesan di channel ini. Ketik dan kirim pesan pertamamu!",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages) { msg ->
                    ChatMessageBubble(msg)
                }
            }
        }
    }
}

@Composable
fun ChatMessageBubble(msg: ChatMessage) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(PrimaryViolet),
            contentAlignment = Alignment.Center
        ) {
            Text(text = msg.senderAvatar, color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = msg.senderName, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(6.dp))
                msg.senderBadge?.let { badge ->
                    Surface(
                        color = Color(android.graphics.Color.parseColor(badge.colorHex)).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = badge.name,
                            color = Color(android.graphics.Color.parseColor(badge.colorHex)),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = msg.timestamp, color = TextMuted, fontSize = 10.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = msg.content, color = TextPrimary, fontSize = 14.sp)

            msg.codeSnippet?.let { code ->
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderDark))
                ) {
                    Text(
                        text = code,
                        color = SecondaryTurquoise,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}
