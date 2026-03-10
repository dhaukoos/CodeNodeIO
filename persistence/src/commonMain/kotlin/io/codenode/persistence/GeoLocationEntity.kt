package io.codenode.persistence

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "geolocations")
data class GeoLocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val lat: Double,
    val lon: Double
)
