package com.example.smartparkingsystem.data.model

import com.google.gson.annotations.SerializedName

data class ChatRequest(
    val message: String,
    @SerializedName("session_id")
    val sessionid: String
)
