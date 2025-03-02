package com.example.smartparkingsystem.ui.chatbot

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartparkingsystem.data.model.Message
import com.example.smartparkingsystem.databinding.FragmentChatbotBinding


class ChatbotFragment : Fragment() {

    private var _binding: FragmentChatbotBinding? = null
    private val binding = _binding

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var chatAdapter: ChatAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentChatbotBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferences = requireContext().getSharedPreferences("chatbot_prefs", Context.MODE_PRIVATE)

        val hasUseChatbot = sharedPreferences.getBoolean("has_used_chatbot", false)
        if (hasUseChatbot) {
            showChatbotLayout()
        } else {
            showOpeningLayout()
        }

        setUpClickListeners()
        setUpRecyclerView()

        // Add welcome message if this is not the first time
        if (hasUseChatbot) {
            addBotMessage("Hello! How may I assist you today?")
        }
    }

    private fun setUpClickListeners() {

        binding?.btnStartChat?.setOnClickListener {
            sharedPreferences.edit().putBoolean("has_used_chatbot", true).apply()
            showChatbotLayout()
            // Add welcome message when starting chat for the first time
            addBotMessage("Hello! How may I assist you today?")
        }

        binding?.icBack?.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding?.btnBack?.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding?.btnSend?.setOnClickListener {
            val message = binding.editTextMessage.text.toString()
            if (message.isNotEmpty()) {
                sendMessage(message)
                binding.editTextMessage.text.clear()
            }
        }
    }

    private fun setUpRecyclerView() {
        chatAdapter = ChatAdapter()
        binding?.recyclerViewChat?.apply {
            layoutManager = LinearLayoutManager(context).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
        }
    }

    private fun sendMessage(message: String) {
        addUserMessage(message)
        processMessage(message)
    }

    private fun addUserMessage(message: String) {
        chatAdapter.addMessage(Message(message, true))
        scrollToBottom()
    }

    private fun addBotMessage(message: String) {
        chatAdapter.addMessage(Message(message, false))
        scrollToBottom()
    }

    private fun scrollToBottom() {
        binding?.recyclerViewChat?.post {
            binding.recyclerViewChat.smoothScrollToPosition(chatAdapter.itemCount - 1)
        }
    }

    private fun processMessage(message: String) {
        // Here you would typically call your chatbot API or service
        // For now, we'll just simulate a response

        // Simulate typing delay
        binding?.recyclerViewChat?.postDelayed({
            when {
                message.contains("parking", ignoreCase = true) -> {
                    addBotMessage("It is Mark Antalya AVM.\nDo you want me to show the location?")
                }
                message.contains("hello", ignoreCase = true) ||
                        message.contains("hi", ignoreCase = true) -> {
                    addBotMessage("Hello! How may I assist you today?")
                }
                message.contains("location", ignoreCase = true) -> {
                    addBotMessage("I can help you find parking locations. Which area are you interested in?")
                }
                else -> {
                    addBotMessage("I'm sorry, I don't understand. Can you please rephrase your question?")
                }
            }
        }, 1000) // 1 second delay to simulate typing
    }


    private fun showChatbotLayout(){
        binding?.layoutChatbotOpening?.visibility = View.GONE
        binding?.layoutChatbot?.visibility = View.VISIBLE
    }

    private fun showOpeningLayout(){
        binding?.layoutChatbotOpening?.visibility = View.VISIBLE
        binding?.layoutChatbot?.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}