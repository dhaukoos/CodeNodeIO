/*
 * RepositoryCodeGenerator Test
 * Tests for generating Room persistence layer code from repository node metadata
 * License: Apache 2.0
 */

package io.codenode.kotlincompiler.generator

import kotlin.test.*

class RepositoryCodeGeneratorTest {

    private val generator = RepositoryCodeGenerator()
    private val testPackage = "com.example.app.persistence"

    // ========== generateBaseDao Tests ==========

    @Test
    fun `generateBaseDao contains package declaration`() {
        val result = generator.generateBaseDao(testPackage)
        assertTrue(result.contains("package $testPackage"))
    }

    @Test
    fun `generateBaseDao contains Insert annotation with REPLACE strategy`() {
        val result = generator.generateBaseDao(testPackage)
        assertTrue(result.contains("@Insert(onConflict = OnConflictStrategy.REPLACE)"))
    }

    @Test
    fun `generateBaseDao contains Update annotation`() {
        val result = generator.generateBaseDao(testPackage)
        assertTrue(result.contains("@Update"))
    }

    @Test
    fun `generateBaseDao contains Delete annotation`() {
        val result = generator.generateBaseDao(testPackage)
        assertTrue(result.contains("@Delete"))
    }

    @Test
    fun `generateBaseDao declares generic interface`() {
        val result = generator.generateBaseDao(testPackage)
        assertTrue(result.contains("interface BaseDao<T>"))
    }

    @Test
    fun `generateBaseDao methods are suspend`() {
        val result = generator.generateBaseDao(testPackage)
        assertTrue(result.contains("suspend fun insert(obj: T)"))
        assertTrue(result.contains("suspend fun update(obj: T)"))
        assertTrue(result.contains("suspend fun delete(obj: T)"))
    }

    @Test
    fun `generateBaseDao contains required imports`() {
        val result = generator.generateBaseDao(testPackage)
        assertTrue(result.contains("import androidx.room.Delete"))
        assertTrue(result.contains("import androidx.room.Insert"))
        assertTrue(result.contains("import androidx.room.OnConflictStrategy"))
        assertTrue(result.contains("import androidx.room.Update"))
    }

    // ========== generateEntity Tests ==========

    @Test
    fun `generateEntity contains Entity annotation with table name`() {
        val properties = listOf(
            EntityProperty("name", "String", true)
        )
        val result = generator.generateEntity("User", properties, testPackage)
        assertTrue(result.contains("@Entity(tableName = \"users\")"))
    }

    @Test
    fun `generateEntity contains PrimaryKey with autoGenerate`() {
        val properties = listOf(
            EntityProperty("name", "String", true)
        )
        val result = generator.generateEntity("User", properties, testPackage)
        assertTrue(result.contains("@PrimaryKey(autoGenerate = true) val id: Long = 0"))
    }

    @Test
    fun `generateEntity maps required properties as non-nullable`() {
        val properties = listOf(
            EntityProperty("name", "String", true),
            EntityProperty("email", "String", true)
        )
        val result = generator.generateEntity("User", properties, testPackage)
        assertTrue(result.contains("val name: String,"))
        assertTrue(result.contains("val email: String"))
    }

    @Test
    fun `generateEntity maps optional properties as nullable with default null`() {
        val properties = listOf(
            EntityProperty("name", "String", true),
            EntityProperty("age", "Int", false)
        )
        val result = generator.generateEntity("User", properties, testPackage)
        assertTrue(result.contains("val age: Int? = null"))
    }

    @Test
    fun `generateEntity generates data class with correct name`() {
        val properties = listOf(
            EntityProperty("title", "String", true)
        )
        val result = generator.generateEntity("Task", properties, testPackage)
        assertTrue(result.contains("data class TaskEntity("))
    }

    @Test
    fun `generateEntity derives table name from entity name`() {
        val properties = listOf(
            EntityProperty("orderId", "Int", true)
        )
        val result = generator.generateEntity("Order", properties, testPackage)
        assertTrue(result.contains("@Entity(tableName = \"orders\")"))
    }

