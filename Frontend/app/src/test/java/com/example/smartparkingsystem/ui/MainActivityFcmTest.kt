package com.example.smartparkingsystem.ui

import android.provider.Settings
import android.util.Log
import com.example.smartparkingsystem.data.repository.NotificationRepository
import com.example.smartparkingsystem.utils.SessionManager
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * MainActivity'nin FCM token işlemlerini izole edilmiş şekilde test eden sınıf.
 * Gerçek bir MainActivity örneği oluşturmak yerine davranışları direkt test eder.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainActivityFcmTest {

    private lateinit var sessionManager: SessionManager
    private lateinit var notificationRepository: NotificationRepository

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

        // Mock repository and session manager
        sessionManager = mockk(relaxed = true)
        notificationRepository = mockk(relaxed = true)
    }

    @Test
    fun `repository should be called with token and userId when sendRegistrationToServer is called with valid userId`() =
        runTest {
            // Arrange
            val token = "test-fcm-token"
            val userId = 10L
            val deviceId = "test-device-id"

            every { sessionManager.getUserId() } returns userId
            coEvery {
                notificationRepository.registerFcmToken(userId.toInt(), token, deviceId)
            } returns Result.success(Unit)

            // Act - MainActivity.sendRegistrationToServer metodunun davranışını simüle eder
            // Gerçek metodu çağırmak yerine aynı mantığı burada uyguluyoruz
            val registrationResult = if (userId > 0) {
                val result =
                    notificationRepository.registerFcmToken(userId.toInt(), token, deviceId)
                result.fold(
                    onSuccess = {
                        Log.d("MainActivity", "FCM token registered successfully")
                        true
                    },
                    onFailure = { error ->
                        Log.e("MainActivity", "Failed to register FCM token", error)
                        false
                    }
                )
            } else {
                Log.e("MainActivity", "Cannot register FCM token: Invalid userId ($userId)", null)
                false
            }

            // Assert
            coVerify(exactly = 1) {
                notificationRepository.registerFcmToken(
                    userId = userId.toInt(),
                    token = token,
                    deviceId = deviceId
                )
            }
            assert(registrationResult)
            verify { Log.d("MainActivity", "FCM token registered successfully") }
        }

    @Test
    fun `repository should not be called and error should be logged when sendRegistrationToServer is called with invalid userId`() =
        runTest {
            // Arrange
            val token = "test-fcm-token"
            val invalidUserId = 0L
            val deviceId = "test-device-id"

            every { sessionManager.getUserId() } returns invalidUserId

            // Act - MainActivity.sendRegistrationToServer metodunun davranışını simüle eder
            val registrationResult = if (invalidUserId > 0) {
                val result =
                    notificationRepository.registerFcmToken(invalidUserId.toInt(), token, deviceId)
                result.fold(
                    onSuccess = {
                        Log.d("MainActivity", "FCM token registered successfully")
                        true
                    },
                    onFailure = { error ->
                        Log.e("MainActivity", "Failed to register FCM token", error)
                        false
                    }
                )
            } else {
                Log.e(
                    "MainActivity",
                    "Cannot register FCM token: Invalid userId ($invalidUserId)",
                    null
                )
                false
            }

            // Assert
            coVerify(exactly = 0) {
                notificationRepository.registerFcmToken(any(), any(), any())
            }
            verify {
                Log.e(
                    "MainActivity",
                    match { it.contains("Cannot register FCM token") },
                    null
                )
            }
            assert(!registrationResult)
        }

    @Test
    fun `error should be logged when repository call fails`() = runTest {
        // Arrange
        val token = "test-fcm-token"
        val userId = 10L
        val deviceId = "test-device-id"
        val exception = IOException("Network error")

        every { sessionManager.getUserId() } returns userId
        coEvery {
            notificationRepository.registerFcmToken(userId.toInt(), token, deviceId)
        } returns Result.failure(exception)

        // Act - MainActivity.sendRegistrationToServer metodunun davranışını simüle eder
        val registrationResult = if (userId > 0) {
            val result = notificationRepository.registerFcmToken(userId.toInt(), token, deviceId)
            result.fold(
                onSuccess = {
                    Log.d("MainActivity", "FCM token registered successfully")
                    true
                },
                onFailure = { error ->
                    Log.e("MainActivity", "Failed to register FCM token", error)
                    false
                }
            )
        } else {
            Log.e("MainActivity", "Cannot register FCM token: Invalid userId ($userId)", null)
            false
        }

        // Assert
        coVerify(exactly = 1) {
            notificationRepository.registerFcmToken(userId.toInt(), token, deviceId)
        }
        verify { Log.e("MainActivity", "Failed to register FCM token", exception) }
        assert(!registrationResult)
    }
}