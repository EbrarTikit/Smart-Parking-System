package com.example.smartparkingsystem.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartparkingsystem.data.model.LocationResponse
import com.example.smartparkingsystem.data.model.NotificationPreferences
import com.example.smartparkingsystem.data.model.ParkingListResponse
import com.example.smartparkingsystem.data.repository.NavigationRepository
import com.example.smartparkingsystem.data.repository.ParkingManagementRepository
import com.example.smartparkingsystem.data.repository.UserRepository
import com.example.smartparkingsystem.utils.state.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val navigationRepository: NavigationRepository,
    private val parkingManagementRepository: ParkingManagementRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<LocationResponse>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<LocationResponse>>> = _uiState

    private val _parkings = MutableStateFlow<UiState<List<ParkingListResponse>>>(UiState.Loading)
    val parkings: StateFlow<UiState<List<ParkingListResponse>>> = _parkings

    private val _notificationState = MutableStateFlow<UiState<NotificationPreferences>>(UiState.Loading)
    val notificationState: StateFlow<UiState<NotificationPreferences>> = _notificationState

    fun updateNotificationPreferences(userId: Int, isEnabled: Boolean) {
        viewModelScope.launch {
            try {
                val response = userRepository.toggleNotificationPreferences(userId)
                response.onSuccess { preferences ->
                    _notificationState.value = UiState.Success(preferences)
                }.onFailure { error ->
                    _notificationState.value = UiState.Error(error.message ?: "Failed to update notification preferences")
                }
            } catch (e: Exception) {
                _notificationState.value = UiState.Error(e.message ?: "Failed to update notification preferences")
            }
        }
    }

    fun fetchParkings() {
        _parkings.value = UiState.Loading
        viewModelScope.launch {
            try {
                val response = parkingManagementRepository.getParkingList()
                _parkings.value = UiState.Success(response)
            } catch (e: Exception) {
                _parkings.value = UiState.Error(e.localizedMessage ?: "Bilinmeyen hata")
            }
        }
    }

    fun fetchLocations() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val locations = navigationRepository.getAllLocations()
                _uiState.value = UiState.Success(locations)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Error when loading locations")
            }
        }
    }
}