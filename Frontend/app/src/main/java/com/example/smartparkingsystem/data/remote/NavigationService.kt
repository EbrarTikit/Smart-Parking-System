package com.example.smartparkingsystem.data.remote

import com.example.smartparkingsystem.data.model.LocationResponse
import com.example.smartparkingsystem.utils.Constants.NAVIGATION
import com.example.smartparkingsystem.utils.Constants.NAVIGATION_LIST
import retrofit2.http.GET
import retrofit2.http.Path

interface NavigationService {

    @GET(NAVIGATION)
    suspend fun getLocationById(
        @Path("id") id: Int
    ): LocationResponse

    @GET(NAVIGATION_LIST)
    suspend fun getAllLocations(): List<LocationResponse>

}