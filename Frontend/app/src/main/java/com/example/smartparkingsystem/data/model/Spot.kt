package com.example.smartparkingsystem.data.model

data class Spot(
    val column: Int,
    val id: Int,
    val occupied: Boolean,
    val row: Int,
    val sensorId: String,
    val spotIdentifier: String
)