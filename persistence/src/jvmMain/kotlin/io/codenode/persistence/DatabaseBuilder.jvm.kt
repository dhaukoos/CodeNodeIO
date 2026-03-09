package io.codenode.persistence

import androidx.room.Room
import androidx.room.RoomDatabase
import io.codenode.userprofiles.persistence.AppDatabase
import java.io.File

actual fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val dbDir = File(System.getProperty("user.home"), ".codenode/data")
    dbDir.mkdirs()
    val dbFile = File(dbDir, "app.db")
    return Room.databaseBuilder<AppDatabase>(name = dbFile.absolutePath)
}
