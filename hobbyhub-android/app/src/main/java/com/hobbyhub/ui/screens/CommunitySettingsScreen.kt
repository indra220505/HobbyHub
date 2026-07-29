package com.hobbyhub.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.hobbyhub.data.local.UserSessionManager
import com.hobbyhub.model.Community
import com.hobbyhub.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunitySettingsScreen(
    community: Community,
    onBackClick: () -> Unit,
    onNavigateToChannelManager: () -> Unit,
    onNavigateToRoleManager: () -> Unit,
    onNavigateToModeration: () -> Unit,
    onCommunityDeleted: () -> Unit,
    onCommunityUpdated: (Community) -> Unit
) {
    val context = LocalContext.current
    val commDb = remember { CommunityRegistryManager(context) }
    val sessionManager = remember { UserSessionManager(context) }
    val currentUser = remember { sessionManager.getUser() }

    val isOwner = remember(currentUser, community) {
        commDb.isOwnerOfCommunity(community.id, currentUser.username)
    }

    var name by remember { mutableStateOf(community.name) }
    var description by remember { mutableStateOf(community.description) }
    var category by remember { mutableStateOf(community.category) }
    var iconEmoji by remember { mutableStateOf(community.iconEmoji) }

    var memberApprovalRequired by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showTransferOwnershipDialog by remember { mutableStateOf(false) }

    val mockMembers = listOf("budi_dev", "siti_coder", "rian_gamer", "dewi_design")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan Komunitas", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            val updatedList = commDb.updateCommunitySettings(community.id, name, description, category, iconEmoji)
                            val updated = updatedList.find { it.id == community.id }
                            if (updated != null) {
                                onCommunityUpdated(updated)
                                Toast.makeText(context, "Pengaturan komunitas berhasil disimpan!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Simpan", color = TextPrimary, fontWeight = FontWeight.Bold)
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
            // 1. INFORMASI KOMUNITAS
            item {
                SettingsSectionHeader("1. INFORMASI KOMUNITAS", Icons.Default.Info, PrimaryViolet)
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Nama Komunitas", color = TextMuted) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryViolet, unfocusedBorderColor = BorderDark, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                        )

                        OutlinedTextField(
                            value = iconEmoji,
                            onValueChange = { iconEmoji = it },
                            label = { Text("Ikon Logo Emoji (misal: 🤖, 🎮, 📱)", color = TextMuted) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryViolet, unfocusedBorderColor = BorderDark, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                        )

                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text("Kategori Hobi", color = TextMuted) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryViolet, unfocusedBorderColor = BorderDark, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                        )

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Deskripsi Komunitas", color = TextMuted) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryViolet, unfocusedBorderColor = BorderDark, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                        )
                    }
                }
            }

            // 2. MANAJEMEN ANGGOTA & ROLE
            item {
                SettingsSectionHeader("2. MANAJEMEN & ROLE", Icons.Default.People, PrimaryViolet)
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column {
                        SettingsRowItem("Kelola Channel (${community.channels.size} Channel)", Icons.Default.Tag) { onNavigateToChannelManager() }
                        Divider(color = BorderDark)
                        SettingsRowItem("Kelola Role & Permission Bitmask", Icons.Default.Shield) { onNavigateToRoleManager() }
                        Divider(color = BorderDark)
                        SettingsRowItem("Kelola Daftar Member & Moderator", Icons.Default.Group) {}
                        Divider(color = BorderDark)
                        SettingsRowItem("Salin Invite Link Komunitas", Icons.Default.Link) {
                            Toast.makeText(context, "Link Invite Salin: hobbyhub.app/i/${community.slug}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            // 3. MODERASI & ANTI-SPAM
            item {
                SettingsSectionHeader("3. MODERASI & SENSOR", Icons.Default.Gavel, PrimaryViolet)
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column {
                        SettingsRowItem("Buka Layar Moderasi (Slow Mode, Kata Terlarang, Anti-Spam)", Icons.Default.Tune) { onNavigateToModeration() }
                    }
                }
            }

            // 4. KEAMANAN & PRIVASI
            item {
                SettingsSectionHeader("4. KEAMANAN & PRIVASI", Icons.Default.Lock, PrimaryViolet)
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Persetujuan Anggota Baru", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Wajibkan verifikasi manual Admin sebelum member baru bisa masuk chat.", color = TextMuted, fontSize = 12.sp)
                            }
                            Switch(
                                checked = memberApprovalRequired,
                                onCheckedChange = { memberApprovalRequired = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = PrimaryViolet)
                            )
                        }
                    }
                }
            }

            // 5. ZONA BERBAHAYA (DANGER ZONE) & LEAVE
            item {
                SettingsSectionHeader("5. ZONA BERBAHAYA & KELUAR", Icons.Default.Warning, TertiaryCoral)
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(TertiaryCoral)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (isOwner) {
                            OutlinedButton(
                                onClick = { showTransferOwnershipDialog = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.SwapHoriz, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Transfer Kepemilikan (Owner)")
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                if (isOwner) {
                                    Toast.makeText(context, "Anda adalah Owner komunitas ini. Anda HARUS mentransfer kepemilikan kepada anggota lain atau menghapus komunitas sebelum bisa keluar.", Toast.LENGTH_LONG).show()
                                } else {
                                    showLeaveDialog = true
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TertiaryCoral),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.ExitToApp, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Keluar dari Komunitas Ini")
                        }

                        if (isOwner) {
                            Button(
                                onClick = { showDeleteDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = TertiaryCoral),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.DeleteForever, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Hapus Komunitas Permanen (Owner Only)", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Leave Community Dialog (Requirement 5)
        if (showLeaveDialog) {
            AlertDialog(
                onDismissRequest = { showLeaveDialog = false },
                containerColor = SurfaceCard,
                title = { Text("Konfirmasi Keluar Komunitas", color = TextPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "Apakah kamu yakin ingin keluar dari komunitas \"${community.name}\"?",
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            sessionManager.leaveCommunity(community.id)
                            showLeaveDialog = false
                            Toast.makeText(context, "Kamu telah keluar dari komunitas", Toast.LENGTH_SHORT).show()
                            onCommunityDeleted()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TertiaryCoral)
                    ) {
                        Text("Keluar Sekarang", color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLeaveDialog = false }) { Text("Batal", color = TextMuted) }
                }
            )
        }

        // Transfer Ownership Dialog
        if (showTransferOwnershipDialog) {
            var selectedNewOwner by remember { mutableStateOf(mockMembers[0]) }
            AlertDialog(
                onDismissRequest = { showTransferOwnershipDialog = false },
                containerColor = SurfaceCard,
                title = { Text("Transfer Kepemilikan (Owner)", color = TextPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Pilih anggota yang akan menjadi Owner baru:", color = TextMuted, fontSize = 12.sp)
                        mockMembers.forEach { member ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedNewOwner = member }
                                    .background(if (selectedNewOwner == member) PrimaryViolet.copy(alpha = 0.2f) else ObsidianBg, RoundedCornerShape(6.dp))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = selectedNewOwner == member, onClick = { selectedNewOwner = member }, colors = RadioButtonDefaults.colors(selectedColor = PrimaryViolet))
                                Text("@$member", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            commDb.transferOwnership(community.id, currentUser.username, selectedNewOwner)
                            showTransferOwnershipDialog = false
                            Toast.makeText(context, "Kepemilikan berhasil ditransfer kepada @$selectedNewOwner!", Toast.LENGTH_SHORT).show()
                            onBackClick()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet)
                    ) {
                        Text("Transfer Sekarang", color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTransferOwnershipDialog = false }) { Text("Batal", color = TextMuted) }
                }
            )
        }

        // Delete Confirmation Dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                containerColor = SurfaceCard,
                icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = TertiaryCoral, modifier = Modifier.size(36.dp)) },
                title = { Text("Konfirmasi Hapus Komunitas", color = TextPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "Apakah Anda yakin ingin menghapus komunitas \"${community.name}\"? Semua channel, chat, media, dan data anggota akan dihapus secara permanen.",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            commDb.deleteCommunity(community.id)
                            showDeleteDialog = false
                            Toast.makeText(context, "Komunitas berhasil dihapus permanen!", Toast.LENGTH_SHORT).show()
                            onCommunityDeleted()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TertiaryCoral)
                    ) {
                        Text("Hapus Sekarang", color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text("Batal", color = TextMuted) }
                }
            )
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SettingsRowItem(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(text = title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
    }
}
