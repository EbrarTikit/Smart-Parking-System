package com.example.smartparkingsystem.data.remote

import com.example.smartparkingsystem.data.model.ParkingListResponse
import com.example.smartparkingsystem.data.model.ViewerTrackResponse
import com.example.smartparkingsystem.utils.Constants.PARKING_LIST
import com.example.smartparkingsystem.utils.Constants.VIEWER_TRACK
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ParkingManagementService {

    @GET(PARKING_LIST)
    suspend fun getParkingList(): List<ParkingListResponse>

    @POST(VIEWER_TRACK)
    suspend fun viewerTrack(
        @Query("userId") userId: Int,
        @Query("parkingId") parkingId: Int
    ): Response<ViewerTrackResponse>
}