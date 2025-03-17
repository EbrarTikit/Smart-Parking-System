package com.example.smartparkingsystem.ui.auth

import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.smartparkingsystem.R
import com.example.smartparkingsystem.databinding.FragmentAuthBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AuthFragment : Fragment(R.layout.fragment_auth) {

    private var _binding: FragmentAuthBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAuthBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val welcomeText = "Welcome to GreenSpot!"
        val spannableString = SpannableString(welcomeText)

        val start = welcomeText.indexOf("GreenSpot")
        val end = start + "GreenSpot".length
        spannableString.setSpan(ForegroundColorSpan(Color.parseColor("#37B089")), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        binding.welcomeText.text = spannableString

        setupClickListeners()
    }

    fun setupClickListeners() {
        binding.signUpButton.setOnClickListener {
            findNavController().navigate(R.id.action_authFragment_to_signUpFragment)
        }

        binding.signInButton.setOnClickListener {
            findNavController().navigate(R.id.action_authFragment_to_signInFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}