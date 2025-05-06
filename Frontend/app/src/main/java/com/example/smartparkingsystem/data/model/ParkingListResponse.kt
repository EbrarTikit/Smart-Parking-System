package com.example.smartparkingsystem.data.model

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
    val rows: Int,
    val columns: Int,
    val parkingSpots: List<ParkingSpotResponse>
)
