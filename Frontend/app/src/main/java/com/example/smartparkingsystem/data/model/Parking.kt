package com.example.smartparkingsystem.data.model

data class Parking(
    val id: String,
    val name: String,
    val image: Int,
    val price: Double,
    val availableSpots: Int,
    val totalSpots: Int,
    val latitude: Double,
    val longitude: Double
)