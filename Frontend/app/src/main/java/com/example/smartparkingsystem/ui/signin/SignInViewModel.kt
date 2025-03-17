package com.example.smartparkingsystem.ui.signin

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartparkingsystem.data.model.SignInResponse
import com.example.smartparkingsystem.data.repository.UserRepository
import com.example.smartparkingsystem.utils.SessionManager
import com.example.smartparkingsystem.utils.state.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val signInRepository: UserRepository,
    private val sessionManager: SessionManager
): ViewModel() {

    private val _signInState = MutableLiveData<UiState<SignInResponse>>()
    val signInState: MutableLiveData<UiState<SignInResponse>> = _signInState

    private val _validationState = MutableLiveData<ValidationState>()
    val validationState: MutableLiveData<ValidationState> = _validationState

    fun signIn(username: String, password: String) {
        if(!validateInput(username,password)) {
            return
        }

        _signInState.value = UiState.Loading
        viewModelScope.launch {
            signInRepository.signIn(username, password).fold(
                onSuccess = { response ->
                    _signInState.value = UiState.Success(response)
                    saveUserSession(response)
                },
                onFailure = { exception ->
                    _signInState.value = UiState.Error(exception.message ?: "An error occurred")
                }
            )
        }
    }

    private fun saveUserSession(response: SignInResponse) {
        sessionManager.saveUserSession(response.id, response.token)
    }

    private fun validateInput(username: String, password: String): Boolean {
        val validationState = ValidationState(
            usernameError = if (username.isBlank()) "Username is required" else null,
            passwordError = if (password.isBlank()) "Password is required" else null
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
    val passwordError: String? = null
) {
    val hasErrors: Boolean
        get() = usernameError != null || passwordError != null
}