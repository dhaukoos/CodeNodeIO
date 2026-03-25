/*
 * GeoLocation - Custom IP Type
 * @IPType
 * @TypeName GeoLocation
 * @TypeId ip_geolocation
 * @Color rgb(63, 81, 181)
 * License: Apache 2.0
 */

package io.codenode.geolocations.iptypes

import io.codenode.persistence.GeoLocationEntity

/**
 * IP type representing a geo location flowing through the pipeline.
 * Decoupled from the Room entity to keep pipeline types independent of persistence.
 */
data class GeoLocation(
    val id: Long = 0,
    val name: String,
    val lat: Double,
    val lon: Double
)

/** Convert IP type to persistence entity */
fun GeoLocation.toEntity() = GeoLocationEntity(id = id, name = name, lat = lat, lon = lon)

/** Convert persistence entity to IP type */
fun GeoLocationEntity.toGeoLocation() = GeoLocation(id = id, name = name, lat = lat, lon = lon)
