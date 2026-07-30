package com.hobbyhub.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import java.util.*

@Configuration
class MailConfig(
    @Value("\${spring.mail.host:smtp.gmail.com}") private val mailHost: String,
    @Value("\${spring.mail.port:587}") private val mailPort: Int,
    @Value("\${spring.mail.username:hobbyhub.auth@gmail.com}") private val mailUsername: String,
    @Value("\${spring.mail.password:none}") private val mailPassword: String
) {

    @Bean
    fun javaMailSender(): JavaMailSender {
        val mailSender = JavaMailSenderImpl()
        mailSender.host = mailHost
        mailSender.port = mailPort
        mailSender.username = mailUsername
        mailSender.password = mailPassword

        val props: Properties = mailSender.javaMailProperties
        props["mail.transport.protocol"] = "smtp"
        props["mail.smtp.auth"] = "true"
        props["mail.smtp.connectiontimeout"] = "5000"
        props["mail.smtp.timeout"] = "5000"
        props["mail.smtp.writetimeout"] = "5000"

        if (mailPort == 465) {
            // SSL Direct configuration for Port 465
            props["mail.smtp.ssl.enable"] = "true"
            props["mail.smtp.socketFactory.port"] = "465"
            props["mail.smtp.socketFactory.class"] = "javax.net.ssl.SSLSocketFactory"
            props["mail.smtp.socketFactory.fallback"] = "false"
            props["mail.smtp.starttls.enable"] = "false"
        } else {
            // STARTTLS configuration for Port 587 (default)
            props["mail.smtp.starttls.enable"] = "true"
            props["mail.smtp.starttls.required"] = "true"
            props["mail.smtp.ssl.enable"] = "false"
        }

        return mailSender
    }
}
