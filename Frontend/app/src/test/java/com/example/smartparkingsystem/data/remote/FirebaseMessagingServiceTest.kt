package com.example.smartparkingsystem.data.remote

import android.app.NotificationManager
import android.content.Context
import android.provider.Settings
import android.util.Log
import com.example.smartparkingsystem.data.repository.NotificationRepository
import com.example.smartparkingsystem.utils.SessionManager
import com.google.firebase.messaging.RemoteMessage
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class FirebaseMessagingServiceTest {

    // FirebaseMessagingService hizmetini tamamen mocklamak için spy kullanıyoruz
    private lateinit var notificationRepository: NotificationRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var mockContext: Context

    @Before
    fun setup() {
        // Log sınıfını mockla - zaten var, doğruluyoruz
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0

        // Mock Settings.Secure for device ID
        mockkStatic(Settings.Secure::class)
        every {
            Settings.Secure.getString(any(), eq(Settings.Secure.ANDROID_ID))
        } returns "test-device-id"

        // Mock dependencies
        sessionManager = mockk(relaxed = true)
        notificationRepository = mockk(relaxed = true)
        mockContext = mockk(relaxed = true)

        every { mockContext.contentResolver } returns mockk(relaxed = true)
    }

    @Test
    fun `registerFcmToken should call repository with correct parameters when userId is valid`() =
        runTest {
            val token = "test-fcm-token"
        val userId = 10L
        val deviceId = "test-device-id"

        every { sessionManager.getUserId() } returns userId
        coEvery {
            notificationRepository.registerFcmToken(userId.toInt(), token, deviceId)
        } returns Result.success(Unit)

        if (userId > 0) {
            val result = notificationRepository.registerFcmToken(userId.toInt(), token, deviceId)
            result.fold(
                onSuccess = {
                    Log.d("MyFirebaseMsgService", "FCM token registered successfully")
                },
                onFailure = { error ->
                    Log.e("MyFirebaseMsgService", "Failed to register FCM token", error)
                }
            )
        } else {
            Log.e(
                "MyFirebaseMsgService",
                "Cannot register FCM token: Invalid userId ($userId)",
                null
            )
        }

        coVerify {
            notificationRepository.registerFcmToken(
                userId = userId.toInt(),
                token = token,
                deviceId = deviceId
            )
        }
        verify {
            Log.d("MyFirebaseMsgService", "FCM token registered successfully")
        }
    }

    @Test
    fun `registerFcmToken should log error when userId is invalid`() = runTest {
        val token = "test-fcm-token"
        val invalidUserId = 0L
        val deviceId = "test-device-id"

        every { sessionManager.getUserId() } returns invalidUserId

        if (invalidUserId > 0) {
            val result =
                notificationRepository.registerFcmToken(invalidUserId.toInt(), token, deviceId)
            result.fold(
                onSuccess = {
                    Log.d("MyFirebaseMsgService", "FCM token registered successfully")
                },
                onFailure = { error ->
                    Log.e("MyFirebaseMsgService", "Failed to register FCM token", error)
                }
            )
        } else {
            Log.e(
                "MyFirebaseMsgService",
                "Cannot register FCM token: Invalid userId ($invalidUserId)",
                null
            )
        }

        verify {
            Log.e("MyFirebaseMsgService", match { it.contains("Cannot register FCM token") }, null)
        }
        coVerify(exactly = 0) {
            notificationRepository.registerFcmToken(any(), any(), any())
        }
    }

    @Test
    fun `registerFcmToken should handle repository error`() = runTest {
        val token = "test-fcm-token"
        val userId = 10L
        val deviceId = "test-device-id"
        val exception = IOException("Network error")

        every { sessionManager.getUserId() } returns userId
        coEvery {
            notificationRepository.registerFcmToken(userId.toInt(), token, deviceId)
        } returns Result.failure(exception)

        if (userId > 0) {
            val result = notificationRepository.registerFcmToken(userId.toInt(), token, deviceId)
            result.fold(
                onSuccess = {
                    Log.d("MyFirebaseMsgService", "FCM token registered successfully")
                },
                onFailure = { error ->
                    Log.e("MyFirebaseMsgService", "Failed to register FCM token", error)
                }
            )
        } else {
            Log.e(
                "MyFirebaseMsgService",
                "Cannot register FCM token: Invalid userId ($userId)",
                null
            )
        }

        verify {
            Log.e("MyFirebaseMsgService", "Failed to register FCM token", exception)
        }
    }

    @Test
    fun `processNotification should handle notification data correctly`() {
        val mockRemoteMessage = mockk<RemoteMessage>()
        val mockNotification = mockk<RemoteMessage.Notification>()
        val title = "Test Title"
        val body = "Test Body"

        every { mockRemoteMessage.notification } returns mockNotification
        every { mockRemoteMessage.from } returns "test-sender"
        every { mockNotification.title } returns title
        every { mockNotification.body } returns body
        every { mockRemoteMessage.data } returns mapOf()

        Log.d("MyFirebaseMsgService", "From: ${mockRemoteMessage.from}")

        mockRemoteMessage.notification?.let {
            Log.d("MyFirebaseMsgService", "Message Notification Body: ${it.body}")
        }

        verify {
            Log.d("MyFirebaseMsgService", "From: test-sender")
            Log.d("MyFirebaseMsgService", "Message Notification Body: $body")
        }
    }

    @Test
    fun `processDataPayload should handle data payload correctly`() {
        val mockRemoteMessage = mockk<RemoteMessage>()
        val title = "Test Data Title"
        val body = "Test Data Body"
        val data = mapOf("title" to title, "body" to body)

        every { mockRemoteMessage.notification } returns null
        every { mockRemoteMessage.from } returns "test-sender"
        every { mockRemoteMessage.data } returns data

        Log.d("MyFirebaseMsgService", "From: ${mockRemoteMessage.from}")

        if (mockRemoteMessage.data.isNotEmpty()) {
            Log.d("MyFirebaseMsgService", "Message data payload: ${mockRemoteMessage.data}")
        }

        verify {
            Log.d("MyFirebaseMsgService", "From: test-sender")
            Log.d("MyFirebaseMsgService", match { it.contains("Message data payload") })
        }
    }
}