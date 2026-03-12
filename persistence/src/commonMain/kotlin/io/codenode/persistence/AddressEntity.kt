package io.codenode.persistence

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "addresss")
data class AddressEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val Street: String,
    val City: String,
    val State: String,
    val Zip: Int
)
