package com.hobbyhub.ui.screens

import android.os.Handler
import android.os.Looper
import android.util.Log
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
    val senderName: String = "",
    val senderAvatar: String = "U",
    val senderBadge: String = "Member",
    val content: String = "",
    val timestamp: String = "Baru saja"
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
    val gson = remember { Gson() }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val coroutineScope = rememberCoroutineScope()

    var messageText by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf(*chatDb.getMessagesForChannel(channelName).toTypedArray()) }
    var webSocket by remember { mutableStateOf<WebSocket?>(null) }
    var isConnected by remember { mutableStateOf(false) }

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
                                            val safeAvatar = item.senderAvatar.ifBlank { item.senderName.take(1).ifBlank { "U" } }
                                            val msg = ChatMessage(
                                                id = item.id.ifBlank { "msg_${System.currentTimeMillis()}" },
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
                            val safeAvatar = payload.senderAvatar.ifBlank { payload.senderName.take(1).ifBlank { "U" } }
                            val newMsg = ChatMessage(
                                id = payload.id.ifBlank { "msg_${System.currentTimeMillis()}" },
                                senderName = payload.senderName.ifBlank { "Member" },
                                senderAvatar = safeAvatar,
                                senderBadge = RoleBadge(payload.senderBadge.ifBlank { "Member" }, "#6C5CE7"),
                                content = payload.content,
                                timestamp = payload.timestamp.ifBlank { "Baru saja" }
                            )

                            // CRITICAL: Post to Main Thread safely for Compose Recomposition!
                            mainHandler.post {
                                if (messages.none { it.id == newMsg.id }) {
                                    messages.add(newMsg)
                                    chatDb.saveMessageToChannel(channelName, newMsg)
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
                            val msgId = "msg_${System.currentTimeMillis()}"
                            val avatarInitial = currentUser.displayName.take(1).ifBlank { "U" }
                            val newMsg = ChatMessage(
                                id = msgId,
                                senderName = currentUser.displayName.ifBlank { "Member" },
                                senderAvatar = avatarInitial,
                                senderBadge = currentUser.roleBadge,
                                content = messageText,
                                timestamp = "Baru saja"
                            )

                            // Add locally on Main thread safely
                            if (messages.none { it.id == newMsg.id }) {
                                messages.add(newMsg)
                                chatDb.saveMessageToChannel(channelName, newMsg)
                            }

                            // Send via WebSocket broadcast safely
                            try {
                                val payload = WsPayload(
                                    id = msgId,
                                    channelName = channelName,
                                    senderName = currentUser.displayName.ifBlank { "Member" },
                                    senderAvatar = avatarInitial,
                                    senderBadge = currentUser.roleBadge?.name ?: "Member",
                                    content = messageText,
                                    timestamp = "Baru saja"
                                )
                                webSocket?.send(gson.toJson(payload))
                            } catch (ex: Exception) {
                                Log.e("ChatScreen", "Error sending WebSocket message", ex)
                            }

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
                Spacer(modifier = Modifier.width(6.dp))
                msg.senderBadge?.let { badge ->
                    val badgeColor = remember(badge.colorHex) {
                        try {
                            if (!badge.colorHex.isNull meOrBlank() && badge.colorHex.startsWith("#")) {
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

private fun String?.isNull meOrBlank(): Boolean = this == null || this.trim().isEmpty()
