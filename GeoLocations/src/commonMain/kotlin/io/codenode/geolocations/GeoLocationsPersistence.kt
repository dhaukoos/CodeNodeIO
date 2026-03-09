package io.codenode.geolocations

import io.codenode.persistence.GeoLocationDao
import io.codenode.persistence.GeoLocationRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.dsl.module

/**
 * Koin module for GeoLocations persistence dependencies.
 * The app layer provides the [GeoLocationDao] singleton;
 * this module wires it into a [GeoLocationRepository].
 */
val geoLocationsModule = module {
    single { GeoLocationRepository(get()) }
}

/**
 * Koin-backed accessor for persistence dependencies.
 * Used by processing logic tick functions that cannot take constructor parameters.
 */
object GeoLocationsPersistence : KoinComponent {
    val dao: GeoLocationDao by inject()
}
