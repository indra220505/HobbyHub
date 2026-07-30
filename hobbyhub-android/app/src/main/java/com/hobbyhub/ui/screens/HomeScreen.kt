package com.hobbyhub.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.hobbyhub.model.Community
import com.hobbyhub.ui.theme.*
import com.hobbyhub.data.remote.NetworkModule
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
@Composable
fun HomeScreen(
    onCommunityClick: (Community) -> Unit = {}
) {
    val context = LocalContext.current
    val commDb = remember { CommunityRegistryManager(context) }
    val sessionManager = remember { UserSessionManager(context) }
    val currentUser = remember { sessionManager.getUser() }

    var allCommunities by remember { mutableStateOf(commDb.getAllCommunities()) }
    var joinedIds by remember { mutableStateOf(sessionManager.getJoinedCommunityIds()) }
    val coroutineScope = rememberCoroutineScope()
    val communityApi = remember { NetworkModule.getCommunityApi(context) }

    LaunchedEffect(Unit) {
        try {
            val response = withContext(Dispatchers.IO) { communityApi.getJoinedCommunities() }
            if (response.isSuccessful) {
                response.body()?.let { serverJoined ->
                    // Update local cache
                    serverJoined.forEach { sessionManager.joinCommunity(it) }
                    // If local cache had items not on server, maybe remove them? 
                    // Let's just override it to sync exactly with server.
                    sessionManager.setJoinedCommunityIds(serverJoined)
                    joinedIds = serverJoined
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val categories = listOf("Semua", "Programming", "AI / ML", "Gaming", "Fotografi", "Trading", "Music")
    var selectedCategory by remember { mutableStateOf("Semua") }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    var showCreateDialog by remember { mutableStateOf(false) }

    val filteredCommunities = remember(selectedCategory, searchQuery, allCommunities) {
        allCommunities.filter { comm ->
            val matchCategory = selectedCategory == "Semua" || comm.category.equals(selectedCategory, ignoreCase = true)
            val matchQuery = searchQuery.isBlank() || comm.name.contains(searchQuery, ignoreCase = true) || comm.description.contains(searchQuery, ignoreCase = true)
            matchCategory && matchQuery
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(ObsidianBg)) {
                TopAppBar(
                    title = {
                        Text(
                            text = "HobbyHub",
                            color = PrimaryViolet,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        IconButton(onClick = { isSearchActive = !isSearchActive }) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = TextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = ObsidianBg)
                )

                if (isSearchActive) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Cari Komunitas atau Guild...", color = TextMuted) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryViolet) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMuted)
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryViolet,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = PrimaryViolet,
                contentColor = TextPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Buat Komunitas Baru", modifier = Modifier.size(28.dp))
            }
        },
        containerColor = ObsidianBg
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Jelajahi Komunitas Hobi",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Category Chips (Dynamic Selection)
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { category ->
                        val isSelected = selectedCategory == category
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = category },
                            label = { Text(category) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryViolet,
                                selectedLabelColor = TextPrimary,
                                containerColor = SurfaceCard,
                                labelColor = TextMuted
                            )
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedCategory == "Semua") "Semua Komunitas" else "Komunitas $selectedCategory",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${filteredCommunities.size} Ditemukan",
                        color = SecondaryTurquoise,
                        fontSize = 12.sp
                    )
                }
            }

            if (filteredCommunities.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "🔍", fontSize = 36.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "Belum Ada Komunitas", color = TextPrimary, fontWeight = FontWeight.Bold)
                            Text(
                                text = "Tidak ada komunitas di kategori $selectedCategory. Buat komunitas pertamamu sekarang!",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            } else {
                items(filteredCommunities) { comm ->
                    val isJoined = joinedIds.contains(comm.id)
                    CommunityCardItem(
                        comm = comm,
                        isJoined = isJoined,
                        onJoinClick = {
                            if (!isJoined) {
                                coroutineScope.launch {
                                    try {
                                        val response = withContext(Dispatchers.IO) { communityApi.joinCommunity(comm.id) }
                                        if (response.isSuccessful) {
                                            sessionManager.joinCommunity(comm.id)
                                            joinedIds = sessionManager.getJoinedCommunityIds()
                                            Toast.makeText(context, "Berhasil bergabung dengan ${comm.name}!", Toast.LENGTH_SHORT).show()
                                            onCommunityClick(comm)
                                        } else {
                                            Toast.makeText(context, "Gagal bergabung: ${response.code()}", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        Toast.makeText(context, "Gagal terhubung ke server", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                onCommunityClick(comm)
                            }
                        }
                    )
                }
            }
        }

        // Create Community Dialog Modal (Enhanced with Category Selector)
        if (showCreateDialog) {
            CreateCommunityDialog(
                onDismiss = { showCreateDialog = false },
                onCreate = { name, category, desc, emoji, isPublic ->
                    val newComm = commDb.createCommunity(currentUser.username, name, category, desc, emoji, isPublic)
                    sessionManager.joinCommunity(newComm.id) // Creator automatically joins their own community
                    allCommunities = commDb.getAllCommunities()
                    joinedIds = sessionManager.getJoinedCommunityIds()
                    showCreateDialog = false
                    Toast.makeText(context, "Komunitas ${newComm.name} berhasil dibuat!", Toast.LENGTH_SHORT).show()
                    onCommunityClick(newComm)
                }
            )
        }
    }
}

@Composable
fun CommunityCardItem(
    comm: Community,
    isJoined: Boolean,
    onJoinClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onJoinClick() }
            .border(1.dp, BorderDark, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = comm.iconEmoji, fontSize = 32.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = comm.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(text = "${comm.category} • ${comm.memberCount}", color = SecondaryTurquoise, fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = comm.description, color = TextMuted, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onJoinClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isJoined) SecondaryTurquoise else PrimaryViolet
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = if (isJoined) Icons.Default.Check else Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isJoined) "Joined • Buka Channel" else "Join Komunitas",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCommunityDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, category: String, desc: String, emoji: String, isPublic: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Programming") }
    var desc by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("🚀") }
    var isPublic by remember { mutableStateOf(true) }

    val categoryList = listOf("Programming", "AI / ML", "Gaming", "Fotografi", "Trading", "Music")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        title = {
            Text(text = "Buat Komunitas / Guild Baru", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Komunitas", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryViolet, unfocusedBorderColor = BorderDark, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )

                OutlinedTextField(
                    value = emoji,
                    onValueChange = { emoji = it },
                    label = { Text("Ikon Emoji (misal: 🤖, 🎮, 🚀)", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryViolet, unfocusedBorderColor = BorderDark, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )

                // KATEGORI HOBI SELECTOR CHIPS (Diperbarui agar dapat dipilih)
                Text(text = "Kategori Hobi:", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categoryList) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryViolet,
                                selectedLabelColor = TextPrimary,
                                containerColor = ObsidianBg,
                                labelColor = TextMuted
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Deskripsi Komunitas", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryViolet, unfocusedBorderColor = BorderDark, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )

                Text(text = "Tipe Komunitas:", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = isPublic,
                        onClick = { isPublic = true },
                        colors = RadioButtonDefaults.colors(selectedColor = PrimaryViolet)
                    )
                    Text("Publik (Bisa dicari semua orang)", color = TextPrimary, fontSize = 13.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = !isPublic,
                        onClick = { isPublic = false },
                        colors = RadioButtonDefaults.colors(selectedColor = PrimaryViolet)
                    )
                    Text("Privat (Hanya dengan undangan)", color = TextPrimary, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onCreate(name, category, desc, emoji, isPublic)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet)
            ) {
                Text("Buat Komunitas Sekarang", color = TextPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = TextMuted)
            }
        }
    )
}
