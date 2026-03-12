package io.codenode.addresses

import io.codenode.persistence.AddressDao
import io.codenode.persistence.AddressRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.dsl.module

/**
 * Koin module for Addresses persistence dependencies.
 * The app layer provides the [AddressDao] singleton;
 * this module wires it into a [AddressRepository].
 */
val addressesModule = module {
    single { AddressRepository(get()) }
}

/**
 * Koin-backed accessor for persistence dependencies.
 * Used by processing logic tick functions that cannot take constructor parameters.
 */
object AddressesPersistence : KoinComponent {
    val dao: AddressDao by inject()
}
