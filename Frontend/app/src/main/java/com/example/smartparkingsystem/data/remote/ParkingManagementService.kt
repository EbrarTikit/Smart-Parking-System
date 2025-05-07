package com.example.smartparkingsystem.data.remote

import com.example.smartparkingsystem.data.model.ParkingListResponse
import com.example.smartparkingsystem.utils.Constants.PARKING_LIST
import retrofit2.Response
import retrofit2.http.GET

interface ParkingManagementService {

    @GET(PARKING_LIST)
    suspend fun getParkingList(): List<ParkingListResponse>
}