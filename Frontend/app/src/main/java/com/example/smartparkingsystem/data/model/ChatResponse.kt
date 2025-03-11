package com.example.smartparkingsystem.data.model

import com.google.gson.annotations.SerializedName

data class ChatResponse(
    val response: String,
    @SerializedName("session_id")
    val sessionid: String
)
