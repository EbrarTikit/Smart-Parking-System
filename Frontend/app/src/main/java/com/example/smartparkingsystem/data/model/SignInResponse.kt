package com.example.smartparkingsystem.data.model

import com.google.gson.annotations.SerializedName

data class SignInResponse(
    @SerializedName("userId")
    val id: Long,
    val token: String,
    val type: String = "Bearer"
)