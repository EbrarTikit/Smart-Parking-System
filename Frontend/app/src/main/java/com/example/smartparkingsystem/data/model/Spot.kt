package com.example.smartparkingsystem.data.model

data class Spot(
    val column: Int,
    val id: Int,
    val occupied: Boolean,
    val row: Int,
    val sensorId: String?,
    val spotIdentifier: String?
) {
    override fun hashCode(): Int {
        var result = column
        result = 31 * result + id
        result = 31 * result + occupied.hashCode()
        result = 31 * result + row
        result = 31 * result + (sensorId?.hashCode() ?: 0)
        result = 31 * result + (spotIdentifier?.hashCode() ?: 0)
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Spot) return false

        if (column != other.column) return false
        if (id != other.id) return false
        if (occupied != other.occupied) return false
        if (row != other.row) return false
        if (sensorId != other.sensorId) return false
        if (spotIdentifier != other.spotIdentifier) return false

        return true
    }
}