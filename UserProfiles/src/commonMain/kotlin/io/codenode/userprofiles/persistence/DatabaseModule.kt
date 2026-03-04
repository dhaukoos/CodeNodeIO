package io.codenode.userprofiles.persistence

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

object DatabaseModule {
    private var instance: AppDatabase? = null

    fun getDatabase(): AppDatabase = instance ?: synchronized(this) {
        instance ?: getRoomDatabase(getDatabaseBuilder()).also { instance = it }
    }
}

fun getRoomDatabase(builder: RoomDatabase.Builder<AppDatabase>): AppDatabase {
    return builder
        .setDriver(BundledSQLiteDriver())
        .build()
}

expect fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase>
