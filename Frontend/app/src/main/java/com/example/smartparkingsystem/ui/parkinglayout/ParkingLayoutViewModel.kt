package com.example.smartparkingsystem.ui.parkinglayout

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartparkingsystem.data.model.ParkingLayoutResponse
import com.example.smartparkingsystem.data.repository.ParkingManagementRepository
import com.example.smartparkingsystem.utils.state.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class ParkingLayoutViewModel @Inject constructor(
    private val parkingManagementRepository: ParkingManagementRepository
) : ViewModel() {
    private val _layout = MutableLiveData<UiState<ParkingLayoutResponse>>()
    val layout: LiveData<UiState<ParkingLayoutResponse>> = _layout

    fun getParkingLayout(parkingId: Int) {
        _layout.value = UiState.Loading
        viewModelScope.launch {
            try {
                val response = parkingManagementRepository.getParkingLayout(parkingId)
                _layout.value = UiState.Success(response)
            } catch (e: Exception) {
                _layout.value = UiState.Error(e.message ?: "An error occurred")
            }
        }
    }

}