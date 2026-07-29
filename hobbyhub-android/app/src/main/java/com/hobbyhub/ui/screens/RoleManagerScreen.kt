package com.hobbyhub.ui.screens

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hobbyhub.data.local.CommunityRoleItem
import com.hobbyhub.ui.theme.*

class PermissionToggleState(
    val key: String,
    val label: String,
    val description: String,
    initialEnabled: Boolean
) {
    var isEnabled by mutableStateOf(initialEnabled)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleManagerScreen(
    communityName: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current

    val rolesList = remember {
        mutableStateListOf(
            CommunityRoleItem("r1", "Owner 👑", "#FF7675", "👑"),
            CommunityRoleItem("r2", "Admin 🛡️", "#6C5CE7", "🛡️"),
            CommunityRoleItem("r3", "Moderator ⚖️", "#00CEC9", "⚖️"),
            CommunityRoleItem("r4", "Member 👤", "#A0A5B5", "👤")
        )
    }

    var selectedRole by remember { mutableStateOf(rolesList[1]) } // Admin
    var showLeaveDialog by remember { mutableStateOf(false) }

    // Fully Interactive Permission Switches list with smooth state update
    val permissionsList = remember(selectedRole) {
        listOf(
            PermissionToggleState("del_msg", "Delete Message", "Izinkan menghapus pesan di channel", true),
            PermissionToggleState("ban_mem", "Ban Member", "Izinkan membanded member dari komunitas", true),
            PermissionToggleState("kick_mem", "Kick Member", "Izinkan mengeluarkan member dari komunitas", true),
            PermissionToggleState("mng_roles", "Manage Roles", "Izinkan membuat, mengedit, dan menghapus role", false),
            PermissionToggleState("mng_ch", "Manage Channels", "Izinkan membuat dan mengelola channel", true),
            PermissionToggleState("mng_comm", "Manage Community", "Izinkan mengelola pengaturan komunitas", true),
            PermissionToggleState("mng_event", "Manage Event", "Izinkan membuat dan mengedit jadwal event", true),
            PermissionToggleState("mention_all", "Mention Everyone", "Izinkan menyebut @everyone di chat", false),
            PermissionToggleState("pin_msg", "Pin Message", "Izinkan menyematkan pesan di bagian atas chat", true),
            PermissionToggleState("audit_log", "View Audit Log", "Izinkan melihat riwayat aktivitas moderasi", true)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kelola Role & Permission", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                // TOP BAR "KELUAR" BUTTON (Requirement 2 & Screenshot Design)
                actions = {
                    Button(
                        onClick = {
                            Toast.makeText(context, "Perubahan permission disimpan!", Toast.LENGTH_SHORT).show()
                            onBackClick()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Keluar", color = TextPrimary, fontWeight = FontWeight.Bold)
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
            // Selected Role Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = PrimaryViolet, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "PERAN", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(text = selectedRole.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
                    }
                }
            }

            // PERAMATER PERMISSION SECTION
            item {
                Text(
                    text = "PERAMATER PERMISSION (${selectedRole.name})",
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Interactive Permission Switches List
            items(permissionsList) { perm ->
                val trackColor by animateColorAsState(
                    targetValue = if (perm.isEnabled) PrimaryViolet else SurfaceCard,
                    animationSpec = tween(durationMillis = 200),
                    label = "trackColor"
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = perm.label, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = perm.description, color = TextMuted, fontSize = 11.sp)
                        }

                        // Smooth Animated Material 3 Switch (Requirement 3 & Screenshot)
                        Switch(
                            checked = perm.isEnabled,
                            onCheckedChange = { newValue ->
                                perm.isEnabled = newValue
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PrimaryViolet,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = ObsidianBg
                            )
                        )
                    }
                }
            }

            // BOTTOM "KELUAR KOMUNITAS" BUTTON (Requirement 2 & Screenshot Design)
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { showLeaveDialog = true },
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

        // Leave Confirmation Dialog (Requirement 2 & 4)
        if (showLeaveDialog) {
            AlertDialog(
                onDismissRequest = { showLeaveDialog = false },
                containerColor = SurfaceCard,
                icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = TertiaryCoral, modifier = Modifier.size(36.dp)) },
                title = { Text("Konfirmasi Keluar Komunitas", color = TextPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "Apakah kamu yakin ingin keluar dari komunitas ini?",
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showLeaveDialog = false
                            Toast.makeText(context, "Kamu telah keluar dari komunitas", Toast.LENGTH_SHORT).show()
                            onBackClick()
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
    }
}
