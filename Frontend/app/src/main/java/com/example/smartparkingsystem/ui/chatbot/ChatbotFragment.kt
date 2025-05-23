package com.example.smartparkingsystem.ui.chatbot

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartparkingsystem.databinding.FragmentChatbotBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChatbotFragment : Fragment() {

    private var _binding: FragmentChatbotBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: ChatbotViewModel by viewModels()
    private lateinit var chatAdapter: ChatAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatbotBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        observeViewModel()

        if (hasUsedChatbot()) {
            binding.layoutChatbotOpening.visibility = View.GONE
            binding.layoutChatbot.visibility = View.VISIBLE
            viewModel.loadChatHistory()
        } else {
            binding.layoutChatbotOpening.visibility = View.VISIBLE
            binding.layoutChatbot.visibility = View.GONE
            showWelcomeMessage()
        }
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter()
        binding.recyclerViewChat.apply {
            layoutManager = LinearLayoutManager(context).apply {
                stackFromEnd = true
                reverseLayout = false
            }
            adapter = chatAdapter
            setHasFixedSize(true)
            itemAnimator = null
        }
    }

    private fun setupClickListeners() {
        binding.btnSend.setOnClickListener {
            val message = binding.editTextMessage.text.toString().trim()
            if (message.isNotEmpty()) {
                viewModel.sendMessage(message)
                binding.editTextMessage.text.clear()
            }
        }

        binding.btnStartChat.setOnClickListener {
            //viewModel.sendMessage("Hello")

            binding.layoutChatbotOpening.visibility = View.GONE
            binding.layoutChatbot.visibility = View.VISIBLE
        }

        binding.icBack.setOnClickListener{
            findNavController().navigateUp()
        }

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

    }

    private fun observeViewModel() {
        viewModel.messages.observe(viewLifecycleOwner) { messages ->
            chatAdapter.submitList(messages)
            scrollToBottom()
        }
        
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            //binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnSend.isEnabled = !isLoading
            if (!isLoading) {
                scrollToBottom()
            }
        }
        
        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }
    
    private fun hasUsedChatbot(): Boolean {
        val sharedPreferences = requireContext().getSharedPreferences("chatbot_prefs", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("has_used_chatbot", false)
    }
    
    private fun showWelcomeMessage() {
        viewModel.addBotMessage("Merhaba! Size nasıl yardımcı olabilirim?")

        requireContext().getSharedPreferences("chatbot_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("has_used_chatbot", true)
            .apply()
    }
    
    private fun scrollToBottom() {
        binding.recyclerViewChat.post {
            if (chatAdapter.itemCount > 0) {
                binding.recyclerViewChat.smoothScrollToPosition(chatAdapter.itemCount - 1)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}