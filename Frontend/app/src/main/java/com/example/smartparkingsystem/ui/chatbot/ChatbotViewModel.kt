package com.example.smartparkingsystem.ui.chatbot

import androidx.lifecycle.ViewModel
import com.example.smartparkingsystem.data.repository.ChatbotRepository
import javax.inject.Inject

class ChatbotViewModel @Inject constructor(
    private val chatbotRepository: ChatbotRepository
) : ViewModel() {

}