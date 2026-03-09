# Research: Persistence Dependency Injection

**Feature**: 046-persistence-dependency-injection
**Date**: 2026-03-09

## R1: Current DatabaseModule Access Points

**Finding**: Two locations in UserProfiles directly call `DatabaseModule.getDatabase()`:

1. **UserProfilesViewModel.kt** (line 91) — creates `UserProfileRepository(DatabaseModule.getDatabase().userProfileDao())` in `init {}` to start collecting profile changes
2. **UserProfileRepositoryProcessLogic.kt** (line 32) — creates `UserProfileRepository(DatabaseModule.getDatabase().userProfileDao())` on each tick of the processing function

Both create a fresh `UserProfileRepository` wrapper each time (though `DatabaseModule.getDatabase()` returns a cached singleton instance). Neither receives the DAO through any constructor or initialization mechanism.

## R2: Injection Strategy for the ViewModel

**Decision**: Add `UserProfileDao` as a constructor parameter to `UserProfilesViewModel`. The factory (`ModuleSessionFactory`) provides the DAO when constructing the ViewModel.

**Rationale**: The ViewModel already accepts `UserProfilesControllerInterface` via constructor — adding a DAO parameter follows the same pattern. The ViewModel creates its own `UserProfileRepository` wrapper internally (it's a thin convenience layer), so injecting the DAO is the right granularity.

**Current**:
```kotlin
class UserProfilesViewModel(private val controller: UserProfilesControllerInterface) : ViewModel() {
    init {
        val repo = UserProfileRepository(DatabaseModule.getDatabase().userProfileDao())
        // ...
    }
}
```

**After**:
```kotlin
class UserProfilesViewModel(
    private val controller: UserProfilesControllerInterface,
    userProfileDao: UserProfileDao
) : ViewModel() {
    init {
        val repo = UserProfileRepository(userProfileDao)
        // ...
    }
}
```

**Alternatives considered**:
- Inject `UserProfileRepository` instead of `UserProfileDao`: Would work, but the DAO is the natural Room abstraction boundary. The repository is a thin wrapper that the ViewModel can create itself.
- Inject `AppDatabase`: Too broad — exposes the entire database to a module that only needs one DAO.

## R3: Injection Strategy for Processing Logic

**Decision**: Use a module-level initialization function that sets the DAO before the flow starts. The processing logic tick function reads from this initialized state.

**Rationale**: The tick function is a top-level lambda (`val userProfileRepositoryTick: In3AnyOut2ProcessBlock<...> = { ... }`). It cannot take constructor parameters. A module-level `lateinit` or nullable property with an initialization function is the standard pattern for this case.

**Design**:
```kotlin
// New: Module-level initialization
object UserProfilesPersistence {
    private var _dao: UserProfileDao? = null

    fun initialize(dao: UserProfileDao) {
        _dao = dao
    }

    fun getDao(): UserProfileDao =
        _dao ?: error("UserProfiles persistence not initialized. Call UserProfilesPersistence.initialize() before starting the flow.")
}
```

The tick function then uses `UserProfilesPersistence.getDao()` instead of `DatabaseModule.getDatabase().userProfileDao()`.

**Alternatives considered**:
- `lateinit var` at file level: Works but lacks error messaging. An object with explicit error is more debuggable.
- Pass DAO through the flow graph: Would require changing the FBP DSL runtime framework. Over-engineered for this use case.

## R4: Wiring Location — ModuleSessionFactory

**Decision**: `ModuleSessionFactory.createUserProfilesSession()` in graphEditor is the wiring point for desktop. It already creates the controller and ViewModel — it will also obtain the DAO from `DatabaseModule` and pass it through.

**Rationale**: ModuleSessionFactory is the existing composition root for desktop runtime sessions. It already has this pattern:
```kotlin
val controller = UserProfilesController(userProfilesFlowGraph)
val adapter = UserProfilesControllerAdapter(controller)
val viewModel = UserProfilesViewModel(adapter)
```

Adding DAO wiring fits naturally:
```kotlin
val dao = DatabaseModule.getDatabase().userProfileDao()
UserProfilesPersistence.initialize(dao)
val viewModel = UserProfilesViewModel(adapter, dao)
```

For mobile (KMPMobileApp), the equivalent wiring happens wherever the UserProfiles module is initialized.

## R5: Entity/DAO Relocation — Breaking the BaseDao Dependency

**Decision**: When moving UserProfileEntity, UserProfileDao, and UserProfileRepository back to UserProfiles (US2), UserProfileDao will continue to extend BaseDao. UserProfiles will depend on the persistence module via `api` for BaseDao access.

**Rationale**: BaseDao provides the standard `@Insert`, `@Update`, `@Delete` methods that any entity DAO needs. Removing this inheritance would require duplicating Room annotations in every DAO — defeating the purpose of shared infrastructure. The persistence module exposes `room-runtime` as `api`, so UserProfiles already has Room annotation visibility. Adding BaseDao as a transitive type via persistence's `api` scope keeps things clean.

**Dependency direction after US2**:
- `UserProfiles` → (no dependency on persistence — UserProfileDao defines its own CRUD methods)
- `persistence` → `UserProfiles` (AppDatabase references UserProfileEntity)

The key: after US1, UserProfiles no longer needs DatabaseModule/AppDatabase from persistence. By also removing the BaseDao dependency (UserProfileDao declares its own CRUD), UserProfiles becomes fully independent of persistence. The dependency inverts: persistence depends on UserProfiles, not vice versa.

**Alternatives considered**:
- Keep UserProfileDao extending BaseDao: Creates circular dependency (UserProfiles → persistence for BaseDao, persistence → UserProfiles for entity types in AppDatabase). Rejected.
- Move BaseDao to fbpDsl: Would add a Room dependency to fbpDsl, which is inappropriate. Rejected.
- Move BaseDao to UserProfiles: Then other future entity modules would depend on UserProfiles just for BaseDao — same artificial coupling. Rejected.

## R6: AppDatabase Stays in Persistence — Dependency Inversion

**Decision**: Keep AppDatabase, DatabaseModule, and platform-specific DatabaseBuilder files in the persistence module. After US2, reverse the dependency direction: persistence depends on UserProfiles (for entity types), UserProfiles does NOT depend on persistence.

**Rationale**: circuitSimulator is JVM-only — it cannot host platform-specific DatabaseBuilder files (Android, iOS). No existing module has all three targets (JVM + Android + iOS) except persistence itself. Creating a new module for 4-5 files is over-engineered.

Instead, the dependency direction simply inverts:
- **Before**: UserProfiles → persistence (UserProfiles calls DatabaseModule)
- **After**: persistence → UserProfiles (AppDatabase references UserProfileEntity)

UserProfiles has zero dependency on persistence because:
1. US1 eliminates `DatabaseModule.getDatabase()` calls (DI replaces them)
2. US2 moves entity/DAO classes back to UserProfiles
3. UserProfileDao defines its own CRUD methods (no BaseDao dependency)

**After US2+US3 dependency graph**:
```
UserProfiles (entity/DAO/repo — no persistence dependency)
    ↑
persistence (BaseDao, AppDatabase, DatabaseModule, DatabaseBuilder.* — depends on UserProfiles)
    ↑
graphEditor / KMPMobileApp (app entry points, wire DAOs at startup)
```

BaseDao remains in persistence as generic infrastructure for future entity modules. It's not referenced by UserProfiles but will be useful when new entity modules are added.

**Alternatives considered**:
- Move AppDatabase to circuitSimulator: JVM-only target — can't host Android/iOS DatabaseBuilder files. Rejected.
- Create a new `appDatabase` module: Adds a module for 4-5 files when persistence already has the right targets. Rejected.
- Duplicate AppDatabase in graphEditor + KMPMobileApp: Maintenance burden, schema divergence risk. Rejected.

## R7: Room KSP Processor Location

**Decision**: Room KSP processors stay in persistence alongside AppDatabase. No change needed from the current setup.

**Rationale**: Room's KSP processor generates code based on `@Database` and `@Entity` annotations. AppDatabase stays in persistence, so KSP stays there. UserProfileEntity (with `@Entity` annotation) is visible to KSP because persistence depends on UserProfiles.

**Build config change**: persistence/build.gradle.kts adds `implementation(project(":UserProfiles"))` to access entity types. graphEditor and KMPMobileApp add `implementation(project(":persistence"))` directly (no longer transitive through UserProfiles).
