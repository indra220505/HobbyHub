package com.hobbyhub.ui.screens

import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import com.hobbyhub.data.remote.AuthResponse
import com.hobbyhub.data.remote.LoginRequest
import com.hobbyhub.data.remote.NetworkModule
import com.hobbyhub.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: (response: AuthResponse) -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "HobbyHub",
            color = PrimaryViolet,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Komunitas Berbasis Minat & Hobi",
            color = TextMuted,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(36.dp))

        // Email Field
        OutlinedTextField(
            value = email,
            onValueChange = { email = it; errorMessage = null },
            label = { Text("Email", color = TextMuted) },
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

        Spacer(modifier = Modifier.height(16.dp))

        // Password Field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; errorMessage = null },
            label = { Text("Password", color = TextMuted) },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryViolet) },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
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

        errorMessage?.let { err ->
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = err, color = TertiaryCoral, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Login Button
        Button(
            onClick = {
                val trimmedEmail = email.trim()
                when {
                    trimmedEmail.isBlank() || password.isBlank() -> {
                        errorMessage = "Email dan password wajib diisi!"
                    }
                    !Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches() -> {
                        errorMessage = "Format email tidak valid! Contoh: nama@gmail.com"
                    }
                    else -> {
                        isLoading = true
                        errorMessage = null
                        coroutineScope.launch {
                            try {
                                val api = NetworkModule.getAuthApi(context)
                                val response = api.login(LoginRequest(trimmedEmail, password))
                                if (response.isSuccessful && response.body() != null) {
                                    onLoginSuccess(response.body()!!)
                                } else {
                                    errorMessage = "Gagal login: Kredensial salah atau email belum diverifikasi."
                                }
                            } catch (e: Exception) {
                                errorMessage = "Terjadi kesalahan jaringan: ${e.message}"
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                }
            },
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = TextPrimary, modifier = Modifier.size(24.dp))
            } else {
                Text("Masuk ke HobbyHub", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Register Link
        Row {
            Text("Belum punya akun? ", color = TextMuted, fontSize = 14.sp)
            Text(
                text = "Daftar Akun Baru",
                color = SecondaryTurquoise,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onNavigateToRegister() }
            )
        }
    }
}
