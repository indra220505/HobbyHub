package com.hobbyhub.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.hobbyhub.R

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Here you would normally send the new token to your backend
        // to associate it with the current logged-in user.
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        remoteMessage.notification?.let {
            showNotification(it.title ?: "HobbyHub", it.body ?: "")
        }
    }

    private fun showNotification(title: String, body: String) {
        val channelId = "HobbyHub_Notifications"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "HobbyHub Notifications", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            // Using a default Android icon for now. You should replace this with your app's icon.
            .setSmallIcon(android.R.drawable.ic_dialog_info) 
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)

        notificationManager.notify(0, notificationBuilder.build())
    }
}
