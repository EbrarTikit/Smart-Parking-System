package com.example.smartparkingsystem.ui.signin

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartparkingsystem.data.model.SignInResponse
import com.example.smartparkingsystem.data.repository.UserRepository
import com.example.smartparkingsystem.utils.state.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val signInRepository: UserRepository
): ViewModel() {

    private val _signInState = MutableLiveData<UiState<SignInResponse>>()
    val signInState: MutableLiveData<UiState<SignInResponse>> = _signInState

    fun signIn(username: String, password: String) {
        _signInState.value = UiState.Loading
        viewModelScope.launch {
            signInRepository.signIn(username, password).fold(
                onSuccess = { response ->
                    _signInState.value = UiState.Success(response)
                },
                onFailure = { exception ->
                    _signInState.value = UiState.Error(exception.message ?: "An error occurred")
                }
            )
        }
    }

}