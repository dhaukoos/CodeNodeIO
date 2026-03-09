package io.codenode.persistence

import android.app.Application
import androidx.room.Room
import androidx.room.RoomDatabase
import io.codenode.userprofiles.persistence.AppDatabase

private lateinit var appContext: Application

fun initializeDatabaseContext(context: Application) {
    appContext = context
}

actual fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val dbFile = appContext.getDatabasePath("app.db")
    return Room.databaseBuilder<AppDatabase>(
        context = appContext,
        name = dbFile.absolutePath
    )
}
