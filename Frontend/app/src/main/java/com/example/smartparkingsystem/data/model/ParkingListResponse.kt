package com.example.smartparkingsystem.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ParkingListResponse(
    val id: Int,
    val name: String,
    val location: String,
    val capacity: Int,
    val openingHours: String,
    val closingHours: String,
    val rate: Double,
    val latitude: Double,
    val longitude: Double,
    val imageUrl: String,
    val rows: Int,
    val columns: Int,
    val description: String?,
    val parkingSpots: List<ParkingSpotResponse>
) : Parcelable