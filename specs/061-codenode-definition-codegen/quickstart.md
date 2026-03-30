# Quickstart: Generate CodeNodeDefinition-Based Repository Modules

**Feature**: 061-codenode-definition-codegen

## Scenario 1: Create a New Repository Module

**Steps**:
1. Launch graphEditor
2. Create a new IP type "TestItem" with properties: `name` (String), `quantity` (Int)
3. Select the TestItem IP type in the palette
4. Click "Create Repository Module" in the Properties panel

**Expected**:
- Module directory `TestItems/` created in the project
- `TestItems/src/commonMain/kotlin/io/codenode/testitems/nodes/` contains:
  - `TestItemCUDCodeNode.kt` (implements CodeNodeDefinition, SOURCE)
  - `TestItemRepositoryCodeNode.kt` (implements CodeNodeDefinition, TRANSFORMER)
  - `TestItemsDisplayCodeNode.kt` (implements CodeNodeDefinition, SINK)
- All three files use `TestItem::class` for entity ports (not `Any::class`)
- No legacy `createTestItemCUD()` or `createTestItemsDisplay()` factory functions generated
- No tick function references in any generated file

---

## Scenario 2: Compile and Run the Generated Module

**Steps**:
1. After Scenario 1, compile: `./gradlew :TestItems:jvmJar`
2. Rebuild classpath: `./gradlew jvmJar writeRuntimeClasspath --rerun-tasks`
3. Launch graphEditor and load the TestItems module
4. Start the runtime pipeline

**Expected**:
- Compilation succeeds with zero errors
- All three nodes appear in the flow graph canvas
- Data flows through the pipeline when CRUD operations are triggered
- The textual view shows `TestItem::class` on entity ports

---

## Scenario 3: Verify Generated Flow.kt Has No Legacy References

**Steps**:
1. After Scenario 1, inspect `TestItems/src/commonMain/kotlin/io/codenode/testitems/generated/TestItemsFlow.kt`

**Expected**:
- Imports reference CodeNodeDefinition objects: `import io.codenode.testitems.nodes.TestItemCUDCodeNode`
- Runtime instances created via: `TestItemCUDCodeNode.createRuntime("TestItemCUD")`
- No `import io.codenode.testitems.testItemRepositoryTick`
- No `createTestItemCUD()` or `createTestItemsDisplay()` calls

---

## Scenario 4: Existing Modules Still Work

**Steps**:
1. Compile all existing modules: `./gradlew :UserProfiles:jvmJar :GeoLocations:jvmJar :Addresses:jvmJar :StopWatch:jvmJar`
2. Launch graphEditor and load UserProfiles
3. Verify runtime pipeline works

**Expected**:
- All existing modules compile with zero errors
- Runtime previews function correctly
- No regressions from generator changes

---

## Scenario 5: Remove Generated Module

**Steps**:
1. After Scenario 1, click "Remove Repository Module" for TestItem
2. Verify cleanup

**Expected**:
- `TestItems/` module directory deleted
- `TestItemEntity.kt`, `TestItemDao.kt`, `TestItemRepository.kt` removed from persistence
- `TestItemCUDCodeNode.kt`, `TestItemRepositoryCodeNode.kt`, `TestItemsDisplayCodeNode.kt` removed
- `include(":TestItems")` removed from settings.gradle.kts
