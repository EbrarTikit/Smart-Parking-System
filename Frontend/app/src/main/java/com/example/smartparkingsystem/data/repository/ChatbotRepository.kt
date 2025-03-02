package com.example.smartparkingsystem.data.repository

import com.example.smartparkingsystem.data.model.ChatHistoryItem
import com.example.smartparkingsystem.data.model.ChatRequest
import com.example.smartparkingsystem.data.model.ChatResponse
import com.example.smartparkingsystem.data.remote.ChatbotService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ChatbotRepository @Inject constructor(
    private val chatbotService: ChatbotService
) {

    suspend fun sendMessage(message: String, sessionId: String) : Result<ChatResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = chatbotService.sendMessage(ChatRequest(message,sessionId))
                if (response.isSuccessful){
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

    suspend fun getChatHistory(sessionId: String) : Result<List<ChatHistoryItem>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = chatbotService.getChatHistory(sessionId)
                if (response.isSuccessful) {
                    response.body()?.let{
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