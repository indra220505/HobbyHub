package com.hobbyhub.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hobbyhub.data.local.CommunityRegistryManager
import com.hobbyhub.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModerationSettingsScreen(
    communityId: String,
    communityName: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val commDb = remember { CommunityRegistryManager(context) }

    // 1. Slow Mode Stepper state (Requirement 6)
    var slowModeIndex by remember { mutableStateOf(1) } // Default 5 detik
    val slowModeLabel = CommunityRegistryManager.SLOW_MODE_STEPS[slowModeIndex]

    // 2. Banned Keywords state
    val bannedKeywords = remember { mutableStateListOf(*commDb.getBannedKeywords(communityId).toTypedArray()) }
    var newKeywordInput by remember { mutableStateOf("") }

    // 3. Auto Moderation state
    var isAutoModEnabled by remember { mutableStateOf(true) }
    var autoModSensitivity by remember { mutableStateOf("Sedang") }

    // 4. Anti Spam Limits state
    var maxMessageRate by remember { mutableStateOf("5 Pesan / 10s") }
    var maxMentions by remember { mutableStateOf("3 Mention") }
    var allowLinks by remember { mutableStateOf(false) }

    // 5. Audit Log list
    val auditLogs = remember { commDb.getAuditLogs(communityId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan Moderasi", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
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
            // 1. SLOW MODE STEPPER CONTROLLER (Requirement 6)
            item {
                Text(text = "1. SLOW MODE CHAT STEPPER", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "Batasi Frekuensi Pengiriman Pesan Member", color = TextMuted, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Minus Button
                            IconButton(
                                onClick = {
                                    if (slowModeIndex > 0) slowModeIndex--
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(PrimaryViolet.copy(alpha = 0.2f), CircleShape)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Kurangi Slow Mode", tint = PrimaryViolet)
                            }

                            Spacer(modifier = Modifier.width(24.dp))

                            Text(
                                text = slowModeLabel,
                                color = TextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.width(24.dp))

                            // Plus Button
                            IconButton(
                                onClick = {
                                    if (slowModeIndex < CommunityRegistryManager.SLOW_MODE_STEPS.size - 1) slowModeIndex++
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(PrimaryViolet.copy(alpha = 0.2f), CircleShape)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Tambah Slow Mode", tint = PrimaryViolet)
                            }
                        }
                    }
                }
            }

            // 2. KATA TERLARANG (BANNED KEYWORDS)
            item {
                Text(text = "2. FILTER KATA TERLARANG", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = newKeywordInput,
                                onValueChange = { newKeywordInput = it },
                                placeholder = { Text("Tambah kata terlarang...", color = TextMuted) },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryViolet, unfocusedBorderColor = BorderDark, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (newKeywordInput.isNotBlank()) {
                                        bannedKeywords.add(newKeywordInput.trim().lowercase())
                                        commDb.saveBannedKeywords(communityId, bannedKeywords)
                                        newKeywordInput = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet)
                            ) {
                                Text("+ Tambah", color = TextPrimary)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Keywords Tags Grid
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            bannedKeywords.forEach { word ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(ObsidianBg, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "🚫 $word", color = TertiaryCoral, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                    IconButton(
                                        onClick = {
                                            bannedKeywords.remove(word)
                                            commDb.saveBannedKeywords(communityId, bannedKeywords)
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Hapus Kata", tint = TextMuted, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. AUTO MODERATION SENSITIVITY
            item {
                Text(text = "3. AI AUTO MODERATION SHIELD", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Aktifkan AI Auto Moderation", color = TextPrimary, fontWeight = FontWeight.Bold)
                            Switch(
                                checked = isAutoModEnabled,
                                onCheckedChange = { isAutoModEnabled = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = PrimaryViolet)
                            )
                        }

                        if (isAutoModEnabled) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Tingkat Sensitivitas AutoMod:", color = TextMuted, fontSize = 12.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                                listOf("Rendah", "Sedang", "Ketat").forEach { level ->
                                    FilterChip(
                                        selected = autoModSensitivity == level,
                                        onClick = { autoModSensitivity = level },
                                        label = { Text(level) },
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryViolet, selectedLabelColor = TextPrimary)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. ANTI SPAM LIMITS
            item {
                Text(text = "4. PENGATURAN ANTI-SPAM LIMITS", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Batas Rate Pesan:", color = TextMuted, fontSize = 13.sp)
                            Text(maxMessageRate, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Batas Mention Spammer:", color = TextMuted, fontSize = 13.sp)
                            Text(maxMentions, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Izinkan Tautan / Link External:", color = TextMuted, fontSize = 13.sp)
                            Switch(checked = allowLinks, onCheckedChange = { allowLinks = it }, colors = SwitchDefaults.colors(checkedThumbColor = PrimaryViolet))
                        }
                    }
                }
            }

            // 5. AUDIT LOG STREAM
            item {
                Text(text = "5. AUDIT LOG & RIWAYAT MODERASI", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            items(auditLogs) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = PrimaryViolet)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "${log.actorName} • ${log.actionType}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(text = log.details, color = TextMuted, fontSize = 11.sp)
                        }
                        Text(text = log.timestamp, color = TextMuted, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}
