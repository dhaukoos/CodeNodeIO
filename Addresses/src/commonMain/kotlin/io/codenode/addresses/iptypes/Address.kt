/*
 * Address - Custom IP Type
 * @IPType
 * @TypeName Address
 * @TypeId ip_address
 * @Color rgb(255, 87, 34)
 * License: Apache 2.0
 */

package io.codenode.addresses.iptypes

import io.codenode.persistence.AddressEntity

/**
 * IP type representing an address flowing through the pipeline.
 * Decoupled from the Room entity to keep pipeline types independent of persistence.
 */
data class Address(
    val id: Long = 0,
    val street: String,
    val city: String,
    val state: String,
    val zip: Int
)

/** Convert IP type to persistence entity */
fun Address.toEntity() = AddressEntity(id = id, Street = street, City = city, State = state, Zip = zip)

/** Convert persistence entity to IP type */
fun AddressEntity.toAddress() = Address(id = id, street = Street, city = City, state = State, zip = Zip)
