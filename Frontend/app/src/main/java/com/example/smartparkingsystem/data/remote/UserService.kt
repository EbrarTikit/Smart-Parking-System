package com.example.smartparkingsystem.data.remote

import com.example.smartparkingsystem.data.model.SignUpRequest
import com.example.smartparkingsystem.data.model.SignUpResponse
import com.example.smartparkingsystem.utils.Constants.SIGNIN
import com.example.smartparkingsystem.utils.Constants.SIGNUP
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface UserService {

    @POST(SIGNUP)
    suspend fun signUp(
        @Body request: SignUpRequest
    ): Response<SignUpResponse>

    @POST(SIGNIN)
    suspend fun signIn(

    )

}