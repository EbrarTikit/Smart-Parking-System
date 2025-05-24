package com.example.smartparkingsystem.ui.signup

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.smartparkingsystem.R
import com.example.smartparkingsystem.data.model.SignUpResponse
import com.example.smartparkingsystem.databinding.FragmentSignUpBinding
import com.example.smartparkingsystem.utils.state.UiState
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SignUpFragment : Fragment() {

    private var _binding: FragmentSignUpBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SignUpViewModel by viewModels()
    private val TAG = "SignUpFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTextChangeListeners()
        setupClickListeners()
        setupObservers()
    }

    private fun setupTextChangeListeners() {
        binding.apply {
            usernameEditText.addTextChangedListener { viewModel.clearValidationState() }
            emailEditText.addTextChangedListener { viewModel.clearValidationState() }
            passwordEditText.addTextChangedListener { viewModel.clearValidationState() }
            confirmPasswordEditText.addTextChangedListener { viewModel.clearValidationState() }
        }
    }

    private fun setupObservers() {
        viewModel.signUpState.observe(viewLifecycleOwner) { state ->
            Log.d(TAG, "SignUp state: $state")
            when(state) {
                is UiState.Error -> {
                    showLoading(false)
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                }
                UiState.Loading -> showLoading(true)
                is UiState.Success<*> -> {
                    showLoading(false)
                    val response = state.data as? SignUpResponse
                    val message = "Sign up successful!"
                    Log.d(TAG, "SignUp success: $message")
                    Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
                    
                    // Use a coroutine to delay navigation
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(1500)
                        if (isAdded && !isDetached) {
                            try {
                                Log.d(TAG, "Attempting navigation to SignIn")
                                findNavController().navigate(R.id.action_signUpFragment_to_signInFragment)
                                Log.d(TAG, "Navigation to SignIn completed")
                            } catch (e: Exception) {
                                Log.e(TAG, "Navigation failed", e)
                            }
                        }
                    }
                }
            }
        }

        viewModel.validationState.observe(viewLifecycleOwner) { state ->
            binding.apply {
                usernameInputLayout.error = state.usernameError
                emailInputLayout.error = state.emailError
                passwordInputLayout.error = state.passwordError
                confirmPasswordInputLayout.error = state.confirmPasswordError
            }
        }
    }

    private fun setupClickListeners() {
        binding.apply {
            signUpButton.setOnClickListener {
                viewModel.signUp(
                    username = usernameEditText.text.toString(),
                    email = emailEditText.text.toString(),
                    password = passwordEditText.text.toString(),
                    confirmPassword = confirmPasswordEditText.text.toString()
                )
            }

            alreadyHaveAccountText.setOnClickListener {
                findNavController().navigate(R.id.action_signUpFragment_to_signInFragment)
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.apply {
            //progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            signUpButton.isEnabled = !isLoading
            usernameEditText.isEnabled = !isLoading
            emailEditText.isEnabled = !isLoading
            passwordEditText.isEnabled = !isLoading
            confirmPasswordEditText.isEnabled = !isLoading
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}