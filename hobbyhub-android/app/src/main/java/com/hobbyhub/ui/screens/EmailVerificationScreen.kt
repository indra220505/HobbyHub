package com.hobbyhub.ui.screens

import androidx.compose.foundation.background
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
import com.hobbyhub.data.remote.ResendOtpRequest
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

    // Cooldown timer (60s)
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
            text = "Verifikasi Email (OTP)",
            color = TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Kode verifikasi 6 digit telah dikirim ke:",
            color = TextMuted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Text(
            text = email,
            color = SecondaryTurquoise,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // OTP Input Field
        OutlinedTextField(
            value = otpInput,
            onValueChange = { input ->
                if (input.length <= 6 && input.all { c -> c.isDigit() }) {
                    otpInput = input
                    errorMessage = null
                    successMessage = null
                }
            },
            label = { Text("Kode OTP 6 Digit", color = TextMuted) },
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
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = err, color = TertiaryCoral, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
        successMessage?.let { msg ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = msg, color = SecondaryTurquoise, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Verify Button
        Button(
            onClick = {
                if (otpInput.length != 6) {
                    errorMessage = "Harap masukkan 6 digit kode OTP!"
                } else {
                    isLoading = true
                    errorMessage = null
                    scope.launch {
                        try {
                            val api = NetworkModule.getAuthApi(context)
                            val response = api.verifyOtp(VerifyEmailRequest(email.trim(), otpInput.trim()))
                            if (response.isSuccessful && response.body() != null) {
                                successMessage = "Verifikasi Berhasil! Selamat datang ✓"
                                delay(800)
                                onVerificationSuccess()
                            } else {
                                errorMessage = "Kode OTP salah atau sudah kedaluwarsa!"
                            }
                        } catch (e: Exception) {
                            errorMessage = "Terjadi kesalahan jaringan: ${e.message}"
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
                Text("Verifikasi & Masuk", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Resend OTP Button with 60s Cooldown
        OutlinedButton(
            onClick = {
                if (canResend) {
                    isLoading = true
                    errorMessage = null
                    successMessage = null
                    scope.launch {
                        try {
                            val sessionManager = com.hobbyhub.data.local.UserSessionManager(context)
                            val targetEmail = email.ifBlank { sessionManager.getSessionEmail() }
                            
                            if (targetEmail.isBlank()) {
                                errorMessage = "Email tidak terdeteksi. Silakan Login kembali."
                                isLoading = false
                                return@launch
                            }

                            val api = NetworkModule.getAuthApi(context)
                            val response = api.resendOtp(ResendOtpRequest(targetEmail.trim()))
                            if (response.isSuccessful) {
                                successMessage = "Kode OTP baru telah dikirim!"
                                otpInput = ""
                                canResend = false
                                cooldownSeconds = 60
                            } else {
                                val errStr = response.errorBody()?.string() ?: ""
                                errorMessage = if (errStr.isNotBlank()) errStr else "Gagal mengirim ulang OTP."
                            }
                        } catch (e: Exception) {
                            errorMessage = "Kesalahan jaringan: ${e.message}"
                        } finally {
                            isLoading = false
                        }
                    }
                }
            },
            enabled = canResend && !isLoading,
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
                text = if (canResend) "Kirim Ulang Kode OTP" else "Kirim Ulang OTP (${cooldownSeconds}s)",
                color = if (canResend) SecondaryTurquoise else TextMuted,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Back to Login
        TextButton(onClick = onBackToLogin) {
            Text("← Kembali ke Halaman Login", color = TextMuted, fontSize = 14.sp)
        }
    }
}
