package com.example.smartparkingsystem.data.model

import androidx.annotation.RawRes

data class OnboardingPage(
    @RawRes val animation: Int,
    val title: String,
    val description: String
)