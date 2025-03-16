package com.example.smartparkingsystem.data.repository

import com.example.smartparkingsystem.data.model.SignInRequest
import com.example.smartparkingsystem.data.model.SignInResponse
import com.example.smartparkingsystem.data.model.SignUpRequest
import com.example.smartparkingsystem.data.model.SignUpResponse
import com.example.smartparkingsystem.data.remote.UserService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UserRepository @Inject constructor(
    private val userService: UserService
) {

    suspend fun signUp(name: String, email: String, password: String): Result<SignUpResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = userService.signUp(SignUpRequest(name, email, password))
                if (response.isSuccessful) {
                    response.body()?.let {
                        Result.success(it)
                    } ?: Result.failure(Exception("No response body"))
                } else {
                    Result.failure(Exception("Error: ${response.code()} - ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun signIn(username: String, password: String): Result<SignInResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = userService.signIn(SignInRequest(username,password))
                if (response.isSuccessful) {
                    response.body()?.let {
                        Result.success(it)
                    } ?: Result.failure(Exception("No response body"))
                } else {
                    Result.failure(Exception("Error: ${response.code()} - ${response.message()}"))
                }
            }catch (e:Exception) {
                Result.failure(e)
            }
        }
    }

}