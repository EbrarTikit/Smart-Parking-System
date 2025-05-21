package com.example.smartparkingsystem.data.repository

import android.provider.Settings
import android.util.Log
import com.example.smartparkingsystem.data.model.FcmTokenDto
import com.example.smartparkingsystem.data.remote.NotificationService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val notificationService: NotificationService
) {
    private val TAG = "NotificationRepository"

    suspend fun registerFcmToken(userId: Int, token: String, deviceId: String): Result<Unit> {
        Log.d(TAG, "registerFcmToken called with userId: $userId, token: $token, deviceId: $deviceId")
        return try {
            val tokenDto = FcmTokenDto(token = token, deviceId = deviceId)
            Log.d(TAG, "Sending token DTO: $tokenDto")
            
            val response = notificationService.registerFcmToken(
                userId = userId,
                token = tokenDto
            )
            
            Log.d(TAG, "Response code: ${response.code()}")
            if (response.isSuccessful) {
                Log.d(TAG, "Token registration successful")
                Result.success(Unit)
            } else {
                val errorMessage = "Failed to register FCM token: ${response.code()}"
                Log.e(TAG, errorMessage)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in registerFcmToken", e)
            Result.failure(e)
        }
    }
}