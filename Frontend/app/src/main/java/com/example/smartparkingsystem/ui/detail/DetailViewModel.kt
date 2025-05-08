package com.example.smartparkingsystem.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartparkingsystem.data.model.ViewerCountResponse
import com.example.smartparkingsystem.data.model.ViewerTrackResponse
import com.example.smartparkingsystem.data.repository.ParkingManagementRepository
import com.example.smartparkingsystem.utils.state.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    val parkingManagementRepository: ParkingManagementRepository
) : ViewModel(){
    private val _userView = MutableStateFlow<UiState<ViewerTrackResponse>>(UiState.Loading)
    val userView: StateFlow<UiState<ViewerTrackResponse>> = _userView

    private val _viewerCount = MutableStateFlow<UiState<ViewerCountResponse>>(UiState.Loading)
    val viewerCount: StateFlow<UiState<ViewerCountResponse>> = _viewerCount

    private var currentParkingId: Int? = null
    private var updateJob: Job? = null

    fun trackUserView(userId: Int, parkingId: Int) {
        _userView.value = UiState.Loading
        viewModelScope.launch {
            try {
                val response = parkingManagementRepository.viewerTrack(
                    userId = userId,
                    parkingId = parkingId
                )
                _userView.value = UiState.Success(response)
                startPeriodicViewerCountUpdates(parkingId)
            } catch (e: Exception) {
                _userView.value = UiState.Error(e.message ?: "Tacking user view failed")
            }
        }
    }

    private fun startPeriodicViewerCountUpdates(parkingId: Int) {
        currentParkingId = parkingId
        updateJob?.cancel()
        updateJob = viewModelScope.launch {
            while (isActive) {
                getParkingViewerCount(parkingId)
                delay(15000) //15saniye
            }
        }
    }

    fun getParkingViewerCount(parkingId: Int) {
        _viewerCount.value = UiState.Loading
        viewModelScope.launch {
            try {
                val response = parkingManagementRepository.viewerCount(parkingId)
                _viewerCount.value = UiState.Success(response)
            } catch (e: Exception) {
                _viewerCount.value = UiState.Error(e.message ?: "Failed to get viewer count")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        updateJob?.cancel()
    }
}