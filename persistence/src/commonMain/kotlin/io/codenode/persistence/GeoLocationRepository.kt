package io.codenode.persistence

import kotlinx.coroutines.flow.Flow

class GeoLocationRepository(private val geoLocationDao: GeoLocationDao) {
    suspend fun save(item: GeoLocationEntity) = geoLocationDao.insert(item)

    suspend fun update(item: GeoLocationEntity) = geoLocationDao.update(item)

    suspend fun remove(item: GeoLocationEntity) = geoLocationDao.delete(item)

    fun observeAll(): Flow<List<GeoLocationEntity>> = geoLocationDao.getAllAsFlow()
}
