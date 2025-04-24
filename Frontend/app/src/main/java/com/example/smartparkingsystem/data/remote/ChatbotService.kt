package com.example.smartparkingsystem.data.remote

import com.example.smartparkingsystem.data.model.ChatHistoryItem
import com.example.smartparkingsystem.data.model.ChatRequest
import com.example.smartparkingsystem.data.model.ChatResponse
import com.example.smartparkingsystem.utils.Constants.CHAT
import com.example.smartparkingsystem.utils.Constants.HISTORY
import retrofit2.Response

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ChatbotService {

    @POST(CHAT)
    suspend fun sendMessage(
        @Body request: ChatRequest
    ): Response<ChatResponse>

    @GET(HISTORY)
    suspend fun getChatHistory(
        @Path("session_id") sessionId: String
    ): Response<List<ChatHistoryItem>>


}