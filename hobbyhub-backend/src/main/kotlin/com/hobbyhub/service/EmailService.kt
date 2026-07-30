package com.hobbyhub.service

import jakarta.mail.internet.MimeMessage
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class EmailService(
    private val mailSender: JavaMailSender,
    @Value("\${spring.mail.username:hobbyhub.auth@gmail.com}") private val fromEmail: String,
    @Value("\${spring.mail.password:none}") private val mailPassword: String
) {
    private val log = LoggerFactory.getLogger(EmailService::class.java)

    @Async
    fun sendOtpEmail(toEmail: String, otpCode: String) {
        val cleanToEmail = toEmail.trim().lowercase()

        // Check if SMTP password is placeholder
        if (mailPassword == "none" || mailPassword.isBlank()) {
            log.warn("==========================================================================")
            log.warn("⚠️ SMTP PASSWORD NOT CONFIGURED IN RAILWAY VARIABLES! ⚠️")
            log.warn("Real emails CANNOT be delivered to Gmail inbox until SPRING_MAIL_PASSWORD is set.")
            log.warn("VERIFICATION OTP CODE FOR [{}]: [{}]", cleanToEmail, otpCode)
            log.warn("==========================================================================")
            return
        }

        val subject = "$otpCode adalah Kode Verifikasi HobbyHub Anda"
        val htmlContent = """
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

        try {
            val message: MimeMessage = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(message, true, "UTF-8")
            helper.setFrom(fromEmail, "HobbyHub Support")
            helper.setTo(cleanToEmail)
            helper.setSubject(subject)
            helper.setText(htmlContent, true)

            mailSender.send(message)
            log.info("SUCCESSFULLY SENT OTP EMAIL via SMTP to [{}]", cleanToEmail)
        } catch (e: Exception) {
            log.error("SMTP DISPATCH ERROR for [{}]: {}", cleanToEmail, e.message, e)
            log.info("FALLBACK OTP LOG FOR [{}]: [{}]", cleanToEmail, otpCode)
        }
    }
}
