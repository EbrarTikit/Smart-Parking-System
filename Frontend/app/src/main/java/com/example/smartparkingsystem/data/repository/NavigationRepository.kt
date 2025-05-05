package com.example.smartparkingsystem.data.repository

import com.example.smartparkingsystem.data.model.LocationResponse
import com.example.smartparkingsystem.data.remote.NavigationService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NavigationRepository @Inject constructor(
    private val navigationService: NavigationService,
    ) {
    suspend fun getLocationById(id: Int): LocationResponse {
        return navigationService.getLocationById(id)
    }

    suspend fun getAllLocations(): List<LocationResponse> {
        return navigationService.getAllLocations()
    }
}