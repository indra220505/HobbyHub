package com.hobbyhub.ui.screens

import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hobbyhub.ui.theme.*

@Composable
fun RegisterScreen(
    onRegisterSuccess: (displayName: String, username: String, email: String, password: String) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var displayName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Daftar Akun Baru",
            color = TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Bergabunglah dengan ribuan komunitas hobi",
            color = TextMuted,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Display Name Field
        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it; errorMessage = null },
            label = { Text("Nama Lengkap / Panggilan", color = TextMuted) },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryViolet) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryViolet,
                unfocusedBorderColor = BorderDark,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Username Field
        OutlinedTextField(
            value = username,
            onValueChange = { username = it; errorMessage = null },
            label = { Text("Username (@username)", color = TextMuted) },
            leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null, tint = PrimaryViolet) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryViolet,
                unfocusedBorderColor = BorderDark,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Email Field
        OutlinedTextField(
            value = email,
            onValueChange = { email = it; errorMessage = null },
            label = { Text("Alamat Email", color = TextMuted) },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = PrimaryViolet) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryViolet,
                unfocusedBorderColor = BorderDark,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Password Field with Visibility Toggle
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; errorMessage = null },
            label = { Text("Kata Sandi (min. 8 karakter)", color = TextMuted) },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryViolet) },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (passwordVisible) "Sembunyikan sandi" else "Tampilkan sandi",
                        tint = TextMuted
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryViolet,
                unfocusedBorderColor = BorderDark,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Confirm Password Field
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it; errorMessage = null },
            label = { Text("Konfirmasi Kata Sandi", color = TextMuted) },
            leadingIcon = { Icon(Icons.Default.LockReset, contentDescription = null, tint = PrimaryViolet) },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryViolet,
                unfocusedBorderColor = BorderDark,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        errorMessage?.let { err ->
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = err, color = TertiaryCoral, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val trimmedEmail = email.trim()
                when {
                    displayName.isBlank() || username.isBlank() || trimmedEmail.isBlank() || password.isBlank() -> {
                        errorMessage = "Semua kolom wajib diisi!"
                    }
                    !Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches() -> {
                        errorMessage = "Format email tidak valid! Contoh: nama@gmail.com"
                    }
                    password.length < 8 -> {
                        errorMessage = "Kata sandi minimal 8 karakter!"
                    }
                    password != confirmPassword -> {
                        errorMessage = "Konfirmasi kata sandi tidak cocok!"
                    }
                    username.contains(" ") || username.length < 3 -> {
                        errorMessage = "Username minimal 3 karakter tanpa spasi!"
                    }
                    else -> {
                        onRegisterSuccess(displayName, username.lowercase(), trimmedEmail, password)
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Lanjut Pilih Hobi & Minat ➔", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row {
            Text("Sudah punya akun? ", color = TextMuted, fontSize = 14.sp)
            Text(
                text = "Masuk di sini",
                color = SecondaryTurquoise,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onNavigateToLogin() }
            )
        }
    }
}
