package com.hobbyhub.service

import com.hobbyhub.exception.EmailDeliveryException
import jakarta.mail.internet.MimeMessage
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service

@Service
class EmailService(
    private val mailSender: JavaMailSender,
    @Value("\${spring.mail.username:hobbyhub.auth@gmail.com}") private val fromEmail: String,
    @Value("\${spring.mail.password:none}") private val mailPassword: String,
    @Value("\${spring.mail.host:smtp.gmail.com}") private val mailHost: String,
    @Value("\${spring.mail.port:587}") private val mailPort: Int,
    @Value("\${spring.profiles.active:dev}") private val activeProfile: String
) {
    private val log = LoggerFactory.getLogger(EmailService::class.java)

    /**
     * Synchronously sends OTP email with retry mechanism (3 attempts with exponential backoff).
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

        // 2. Check if password is missing/default
        if (mailPassword == "none" || mailPassword.isBlank()) {
            log.error("SMTP Connection Failed: SPRING_MAIL_PASSWORD is not set or set to 'none'.")
            log.error("Registration / OTP Action Rolled Back.")
            throw EmailDeliveryException("Layanan email belum dikonfigurasi pada server. Silakan hubungi administrator.")
        }

        log.info("Connecting Gmail SMTP [{}:{}...] ", mailHost, mailPort)

        val subject = "$otpCode adalah Kode Verifikasi HobbyHub Anda"
        val htmlContent = buildEmailTemplate(otpCode)

        val maxRetries = 3
        var lastException: Exception? = null

        // 3. Retry Loop with Exponential Backoff
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
                log.warn("SMTP Connection Failed (Attempt {}/{}). Cause: {} - {}", attempt, maxRetries, ex.javaClass.simpleName, ex.message)

                if (attempt < maxRetries) {
                    val backoffMs = (1000L * Math.pow(2.0, (attempt - 1).toDouble())).toLong() // 1000ms, 2000ms
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
        log.error("❌ All {} SMTP attempts failed for [{}]. Cause: {}", maxRetries, cleanToEmail, lastException?.message)
        log.error("Registration / OTP Action Rolled Back.")
        log.warn("👉 Hint: Verify SPRING_MAIL_PASSWORD is a 16-character Gmail App Password. If port 587 times out on Railway, try port 465.")

        throw EmailDeliveryException(
            "Verification email could not be sent to $cleanToEmail. Please try again later.",
            lastException
        )
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
