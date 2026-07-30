package com.hobbyhub.service

import com.hobbyhub.exception.EmailDeliveryException
import jakarta.mail.internet.MimeMessage
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.client.RestTemplate
import org.springframework.boot.web.client.RestTemplateBuilder

@Service
class EmailService(
    private val mailSender: JavaMailSender,
    private val restTemplateBuilder: RestTemplateBuilder,
    @Value("\${spring.mail.username:hobbyhub.auth@gmail.com}") private val fromEmail: String,
    @Value("\${spring.mail.password:none}") private val mailPassword: String,
    @Value("\${spring.mail.host:smtp.gmail.com}") private val mailHost: String,
    @Value("\${spring.mail.port:587}") private val mailPort: Int,
    @Value("\${spring.profiles.active:dev}") private val activeProfile: String,
    @Value("\${brevo.api-key:}") private val brevoApiKey: String,
    @Value("\${resend.api-key:}") private val resendApiKey: String
) {
    private val log = LoggerFactory.getLogger(EmailService::class.java)
    private val restTemplate: RestTemplate = restTemplateBuilder.build()

    /**
     * Synchronously sends OTP email with retry mechanism (3 attempts with 10s timeouts).
     * Throws [EmailDeliveryException] if all attempts fail, causing transaction rollback in calling service.
     */
    fun sendOtpEmail(toEmail: String, otpCode: String) {
        val cleanToEmail = toEmail.trim().lowercase()

        // 1. Log OTP according to active profile (Hide OTP in production log for security)
        log.info("Creating OTP...")
        if (activeProfile == "dev") {
            log.info("==========================================================================")
            log.info("VERIFICATION OTP CODE FOR [{}]: [{}]", cleanToEmail, otpCode)
            log.info("==========================================================================")
        } else {
            log.info("OTP generated successfully for [{}]. (Hidden in production)", cleanToEmail)
        }

        val subject = "$otpCode adalah Kode Verifikasi HobbyHub Anda"
        val htmlContent = buildEmailTemplate(otpCode)

        // 2. HTTP API Priorities (Port 443 - Never blocked by Railway)
        if (brevoApiKey.isNotBlank()) {
            log.info("BREVO_API_KEY detected. Using Brevo HTTP REST API (Port 443)...")
            try {
                sendViaBrevo(cleanToEmail, subject, htmlContent)
                return
            } catch (ex: Exception) {
                log.error("Failed to send email via Brevo HTTP API.", ex)
                if (resendApiKey.isBlank() && (mailPassword == "none" || mailPassword.isBlank())) {
                    throw EmailDeliveryException("Gagal mengirim email via Brevo HTTP API: ${ex.message}", ex)
                }
            }
        }

        if (resendApiKey.isNotBlank()) {
            log.info("RESEND_API_KEY detected. Using Resend HTTP REST API (Port 443)...")
            try {
                sendViaResend(cleanToEmail, subject, htmlContent)
                return
            } catch (ex: Exception) {
                log.error("Failed to send email via Resend API.", ex)
                if (mailPassword == "none" || mailPassword.isBlank()) {
                    throw EmailDeliveryException("Gagal mengirim email via Resend HTTP API: ${ex.message}", ex)
                }
            }
        }

        // 3. Fallback to SMTP
        if (mailPassword == "none" || mailPassword.isBlank()) {
            log.error("Email Configuration Failed: No BREVO_API_KEY, RESEND_API_KEY, or SPRING_MAIL_PASSWORD provided.")
            log.error("Registration / OTP Action Rolled Back.")
            throw EmailDeliveryException("Layanan email belum dikonfigurasi pada server. Silakan set BREVO_API_KEY atau SPRING_MAIL_PASSWORD.")
        }

        log.info("Connecting Gmail SMTP [{}:{}...] with 10s timeout", mailHost, mailPort)


        val maxRetries = 3
        var lastException: Exception? = null

        // 3. Fast Retry Loop with Exponential Backoff (500ms, 1000ms)
        for (attempt in 1..maxRetries) {
            try {
                log.info("Sending Email (Attempt {}/{})...", attempt, maxRetries)

                val message: MimeMessage = mailSender.createMimeMessage()
                val helper = MimeMessageHelper(message, true, "UTF-8")
                helper.setFrom(fromEmail, "HobbyHub Support")
                helper.setTo(cleanToEmail)
                helper.setSubject(subject)
                helper.setText(htmlContent, true)

                mailSender.send(message)
                log.info("SMTP Connected.")
                log.info("Email Delivered successfully to [{}].", cleanToEmail)
                return // Email sent successfully!
            } catch (ex: Exception) {
                lastException = ex
                log.error("❌ SMTP Connection Failed (Attempt {}/{}). Type: {}, Message: {}", attempt, maxRetries, ex.javaClass.name, ex.message)
                log.error("Full Exception Trace for Railway Logging:", ex)

                if (attempt < maxRetries) {
                    val backoffMs = (500L * Math.pow(2.0, (attempt - 1).toDouble())).toLong() // 500ms, 1000ms
                    log.info("Retrying in {}ms (Retry {})...", backoffMs, attempt)
                    try {
                        Thread.sleep(backoffMs)
                    } catch (_: InterruptedException) {
                        Thread.currentThread().interrupt()
                    }
                }
            }
        }

        // 4. Failure after max retries
        log.error("❌ All {} SMTP attempts failed for [{}]. Root Cause: {}", maxRetries, cleanToEmail, lastException?.message, lastException)
        log.error("Registration / OTP Action Rolled Back.")

        throw EmailDeliveryException(
            "Gagal mengirim email verifikasi ke $cleanToEmail. Detail Error: ${lastException?.message ?: "Unknown SMTP error"}",
            lastException
        )
    }

    private fun sendViaBrevo(toEmail: String, subject: String, htmlContent: String) {
        val url = "https://api.brevo.com/v3/smtp/email"
        
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.set("api-key", brevoApiKey)
        
        val requestBody = mapOf(
            "sender" to mapOf("name" to "HobbyHub Support", "email" to fromEmail),
            "to" to listOf(mapOf("email" to toEmail)),
            "subject" to subject,
            "htmlContent" to htmlContent
        )

        val request = HttpEntity(requestBody, headers)
        
        log.info("Sending Email via Brevo REST API to [{}]...", toEmail)
        val response = restTemplate.postForEntity(url, request, String::class.java)
        
        if (response.statusCode.is2xxSuccessful) {
            log.info("Email Delivered successfully to [{}] via Brevo HTTP API.", toEmail)
        } else {
            throw Exception("Brevo API returned status: ${response.statusCode}")
        }
    }

    private fun sendViaResend(toEmail: String, subject: String, htmlContent: String) {
        val url = "https://api.resend.com/emails"
        
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.setBearerAuth(resendApiKey)
        
        // Resend doesn't allow gmail.com sender without verifying domain
        val sender = if (fromEmail.endsWith("@gmail.com")) "onboarding@resend.dev" else "HobbyHub Support <$fromEmail>"

        val requestBody = mapOf(
            "from" to sender,
            "to" to listOf(toEmail),
            "subject" to subject,
            "html" to htmlContent
        )

        val request = HttpEntity(requestBody, headers)
        
        log.info("Sending Email via Resend to [{}]...", toEmail)
        val response = restTemplate.postForEntity(url, request, String::class.java)
        
        if (response.statusCode.is2xxSuccessful) {
            log.info("Email Delivered successfully to [{}] via Resend API.", toEmail)
        } else {
            throw Exception("Resend API returned status: \${response.statusCode}")
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
