package com.hobbyhub.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.hobbyhub.model.User
import com.hobbyhub.ui.theme.*
import com.hobbyhub.webrtc.*
import org.webrtc.AudioTrack

data class VoiceParticipant(
    val id: String,
    val name: String,
    val avatarInitial: String,
    val isSpeaking: Boolean,
    val isMuted: Boolean,
    val isVirtual: Boolean = false,
    val audioTrack: AudioTrack? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceRoomScreen(
    channelName: String,
    currentUser: User,
    onDisconnect: () -> Unit
) {
    val context = LocalContext.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    
    var isMuted by remember { mutableStateOf(false) }
    var isDeafened by remember { mutableStateOf(false) }
    var hasMicPermission by remember { mutableStateOf(
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    ) }

    val participants = remember {
        mutableStateListOf(
            VoiceParticipant(
                id = currentUser.id,
                name = currentUser.displayName + " (Anda)",
                avatarInitial = currentUser.displayName.take(1).ifBlank { "U" }.uppercase(),
                isSpeaking = !isMuted,
                isMuted = isMuted
            )
        )
    }

    var webRtcClient by remember { mutableStateOf<WebRtcClient?>(null) }
    var signalingClient by remember { mutableStateOf<SignalingClient?>(null) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasMicPermission = isGranted
    }

    fun updateOrAddParticipant(id: String, name: String?, track: AudioTrack? = null, isSpeaking: Boolean = false) {
        if (id == currentUser.id) return // Don't overwrite self display name with remote signals

        val displayName = name ?: "Anggota ${id.takeLast(4)}"
        val avatarInitial = displayName.take(1).ifBlank { "U" }.uppercase()
        val index = participants.indexOfFirst { it.id == id }

        if (index != -1) {
            val current = participants[index]
            participants[index] = current.copy(
                name = displayName,
                avatarInitial = avatarInitial,
                audioTrack = track ?: current.audioTrack,
                isSpeaking = if (track != null) isSpeaking else current.isSpeaking
            )
        } else {
            participants.add(
                VoiceParticipant(
                    id = id,
                    name = displayName,
                    avatarInitial = avatarInitial,
                    isSpeaking = isSpeaking,
                    isMuted = false,
                    audioTrack = track
                )
            )
        }
    }

    DisposableEffect(hasMicPermission) {
        if (!hasMicPermission) {
            launcher.launch(Manifest.permission.RECORD_AUDIO)
            return@DisposableEffect onDispose {}
        }

        val sigClient = SignalingClient(
            userId = currentUser.id,
            userName = currentUser.displayName,
            roomId = channelName,
            listener = object : SignalingListener {
                override fun onConnectionEstablished() {}
                
                override fun onOfferReceived(senderId: String, senderName: String?, sdp: String) {
                    mainHandler.post {
                        updateOrAddParticipant(senderId, senderName)
                    }
                    webRtcClient?.handleOfferReceived(senderId, sdp)
                }

                override fun onAnswerReceived(senderId: String, sdp: String) {
                    webRtcClient?.handleAnswerReceived(senderId, sdp)
                }

                override fun onIceCandidateReceived(senderId: String, candidate: String, sdpMid: String, sdpMLineIndex: Int) {
                    webRtcClient?.handleIceCandidateReceived(senderId, candidate, sdpMid, sdpMLineIndex)
                }

                override fun onUserJoined(senderId: String, senderName: String?) {
                    mainHandler.post {
                        updateOrAddParticipant(senderId, senderName)
                    }
                    webRtcClient?.handleUserJoined(senderId)
                }

                override fun onUserLeft(senderId: String) {
                    mainHandler.post {
                        participants.removeAll { it.id == senderId }
                    }
                    webRtcClient?.handleUserLeft(senderId)
                }
            }
        )
        signalingClient = sigClient

        val rtcClient = WebRtcClient(
            context = context,
            userId = currentUser.id,
            signalingClient = sigClient,
            listener = object : WebRtcListener {
                override fun onRemoteAudioTrackAdded(userId: String, track: AudioTrack) {
                    mainHandler.post {
                        updateOrAddParticipant(userId, null, track = track, isSpeaking = true)
                    }
                }

                override fun onRemoteAudioTrackRemoved(userId: String) {
                    mainHandler.post {
                        val index = participants.indexOfFirst { it.id == userId }
                        if (index != -1) {
                            participants[index] = participants[index].copy(audioTrack = null, isSpeaking = false)
                        }
                    }
                }

                override fun onError(error: String) {}
            }
        )
        webRtcClient = rtcClient

        sigClient.connect()

        onDispose {
            sigClient.disconnect()
            rtcClient.disconnect()
        }
    }

    LaunchedEffect(isMuted) {
        webRtcClient?.setMicrophoneMute(isMuted)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = "🔊 $channelName", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(text = "${participants.size} Peserta Terhubung", color = SecondaryTurquoise, fontSize = 12.sp)
                    }
                },
                actions = {
                    IconButton(onClick = onDisconnect) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
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
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        isMuted = !isMuted
                        val selfIndex = participants.indexOfFirst { it.id == currentUser.id }
                        if (selfIndex != -1) {
                            participants[selfIndex] = participants[selfIndex].copy(isMuted = isMuted, isSpeaking = !isMuted)
                        }
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .background(if (isMuted) TertiaryCoral else SurfaceCard, CircleShape)
                        .border(1.dp, BorderDark, CircleShape)
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Mute Mic",
                        tint = TextPrimary
                    )
                }

                IconButton(
                    onClick = { isDeafened = !isDeafened },
                    modifier = Modifier
                        .size(56.dp)
                        .background(if (isDeafened) TertiaryCoral else SurfaceCard, CircleShape)
                        .border(1.dp, BorderDark, CircleShape)
                ) {
                    Icon(
                        imageVector = if (isDeafened) Icons.Default.HeadsetOff else Icons.Default.Headset,
                        contentDescription = "Deafen Audio",
                        tint = TextPrimary
                    )
                }

                Button(
                    onClick = onDisconnect,
                    colors = ButtonDefaults.buttonColors(containerColor = TertiaryCoral),
                    shape = CircleShape,
                    modifier = Modifier.height(56.dp)
                ) {
                    Icon(Icons.Default.CallEnd, contentDescription = "Leave Voice")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Keluar", fontWeight = FontWeight.Bold)
                }
            }
        },
        containerColor = ObsidianBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!hasMicPermission) {
                Text("Memerlukan izin mikrofon untuk Voice Chat.", color = TertiaryCoral)
            }
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(items = participants, key = { participant -> participant.id }) { participant ->
                    VoiceParticipantCard(participant = participant)
                }
            }
        }
    }
}

@Composable
fun VoiceParticipantCard(participant: VoiceParticipant) {
    val borderColor = if (participant.isSpeaking) SecondaryTurquoise else BorderDark
    val borderThickness = if (participant.isSpeaking) 3.dp else 1.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(borderThickness, borderColor, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .border(
                        width = if (participant.isSpeaking) 3.dp else 0.dp,
                        color = SecondaryTurquoise,
                        shape = CircleShape
                    )
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(if (participant.isVirtual) SecondaryTurquoise else PrimaryViolet),
                contentAlignment = Alignment.Center
            ) {
                Text(text = participant.avatarInitial, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = participant.name,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1
                )
                if (participant.isMuted) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.MicOff,
                        contentDescription = "Muted",
                        tint = TertiaryCoral,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Text(
                text = if (participant.audioTrack != null) "🔊 Terhubung (Suara Aktif)" else if (participant.isSpeaking) "🎙️ Sedang Bicara..." else if (participant.isMuted) "🔇 Muted" else "Mendengarkan",
                color = if (participant.isSpeaking || participant.audioTrack != null) SecondaryTurquoise else TextMuted,
                fontSize = 11.sp
            )
        }
    }
}
