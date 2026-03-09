package io.codenode.persistence

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GeoLocationDao : BaseDao<GeoLocationEntity> {
    @Query("SELECT * FROM geolocations")
    fun getAllAsFlow(): Flow<List<GeoLocationEntity>>
}
