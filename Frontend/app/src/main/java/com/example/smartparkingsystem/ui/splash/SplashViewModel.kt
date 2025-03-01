package com.example.smartparkingsystem.ui.splash

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor() : ViewModel() {

    private val _navigateToOnBoarding = MutableLiveData<Boolean>()
    val navigateToOnBoarding: LiveData<Boolean> = _navigateToOnBoarding

    init {
        startSplashScreen()
    }

    private fun startSplashScreen() {
        viewModelScope.launch {
            delay(1500)
            _navigateToOnBoarding.value = true
        }
    }
} 