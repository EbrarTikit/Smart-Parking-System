package com.example.smartparkingsystem.ui.favorites

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartparkingsystem.data.model.FavoriteParking
import com.example.smartparkingsystem.data.repository.UserRepository
import com.example.smartparkingsystem.utils.state.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _favoritesState = MutableStateFlow<UiState<List<FavoriteParking>>>(UiState.Loading)
    val favoritesState: StateFlow<UiState<List<FavoriteParking>>> = _favoritesState

    fun loadFavorites(userId: Int) {
        _favoritesState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val response = userRepository.getFavorites(userId)
                response.onSuccess { favoriteList ->
                    _favoritesState.value = if (favoriteList.isEmpty()) {
                        UiState.Success(emptyList())
                    } else {
                        UiState.Success(favoriteList)
                    }
                }.onFailure { error ->
                    Log.e("FavoritesViewModel", "Failed to load favorites", error)
                    _favoritesState.value =
                        UiState.Error(error.message ?: "Failed to load favorites")
                }
            } catch (e: Exception) {
                Log.e("FavoritesViewModel", "Exception loading favorites", e)
                _favoritesState.value = UiState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }

    fun removeFavorite(userId: Int, parkingId: Int) {
        viewModelScope.launch {
            try {
                val currentState = _favoritesState.value
                if (currentState is UiState.Success) {
                    val updatedList = currentState.data.filter { it.id != parkingId }
                    _favoritesState.value = UiState.Success(updatedList)
                    userRepository.removeFavorite(userId, parkingId).onFailure { error ->
                        Log.e("FavoritesViewModel", "Failed to remove favorite", error)
                        loadFavorites(userId)
                    }
                }
            } catch (e: Exception) {
                Log.e("FavoritesViewModel", "Exception removing favorite", e)
                loadFavorites(userId)
            }
        }
    }
}
