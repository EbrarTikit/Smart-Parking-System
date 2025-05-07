package com.example.smartparkingsystem.data.repository

import com.example.smartparkingsystem.data.model.ParkingListResponse
import com.example.smartparkingsystem.data.remote.ParkingManagementService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ParkingManagementRepository @Inject constructor(
    private val parkingManagementService: ParkingManagementService
) {
    suspend fun getParkingList(): List<ParkingListResponse> {
        return parkingManagementService.getParkingList()
    }
}