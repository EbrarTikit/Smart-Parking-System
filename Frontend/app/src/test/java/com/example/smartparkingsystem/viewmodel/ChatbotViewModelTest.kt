package com.example.smartparkingsystem.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.smartparkingsystem.data.model.ChatHistoryItem
import com.example.smartparkingsystem.data.model.ChatResponse
import com.example.smartparkingsystem.data.repository.ChatbotRepository
import com.example.smartparkingsystem.ui.chatbot.ChatbotViewModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class ChatbotViewModelTest {

    private lateinit var viewModel: ChatbotViewModel
    private lateinit var repository: ChatbotRepository

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        viewModel = ChatbotViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `sendMessage success should update messages and sessionId`() = testScope.runTest {
        val userMessage = "Hi"
        val botReply = "Hello! How can I help you?"
        val response = ChatResponse(response = botReply, sessionid = "newSession")

        coEvery { repository.sendMessage(userMessage, any()) } returns Result.success(response)

        viewModel.sendMessage(userMessage)
        advanceUntilIdle()

        val messages = viewModel.messages.value!!
        assertEquals(2, messages.size)
        assertTrue(messages[0].isFromUser)
        assertEquals(userMessage, messages[0].text)
        assertFalse(messages[1].isFromUser)
        assertEquals(botReply, messages[1].text)
        assertEquals(false, viewModel.isLoading.value)
        assertEquals(null, viewModel.error.value)
    }

    @Test
    fun `sendMessage failure should update error`() = testScope.runTest {
        val userMessage = "Hi"
        val errorMessage = "Network error"

        coEvery { repository.sendMessage(userMessage, any()) } returns Result.failure(Exception(errorMessage))

        viewModel.sendMessage(userMessage)
        advanceUntilIdle()

        val messages = viewModel.messages.value!!
        assertEquals(1, messages.size)
        assertTrue(messages[0].isFromUser)
        assertEquals(userMessage, messages[0].text)
        assertEquals(false, viewModel.isLoading.value)
        assertEquals(errorMessage, viewModel.error.value)
    }

    @Test
    fun `loadChatHistory success should populate messages`() = testScope.runTest {
        val history = listOf(
            ChatHistoryItem("user","Hello", "2023-01-01T10:00:00Z"),
            ChatHistoryItem("assistant","Hi, how can I help?", "2023-01-01T10:00:01Z")
        )
        coEvery { repository.getChatHistory(any()) } returns Result.success(history)

        viewModel.loadChatHistory()
        advanceUntilIdle()

        val messages = viewModel.messages.value!!

        // Loglama ekleyelim:
        messages.forEachIndexed { index, message ->
            println("Message[$index] = ${message.text}, isFromUser=${message.isFromUser}")
        }

        assertEquals(2, messages.size)
        assertTrue(messages[0].isFromUser) // user
        assertTrue(!messages[1].isFromUser) // assistant
    }


    @Test
    fun `loadChatHistory failure should update error`() = testScope.runTest {
        val errorMsg = "Server error"
        coEvery { repository.getChatHistory(any()) } returns Result.failure(Exception(errorMsg))

        viewModel.loadChatHistory()
        advanceUntilIdle()

        assertEquals(errorMsg, viewModel.error.value)
    }
}
