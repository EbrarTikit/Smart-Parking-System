package com.example.smartparkingsystem.ui.signin

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.smartparkingsystem.R
import com.example.smartparkingsystem.databinding.FragmentSignInBinding
import com.example.smartparkingsystem.utils.state.UiState
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignInFragment : Fragment() {

    private var _binding: FragmentSignInBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SignInViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignInBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTextChangedListeners()
        setupClickListeners()
        setupObservers()
    }

    private fun setupTextChangedListeners() {
        binding.apply {
            emailEditText.addTextChangedListener { viewModel.clearValidationState()}
            passwordEditText.addTextChangedListener { viewModel.clearValidationState()}
        }
    }

    private fun setupClickListeners() {
        binding.apply {
            loginButton.setOnClickListener {
                viewModel.signIn(
                    emailEditText.text.toString(),
                    passwordEditText.text.toString()
                )
            }
            createAccountText.setOnClickListener {
                findNavController().navigate(R.id.action_signInFragment_to_signUpFragment)
            }
        }
    }

    private fun setupObservers() {
        viewModel.signInState.observe(viewLifecycleOwner) { state ->
            when(state) {
                is UiState.Loading -> showLoading(true)
                is UiState.Success -> {
                    showLoading(false)
                    findNavController().navigate(R.id.action_signInFragment_to_locationAccessFragment)
                }
                is UiState.Error -> {
                    showLoading(false)
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }

        viewModel.validationState.observe(viewLifecycleOwner) { state ->
            binding.apply {
                emailInputLayout.error = state.usernameError
                passwordInputLayout.error = state.passwordError
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.apply {
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            loginButton.isEnabled = !isLoading
            emailEditText.isEnabled = !isLoading
            passwordEditText.isEnabled = !isLoading
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}