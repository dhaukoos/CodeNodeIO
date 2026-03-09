package io.codenode.persistence

import kotlinx.coroutines.flow.Flow

class UserProfileRepository(private val userProfileDao: UserProfileDao) {
    suspend fun save(item: UserProfileEntity) = userProfileDao.insert(item)

    suspend fun update(item: UserProfileEntity) = userProfileDao.update(item)

    suspend fun remove(item: UserProfileEntity) = userProfileDao.delete(item)

    fun observeAll(): Flow<List<UserProfileEntity>> = userProfileDao.getAllAsFlow()
}
