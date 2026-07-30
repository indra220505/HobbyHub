package com.hobbyhub.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.stereotype.Component

@Component
class SmtpHealthCheck(
    private val mailSender: JavaMailSender,
    @Value("\${spring.mail.host:smtp.gmail.com}") private val mailHost: String,
    @Value("\${spring.mail.port:587}") private val mailPort: Int,
    @Value("\${spring.mail.username:none}") private val mailUsername: String,
    @Value("\${spring.mail.password:none}") private val mailPassword: String,
    @Value("\${spring.profiles.active:dev}") private val activeProfile: String
) : CommandLineRunner {

    private val log = LoggerFactory.getLogger(SmtpHealthCheck::class.java)

    override fun run(vararg args: String?) {
        log.info("==========================================================================")
        log.info("🔍 STARTUP SMTP CONFIGURATION & HEALTH CHECK")
        log.info("Active Profile : [{}]", activeProfile)
        log.info("MAIL_HOST      : [{}]", mailHost)
        log.info("MAIL_PORT      : [{}]", mailPort)
        log.info("MAIL_USERNAME  : [{}]", mailUsername)

        // Environment Variable Validation
        var hasConfigError = false
        if (mailUsername == "none" || mailUsername.isBlank()) {
            log.error("❌ CRITICAL: SPRING_MAIL_USERNAME environment variable is missing or empty!")
            hasConfigError = true
        }

        if (mailPassword == "none" || mailPassword.isBlank()) {
            log.error("❌ CRITICAL: SPRING_MAIL_PASSWORD environment variable is missing or placeholder 'none'.")
            log.warn("👉 Action Required: Set SPRING_MAIL_PASSWORD to a valid 16-character Gmail App Password.")
            hasConfigError = true
        }

        if (hasConfigError) {
            log.error("--------------------------------------------------------------------------")
            log.error("⚠️ SMTP Status : FAILED - Invalid/Missing Environment Variables.")
            log.error("Email dispatch will fail during registration until valid credentials are set.")
            log.info("==========================================================================")
            return
        }

        // SMTP Connection Test
        log.info("Connecting Gmail SMTP [{}:{}...] ", mailHost, mailPort)
        try {
            if (mailSender is JavaMailSenderImpl) {
                mailSender.testConnection()
            }
            log.info("✅ SMTP Status : OK - Successfully connected to Gmail SMTP server!")
        } catch (ex: Exception) {
            log.error("❌ SMTP Status : FAILED - Cannot connect to SMTP server [{}:{}].", mailHost, mailPort)
            log.error("Cause: {} - {}", ex.javaClass.simpleName, ex.message)
            log.warn("👉 Troubleshooting Guide:")
            log.warn("   1. Verify SPRING_MAIL_PASSWORD is a 16-character Gmail App Password (not your normal Google account password).")
            log.warn("   2. If using Railway/Cloud and getting SocketTimeoutException on port 587, change SPRING_MAIL_PORT to 465.")
            log.warn("   3. Ensure 2-Step Verification is active on the Google Account.")
        }
        log.info("==========================================================================")
    }
}
