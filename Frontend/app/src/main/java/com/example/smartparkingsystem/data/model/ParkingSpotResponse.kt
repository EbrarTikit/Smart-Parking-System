package com.example.smartparkingsystem.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ParkingSpotResponse(
    val id: Int,
    val parking: String,
    val row: Int,
    val column: Int,
    val spotIdentifier: String,
    val sensorId: String,
    val occupied: Boolean
) : Parcelable