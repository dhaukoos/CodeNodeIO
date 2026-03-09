package io.codenode.persistence

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.ConstructedBy

@Database(entities = [UserProfileEntity::class], version = 1)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
}

expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase>
