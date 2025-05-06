package com.example.smartparkingsystem.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartparkingsystem.data.model.ViewerTrackResponse
import com.example.smartparkingsystem.data.repository.ParkingManagementRepository
import com.example.smartparkingsystem.utils.state.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    val parkingManagementRepository: ParkingManagementRepository
) : ViewModel(){
    private val _userView = MutableStateFlow<UiState<ViewerTrackResponse>>(UiState.Loading)
    val userView: StateFlow<UiState<ViewerTrackResponse>> = _userView

    fun trackUserView(userId: Int, parkingId: Int) {
        _userView.value = UiState.Loading
        viewModelScope.launch {
            try {
                val response = parkingManagementRepository.viewerTrack(
                    userId = userId,
                    parkingId = parkingId
                )
                _userView.value = UiState.Success(response)
            } catch (e: Exception) {
                _userView.value = UiState.Error(e.message ?: "Tacking user view failed")
            }
        }
    }
}