package com.example.smartparkingsystem.data.remote

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.smartparkingsystem.R
import com.example.smartparkingsystem.data.repository.NotificationRepository
import com.example.smartparkingsystem.utils.SessionManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "MyFirebaseMsgService"

    @Inject
    lateinit var notificationRepository: NotificationRepository

    @Inject
    lateinit var sessionManager: SessionManager

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")

        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            showNotification(it.title, it.body)
        }

        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            val title = remoteMessage.data["title"]
            val body = remoteMessage.data["body"]
            if (title != null && body != null) {
                showNotification(title, body)
            }
        }
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String) {
        val userId = sessionManager.getUserId()
        Log.d(TAG, "Attempting to register FCM token for userId: $userId")
        
        if (userId > 0) {
            val deviceId = Settings.Secure.getString(
                applicationContext.contentResolver,
                Settings.Secure.ANDROID_ID
            )
            Log.d(TAG, "Device ID: $deviceId")
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    Log.d(TAG, "Calling notificationRepository.registerFcmToken")
                    val result = notificationRepository.registerFcmToken(userId.toInt(), token, deviceId)
                    result.fold(
                        onSuccess = {
                            Log.d(TAG, "FCM token registered successfully")
                        },
                        onFailure = { error ->
                            Log.e(TAG, "Failed to register FCM token", error)
                        }
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Exception while registering FCM token", e)
                }
            }
        } else {
            Log.e(TAG, "Cannot register FCM token: Invalid userId ($userId)")
        }
    }

    private fun showNotification(title: String?, body: String?) {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "parking_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Parking Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for parking updates"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title ?: "Otopark Bildirimi")
            .setContentText(body ?: "Yeni bir bildirim var.")
            .setSmallIcon(R.drawable.ic_notifications)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}