package com.example.smartparkingsystem.ui.signup

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.smartparkingsystem.R
import com.example.smartparkingsystem.databinding.FragmentSignUpBinding
import com.example.smartparkingsystem.utils.state.UiState
import com.google.android.material.snackbar.Snackbar

class SignUpFragment : Fragment() {

    private var _binding: FragmentSignUpBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SignUpViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

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
            when(state) {
                is UiState.Error -> {
                    showLoading(false)
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                }
                UiState.Loading -> showLoading(true)
                is UiState.Success<*> -> {
                    showLoading(false)
                    Snackbar.make(binding.root, "Sign up successfully!", Snackbar.LENGTH_LONG).show()
                    findNavController().navigate(R.id.action_signUpFragment_to_signInFragment)
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
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
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