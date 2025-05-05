package com.example.smartparkingsystem.ui.signup

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartparkingsystem.data.model.SignUpResponse
import com.example.smartparkingsystem.data.repository.UserRepository
import com.example.smartparkingsystem.utils.state.UiState
import com.example.smartparkingsystem.utils.validation.EmailValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val repository: UserRepository,
    private val emailValidator: EmailValidator
) : ViewModel() {

    private val _signUpState = MutableLiveData<UiState<SignUpResponse>>()
    val signUpState: LiveData<UiState<SignUpResponse>> = _signUpState

    private val _validationState = MutableLiveData<ValidationState>()
    val validationState: LiveData<ValidationState> = _validationState

    fun signUp(username: String, email: String, password: String, confirmPassword: String) {
        if (!validateInput(username, email, password, confirmPassword)) {
            return
        }

        _signUpState.value = UiState.Loading
        viewModelScope.launch {
            repository.signUp(username, email, password).fold(
                onSuccess = { response ->
                    _signUpState.value = UiState.Success(response)
                },
                onFailure = { exception ->
                    _signUpState.value = UiState.Error(exception.message ?: "An error occurred")
                }
            )
        }
    }

    private fun validateInput(username: String, email: String, password: String, confirmPassword: String): Boolean {
        val validationState = ValidationState(
            usernameError = if (username.isBlank()) "Username is required" else null,
            emailError =  when {
                email.isBlank() -> "Email is required!"
                !emailValidator.isValid(email) -> "Invalid email format!"
                else -> null
            },
            passwordError = when {
                password.isBlank() -> "Password is required!"
                password.length < 6 -> "Password must be at least 6 characters long!"
                else -> null
            },
            confirmPasswordError = when {
                confirmPassword.isBlank() -> "Confirm password is required!"
                confirmPassword != password -> "Passwords do not match!"
                else -> null
            }
        )
        _validationState.value = validationState
        return !validationState.hasErrors
    }

    fun clearValidationState() {
        _validationState.value = ValidationState()
    }
}

data class ValidationState(
    val usernameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null
) {
    val hasErrors: Boolean
        get() = usernameError != null || emailError != null ||
                passwordError != null || confirmPasswordError != null
}