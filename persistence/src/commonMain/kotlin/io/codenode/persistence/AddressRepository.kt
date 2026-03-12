package io.codenode.persistence

import kotlinx.coroutines.flow.Flow

class AddressRepository(private val addressDao: AddressDao) {
    suspend fun save(item: AddressEntity) = addressDao.insert(item)

    suspend fun update(item: AddressEntity) = addressDao.update(item)

    suspend fun remove(item: AddressEntity) = addressDao.delete(item)

    fun observeAll(): Flow<List<AddressEntity>> = addressDao.getAllAsFlow()
}
