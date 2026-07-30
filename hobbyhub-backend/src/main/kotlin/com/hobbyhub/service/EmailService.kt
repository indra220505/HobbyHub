package com.hobbyhub.service

import com.hobbyhub.exception.EmailDeliveryException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class EmailService(
    private val restTemplateBuilder: RestTemplateBuilder,
    @Value("\${resend.api-key:}") private val resendApiKey: String,
    @Value("\${resend.from-email:noreply@hobbyhub.web.id}") private val resendFromEmail: String,
    @Value("\${spring.profiles.active:dev}") private val activeProfile: String
) {
    private val log = LoggerFactory.getLogger(EmailService::class.java)
    private val restTemplate: RestTemplate = restTemplateBuilder.build()
    private val resendApiUrl = "https://api.resend.com/emails"

    /**
     * Synchronously sends OTP email via Resend HTTP REST API (Port 443).
     * Throws [EmailDeliveryException] if sending fails, causing transaction rollback in calling service.
     */
    fun sendOtpEmail(toEmail: String, otpCode: String) {
        val cleanToEmail = toEmail.trim().lowercase()

        // 1. Log OTP according to active profile
        log.info("Creating OTP for [{}]...", cleanToEmail)
        if (activeProfile == "dev") {
            log.info("==========================================================================")
            log.info("VERIFICATION OTP CODE FOR [{}]: [{}]", cleanToEmail, otpCode)
            log.info("==========================================================================")
        } else {
            log.info("OTP generated successfully for [{}]. (Hidden in production)", cleanToEmail)
        }

        // 2. Validate API Key
        if (resendApiKey.isBlank()) {
            log.error("❌ CRITICAL: RESEND_API_KEY environment variable is not configured!")
            log.error("Registration / OTP Action Rolled Back.")
            throw EmailDeliveryException("Layanan email belum dikonfigurasi pada server. Harap set RESEND_API_KEY di Railway.")
        }

        val subject = "$otpCode adalah Kode Verifikasi HobbyHub Anda"
        val htmlContent = buildEmailTemplate(otpCode)

        // 3. Prepare Resend API Request
        val sender = "HobbyHub <$resendFromEmail>"
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            setBearerAuth(resendApiKey)
        }

        val requestBody = mapOf(
            "from" to sender,
            "to" to listOf(cleanToEmail),
            "subject" to subject,
            "html" to htmlContent
        )

        val requestEntity = HttpEntity(requestBody, headers)

        // 4. Send via Resend REST API
        try {
            log.info("Sending OTP email to [{}] via Resend REST API (From: {})...", cleanToEmail, sender)
            val response = restTemplate.postForEntity(resendApiUrl, requestEntity, String::class.java)

            if (response.statusCode.is2xxSuccessful) {
                log.info("✅ Email Delivered successfully to [{}] via Resend API.", cleanToEmail)
            } else {
                log.error("❌ Resend API returned non-2xx status: {}", response.statusCode)
                throw Exception("Resend API response status: ${response.statusCode}")
            }
        } catch (ex: Exception) {
            log.error("❌ Failed to send OTP email to [{}] via Resend API. Cause: {}", cleanToEmail, ex.message, ex)
            throw EmailDeliveryException(
                "Gagal mengirim email verifikasi ke $cleanToEmail. Detail: ${ex.message}",
                ex
            )
        }
    }

    private fun buildEmailTemplate(otpCode: String): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #0d0e15; color: #ffffff; margin: 0; padding: 0; }
                    .container { max-width: 500px; margin: 30px auto; background: #161824; border-radius: 16px; padding: 32px; border: 1px solid #2a2d3d; text-align: center; }
                    .logo { font-size: 28px; font-weight: 800; color: #6c5ce7; margin-bottom: 8px; letter-spacing: 1px; }
                    .tagline { color: #a0a5b5; font-size: 14px; margin-bottom: 24px; }
                    .title { font-size: 20px; font-weight: 600; color: #f1f2f6; margin-bottom: 12px; }
                    .otp-box { background: linear-gradient(135deg, #6c5ce7 0%, #a29bfe 100%); font-size: 36px; font-weight: 800; letter-spacing: 10px; color: #ffffff; padding: 18px 24px; border-radius: 12px; display: inline-block; margin: 20px 0; text-shadow: 0 2px 4px rgba(0,0,0,0.2); }
                    .expiry { color: #ff7675; font-size: 13px; font-weight: 600; margin-top: 10px; }
                    .footer { color: #747d8c; font-size: 12px; margin-top: 32px; border-top: 1px solid #2a2d3d; padding-top: 16px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="logo">HobbyHub</div>
                    <div class="tagline">Komunitas Berbasis Minat & Hobi</div>
                    <div class="title">Kode Verifikasi Anda</div>
                    <p style="color: #cbd5e1; font-size: 14px; margin: 0;">Gunakan kode 6 digit di bawah ini untuk memverifikasi akun HobbyHub Anda:</p>
                    <div class="otp-box">$otpCode</div>
                    <div class="expiry">⏱ Kode berlaku selama 5 menit.</div>
                    <div class="footer">
                        Jika Anda tidak merasa melakukan pendaftaran akun di HobbyHub, silakan abaikan email ini.<br>
                        &copy; 2026 HobbyHub Team. All rights reserved.
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }
}
