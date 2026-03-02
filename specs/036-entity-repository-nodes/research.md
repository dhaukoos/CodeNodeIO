# Research: Entity Repository Nodes

**Feature**: 036-entity-repository-nodes
**Date**: 2026-03-01

## R1: KMP Room Database Setup

**Decision**: Use Room 2.8.4 with KSP and BundledSQLiteDriver for cross-platform database support.

**Rationale**: Room 2.8.4 (November 2025) is the latest stable release with full KMP support including JVM/Desktop. It uses the `@ConstructedBy` + `expect object : RoomDatabaseConstructor<T>` pattern for multiplatform database initialization. The `BundledSQLiteDriver` provides consistent SQLite behavior across all targets.

**Alternatives considered**:
- SQLDelight: More mature KMP support but less familiar to Android developers and doesn't support the Room annotation pattern the user specified.
- Raw SQLite: Too low-level, loses all the compile-time query verification and code generation benefits.

**Key dependencies**:
- `androidx.room:room-runtime:2.8.4` (KMP common)
- `androidx.room:room-compiler:2.8.4` (KSP, per-platform)
- `androidx.sqlite:sqlite-bundled:2.6.2` (KMP common)
- KSP plugin: `com.google.devtools.ksp` version `2.1.21-2.0.1` (matching Kotlin 2.1.21)

**Constraints**:
- `@Query` methods cannot be in generic BaseDao (require concrete table names in SQL)
- Non-Android DAO functions must be `suspend` (except `Flow`-returning queries)
- KSP compiler must be added per-platform target (`kspJvm`, `kspAndroid`, etc.)

## R2: Repository Node as CustomNodeDefinition Extension

**Decision**: Repository node definitions extend the existing `CustomNodeDefinition` pattern with additional metadata fields (`sourceIPTypeId`, `isRepository` flag) rather than creating a new definition type.

**Rationale**: The existing `CustomNodeDefinition` → `NodeTypeDefinition` → palette display pipeline is well-established. Adding fields to it is simpler than creating a parallel definition system. The `toNodeTypeDefinition()` method already handles conversion to palette-displayable nodes.

**Alternatives considered**:
- New `RepositoryNodeDefinition` class: Would require duplicating the palette display and persistence pipeline. Unnecessary complexity.
- Configuration-only approach (using `_genericType` like any-input): Insufficient — repository nodes need to carry their source IP type ID and property metadata for code generation.

## R3: Code Generation Strategy for Repository Nodes

**Decision**: Add a new `RepositoryCodeGenerator` class in the kotlinCompiler module that generates Entity, DAO, Repository, and Database classes from repository node metadata. Integrate it into `ModuleSaveService` alongside existing generators.

**Rationale**: Repository nodes require fundamentally different code generation than standard runtime nodes. They don't map to the `CodeNodeFactory.create*()` pattern — they produce persistence layer classes (Entity/DAO/Repository/Database) rather than runtime channel-based nodes. A dedicated generator keeps the existing generators clean.

**Alternatives considered**:
- Extending RuntimeFlowGenerator: Would bloat the existing generator with persistence-specific logic that doesn't follow the factory method pattern.
- Template-based generation (Mustache/FreeMarker): Adds a dependency for templating. String-based generation (like existing generators) is simpler and consistent with the codebase.

## R4: Reactive Observe-All Stream Mechanism

**Decision**: The result output port uses Room's built-in `Flow<List<Entity>>` from `@Query("SELECT * FROM table")` DAO method. The repository node runtime collects this Flow and forwards emissions to the output channel.

**Rationale**: Room automatically re-emits query results when the underlying table changes. This provides the reactive observe-all behavior without additional infrastructure. The repository node's runtime wraps the DAO Flow, collecting it in a coroutine and sending each emission to the output channel.

**Alternatives considered**:
- Manual change notification: Requires tracking mutations and re-querying. Room already does this automatically.
- SharedFlow/StateFlow wrapper: Unnecessary indirection — the Room Flow can be collected directly.

## R5: Singleton Database Module Pattern

**Decision**: Generate a `DatabaseModule` object in `commonMain` that provides the singleton Room database instance. Platform-specific `getDatabaseBuilder()` functions provide the `RoomDatabase.Builder`. All repository nodes reference this singleton.

**Rationale**: Room documentation explicitly recommends singleton pattern ("each RoomDatabase instance is fairly expensive"). A generated object with lazy initialization is the standard KMP Room pattern.

**Implementation pattern**:
```kotlin
// commonMain - generated
@Database(entities = [UserEntity::class, OrderEntity::class], version = 1)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun orderDao(): OrderDao
}

// jvmMain - generated
fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val dbFile = File(System.getProperty("user.home"), ".codenode/data/app.db")
    return Room.databaseBuilder<AppDatabase>(name = dbFile.absolutePath)
}

// commonMain - generated singleton
object DatabaseModule {
    private var instance: AppDatabase? = null
    fun getDatabase(): AppDatabase = instance ?: synchronized(this) {
        instance ?: getRoomDatabase(getDatabaseBuilder()).also { instance = it }
    }
}
```

## R6: BaseDao Pattern

**Decision**: Use `BaseDao<T>` interface with `@Insert(onConflict = OnConflictStrategy.REPLACE)`, `@Update`, `@Delete` — exactly as specified by the user.

**Rationale**: This matches the user's stated design. `OnConflictStrategy.REPLACE` is the simplest approach for initial implementation. Future iterations could switch to `@Upsert` (Room 2.5.0+) if cascading delete issues arise.

**Generated pattern**:
```kotlin
interface BaseDao<T> {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(obj: T)
    @Update
    suspend fun update(obj: T)
    @Delete
    suspend fun delete(obj: T)
}
```

## R7: IP Type Property to Entity Column Mapping

**Decision**: Map each `IPProperty` to an Entity column using the property name as the column name and the property's IP type as the column type. Add an auto-generated `id: Long` primary key column to every entity.

**Rationale**: IP types map cleanly to Room column types: Int→Int, Double→Double, Boolean→Boolean, String→String. Custom IP types (nested objects) are not supported in the initial implementation — only primitive IP types are valid for entity properties.

**Type mapping**:
| IP Type | Kotlin Column Type | Room Support |
|---------|-------------------|--------------|
| Int     | Int               | Direct       |
| Double  | Double            | Direct       |
| Boolean | Boolean           | Direct       |
| String  | String            | Direct       |
| Any     | String            | Serialized   |
| Custom  | Not supported     | Error/skip   |

## R8: Integration with Existing Code Generation Pipeline

**Decision**: Repository code generation is triggered alongside existing generators in `ModuleSaveService.saveModule()`. When repository nodes are detected in a FlowGraph, additional files are generated in a `persistence/` package within the module.

**Generated file structure**:
```
moduleName/src/commonMain/kotlin/{package}/
├── generated/           (existing: Flow, Controller, etc.)
├── processingLogic/     (existing: tick stubs)
└── persistence/         (NEW: repository-specific code)
    ├── BaseDao.kt
    ├── {Entity}Entity.kt
    ├── {Entity}Dao.kt
    ├── {Entity}Repository.kt
    └── AppDatabase.kt
```

**Platform-specific files**:
```
moduleName/src/jvmMain/kotlin/{package}/persistence/
└── DatabaseBuilder.jvm.kt
moduleName/src/androidMain/kotlin/{package}/persistence/
└── DatabaseBuilder.android.kt
moduleName/src/iosMain/kotlin/{package}/persistence/
└── DatabaseBuilder.ios.kt
```
