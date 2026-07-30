package com.hobbyhub.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import java.net.URI
import javax.sql.DataSource

@Configuration
class DataSourceConfig(
    @Value("\${DATABASE_URL:}") private val databaseUrl: String,
    @Value("\${SPRING_DATASOURCE_URL:}") private val springUrl: String,
    @Value("\${SPRING_DATASOURCE_USERNAME:}") private val springUsername: String,
    @Value("\${SPRING_DATASOURCE_PASSWORD:}") private val springPassword: String,
    @Value("\${PGHOST:}") private val pgHost: String,
    @Value("\${PGPORT:5432}") private val pgPort: String,
    @Value("\${PGDATABASE:}") private val pgDatabase: String,
    @Value("\${PGUSER:}") private val pgUser: String,
    @Value("\${PGPASSWORD:}") private val pgPassword: String
) {
    private val log = LoggerFactory.getLogger(DataSourceConfig::class.java)

    @Bean
    @Primary
    fun dataSource(): DataSource {
        val config = HikariConfig()

        // 1. Try Railway DATABASE_URL (postgresql://user:pass@host:port/db)
        if (databaseUrl.isNotBlank() && (databaseUrl.startsWith("postgres://") || databaseUrl.startsWith("postgresql://"))) {
            log.info("✅ Detected DATABASE_URL from Railway environment. Parsing to JDBC format...")
            try {
                val dbUri = URI(databaseUrl)
                val userInfo = dbUri.userInfo?.split(":")
                val username = userInfo?.getOrNull(0) ?: ""
                val password = userInfo?.getOrNull(1) ?: ""

                val host = dbUri.host
                val port = if (dbUri.port != -1) dbUri.port else 5432
                val path = dbUri.path

                var jdbcUrl = "jdbc:postgresql://$host:$port$path"
                if (dbUri.query != null) {
                    jdbcUrl += "?${dbUri.query}"
                }

                config.jdbcUrl = jdbcUrl
                config.username = username
                config.password = password
                config.driverClassName = "org.postgresql.Driver"
                log.info("✅ Successfully configured HikariCP DataSource from DATABASE_URL ($host:$port)")
            } catch (e: Exception) {
                log.error("❌ Failed to parse DATABASE_URL: ${e.message}", e)
                throw RuntimeException("Invalid DATABASE_URL format", e)
            }
        } 
        // 1.5. Try if DATABASE_URL is already a JDBC URL
        else if (databaseUrl.isNotBlank() && databaseUrl.startsWith("jdbc:postgresql://")) {
            log.info("✅ Detected JDBC URL in DATABASE_URL from Railway environment.")
            config.jdbcUrl = databaseUrl
            config.driverClassName = "org.postgresql.Driver"
            // Note: username and password should ideally be in SPRING_DATASOURCE_USERNAME etc if not embedded in JDBC URL 
            // but Neon JDBC URLs usually contain the username/password in the URL or they are passed separately.
            // If they are in the URL, HikariCP will use them.
            if (springUsername.isNotBlank()) config.username = springUsername
            if (springPassword.isNotBlank()) config.password = springPassword
        }
        // 2. Try individual Railway Postgres variables (PGHOST, PGUSER, etc.)
        else if (pgHost.isNotBlank() && pgDatabase.isNotBlank()) {
            log.info("✅ Detected Railway PGHOST ($pgHost) and PGDATABASE ($pgDatabase). Constructing JDBC URL...")
            config.jdbcUrl = "jdbc:postgresql://$pgHost:$pgPort/$pgDatabase"
            config.username = pgUser
            config.password = pgPassword
            config.driverClassName = "org.postgresql.Driver"
        }
        // 3. Try standard SPRING_DATASOURCE_URL
        else if (springUrl.isNotBlank()) {
            log.info("✅ Using standard SPRING_DATASOURCE_URL configuration.")
            config.jdbcUrl = springUrl
            config.username = springUsername
            config.password = springPassword
            config.driverClassName = "org.postgresql.Driver"
        } 
        // 4. Fallback (LOCAL DEV ONLY)
        else {
            log.error("==========================================================================")
            log.error("⚠️ CRITICAL WARNING: NO DATABASE_URL OR POSTGRES VARIABLES FOUND IN RAILWAY! ⚠️")
            log.error("Please add 'DATABASE_URL' variable in Railway Dashboard referencing PostgreSQL.")
            log.error("==========================================================================")
            config.jdbcUrl = "jdbc:postgresql://localhost:5432/hobbyhub?sslmode=require"
            config.username = "postgres"
            config.password = "postgres"
            config.driverClassName = "org.postgresql.Driver"
        }

        // HikariCP Hardening Settings
        config.maximumPoolSize = 5
        config.connectionTimeout = 60000
        config.maxLifetime = 1800000
        config.idleTimeout = 600000
        config.leakDetectionThreshold = 30000
        config.initializationFailTimeout = 0
        config.validationTimeout = 5000

        return HikariDataSource(config)
    }
}
