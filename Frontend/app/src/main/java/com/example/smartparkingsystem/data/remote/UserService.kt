package com.example.smartparkingsystem.data.remote

import com.example.smartparkingsystem.data.model.FavoriteListResponse
import com.example.smartparkingsystem.data.model.FavoriteResponse
import com.example.smartparkingsystem.data.model.NotificationPreferences
import com.example.smartparkingsystem.data.model.SignInRequest
import com.example.smartparkingsystem.data.model.SignInResponse
import com.example.smartparkingsystem.data.model.SignUpRequest
import com.example.smartparkingsystem.data.model.SignUpResponse
import com.example.smartparkingsystem.utils.Constants.ADD_FAVORITE
import com.example.smartparkingsystem.utils.Constants.DELETE_FAVORITE
import com.example.smartparkingsystem.utils.Constants.GET_FAVORITE
import com.example.smartparkingsystem.utils.Constants.NOTIFICATION_PREFERENCE
import com.example.smartparkingsystem.utils.Constants.NOTIFICATION_TOGGLE
import com.example.smartparkingsystem.utils.Constants.SIGNIN
import com.example.smartparkingsystem.utils.Constants.SIGNUP
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface UserService {

    @POST(SIGNUP)
    suspend fun signUp(
        @Body request: SignUpRequest
    ): Response<SignUpResponse>

    @POST(SIGNIN)
    suspend fun signIn(
        @Body request: SignInRequest
    ): Response<SignInResponse>

    @POST(ADD_FAVORITE)
    suspend fun addFavorite(
        @Path("userId") userId: Int,
        @Path("parkingId") parkingId: Int
    ): Response<Unit>

    @GET(GET_FAVORITE)
    suspend fun getFavorites(
        @Path("userId") userId: Int
    ): Response<FavoriteListResponse>

    @DELETE(DELETE_FAVORITE)
    suspend fun removeFavorite(
        @Path("userId") userId: Int,
        @Path("parkingId") parkingId: Int
    ): Response<Unit>

    @GET(NOTIFICATION_PREFERENCE)
    suspend fun getNotificationPreferences(
        @Path("userId") userId: Int
    ): Response<NotificationPreferences>

    @PUT(NOTIFICATION_TOGGLE)
    suspend fun toggleNotificationPreferences(
        @Path("userId") userId: Int
    ): Response<NotificationPreferences>

}