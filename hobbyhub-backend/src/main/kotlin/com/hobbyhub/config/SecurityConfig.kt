package com.hobbyhub.config

import com.hobbyhub.security.JwtAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter
) {

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .headers { headers -> headers.frameOptions { it.disable() } }
            .authorizeHttpRequests { auth ->
                auth.requestMatchers(
                    "/api/v1/auth/**",
                    "/api/auth/**",
                    "/api/chat/**",
                    "/chat/**",
                    "/chat",
                    "/signaling/**",
                    "/signaling",
                    "/error",
                    "/actuator/health/**"
                ).permitAll()
                    .anyRequest().authenticated()
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
        
        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): org.springframework.web.cors.CorsConfigurationSource {
        val configuration = org.springframework.web.cors.CorsConfiguration()
        configuration.allowedOriginPatterns = listOf("*") // Allow all for mobile, but could be restricted to specific web domains
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
        configuration.allowedHeaders = listOf("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin")
        configuration.allowCredentials = true
        configuration.maxAge = 3600L
        
        val source = org.springframework.web.cors.UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}
