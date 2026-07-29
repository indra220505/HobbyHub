package com.hobbyhub.service

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

@Service
class FcmService {
    private val logger = LoggerFactory.getLogger(FcmService::class.java)

    @Value("\${fcm.service-account-file:firebase-service-account.json}")
    private lateinit var serviceAccountFile: String

    @PostConstruct
    fun initFirebase() {
        try {
            val resource = ClassPathResource(serviceAccountFile)
            if (resource.exists()) {
                val options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(resource.inputStream))
                    .build()
                
                if (FirebaseApp.getApps().isEmpty()) {
                    FirebaseApp.initializeApp(options)
                    logger.info("Firebase Admin initialized successfully")
                }
            } else {
                logger.warn("Firebase service account file not found. Push notifications will be disabled.")
            }
        } catch (e: Exception) {
            logger.error("Failed to initialize Firebase Admin", e)
        }
    }

    fun sendNotification(targetToken: String, title: String, body: String) {
        if (FirebaseApp.getApps().isEmpty()) return

        try {
            val notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build()

            val message = Message.builder()
                .setToken(targetToken)
                .setNotification(notification)
                .build()

            FirebaseMessaging.getInstance().send(message)
            logger.info("FCM Notification sent to token: \$targetToken")
        } catch (e: Exception) {
            logger.error("Failed to send FCM notification", e)
        }
    }
}
