package com.example.smartparkingsystem.data.repository

import android.util.Log
import com.example.smartparkingsystem.data.model.FcmTokenDto
import com.example.smartparkingsystem.data.remote.NotificationService
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationRepositoryTest {

    private lateinit var notificationService: NotificationService
    private lateinit var notificationRepository: NotificationRepository

    @Before
    fun setup() {
        // Log sınıfını mockla
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0

        notificationService = mockk()
        notificationRepository = NotificationRepository(notificationService)
    }

    @Test
    fun `registerFcmToken returns success when service call is successful`() = runTest {
        // Arrange
        val userId = 1
        val token = "test-token"
        val deviceId = "test-device-id"
        val tokenDto = FcmTokenDto(token, deviceId)

        coEvery {
            notificationService.registerFcmToken(userId, tokenDto)
        } returns Response.success(Unit)

        // Act
        val result = notificationRepository.registerFcmToken(userId, token, deviceId)

        // Assert
        assertTrue(result.isSuccess)
        coVerify { notificationService.registerFcmToken(userId, tokenDto) }
    }

    @Test
    fun `registerFcmToken returns failure when service call is unsuccessful`() = runTest {
        // Arrange
        val userId = 1
        val token = "test-token"
        val deviceId = "test-device-id"
        val tokenDto = FcmTokenDto(token, deviceId)

        coEvery {
            notificationService.registerFcmToken(userId, tokenDto)
        } returns Response.error(400, mockk(relaxed = true))

        // Act
        val result = notificationRepository.registerFcmToken(userId, token, deviceId)

        // Assert
        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull()?.message?.contains("Failed to register FCM token") == true)
        coVerify { notificationService.registerFcmToken(userId, tokenDto) }
    }

    @Test
    fun `registerFcmToken returns failure when exception occurs`() = runTest {
        // Arrange
        val userId = 1
        val token = "test-token"
        val deviceId = "test-device-id"
        val exception = IOException("Network error")

        coEvery {
            notificationService.registerFcmToken(any(), any())
        } throws exception

        // Act
        val result = notificationRepository.registerFcmToken(userId, token, deviceId)

        // Assert
        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull() is IOException)
        coVerify { notificationService.registerFcmToken(userId, any()) }
    }

    @Test
    fun `registerFcmToken creates correct FcmTokenDto`() = runTest {
        // Arrange
        val userId = 1
        val token = "specific-test-token-123"
        val deviceId = "specific-device-id-456"

        coEvery {
            notificationService.registerFcmToken(any(), any())
        } returns Response.success(Unit)

        // Act
        notificationRepository.registerFcmToken(userId, token, deviceId)

        // Assert
        coVerify {
            notificationService.registerFcmToken(
                userId,
                withArg { tokenDto ->
                    assertTrue(tokenDto.token == token)
                    assertTrue(tokenDto.deviceId == deviceId)
                }
            )
        }
    }
}