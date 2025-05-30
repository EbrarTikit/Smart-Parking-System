package com.example.smartparkingsystem.data.repository

import android.util.Log
import com.example.smartparkingsystem.data.model.FavoriteListResponse
import com.example.smartparkingsystem.data.model.FavoriteResponse
import com.example.smartparkingsystem.data.model.NotificationPreferences
import com.example.smartparkingsystem.data.model.SignInRequest
import com.example.smartparkingsystem.data.model.SignInResponse
import com.example.smartparkingsystem.data.model.SignUpRequest
import com.example.smartparkingsystem.data.model.SignUpResponse
import com.example.smartparkingsystem.data.remote.UserService
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userService: UserService,
    private val gson: Gson
) {

    suspend fun signUp(name: String, email: String, password: String): Result<SignUpResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = userService.signUp(SignUpRequest(name, email, password))
                Log.d("UserRepository", "SignUp response: ${response.isSuccessful}, code: ${response.code()}")
                
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody != null) {
                        Log.d("UserRepository", "SignUp success: ${responseBody.message}")
                        Result.success(responseBody)
                    } else {
                        val successMessage = response.raw().message
                        Log.d("UserRepository", "SignUp success but empty body, using raw message: $successMessage")
                        Result.success(SignUpResponse(message = successMessage))
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("UserRepository", "SignUp error: $errorBody")
                    
                    if (errorBody != null) {
                        try {
                            val errorResponse = gson.fromJson(errorBody, SignUpResponse::class.java)
                            Result.failure(Exception(errorResponse.message))
                        } catch (e: Exception) {
                            Result.failure(Exception(errorBody))
                        }
                    } else {
                        Result.failure(Exception("Error: ${response.code()} - ${response.message()}"))
                    }
                }
            } catch (e: Exception) {
                Log.e("UserRepository", "SignUp exception", e)
                Result.failure(e)
            }
        }
    }

    suspend fun signIn(username: String, password: String): Result<SignInResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = userService.signIn(SignInRequest(username, password))
                if (response.isSuccessful) {
                    response.body()?.let {
                        Log.d("UserRepository", "SignIn success, raw body: ${gson.toJson(it)}")
                        Log.d(
                            "UserRepository",
                            "SignIn response values: userId=${it.id}, token=${it.token}"
                        )
                        Result.success(it)
                    } ?: Result.failure(Exception("No response body"))
                } else {
                    Result.failure(Exception("Error: ${response.code()} - ${response.message()}"))
                }
            } catch (e: Exception) {
                Log.e("UserRepository", "SignIn exception", e)
                Result.failure(e)
            }
        }
    }

    suspend fun addFavorite(userId: Int, parkingId: Int): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("UserRepository", "Adding favorite: userId=$userId, parkingId=$parkingId")
                val response = userService.addFavorite(userId, parkingId)
                Log.d("UserRepository", "Add favorite response: isSuccessful=${response.isSuccessful}, code=${response.code()}")

                if (response.isSuccessful) {
                    response.body()?.let {
                        Log.d("UserRepository", "Add favorite success: ${gson.toJson(it)}")
                        Result.success(it)
                    } ?: run {
                        Log.e("UserRepository", "Add favorite error: No response body")
                        Result.failure(Exception("No response body"))
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("UserRepository", "Add favorite error: $errorBody")
                    Result.failure(Exception(errorBody ?: "Error: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e("UserRepository", "Add favorite exception", e)
                Result.failure(e)
            }
        }
    }

    suspend fun removeFavorite(userId: Int, parkingId: Int): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("UserRepository", "Removing favorite: userId=$userId, parkingId=$parkingId")
                val response = userService.removeFavorite(userId, parkingId)
                Log.d("UserRepository", "Remove favorite response: isSuccessful=${response.isSuccessful}, code=${response.code()}")

                if (response.isSuccessful) {
                    response.body()?.let {
                        Log.d("UserRepository", "Remove favorite success: ${gson.toJson(it)}")
                        Result.success(it)
                    } ?: run {
                        Log.e("UserRepository", "Remove favorite error: No response body")
                        Result.failure(Exception("No response body"))
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("UserRepository", "Remove favorite error: $errorBody")
                    Result.failure(Exception(errorBody ?: "Error: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e("UserRepository", "Remove favorite exception", e)
                Result.failure(e)
            }
        }
    }

    suspend fun getFavorites(userId: Int): Result<FavoriteListResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = userService.getFavorites(userId)
                if (response.isSuccessful) {
                    response.body()?.let { body: FavoriteListResponse ->
                        Result.success(body)
                    } ?: run {
                        Log.e("UserRepository", "Get favorites error: No response body")
                        Result.failure(Exception("No response body"))
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Result.failure(Exception(errorBody ?: "Error: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getNotificationPreferences(userId: Int): Result<NotificationPreferences> {
        return withContext(Dispatchers.IO) {
            try {
                val response = userService.getNotificationPreferences(userId)
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

    suspend fun toggleNotificationPreferences(userId: Int): Result<NotificationPreferences> {
        return withContext(Dispatchers.IO) {
            try {
                val response = userService.toggleNotificationPreferences(userId)
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
    
}