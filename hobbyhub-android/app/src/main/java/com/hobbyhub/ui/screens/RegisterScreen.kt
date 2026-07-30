package com.hobbyhub.ui.screens

import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hobbyhub.data.remote.NetworkModule
import com.hobbyhub.ui.theme.*
import kotlinx.coroutines.delay

sealed class ValidationState {
    object Idle : ValidationState()
    object Loading : ValidationState()
    data class Success(val message: String) : ValidationState()
    data class Error(val message: String) : ValidationState()
}

val SuccessGreen = Color(0xFF00B894)

@Composable
fun RegisterScreen(
    onRegisterSuccess: (displayName: String, username: String, email: String, password: String) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var displayName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var emailState by remember { mutableStateOf<ValidationState>(ValidationState.Idle) }
    var usernameState by remember { mutableStateOf<ValidationState>(ValidationState.Idle) }

    // Real-Time Debounced Email Validation (500ms)
    LaunchedEffect(email) {
        val trimmed = email.trim()
        if (trimmed.isBlank()) {
            emailState = ValidationState.Idle
            return@LaunchedEffect
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()) {
            emailState = ValidationState.Error("Format email tidak valid (contoh: nama@gmail.com).")
            return@LaunchedEffect
        }

        emailState = ValidationState.Loading
        delay(500L) // 500ms debounce

        try {
            val api = NetworkModule.getAuthApi(context)
            val response = api.checkEmail(trimmed)
            if (response.isSuccessful && response.body() != null) {
                val res = response.body()!!
                emailState = if (res.available) {
                    ValidationState.Success(res.message)
                } else {
                    ValidationState.Error(res.message)
                }
            } else {
                emailState = ValidationState.Error("Gagal memeriksa ketersediaan email.")
            }
        } catch (e: Exception) {
            emailState = ValidationState.Error("Koneksi gagal saat memeriksa email.")
        }
    }

    // Real-Time Debounced Username Validation (500ms)
    LaunchedEffect(username) {
        val trimmed = username.trim()
        if (trimmed.isBlank()) {
            usernameState = ValidationState.Idle
            return@LaunchedEffect
        }
        if (trimmed.length < 3 || trimmed.contains(" ")) {
            usernameState = ValidationState.Error("Username minimal 3 karakter tanpa spasi.")
            return@LaunchedEffect
        }

        usernameState = ValidationState.Loading
        delay(500L) // 500ms debounce

        try {
            val api = NetworkModule.getAuthApi(context)
            val response = api.checkUsername(trimmed.lowercase())
            if (response.isSuccessful && response.body() != null) {
                val res = response.body()!!
                usernameState = if (res.available) {
                    ValidationState.Success(res.message)
                } else {
                    ValidationState.Error(res.message)
                }
            } else {
                usernameState = ValidationState.Error("Gagal memeriksa ketersediaan username.")
            }
        } catch (e: Exception) {
            usernameState = ValidationState.Error("Koneksi gagal saat memeriksa username.")
        }
    }

    // Form Field Local Validations
    val isDisplayNameValid = displayName.trim().length >= 2
    val isEmailValid = emailState is ValidationState.Success
    val isUsernameValid = usernameState is ValidationState.Success
    val isPasswordValid = password.length >= 8
    val isConfirmPasswordValid = confirmPassword.isNotEmpty() && confirmPassword == password

    val isFormValid = isDisplayNameValid && isEmailValid && isUsernameValid && isPasswordValid && isConfirmPasswordValid

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(20.dp))

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

        // 1. Display Name Field
        val displayNameBorderColor = if (displayName.isBlank()) BorderDark else if (isDisplayNameValid) SuccessGreen else TertiaryCoral
        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = { Text("Nama Lengkap / Panggilan", color = TextMuted) },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryViolet) },
            trailingIcon = {
                if (displayName.isNotBlank()) {
                    if (isDisplayNameValid) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Valid", tint = SuccessGreen)
                    } else {
                        Icon(Icons.Default.Cancel, contentDescription = "Invalid", tint = TertiaryCoral)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = displayNameBorderColor,
                unfocusedBorderColor = displayNameBorderColor,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
        if (displayName.isNotBlank() && !isDisplayNameValid) {
            Text(
                text = "Nama lengkap minimal 2 karakter.",
                color = TertiaryCoral,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Username Field
        val usernameBorderColor = when (usernameState) {
            is ValidationState.Success -> SuccessGreen
            is ValidationState.Error -> TertiaryCoral
            else -> if (username.isBlank()) BorderDark else PrimaryViolet
        }
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username (@username)", color = TextMuted) },
            leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null, tint = PrimaryViolet) },
            trailingIcon = {
                when (usernameState) {
                    is ValidationState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = PrimaryViolet)
                    }
                    is ValidationState.Success -> {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Tersedia", tint = SuccessGreen)
                    }
                    is ValidationState.Error -> {
                        Icon(Icons.Default.Cancel, contentDescription = "Terpakai", tint = TertiaryCoral)
                    }
                    else -> {}
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = usernameBorderColor,
                unfocusedBorderColor = usernameBorderColor,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
        when (val state = usernameState) {
            is ValidationState.Success -> {
                Text(
                    text = state.message,
                    color = SuccessGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 4.dp)
                )
            }
            is ValidationState.Error -> {
                Text(
                    text = state.message,
                    color = TertiaryCoral,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 4.dp)
                )
            }
            else -> {}
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Email Field
        val emailBorderColor = when (emailState) {
            is ValidationState.Success -> SuccessGreen
            is ValidationState.Error -> TertiaryCoral
            else -> if (email.isBlank()) BorderDark else PrimaryViolet
        }
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Alamat Email", color = TextMuted) },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = PrimaryViolet) },
            trailingIcon = {
                when (emailState) {
                    is ValidationState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = PrimaryViolet)
                    }
                    is ValidationState.Success -> {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Tersedia", tint = SuccessGreen)
                    }
                    is ValidationState.Error -> {
                        Icon(Icons.Default.Cancel, contentDescription = "Terpakai", tint = TertiaryCoral)
                    }
                    else -> {}
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = emailBorderColor,
                unfocusedBorderColor = emailBorderColor,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
        when (val state = emailState) {
            is ValidationState.Success -> {
                Text(
                    text = state.message,
                    color = SuccessGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 4.dp)
                )
            }
            is ValidationState.Error -> {
                Text(
                    text = state.message,
                    color = TertiaryCoral,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 4.dp)
                )
            }
            else -> {}
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 4. Password Field
        val passwordBorderColor = if (password.isBlank()) BorderDark else if (isPasswordValid) SuccessGreen else TertiaryCoral
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
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
                focusedBorderColor = passwordBorderColor,
                unfocusedBorderColor = passwordBorderColor,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
        if (password.isNotEmpty() && !isPasswordValid) {
            Text(
                text = "Kata sandi minimal 8 karakter.",
                color = TertiaryCoral,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 5. Confirm Password Field
        val confirmBorderColor = if (confirmPassword.isBlank()) BorderDark else if (isConfirmPasswordValid) SuccessGreen else TertiaryCoral
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Konfirmasi Kata Sandi", color = TextMuted) },
            leadingIcon = { Icon(Icons.Default.LockReset, contentDescription = null, tint = PrimaryViolet) },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = confirmBorderColor,
                unfocusedBorderColor = confirmBorderColor,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
        if (confirmPassword.isNotEmpty()) {
            if (isConfirmPasswordValid) {
                Text(
                    text = "Kata sandi cocok.",
                    color = SuccessGreen,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 4.dp)
                )
            } else {
                Text(
                    text = "Konfirmasi kata sandi tidak cocok!",
                    color = TertiaryCoral,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 4.dp)
                )
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

        // Register Button (Disabled unless all validations pass)
        Button(
            onClick = {
                if (isFormValid && isCaptchaVerified) {
                    onRegisterSuccess(displayName.trim(), username.trim().lowercase(), email.trim().lowercase(), password)
                }
            },
            enabled = isFormValid && isCaptchaVerified,
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryViolet,
                disabledContainerColor = BorderDark,
                contentColor = TextPrimary,
                disabledContentColor = TextMuted
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Lanjut Pilih Hobi & Minat ➔",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
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

        Spacer(modifier = Modifier.height(20.dp))
    }
}
