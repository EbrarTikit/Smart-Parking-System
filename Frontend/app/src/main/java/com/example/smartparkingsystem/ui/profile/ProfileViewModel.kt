package com.example.smartparkingsystem.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartparkingsystem.data.model.NotificationPreferences
import com.example.smartparkingsystem.data.repository.UserRepository
import com.example.smartparkingsystem.utils.state.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _notificationState = MutableStateFlow<UiState<NotificationPreferences>>(UiState.Loading)
    val notificationState: StateFlow<UiState<NotificationPreferences>> = _notificationState

    fun getNotificationPreferences(userId: Int) {
        _notificationState.value = UiState.Loading
        viewModelScope.launch {
            userRepository.getNotificationPreferences(userId).onSuccess {
                _notificationState.value = UiState.Success(it)
            }.onFailure {
                _notificationState.value = UiState.Error(it.message ?: "Hata oluştu")
            }
        }
    }

    fun setNotificationPreferences(userId: Int, enabled: Boolean) {
        _notificationState.value = UiState.Loading
        viewModelScope.launch {
            userRepository.getNotificationPreferences(userId).onSuccess { current ->
                if (current.parkingFullNotification != enabled) {
                    userRepository.toggleNotificationPreferences(userId).onSuccess { updated ->
                        _notificationState.value = UiState.Success(updated)
                    }.onFailure {
                        _notificationState.value = UiState.Error(it.message ?: "Güncellenemedi")
                    }
                } else {
                    _notificationState.value = UiState.Success(current)
                }
            }.onFailure {
                _notificationState.value = UiState.Error(it.message ?: "Hata oluştu")
            }
        }
    }
}