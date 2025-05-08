package com.example.smartparkingsystem.data.repository

import com.example.smartparkingsystem.data.model.ParkingListResponse
import com.example.smartparkingsystem.data.model.ViewerCountResponse
import com.example.smartparkingsystem.data.model.ViewerTrackResponse
import com.example.smartparkingsystem.data.remote.ParkingManagementService
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ParkingManagementRepository @Inject constructor(
    private val parkingManagementService: ParkingManagementService
) {
    suspend fun getParkingList(): List<ParkingListResponse> {
        return parkingManagementService.getParkingList()
    }

    suspend fun viewerTrack(
        userId: Int,
        parkingId: Int,
    ): ViewerTrackResponse {
        return parkingManagementService.viewerTrack(userId, parkingId)
    }

    suspend fun viewerCount(parkingId: Int): ViewerCountResponse {
        return parkingManagementService.getParkingViewerCount(parkingId = parkingId)
    }
}