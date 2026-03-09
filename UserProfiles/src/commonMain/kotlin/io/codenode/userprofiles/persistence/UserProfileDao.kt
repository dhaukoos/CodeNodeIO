package io.codenode.userprofiles.persistence

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(obj: UserProfileEntity)

    @Update
    suspend fun update(obj: UserProfileEntity)

    @Delete
    suspend fun delete(obj: UserProfileEntity)

    @Query("SELECT * FROM userprofiles")
    fun getAllAsFlow(): Flow<List<UserProfileEntity>>
}
