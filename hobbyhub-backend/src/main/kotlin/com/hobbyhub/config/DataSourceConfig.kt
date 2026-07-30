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
    @Value("\${database.url:\${DATABASE_URL:}}") private val databaseUrl: String,
    @Value("\${spring.datasource.url:}") private val springUrl: String,
    @Value("\${spring.datasource.username:}") private val springUsername: String,
    @Value("\${spring.datasource.password:}") private val springPassword: String
) {
    private val log = LoggerFactory.getLogger(DataSourceConfig::class.java)

    @Bean
    @Primary
    fun dataSource(): DataSource {
        val config = HikariConfig()
        config.driverClassName = "org.postgresql.Driver"

        if (databaseUrl.isNotBlank() && (databaseUrl.startsWith("postgres://") || databaseUrl.startsWith("postgresql://"))) {
            log.info("Detected raw DATABASE_URL (Railway/Neon format). Parsing to JDBC format...")
            try {
                val dbUri = URI(databaseUrl)
                val username = dbUri.userInfo?.split(":")?.get(0)
                val password = dbUri.userInfo?.split(":")?.get(1)
                
                var jdbcUrl = "jdbc:postgresql://${dbUri.host}:${dbUri.port}${dbUri.path}"
                if (dbUri.query != null) {
                    jdbcUrl += "?${dbUri.query}"
                } else {
                    jdbcUrl += "?sslmode=require"
                }

                config.jdbcUrl = jdbcUrl
                config.username = username
                config.password = password
                
                log.info("Successfully parsed DATABASE_URL into JDBC format.")
            } catch (e: Exception) {
                log.error("Failed to parse DATABASE_URL: ${e.message}")
                throw RuntimeException("Invalid DATABASE_URL format", e)
            }
        } else if (springUrl.isNotBlank()) {
            log.info("Using standard Spring Boot datasource properties")
            config.jdbcUrl = springUrl
            config.username = springUsername
            config.password = springPassword
        } else {
            log.warn("No database URL provided!")
        }

        // HikariCP Hardening
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
