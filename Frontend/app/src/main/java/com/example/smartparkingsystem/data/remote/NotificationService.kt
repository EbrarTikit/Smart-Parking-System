package com.example.smartparkingsystem.data.remote

import com.example.smartparkingsystem.data.model.FcmTokenDto
import com.example.smartparkingsystem.utils.Constants.REGISTER_FCM
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface NotificationService {

    @POST(REGISTER_FCM)
    suspend fun registerFcmToken(
        @Path("userId") userId: Int,
        @Body token: FcmTokenDto
    ): Response<Unit>
}