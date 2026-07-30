package com.hobbyhub.ui.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
    val activity = context as? Activity
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    
    var isMuted by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(true) }
    var callVolume by remember { mutableStateOf(0.85f) }
    var showVolumeControl by remember { mutableStateOf(false) }
    var isLeaving by remember { mutableStateOf(false) }

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

    // Set hardware volume keys to control STREAM_VOICE_CALL while in VoiceRoom
    DisposableEffect(Unit) {
        activity?.volumeControlStream = AudioManager.STREAM_VOICE_CALL
        onDispose {
            activity?.volumeControlStream = AudioManager.USE_DEFAULT_STREAM_TYPE
        }
    }

    fun safeLeaveRoom() {
        if (isLeaving) return
        isLeaving = true

        try {
            signalingClient?.disconnect()
        } catch (e: Exception) {
            Log.e("VoiceRoomScreen", "Error disconnecting signaling", e)
        }

        try {
            webRtcClient?.disconnect()
        } catch (e: Exception) {
            Log.e("VoiceRoomScreen", "Error disconnecting WebRTC", e)
        }

        mainHandler.post {
            onDisconnect()
        }
    }

    fun updateOrAddParticipant(id: String, name: String?, track: AudioTrack? = null, isSpeaking: Boolean = false) {
        if (isLeaving || id == currentUser.id) return

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
                    if (isLeaving) return
                    mainHandler.post {
                        updateOrAddParticipant(senderId, senderName)
                    }
                    webRtcClient?.handleOfferReceived(senderId, sdp)
                }

                override fun onAnswerReceived(senderId: String, sdp: String) {
                    if (isLeaving) return
                    webRtcClient?.handleAnswerReceived(senderId, sdp)
                }

                override fun onIceCandidateReceived(senderId: String, candidate: String, sdpMid: String, sdpMLineIndex: Int) {
                    if (isLeaving) return
                    webRtcClient?.handleIceCandidateReceived(senderId, candidate, sdpMid, sdpMLineIndex)
                }

                override fun onUserJoined(senderId: String, senderName: String?) {
                    if (isLeaving) return
                    mainHandler.post {
                        updateOrAddParticipant(senderId, senderName)
                    }
                    webRtcClient?.handleUserJoined(senderId)
                }

                override fun onUserLeft(senderId: String) {
                    if (isLeaving) return
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
                    if (isLeaving) return
                    mainHandler.post {
                        updateOrAddParticipant(userId, null, track = track, isSpeaking = true)
                    }
                }

                override fun onRemoteAudioTrackRemoved(userId: String) {
                    if (isLeaving) return
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
            if (!isLeaving) {
                sigClient.disconnect()
                rtcClient.disconnect()
            }
        }
    }

    LaunchedEffect(isMuted) {
        webRtcClient?.setMicrophoneMute(isMuted)
    }

    LaunchedEffect(isSpeakerOn) {
        webRtcClient?.setSpeakerphoneOn(isSpeakerOn)
    }

    LaunchedEffect(callVolume) {
        webRtcClient?.setCallVolume(callVolume)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = "🔊 $channelName", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(text = "${participants.size} Peserta • Mode Telepon Panggilan", color = SecondaryTurquoise, fontSize = 12.sp)
                    }
                },
                actions = {
                    IconButton(onClick = { safeLeaveRoom() }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ObsidianBg)
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceCard)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Animated In-Call Volume Slider Card
                AnimatedVisibility(visible = showVolumeControl) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = ObsidianBg),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🔊 Volume Panggilan Suara",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "${(callVolume * 100).toInt()}%",
                                    color = SecondaryTurquoise,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Slider(
                                value = callVolume,
                                onValueChange = { callVolume = it },
                                valueRange = 0.0f..1.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = SecondaryTurquoise,
                                    activeTrackColor = SecondaryTurquoise,
                                    inactiveTrackColor = BorderDark
                                )
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mute Mic Button
                    IconButton(
                        onClick = {
                            isMuted = !isMuted
                            val selfIndex = participants.indexOfFirst { it.id == currentUser.id }
                            if (selfIndex != -1) {
                                participants[selfIndex] = participants[selfIndex].copy(isMuted = isMuted, isSpeaking = !isMuted)
                            }
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .background(if (isMuted) TertiaryCoral else SurfaceCard, CircleShape)
                            .border(1.dp, BorderDark, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Mute Mic",
                            tint = TextPrimary
                        )
                    }

                    // Speakerphone Toggle Button
                    IconButton(
                        onClick = { isSpeakerOn = !isSpeakerOn },
                        modifier = Modifier
                            .size(52.dp)
                            .background(if (isSpeakerOn) PrimaryViolet else SurfaceCard, CircleShape)
                            .border(1.dp, BorderDark, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                            contentDescription = "Speaker Mode",
                            tint = TextPrimary
                        )
                    }

                    // In-Call Volume Slider Toggle Button
                    IconButton(
                        onClick = { showVolumeControl = !showVolumeControl },
                        modifier = Modifier
                            .size(52.dp)
                            .background(if (showVolumeControl) SecondaryTurquoise else SurfaceCard, CircleShape)
                            .border(1.dp, BorderDark, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Volume Slider",
                            tint = TextPrimary
                        )
                    }

                    // Leave / End Call Button
                    Button(
                        onClick = { safeLeaveRoom() },
                        colors = ButtonDefaults.buttonColors(containerColor = TertiaryCoral),
                        shape = CircleShape,
                        modifier = Modifier.height(52.dp)
                    ) {
                        Icon(Icons.Default.CallEnd, contentDescription = "Leave Voice")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Keluar", fontWeight = FontWeight.Bold)
                    }
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
