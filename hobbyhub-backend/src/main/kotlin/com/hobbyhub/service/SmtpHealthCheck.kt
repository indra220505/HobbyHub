package com.hobbyhub.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

@Component
class SmtpHealthCheck(
    @Value("\${resend.api-key:}") private val resendApiKey: String,
    @Value("\${resend.from-email:noreply@hobbyhub.web.id}") private val resendFromEmail: String,
    @Value("\${spring.profiles.active:dev}") private val activeProfile: String
) : CommandLineRunner {

    private val log = LoggerFactory.getLogger(SmtpHealthCheck::class.java)

    override fun run(vararg args: String?) {
        log.info("==========================================================================")
        log.info("🔍 STARTUP EMAIL CONFIGURATION CHECK (Resend REST API)")
        log.info("Active Profile    : [{}]", activeProfile)
        log.info("RESEND_FROM_EMAIL : [{}]", resendFromEmail)
        log.info("RESEND_API_KEY    : [{}]", if (resendApiKey.isNotBlank()) "CONFIGURED (***${resendApiKey.takeLast(4)})" else "NOT SET")

        if (resendApiKey.isBlank()) {
            log.error("❌ CRITICAL: RESEND_API_KEY is missing or empty in environment variables!")
            log.error("Email OTP delivery will fail during user registration.")
            log.warn("👉 Action Required: Add RESEND_API_KEY variable in Railway dashboard.")
        } else {
            log.info("✅ Resend API Configuration Status : OK")
        }
        log.info("==========================================================================")
    }
}
