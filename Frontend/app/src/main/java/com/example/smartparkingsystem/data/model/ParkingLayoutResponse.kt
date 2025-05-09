package com.example.smartparkingsystem.data.model

data class ParkingLayoutResponse(
    val capacity: Int,
    val columns: Int,
    val parkingId: Int,
    val parkingName: String,
    val rows: Int,
    val spots: List<Spot>
)