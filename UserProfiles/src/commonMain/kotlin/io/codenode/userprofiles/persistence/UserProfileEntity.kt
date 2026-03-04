package io.codenode.userprofiles.persistence

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "userprofiles")
data class UserProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val age: Int? = null,
    val isActive: Boolean
)
