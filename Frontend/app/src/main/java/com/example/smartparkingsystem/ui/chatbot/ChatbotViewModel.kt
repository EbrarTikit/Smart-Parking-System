package com.example.smartparkingsystem.ui.chatbot

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartparkingsystem.data.model.ChatHistoryItem
import com.example.smartparkingsystem.data.model.Message
import com.example.smartparkingsystem.data.repository.ChatbotRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatbotViewModel @Inject constructor(
    private val chatbotRepository: ChatbotRepository
) : ViewModel() {

    private val _messages = MutableLiveData<List<Message>>(emptyList())
    val messages: LiveData<List<Message>> = _messages

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private var sessionId = "ebrar"

    fun sendMessage(messageText: String) {
        _isLoading.value = true
        addMessage(Message(messageText, true))

        viewModelScope.launch {
            chatbotRepository.sendMessage(messageText, sessionId).fold(
                onSuccess = { response ->
                    addMessage(Message(response.response, false))
                    sessionId = response.sessionid
                    _isLoading.value = false
                },
                onFailure = { exception ->
                    _error.value = exception.message
                    _isLoading.value = false
                }
            )
        }
    }

    fun loadChatHistory() {
        _isLoading.value = true

        viewModelScope.launch {
            chatbotRepository.getChatHistory(sessionId).fold(
                onSuccess = { historyItems ->
                    val messages = historyItems.map { item ->
                        Message(
                            text = item.content,
                            isFromUser = item.role == "user"
                        )
                    }
                    _messages.value = messages
                    _isLoading.value = false
                },
                onFailure = { exception ->
                    _error.value = exception.message
                    _isLoading.value = false
                }
            )
        }
    }

    private fun addMessage(message: Message) {
        val currentMessages = _messages.value ?: emptyList()
        _messages.value = currentMessages + message
    }

    fun clearError() {
        _error.value = null
    }
}