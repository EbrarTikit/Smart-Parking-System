package com.example.smartparkingsystem.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ParkingSpotResponse(
    val id: Int,
    val parking: String?,
    val row: Int,
    val column: Int,
    val spotIdentifier: String?,
    val sensorId: String?,
    val occupied: Boolean
) : Parcelable {
    override fun hashCode(): Int {
        var result = id
        result = 31 * result + (parking?.hashCode() ?: 0)
        result = 31 * result + row
        result = 31 * result + column
        result = 31 * result + (spotIdentifier?.hashCode() ?: 0)
        result = 31 * result + (sensorId?.hashCode() ?: 0)
        result = 31 * result + occupied.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ParkingSpotResponse) return false

        if (id != other.id) return false
        if (parking != other.parking) return false
        if (row != other.row) return false
        if (column != other.column) return false
        if (spotIdentifier != other.spotIdentifier) return false
        if (sensorId != other.sensorId) return false
        if (occupied != other.occupied) return false

        return true
    }
}