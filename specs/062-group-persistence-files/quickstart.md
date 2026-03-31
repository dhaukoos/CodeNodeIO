# Quickstart: Group Persistence Files by Entity

**Feature**: 062-group-persistence-files

## Scenario 1: Verify Existing Module Migration

**Steps**:
1. Compile all existing modules: `./gradlew :UserProfiles:jvmJar :Addresses:jvmJar :StopWatch:jvmJar`
2. Verify persistence directory structure:
   - `persistence/src/commonMain/kotlin/io/codenode/persistence/UserProfile/` contains `UserProfileEntity.kt`, `UserProfileDao.kt`, `UserProfileRepository.kt`
   - `persistence/src/commonMain/kotlin/io/codenode/persistence/Address/` contains `AddressEntity.kt`, `AddressDao.kt`, `AddressRepository.kt`
3. Verify no entity files remain at the flat persistence root (only `AppDatabase.kt`, `BaseDao.kt`, `DatabaseModule.kt`, `PersistenceBootstrap.kt`)

**Expected**:
- Compilation succeeds with zero errors
- Each entity's three files are grouped in their own PascalCase subdirectory
- Package declarations in moved files use lowercase sub-package (e.g., `package io.codenode.persistence.userprofile`)
- No persistence files at root level except shared infrastructure files

---

## Scenario 2: Runtime Preview Still Works After Migration

**Steps**:
1. Rebuild classpath: `./gradlew jvmJar writeRuntimeClasspath --rerun-tasks`
2. Launch graphEditor and load the UserProfiles module
3. Open Runtime Preview and perform a CRUD operation (add a user profile)
4. Load the Addresses module and verify the same

**Expected**:
- Runtime preview launches successfully for both modules
- CRUD operations work correctly (create, read, update, delete)
- No errors in console output related to persistence or Room

---

## Scenario 3: Create New Repository Module with Grouped Files

**Steps**:
1. Launch graphEditor
2. Create a new IP type "Product" with properties: `name` (String), `price` (Int)
3. Select the Product IP type and click "Create Repository Module"
4. Inspect `persistence/src/commonMain/kotlin/io/codenode/persistence/Product/`

**Expected**:
- `ProductEntity.kt` is in `persistence/.../Product/` subdirectory (not at flat root)
- `ProductDao.kt` is in `persistence/.../Product/` subdirectory
- `ProductRepository.kt` is in `persistence/.../Product/` subdirectory
- All three files declare `package io.codenode.persistence.product`
- No entity/DAO/repository files at the flat persistence root

---

## Scenario 4: Compile and Run New Module

**Steps**:
1. After Scenario 3, compile: `./gradlew :Products:jvmJar`
2. Rebuild classpath: `./gradlew jvmJar writeRuntimeClasspath --rerun-tasks`
3. Launch graphEditor, load the Products module
4. Open Runtime Preview and perform CRUD operations

**Expected**:
- Compilation succeeds with zero errors
- Runtime pipeline functions correctly
- Data flows through CUD → Repository → Display

---

## Scenario 5: Remove Generated Module Cleans Up Subdirectory

**Steps**:
1. After Scenario 3, click "Remove Repository Module" for Product
2. Inspect `persistence/src/commonMain/kotlin/io/codenode/persistence/`

**Expected**:
- The entire `Product/` subdirectory is deleted (not just individual files)
- `AppDatabase.kt` no longer references `ProductEntity`
- `PersistenceBootstrap.kt` no longer registers `ProductDao`
- `include(":Products")` removed from `settings.gradle.kts`
- `graphEditorRuntime(project(":Products"))` removed from root `build.gradle.kts`
