package com.example.smartparkingsystem.ui.detail

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartparkingsystem.data.model.FavoriteResponse
import com.example.smartparkingsystem.data.model.ViewerCountResponse
import com.example.smartparkingsystem.data.model.ViewerTrackResponse
import com.example.smartparkingsystem.data.repository.ParkingManagementRepository
import com.example.smartparkingsystem.data.repository.UserRepository
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
    val parkingManagementRepository: ParkingManagementRepository,
    val userRepository: UserRepository
) : ViewModel(){
    private val _userView = MutableStateFlow<UiState<ViewerTrackResponse>>(UiState.Loading)
    val userView: StateFlow<UiState<ViewerTrackResponse>> = _userView

    private val _viewerCount = MutableStateFlow<UiState<ViewerCountResponse>>(UiState.Loading)
    val viewerCount: StateFlow<UiState<ViewerCountResponse>> = _viewerCount

    private val _favoriteState = MutableStateFlow<UiState<Boolean>>(UiState.Loading)
    val favoriteState: StateFlow<UiState<Boolean>> = _favoriteState

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

    fun checkIfFavorite(userId: Int, parkingId: Int) {
        _favoriteState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val response = userRepository.getFavorites(userId)
                response.onSuccess { favoriteList ->
                    val isFavorite = favoriteList.any { it.id == parkingId }
                    _favoriteState.value = UiState.Success(isFavorite)
                }.onFailure { error ->
                    _favoriteState.value =
                        UiState.Error(error.message ?: "Failed to check favorite status")
                }
            } catch (e: Exception) {
                _favoriteState.value = UiState.Error(e.message ?: "Failed to check favorite status")
            }
        }
    }

    fun toggleFavorite(userId: Int, parkingId: Int) {
        // İşlem başladığında loading durumuna geç
        Log.d("DetailViewModel", "toggleFavorite called with userId=$userId, parkingId=$parkingId")
        _favoriteState.value = UiState.Loading

        viewModelScope.launch {
            try {
                // Önce mevcut favori durumunu kontrol et
                Log.d("DetailViewModel", "Getting current favorites list")
                val response = userRepository.getFavorites(userId)
                response.onSuccess { favoriteList ->
                    val isFavorite = favoriteList.any { it.id == parkingId }
                    Log.d("DetailViewModel", "Current favorite status: $isFavorite")

                    if (isFavorite) {
                        // Favorilerden çıkar
                        Log.d("DetailViewModel", "Removing from favorites")
                        userRepository.removeFavorite(userId, parkingId).onSuccess {
                            Log.d("DetailViewModel", "Successfully removed from favorites")
                            _favoriteState.value = UiState.Success(false)
                        }.onFailure { error ->
                            Log.e("DetailViewModel", "Failed to remove favorite", error)
                            _favoriteState.value = UiState.Error(error.message ?: "Failed to remove favorite")
                        }
                    } else {
                        // Favorilere ekle
                        Log.d("DetailViewModel", "Adding to favorites")
                        userRepository.addFavorite(userId, parkingId).onSuccess {
                            Log.d("DetailViewModel", "Successfully added to favorites")
                            _favoriteState.value = UiState.Success(true)
                        }.onFailure { error ->
                            Log.e("DetailViewModel", "Failed to add favorite", error)
                            _favoriteState.value = UiState.Error(error.message ?: "Failed to add favorite")
                        }
                    }
                }.onFailure { error ->
                    Log.e("DetailViewModel", "Failed to get favorites", error)
                    _favoriteState.value = UiState.Error(error.message ?: "Failed to check favorite status")
                }
            } catch (e: Exception) {
                Log.e("DetailViewModel", "Exception in toggleFavorite", e)
                _favoriteState.value = UiState.Error(e.message ?: "Failed to update favorite status")
            }
        }
    }


    override fun onCleared() {
        super.onCleared()
        updateJob?.cancel()
    }
}