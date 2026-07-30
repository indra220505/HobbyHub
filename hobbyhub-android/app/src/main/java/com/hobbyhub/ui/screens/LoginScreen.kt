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
import androidx.compose.material.icons.filled.Verified
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
    onNavigateToRegister: () -> Unit,
    onNavigateToVerification: (email: String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isUnverifiedEmail by remember { mutableStateOf(false) }
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

        Spacer(modifier = Modifier.height(32.dp))

        // Email Field
        OutlinedTextField(
            value = email,
            onValueChange = { 
                email = it
                errorMessage = null 
                isUnverifiedEmail = false
            },
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
            onValueChange = { 
                password = it
                errorMessage = null 
                isUnverifiedEmail = false
            },
            label = { Text("Password", color = TextMuted) },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryViolet) },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
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
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = err, color = TertiaryCoral, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        if (isUnverifiedEmail) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = { onNavigateToVerification(email.trim().lowercase()) },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SecondaryTurquoise),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Verified, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Verifikasi Kode OTP Sekarang ➔", fontWeight = FontWeight.Bold)
            }
        }

        var isCaptchaVerified by remember { mutableStateOf(false) }

        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceCard, RoundedCornerShape(8.dp))
                .border(1.dp, BorderDark, RoundedCornerShape(8.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isCaptchaVerified,
                onCheckedChange = { isCaptchaVerified = it },
                colors = CheckboxDefaults.colors(checkedColor = PrimaryViolet)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Saya bukan robot (Anti-Spam CAPTCHA)", color = TextPrimary, fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(20.dp))

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
                    !isCaptchaVerified -> {
                        errorMessage = "Silakan centang 'Saya bukan robot' untuk melanjutkan."
                    }
                    else -> {
                        isLoading = true
                        errorMessage = null
                        isUnverifiedEmail = false
                        coroutineScope.launch {
                            try {
                                val api = NetworkModule.getAuthApi(context)
                                val response = api.login(LoginRequest(trimmedEmail, password))
                                if (response.isSuccessful && response.body() != null) {
                                    onLoginSuccess(response.body()!!)
                                } else {
                                    val errStr = response.errorBody()?.string() ?: ""
                                    if (errStr.contains("verifikasi", ignoreCase = true) || errStr.contains("unverified", ignoreCase = true)) {
                                        errorMessage = "Email Anda belum diverifikasi! Silakan verifikasi OTP."
                                        isUnverifiedEmail = true
                                    } else {
                                        errorMessage = "Gagal login: Email tidak terdaftar atau kata sandi salah."
                                    }
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
