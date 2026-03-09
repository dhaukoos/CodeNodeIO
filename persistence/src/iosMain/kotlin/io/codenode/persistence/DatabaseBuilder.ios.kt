package io.codenode.persistence

import androidx.room.Room
import androidx.room.RoomDatabase
import io.codenode.userprofiles.persistence.AppDatabase
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null
    )!!.path!!
    val dbFilePath = documentDirectory + "/app.db"
    return Room.databaseBuilder<AppDatabase>(name = dbFilePath)
}
