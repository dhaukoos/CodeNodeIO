package io.codenode.persistence

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.ConstructedBy

@Database(entities = [UserProfileEntity::class, GeoLocationEntity::class], version = 1)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun geoLocationDao(): GeoLocationDao
}

//   This is a known quirk of Room's expect/actual + KSP pattern. You can suppress the IDE warning by adding @Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA") if it
//  bothers you, but it's cosmetic only — it doesn't affect compilation or runtime.
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase>
