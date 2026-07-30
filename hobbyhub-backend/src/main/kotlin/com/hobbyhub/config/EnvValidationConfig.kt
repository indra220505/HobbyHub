package com.hobbyhub.config

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import kotlin.system.exitProcess

@Configuration
class EnvValidationConfig(
    @Value("\${spring.datasource.url:}") private val dbUrl: String,
    @Value("\${jwt.secret:}") private val jwtSecret: String
) {
    private val log = LoggerFactory.getLogger(EnvValidationConfig::class.java)

    @PostConstruct
    fun validateEnvVars() {
        var hasErrors = false

        if (dbUrl.isBlank() || dbUrl.contains("\${PGHOST}")) {
            log.error("CRITICAL ERROR: Database variables (PGHOST, PGPORT, etc.) are not injected from Railway!")
            hasErrors = true
        } else if (!dbUrl.startsWith("jdbc:")) {
            log.error("CRITICAL ERROR: Database URL must start with 'jdbc:' (Current: $dbUrl)")
            hasErrors = true
        }

        if (jwtSecret.isBlank() || jwtSecret == "\${JWT_SECRET}") {
            log.error("CRITICAL ERROR: JWT_SECRET environment variable is not set!")
            hasErrors = true
        } else if (jwtSecret.length < 32) {
            log.error("CRITICAL ERROR: JWT_SECRET must be at least 32 characters long for security!")
            hasErrors = true
        }

        if (hasErrors) {
            log.error("==========================================================")
            log.error("Startup failed due to missing or invalid environment variables.")
            log.error("Please check your Railway / Deployment configuration.")
            log.error("==========================================================")
            // We exit here because the app cannot function without a database or JWT secret.
            // This prevents ambiguous errors later during runtime.
            exitProcess(1)
        } else {
            log.info("Environment variables validation passed successfully.")
        }
    }
}
