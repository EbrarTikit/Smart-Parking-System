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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartparkingsystem.databinding.FragmentChatbotBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChatbotFragment : Fragment() {

    private var _binding: FragmentChatbotBinding? = null
    private val binding get() = _binding!!

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var chatAdapter: ChatAdapter
    
    private val viewModel: ChatbotViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatbotBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferences = requireContext().getSharedPreferences("chatbot_prefs", Context.MODE_PRIVATE)

        val hasUsedChatbot = sharedPreferences.getBoolean("has_used_chatbot", false)
        if (hasUsedChatbot) {
            showChatbotLayout()
            // Geçmiş sohbeti yükle
            viewModel.loadChatHistory()
        } else {
            showOpeningLayout()
        }

        setUpRecyclerView()
        setUpClickListeners()
        observeViewModel()
    }

    private fun setUpRecyclerView() {
        chatAdapter = ChatAdapter()
        binding.recyclerViewChat.apply {
            layoutManager = LinearLayoutManager(context).apply {
                stackFromEnd = true // Mesajlar aşağıdan yukarıya doğru görünür
            }
            adapter = chatAdapter
        }
    }

    private fun setUpClickListeners() {
        binding.btnStartChat.setOnClickListener {
            sharedPreferences.edit().putBoolean("has_used_chatbot", true).apply()
            showChatbotLayout()
        }

        binding.icBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.btnSend.setOnClickListener {
            val message = binding.editTextMessage.text.toString().trim()
            if (message.isNotEmpty()) {
                viewModel.sendMessage(message)
                binding.editTextMessage.text.clear()
            }
        }
    }

    private fun observeViewModel() {
        viewModel.messages.observe(viewLifecycleOwner) { messages ->
            chatAdapter.submitList(messages)
            if (messages.isNotEmpty()) {
                binding.recyclerViewChat.post {
                    binding.recyclerViewChat.smoothScrollToPosition(messages.size - 1)
                }
            }
        }
        
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnSend.isEnabled = !isLoading
        }
        
        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    private fun showChatbotLayout(){
        binding.layoutChatbotOpening.visibility = View.GONE
        binding.layoutChatbot.visibility = View.VISIBLE
    }

    private fun showOpeningLayout(){
        binding.layoutChatbotOpening.visibility = View.VISIBLE
        binding.layoutChatbot.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}