package com.hobbyhub.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hobbyhub.ui.theme.*

data class HobbyItem(val name: String, val emoji: String)

@Composable
fun HobbyOnboardingScreen(
    onCompleteOnboarding: (selectedHobbies: Set<String>) -> Unit
) {
    val hobbiesList = listOf(
        HobbyItem("Programming", "💻"),
        HobbyItem("AI & ML", "🤖"),
        HobbyItem("Gaming", "🎮"),
        HobbyItem("Anime & Manga", "⛩️"),
        HobbyItem("Fotografi", "📷"),
        HobbyItem("Editing Video", "🎬"),
        HobbyItem("Trading & Saham", "📈"),
        HobbyItem("Crypto", "🪙"),
        HobbyItem("Memasak", "🍳"),
        HobbyItem("Otomotif", "🏎️"),
        HobbyItem("Sepak Bola", "⚽"),
        HobbyItem("Membaca Novel", "📚"),
        HobbyItem("Fashion & Cosplay", "👗"),
        HobbyItem("Berkebun", "🌱"),
        HobbyItem("Elektronik / DIY", "🛠️")
    )

    val selectedHobbies = remember { mutableStateListOf<String>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(20.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Pilih 3+ Minat & Hobimu",
            color = TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "HobbyHub akan merekomendasikan komunitas terbaik sesuai pilihanmu.",
            color = TextMuted,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(hobbiesList) { hobby ->
                val isSelected = selectedHobbies.contains(hobby.name)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (isSelected) selectedHobbies.remove(hobby.name)
                            else selectedHobbies.add(hobby.name)
                        }
                        .border(
                            width = 2.dp,
                            color = if (isSelected) PrimaryViolet else BorderDark,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) PrimaryViolet.copy(alpha = 0.2f) else SurfaceCard
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = hobby.emoji, fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = hobby.name,
                            color = if (isSelected) TextPrimary else TextMuted,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (selectedHobbies.size >= 1) {
                    onCompleteOnboarding(selectedHobbies.toSet())
                }
            },
            enabled = selectedHobbies.size >= 1,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Mulai Jelajahi Komunitas (${selectedHobbies.size} Terpilih)",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
