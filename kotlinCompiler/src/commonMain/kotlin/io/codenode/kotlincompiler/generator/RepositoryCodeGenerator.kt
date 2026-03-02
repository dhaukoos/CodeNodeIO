/*
 * RepositoryCodeGenerator - Generates Room persistence layer code from repository node metadata
 * License: Apache 2.0
 */

package io.codenode.kotlincompiler.generator

/**
 * Represents a property of an entity, mapped from an IP type property.
 *
 * @param name Property/column name
 * @param kotlinType Kotlin type string (e.g., "Int", "String", "Double", "Boolean")
 * @param isRequired Whether the property is required (non-nullable) or optional (nullable)
 */
data class EntityProperty(
    val name: String,
    val kotlinType: String,
    val isRequired: Boolean
)

/**
 * Metadata about a generated entity for database registration.
 *
 * @param entityName Entity class name without "Entity" suffix (e.g., "User")
 * @param tableName Database table name (e.g., "users")
 * @param daoName DAO class name (e.g., "UserDao")
 */
data class EntityInfo(
    val entityName: String,
    val tableName: String,
    val daoName: String
)

/**
 * Generates Room persistence layer code (Entity, DAO, Repository, BaseDao, Database)
 * from repository node metadata. Each method produces a complete Kotlin source file
 * as a string.
 */
class RepositoryCodeGenerator {

    /**
     * Generates a BaseDao<T> interface with @Insert, @Update, @Delete methods.
     *
     * @param packageName Package name for the generated file
     * @return Complete Kotlin source file content for BaseDao.kt
     */
    fun generateBaseDao(packageName: String): String {
        return buildString {
            appendLine("package $packageName")
            appendLine()
            appendLine("import androidx.room.Delete")
            appendLine("import androidx.room.Insert")
            appendLine("import androidx.room.OnConflictStrategy")
            appendLine("import androidx.room.Update")
            appendLine()
            appendLine("interface BaseDao<T> {")
            appendLine("    @Insert(onConflict = OnConflictStrategy.REPLACE)")
            appendLine("    suspend fun insert(obj: T)")
            appendLine()
            appendLine("    @Update")
            appendLine("    suspend fun update(obj: T)")
            appendLine()
            appendLine("    @Delete")
            appendLine("    suspend fun delete(obj: T)")
            appendLine("}")
        }
    }

    /**
     * Generates an @Entity data class with @PrimaryKey and columns mapped from properties.
     *
     * @param entityName Entity name without suffix (e.g., "User")
     * @param properties List of entity properties to generate as columns
     * @param packageName Package name for the generated file
     * @return Complete Kotlin source file content for {Entity}Entity.kt
     */
    fun generateEntity(
        entityName: String,
        properties: List<EntityProperty>,
        packageName: String
    ): String {
        val tableName = entityName.lowercase() + "s"
        return buildString {
            appendLine("package $packageName")
            appendLine()
            appendLine("import androidx.room.Entity")
            appendLine("import androidx.room.PrimaryKey")
            appendLine()
            appendLine("@Entity(tableName = \"$tableName\")")
            appendLine("data class ${entityName}Entity(")
            appendLine("    @PrimaryKey(autoGenerate = true) val id: Long = 0,")
            properties.forEachIndexed { index, prop ->
                val nullableSuffix = if (prop.isRequired) "" else "?"
                val defaultValue = if (prop.isRequired) "" else " = null"
                val comma = if (index < properties.size - 1) "," else ""
                appendLine("    val ${prop.name}: ${prop.kotlinType}${nullableSuffix}${defaultValue}${comma}")
            }
            appendLine(")")
        }
    }

    /**
     * Generates a @Dao interface extending BaseDao with a getAllAsFlow() query method.
     *
     * @param entityName Entity name without suffix (e.g., "User")
     * @param tableName Database table name (e.g., "users")
     * @param packageName Package name for the generated file
     * @return Complete Kotlin source file content for {Entity}Dao.kt
     */
    fun generateDao(
        entityName: String,
        tableName: String,
        packageName: String
    ): String {
        return buildString {
            appendLine("package $packageName")
            appendLine()
            appendLine("import androidx.room.Dao")
            appendLine("import androidx.room.Query")
            appendLine("import kotlinx.coroutines.flow.Flow")
            appendLine()
            appendLine("@Dao")
            appendLine("interface ${entityName}Dao : BaseDao<${entityName}Entity> {")
            appendLine("    @Query(\"SELECT * FROM $tableName\")")
            appendLine("    fun getAllAsFlow(): Flow<List<${entityName}Entity>>")
            appendLine("}")
        }
    }

    /**
     * Generates a Repository class wrapping the DAO with save/update/remove/observeAll methods.
     *
     * @param entityName Entity name without suffix (e.g., "User")
     * @param packageName Package name for the generated file
     * @return Complete Kotlin source file content for {Entity}Repository.kt
     */
    fun generateRepository(
        entityName: String,
        packageName: String
    ): String {
        val daoName = "${entityName}Dao"
        val daoParam = entityName.replaceFirstChar { it.lowercase() } + "Dao"
        return buildString {
            appendLine("package $packageName")
            appendLine()
            appendLine("import kotlinx.coroutines.flow.Flow")
            appendLine()
            appendLine("class ${entityName}Repository(private val $daoParam: $daoName) {")
            appendLine("    suspend fun save(item: ${entityName}Entity) = $daoParam.insert(item)")
            appendLine()
            appendLine("    suspend fun update(item: ${entityName}Entity) = $daoParam.update(item)")
            appendLine()
            appendLine("    suspend fun remove(item: ${entityName}Entity) = $daoParam.delete(item)")
            appendLine()
            appendLine("    fun observeAll(): Flow<List<${entityName}Entity>> = $daoParam.getAllAsFlow()")
            appendLine("}")
        }
    }
}
