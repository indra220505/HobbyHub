package com.hobbyhub.ui.screens

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hobbyhub.BuildConfig
import com.hobbyhub.data.local.ChatLocalDatabaseManager
import com.hobbyhub.data.local.UserSessionManager
import com.hobbyhub.model.ChatMessage
import com.hobbyhub.model.RoleBadge
import com.hobbyhub.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import java.io.IOException

data class WsPayload(
    val id: String = "",
    val channelName: String = "",
    val senderId: String = "",
    val senderUsername: String = "",
    val senderName: String = "",
    val senderAvatar: String = "U",
    val senderBadge: String = "Member",
    val content: String = "",
    val timestamp: String = "Baru saja",
    val type: String = "CHAT" // "CHAT" or "DELETE"
)

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
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val gson = remember { Gson() }
    val coroutineScope = rememberCoroutineScope()

    var messageText by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf(*chatDb.getMessagesForChannel(channelName).toTypedArray()) }
    var webSocket by remember { mutableStateOf<WebSocket?>(null) }
    var isConnected by remember { mutableStateOf(false) }

    var messageToDelete by remember { mutableStateOf<ChatMessage?>(null) }

    fun deleteMessageLocallyAndRemote(msg: ChatMessage) {
        // 1. Remove from local memory list and local database
        messages.removeAll { it.id == msg.id }
        chatDb.deleteMessageFromChannel(channelName, msg.id)

        // 2. Send DELETE payload over WebSocket to broadcast deletion to active peers
        try {
            val deletePayload = WsPayload(
                id = msg.id,
                channelName = channelName,
                type = "DELETE"
            )
            webSocket?.send(gson.toJson(deletePayload))
        } catch (e: Exception) {
            Log.e("ChatScreen", "Error sending WebSocket delete message", e)
        }

        // 3. Send DELETE HTTP REST request to Railway backend for database persistence
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val httpUrl = BuildConfig.API_BASE_URL + "api/chat/history/" + channelName + "/" + msg.id
                val client = OkHttpClient.Builder()
                    .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                val request = Request.Builder().url(httpUrl).delete().build()
                client.newCall(request).execute()
            } catch (e: Exception) {
                Log.e("ChatScreen", "Error sending REST delete message", e)
            }
        }
    }

    // Fetch initial chat history safely from Railway REST API
    LaunchedEffect(channelName) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val httpUrl = BuildConfig.API_BASE_URL + "api/chat/history/" + channelName
                val client = OkHttpClient.Builder()
                    .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                val request = Request.Builder().url(httpUrl).build()
                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.e("ChatScreen", "Failed to fetch chat history safely: ${e.message}")
                    }

                    override fun onResponse(call: Call, response: Response) {
                        try {
                            response.body?.string()?.let { json ->
                                if (json.isNotBlank() && json.startsWith("[")) {
                                    val listType = object : TypeToken<List<WsPayload>>() {}.type
                                    val remoteHistory: List<WsPayload> = gson.fromJson(json, listType) ?: emptyList()
                                    mainHandler.post {
                                        remoteHistory.forEach { item ->
                                            if (item.type == "DELETE") {
                                                messages.removeAll { it.id == item.id }
                                                chatDb.deleteMessageFromChannel(channelName, item.id)
                                            } else {
                                                val safeAvatar = item.senderAvatar.ifBlank { item.senderName.take(1).ifBlank { "U" } }
                                                val msg = ChatMessage(
                                                    id = item.id.ifBlank { "msg_${System.currentTimeMillis()}" },
                                                    senderId = item.senderId,
                                                    senderUsername = item.senderUsername,
                                                    senderName = item.senderName.ifBlank { "Member" },
                                                    senderAvatar = safeAvatar,
                                                    senderBadge = RoleBadge(item.senderBadge.ifBlank { "Member" }, "#6C5CE7"),
                                                    content = item.content,
                                                    timestamp = item.timestamp.ifBlank { "Baru saja" }
                                                )
                                                if (messages.none { it.id == msg.id }) {
                                                    messages.add(msg)
                                                    chatDb.saveMessageToChannel(channelName, msg)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (ex: Exception) {
                            Log.e("ChatScreen", "Error parsing remote chat history", ex)
                        }
                    }
                })
            } catch (e: Exception) {
                Log.e("ChatScreen", "Error launching history request", e)
            }
        }
    }

    // Connect to WebSocket /chat safely when entering screen
    DisposableEffect(channelName) {
        val client = OkHttpClient.Builder()
            .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        val wsUrl = BuildConfig.WS_BASE_URL + "chat"
        val request = Request.Builder().url(wsUrl).build()

        val ws = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Log.d("ChatScreen", "Connected to Chat WebSocket: $wsUrl")
                mainHandler.post { isConnected = true }
            }

            override fun onMessage(ws: WebSocket, text: String) {
                try {
                    Log.d("ChatScreen", "WebSocket message received: $text")
                    if (text.isNotBlank() && text.startsWith("{")) {
                        val payload = gson.fromJson(text, WsPayload::class.java)
                        if (payload != null && payload.channelName.equals(channelName, ignoreCase = true)) {
                            if (payload.type == "DELETE") {
                                mainHandler.post {
                                    messages.removeAll { it.id == payload.id }
                                    chatDb.deleteMessageFromChannel(channelName, payload.id)
                                }
                            } else if (payload.type == "USER_DELETED") {
                                mainHandler.post {
                                    val deletedUserId = payload.senderId
                                    messages.forEachIndexed { index, msg ->
                                        if (msg.senderId == deletedUserId) {
                                            messages[index] = msg.copy(
                                                senderId = "",
                                                senderUsername = "",
                                                senderName = "Pengguna Dihapus",
                                                senderAvatar = "U"
                                            )
                                        }
                                    }
                                    chatDb.nullifyUserMessages(channelName, deletedUserId)
                                }
                            } else {
                                val safeAvatar = payload.senderAvatar.ifBlank { payload.senderName.take(1).ifBlank { "U" } }
                                val newMsg = ChatMessage(
                                    id = payload.id.ifBlank { "msg_${System.currentTimeMillis()}" },
                                    senderId = payload.senderId,
                                    senderUsername = payload.senderUsername,
                                    senderName = payload.senderName.ifBlank { "Member" },
                                    senderAvatar = safeAvatar,
                                    senderBadge = RoleBadge(payload.senderBadge.ifBlank { "Member" }, "#6C5CE7"),
                                    content = payload.content,
                                    timestamp = payload.timestamp.ifBlank { "Baru saja" }
                                )

                                mainHandler.post {
                                    if (messages.none { it.id == newMsg.id }) {
                                        messages.add(newMsg)
                                        chatDb.saveMessageToChannel(channelName, newMsg)
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ChatScreen", "Error parsing WebSocket message", e)
                }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.e("ChatScreen", "Chat WebSocket error: ${t.message}")
                mainHandler.post { isConnected = false }
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                mainHandler.post { isConnected = false }
            }
        })

        webSocket = ws

        onDispose {
            try {
                ws.close(1000, "Leaving screen")
            } catch (_: Exception) {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "# $channelName", color = TextPrimary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = if (isConnected) SecondaryTurquoise.copy(alpha = 0.2f) else TertiaryCoral.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (isConnected) "ONLINE" else "OFFLINE",
                                color = if (isConnected) SecondaryTurquoise else TertiaryCoral,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
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
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    placeholder = { Text("Ketik pesan di #$channelName...", color = TextMuted) },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryViolet,
                        unfocusedBorderColor = BorderDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(24.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            val textToSend = messageText
                            messageText = ""

                            val currentAvatar = currentUser.displayName.take(1).ifBlank { "U" }.uppercase()
                            val msgId = "msg_${System.currentTimeMillis()}"

                            val newMsg = ChatMessage(
                                id = msgId,
                                senderId = currentUser.id,
                                senderUsername = currentUser.username,
                                senderName = currentUser.displayName,
                                senderAvatar = currentAvatar,
                                senderBadge = currentUser.roleBadge ?: RoleBadge("Member", "#6C5CE7"),
                                content = textToSend,
                                timestamp = "Baru saja"
                            )

                            // 1. Save to local list immediately
                            messages.add(newMsg)
                            chatDb.saveMessageToChannel(channelName, newMsg)

                            // 2. Broadcast via WebSocket if connected
                            try {
                                val payload = WsPayload(
                                    id = msgId,
                                    channelName = channelName,
                                    senderId = currentUser.id,
                                    senderUsername = currentUser.username,
                                    senderName = currentUser.displayName,
                                    senderAvatar = currentAvatar,
                                    senderBadge = currentUser.roleBadge?.name ?: "Member",
                                    content = textToSend,
                                    timestamp = "Baru saja",
                                    type = "CHAT"
                                )
                                webSocket?.send(gson.toJson(payload))
                            } catch (e: Exception) {
                                Log.e("ChatScreen", "Failed to send WebSocket message", e)
                            }
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(PrimaryViolet, CircleShape)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Kirim Pesan", tint = Color.White)
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
                items(items = messages, key = { it.id }) { msg ->
                    ChatMessageBubble(
                        msg = msg,
                        isOwnerOrSelf = (msg.senderId == currentUser.id),
                        onDeleteClick = { messageToDelete = msg }
                    )
                }
            }
        }

        // Delete Confirmation Modal (CRUD Delete)
        messageToDelete?.let { targetMsg ->
            val isOwner = (targetMsg.senderId == currentUser.id)

            AlertDialog(
                onDismissRequest = { messageToDelete = null },
                containerColor = SurfaceCard,
                icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = TertiaryCoral, modifier = Modifier.size(36.dp)) },
                title = { Text(if (isOwner) "Hapus Pesan Ini?" else "Hapus Untuk Saya?", color = TextPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        if (isOwner) "Apakah kamu yakin ingin menghapus pesan ini? Pesan dapat dihapus untuk semua orang atau hanya untukmu."
                        else "Hapus pesan ini dari tampilanmu? (Pesan tetap terlihat oleh orang lain).",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    Column {
                        if (isOwner) {
                            Button(
                                onClick = {
                                    deleteMessageLocallyAndRemote(targetMsg)
                                    messageToDelete = null
                                    Toast.makeText(context, "Pesan dihapus untuk semua orang", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = TertiaryCoral),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                            ) {
                                Text("Hapus untuk Semua Orang", color = TextPrimary, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = {
                                mainHandler.post {
                                    messages.removeAll { it.id == targetMsg.id }
                                    chatDb.deleteMessageFromChannel(channelName, targetMsg.id)
                                }
                                messageToDelete = null
                                Toast.makeText(context, "Pesan dihapus untuk Anda", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ObsidianBg),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Hapus untuk Saya", color = TextPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { messageToDelete = null }) {
                        Text("Batal", color = TextMuted)
                    }
                }
            )
        }
    }
}

@Composable
fun ChatMessageBubble(
    msg: ChatMessage,
    isOwnerOrSelf: Boolean = false,
    onDeleteClick: () -> Unit = {}
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(PrimaryViolet),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = msg.senderAvatar.ifBlank { "U" },
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = msg.senderName.ifBlank { "Member" },
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                val usernameDisplay = if (msg.senderUsername.isNotBlank()) "@${msg.senderUsername}" else "@user_${msg.senderId.takeLast(4)}"
                Text(
                    text = usernameDisplay,
                    color = TextMuted,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                msg.senderBadge?.let { badge ->
                    val badgeColor = remember(badge.colorHex) {
                        try {
                            if (!badge.colorHex.isNullOrBlank() && badge.colorHex.startsWith("#")) {
                                Color(android.graphics.Color.parseColor(badge.colorHex))
                            } else {
                                SecondaryTurquoise
                            }
                        } catch (_: Exception) {
                            SecondaryTurquoise
                        }
                    }

                    Surface(
                        color = badgeColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = badge.name.ifBlank { "Member" },
                            color = badgeColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = msg.timestamp.ifBlank { "Baru saja" }, color = TextMuted, fontSize = 10.sp)

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus Pesan",
                        tint = TertiaryCoral.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = msg.content, color = TextPrimary, fontSize = 14.sp)

            msg.codeSnippet?.let { code ->
                if (code.isNotBlank()) {
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
}
