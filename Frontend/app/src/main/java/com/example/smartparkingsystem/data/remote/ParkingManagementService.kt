package com.example.smartparkingsystem.data.remote

import com.example.smartparkingsystem.data.model.ParkingLayoutResponse
import com.example.smartparkingsystem.data.model.ParkingListResponse
import com.example.smartparkingsystem.data.model.ViewerCountResponse
import com.example.smartparkingsystem.data.model.ViewerTrackResponse
import com.example.smartparkingsystem.utils.Constants.LAYOUT
import com.example.smartparkingsystem.utils.Constants.PARKING_LIST
import com.example.smartparkingsystem.utils.Constants.VIEWER_COUNT
import com.example.smartparkingsystem.utils.Constants.VIEWER_TRACK
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ParkingManagementService {

    @GET(PARKING_LIST)
    suspend fun getParkingList(): List<ParkingListResponse>

    @POST(VIEWER_TRACK)
    suspend fun viewerTrack(
        @Query("userId") userId: Int,
        @Query("parkingId") parkingId: Int
    ): ViewerTrackResponse

    @GET(VIEWER_COUNT)
    suspend fun getParkingViewerCount(
        @Path("parkingId") parkingId: Int
    ): ViewerCountResponse

    @GET(LAYOUT)
    suspend fun getParkingLayout(
        @Path("parkingId") parkingId: Int
    ): ParkingLayoutResponse
}