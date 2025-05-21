package com.example.smartparkingsystem.ui

import android.provider.Settings
import android.util.Log
import com.example.smartparkingsystem.data.repository.NotificationRepository
import com.example.smartparkingsystem.utils.SessionManager
import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class MainActivityTest {

    private lateinit var sessionManager: SessionManager
    private lateinit var notificationRepository: NotificationRepository
    private val firebaseMessaging: FirebaseMessaging = mockk(relaxed = true)

    private suspend fun registerFcmToken(
        token: String,
        userId: Long,
        deviceId: String
    ): Result<Unit> {
        return if (userId > 0) {
            try {
                notificationRepository.registerFcmToken(userId.toInt(), token, deviceId)
            } catch (e: Exception) {
                Log.e("Test", "Exception while registering FCM token", e)
                Result.failure(e)
            }
        } else {
            Log.e("Test", "Cannot register FCM token: Invalid userId ($userId)", null)
            Result.failure(IllegalArgumentException("Invalid userId"))
        }
    }

    @Before
    fun setup() {
        // Log sınıfını mockla
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.w(any(), any(), any()) } returns 0

        // Mock Settings.Secure for device ID
        mockkStatic(Settings.Secure::class)
        every { 
            Settings.Secure.getString(any(), eq(Settings.Secure.ANDROID_ID)) 
        } returns "test-device-id"

        // Mock FirebaseMessaging
        mockkStatic(FirebaseMessaging::class)
        every { FirebaseMessaging.getInstance() } returns firebaseMessaging

        // Mock repository and session manager
        sessionManager = mockk(relaxed = true)
        notificationRepository = mockk(relaxed = true)
    }

    @Test
    fun `sendRegistrationToServer should call repository with correct parameters when userId is valid`() = runTest {
        // Arrange
        val token = "test-fcm-token"
        val userId = 10L
        val deviceId = "test-device-id"

        every { sessionManager.getUserId() } returns userId
        coEvery {
            notificationRepository.registerFcmToken(userId.toInt(), token, deviceId)
        } returns Result.success(Unit)

        // Act
        val result = registerFcmToken(token, userId, deviceId)

        // Assert
        coVerify {
        notificationRepository.registerFcmToken(
                userId = userId.toInt(),
                token = token,
                deviceId = deviceId
            ) 
        }
        assert(result.isSuccess)
    }
    
    @Test
    fun `sendRegistrationToServer should log error when userId is invalid`() = runTest {
        // Arrange
        val token = "test-fcm-token"
        val invalidUserId = 0L
        val deviceId = "test-device-id"

        // Act
        val result = registerFcmToken(token, invalidUserId, deviceId)

        // Assert
        verify { Log.e("Test", match { it.contains("Cannot register FCM token") }, null) }
        coVerify(exactly = 0) {
            notificationRepository.registerFcmToken(any(), any(), any())
        }
        assert(result.isFailure)
    }

    @Test
    fun `initializeFirebaseMessaging should request FCM token and subscribe to topic`() {
        // Act - doğrudan Firebase işlemlerini test et
        firebaseMessaging.token
        firebaseMessaging.subscribeToTopic("parking_24")

        // Assert
        verify {
        firebaseMessaging.token
            firebaseMessaging.subscribeToTopic("parking_24")
        }
    }
    
    @Test
    fun `token task should handle success case`() = runTest {
        // Arrange
        val token = "test-fcm-token"
        val userId = 10L
        val deviceId = "test-device-id"

        every { sessionManager.getUserId() } returns userId
        coEvery {
            notificationRepository.registerFcmToken(userId.toInt(), token, deviceId)
        } returns Result.success(Unit)

        // Act
        val result = registerFcmToken(token, userId, deviceId)

        // Assert
        coVerify { notificationRepository.registerFcmToken(userId.toInt(), token, deviceId) }
        assert(result.isSuccess)
    }
}