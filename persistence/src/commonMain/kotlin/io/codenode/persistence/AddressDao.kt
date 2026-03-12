package io.codenode.persistence

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AddressDao : BaseDao<AddressEntity> {
    @Query("SELECT * FROM addresss")
    fun getAllAsFlow(): Flow<List<AddressEntity>>
}
