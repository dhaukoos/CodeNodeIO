package io.codenode.persistence

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "geolocations")
data class GeoLocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val label: String,
    val altitude: Double? = null,
    val isActive: Boolean
)
