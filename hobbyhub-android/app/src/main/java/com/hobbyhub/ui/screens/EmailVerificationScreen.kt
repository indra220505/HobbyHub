package com.hobbyhub.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hobbyhub.data.remote.NetworkModule
import com.hobbyhub.data.remote.VerifyEmailRequest
import com.hobbyhub.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun EmailVerificationScreen(
    email: String,
    onVerificationSuccess: () -> Unit,
    onBackToLogin: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var otpInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var canResend by remember { mutableStateOf(false) }
    var cooldownSeconds by remember { mutableIntStateOf(60) }
    var isLoading by remember { mutableStateOf(false) }

    // Cooldown timer
    LaunchedEffect(Unit) {
        while (cooldownSeconds > 0) {
            delay(1000)
            cooldownSeconds--
        }
        canResend = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Mail Icon
        Icon(
            imageVector = Icons.Default.MarkEmailUnread,
            contentDescription = null,
            tint = PrimaryViolet,
            modifier = Modifier.size(72.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Verifikasi Email",
            color = TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Masukkan kode 6-digit yang dikirim ke",
            color = TextMuted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Text(
            text = email,
            color = SecondaryTurquoise,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // OTP Input Field
        OutlinedTextField(
            value = otpInput,
            onValueChange = {
                if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                    otpInput = it
                    errorMessage = null
                    successMessage = null
                }
            },
            label = { Text("Kode Verifikasi (6 digit)", color = TextMuted) },
            leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null, tint = PrimaryViolet) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryViolet,
                unfocusedBorderColor = BorderDark,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        // Error / Success Messages
        errorMessage?.let { err ->
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = err, color = TertiaryCoral, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        successMessage?.let { msg ->
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = msg, color = SecondaryTurquoise, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Verify Button
        Button(
            onClick = {
                if (otpInput.length != 6) {
                    errorMessage = "Masukkan kode 6 digit!"
                } else {
                    isLoading = true
                    errorMessage = null
                    scope.launch {
                        try {
                            val api = NetworkModule.getAuthApi(context)
                            val response = api.verifyEmail(VerifyEmailRequest(email, otpInput))
                            if (response.isSuccessful) {
                                successMessage = "Email berhasil diverifikasi! ✓"
                                delay(1000)
                                onVerificationSuccess()
                            } else {
                                errorMessage = "Kode verifikasi salah atau sudah kedaluwarsa!"
                            }
                        } catch (e: Exception) {
                            errorMessage = "Kesalahan jaringan: ${e.message}"
                        } finally {
                            isLoading = false
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
                Icon(Icons.Default.Verified, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Verifikasi Email", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Resend Button with Cooldown
        OutlinedButton(
            onClick = {
                if (canResend) {
                    // MOCK RESEND FOR NOW (To be integrated with real backend)
                    successMessage = "Kode verifikasi baru berhasil dikirim (Mock)!"
                    errorMessage = null
                    otpInput = ""
                    canResend = false
                    cooldownSeconds = 60
                    scope.launch {
                        while (cooldownSeconds > 0) {
                            delay(1000)
                            cooldownSeconds--
                        }
                        canResend = true
                    }
                }
            },
            enabled = canResend,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                tint = if (canResend) SecondaryTurquoise else TextMuted
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (canResend) "Kirim Ulang Kode Verifikasi" else "Kirim Ulang (${cooldownSeconds}s)",
                color = if (canResend) SecondaryTurquoise else TextMuted,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Back to Login
        TextButton(onClick = onBackToLogin) {
            Text("← Kembali ke halaman Login", color = TextMuted, fontSize = 14.sp)
        }
    }
}