    @Test
    fun `generateEntity supports multiple property types`() {
        val properties = listOf(
            EntityProperty("name", "String", true),
            EntityProperty("age", "Int", false),
            EntityProperty("score", "Double", true),
            EntityProperty("active", "Boolean", true)
        )
        val result = generator.generateEntity("Player", properties, testPackage)
        assertTrue(result.contains("val name: String,"))
        assertTrue(result.contains("val age: Int? = null,"))
        assertTrue(result.contains("val score: Double,"))
        assertTrue(result.contains("val active: Boolean"))
    }

    @Test
    fun `generateEntity contains package and imports`() {
        val properties = listOf(EntityProperty("name", "String", true))
        val result = generator.generateEntity("User", properties, testPackage)
        assertTrue(result.contains("package $testPackage"))
        assertTrue(result.contains("import androidx.room.Entity"))
        assertTrue(result.contains("import androidx.room.PrimaryKey"))
    }

    // ========== generateDao Tests ==========

    @Test
    fun `generateDao contains Dao annotation`() {
        val result = generator.generateDao("User", "users", testPackage)
        assertTrue(result.contains("@Dao"))
    }

    @Test
    fun `generateDao extends BaseDao with correct entity type`() {
        val result = generator.generateDao("User", "users", testPackage)
        assertTrue(result.contains("interface UserDao : BaseDao<UserEntity>"))
    }

    @Test
    fun `generateDao contains getAllAsFlow query`() {
        val result = generator.generateDao("User", "users", testPackage)
        assertTrue(result.contains("@Query(\"SELECT * FROM users\")"))
        assertTrue(result.contains("fun getAllAsFlow(): Flow<List<UserEntity>>"))
    }

    @Test
    fun `generateDao contains required imports`() {
        val result = generator.generateDao("User", "users", testPackage)
        assertTrue(result.contains("import androidx.room.Dao"))
        assertTrue(result.contains("import androidx.room.Query"))
        assertTrue(result.contains("import kotlinx.coroutines.flow.Flow"))
    }

    @Test
    fun `generateDao uses correct table name in query`() {
        val result = generator.generateDao("Order", "orders", testPackage)
        assertTrue(result.contains("@Query(\"SELECT * FROM orders\")"))
        assertTrue(result.contains("interface OrderDao : BaseDao<OrderEntity>"))
    }

    // ========== generateRepository Tests ==========

    @Test
    fun `generateRepository wraps DAO save method`() {
        val result = generator.generateRepository("User", testPackage)
        assertTrue(result.contains("suspend fun save(item: UserEntity)"))
        assertTrue(result.contains("userDao.insert(item)"))
    }

    @Test
    fun `generateRepository wraps DAO update method`() {
        val result = generator.generateRepository("User", testPackage)
        assertTrue(result.contains("suspend fun update(item: UserEntity)"))
        assertTrue(result.contains("userDao.update(item)"))
    }

    @Test
    fun `generateRepository wraps DAO remove method`() {
        val result = generator.generateRepository("User", testPackage)
        assertTrue(result.contains("suspend fun remove(item: UserEntity)"))
        assertTrue(result.contains("userDao.delete(item)"))
    }

    @Test
    fun `generateRepository wraps DAO observeAll method`() {
        val result = generator.generateRepository("User", testPackage)
        assertTrue(result.contains("fun observeAll(): Flow<List<UserEntity>>"))
        assertTrue(result.contains("userDao.getAllAsFlow()"))
    }

    @Test
    fun `generateRepository has correct class name and constructor`() {
        val result = generator.generateRepository("User", testPackage)
        assertTrue(result.contains("class UserRepository(private val userDao: UserDao)"))
    }

    @Test
    fun `generateRepository contains package and imports`() {
        val result = generator.generateRepository("User", testPackage)
        assertTrue(result.contains("package $testPackage"))
        assertTrue(result.contains("import kotlinx.coroutines.flow.Flow"))
    }

    @Test
    fun `generateRepository handles different entity names`() {
        val result = generator.generateRepository("Order", testPackage)
        assertTrue(result.contains("class OrderRepository(private val orderDao: OrderDao)"))
        assertTrue(result.contains("suspend fun save(item: OrderEntity)"))
        assertTrue(result.contains("fun observeAll(): Flow<List<OrderEntity>>"))
    }
}
