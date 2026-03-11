package io.codenode.persistence

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.ConstructedBy

@Database(entities = [AddressEntity::class, GeoLocationEntity::class, UserProfileEntity::class], version = 1)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun addressDao(): AddressDao
    abstract fun geoLocationDao(): GeoLocationDao
    abstract fun userProfileDao(): UserProfileDao
}

expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase>
