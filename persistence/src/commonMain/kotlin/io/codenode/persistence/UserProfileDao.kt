package io.codenode.persistence

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao : BaseDao<UserProfileEntity> {
    @Query("SELECT * FROM userprofiles")
    fun getAllAsFlow(): Flow<List<UserProfileEntity>>
}
