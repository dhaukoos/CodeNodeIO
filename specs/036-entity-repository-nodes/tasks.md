# Tasks: Entity Repository Nodes

**Input**: Design documents from `/specs/036-entity-repository-nodes/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, quickstart.md

**Tests**: Included per constitution TDD mandate. Code generators require verification of correct output.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: User Story 1 — Display IP Type Properties in Properties Panel (Priority: P1)

**Goal**: When a user selects a custom IP Type in the IP Types palette, the Properties Panel displays the type's properties with name, resolved type, and required/optional badge. Add a "Create Repository Node" button placeholder (disabled, visible only for custom types with properties) to prepare for US2.

**Note**: `IPTypePropertiesPanel` already exists in `PropertiesPanel.kt` (line 1168) and already displays type name, description, color swatch, and custom properties. The `CompactPropertiesPanelWithViewModel` already routes to it when `selectedIPType != null`. The remaining work is adding the "Create Repository Node" button and wiring the callback.

**Independent Test**: Select a custom IP Type in the IP Types palette. Verify the Properties Panel shows the type name, color swatch, and properties. Verify a "Create Repository Node" button appears for custom types with properties but not for built-in types.

### Implementation for User Story 1

- [X] T001 [US1] Add `onCreateRepositoryNode: (() -> Unit)? = null` and `repositoryExists: Boolean = false` parameters to `IPTypePropertiesPanel` composable. Add a "Create Repository Node" `Button` below the properties section, visible only when `onCreateRepositoryNode != null` (custom type with properties), disabled when `repositoryExists == true` with label "Repository exists" in `graphEditor/src/jvmMain/kotlin/ui/PropertiesPanel.kt`
- [X] T002 [US1] Update `CompactPropertiesPanelWithViewModel` to pass `onCreateRepositoryNode` and `repositoryExists` parameters through to `IPTypePropertiesPanel` — add these as new parameters to `CompactPropertiesPanelWithViewModel` itself in `graphEditor/src/jvmMain/kotlin/ui/PropertiesPanel.kt`
- [X] T003 [US1] Wire `onCreateRepositoryNode` callback in `Main.kt` — pass a lambda to `CompactPropertiesPanelWithViewModel` that will be implemented in US2 (for now, pass `null` or a placeholder). Compute `repositoryExists` by checking `customNodeRepository.getAll().any { it.isRepository && it.sourceIPTypeId == selectedIPType?.id }` (requires T005 for the `isRepository` field) in `graphEditor/src/jvmMain/kotlin/Main.kt`
- [X] T004 [US1] Compile and verify: `./gradlew :graphEditor:compileKotlinJvm`

**Checkpoint**: Properties Panel shows custom IP type properties with a "Create Repository Node" button. Button visible only for custom types with properties. Built-in types show no button.

---

## Phase 2: User Story 2 — Create Repository Node from Custom IP Type (Priority: P2)

**Goal**: Clicking "Create Repository Node" generates a `CustomNodeDefinition` with `isRepository = true`, standardized ports (save, update, remove inputs; result, error outputs), and persists it. The node appears in the Node Palette.

**Independent Test**: Select a custom IP Type "User", click "Create Repository Node", verify "UserRepository" appears in Node Palette with 3 input ports and 2 output ports. Verify duplicate creation is prevented.

### Implementation for User Story 2

- [X] T005 [US2] Add `isRepository: Boolean = false`, `sourceIPTypeId: String? = null`, `sourceIPTypeName: String? = null` fields to `CustomNodeDefinition` data class. Add `createRepository(ipTypeName: String, sourceIPTypeId: String): CustomNodeDefinition` factory method that returns a definition with `inputCount = 3, outputCount = 2, genericType = "in3out2", isRepository = true` in `graphEditor/src/jvmMain/kotlin/repository/CustomNodeDefinition.kt`
- [X] T006 [US2] Update `toNodeTypeDefinition()` in `CustomNodeDefinition` — when `isRepository == true`, create a `NodeTypeDefinition` with named ports: input ports (save, update, remove) typed to `sourceIPTypeName`, output ports (result typed to `sourceIPTypeName`, error typed to String). Use `createGenericNodeType()` with a `_repository` configuration flag, or build the `NodeTypeDefinition` directly with explicit port definitions in `graphEditor/src/jvmMain/kotlin/repository/CustomNodeDefinition.kt`
- [X] T007 [US2] Implement `createRepositoryNode()` logic in `Main.kt` — wire the `onCreateRepositoryNode` callback to: (1) call `CustomNodeDefinition.createRepository(ipTypeName, ipTypeId)`, (2) add to `customNodeRepository`, (3) increment `customNodesVersion` to refresh the Node Palette, (4) show status message "Created {name}Repository node" in `graphEditor/src/jvmMain/kotlin/Main.kt`
- [X] T008 [US2] Add duplicate prevention — compute `repositoryExists` from `customNodeRepository.getAll().any { it.isRepository && it.sourceIPTypeId == selectedIPType?.id }` and pass to `CompactPropertiesPanelWithViewModel`. Also check in `onCreateRepositoryNode` callback and skip if already exists in `graphEditor/src/jvmMain/kotlin/Main.kt`
- [X] T009 [US2] Compile and verify: `./gradlew :graphEditor:compileKotlinJvm`

**Checkpoint**: "Create Repository Node" button creates a persisted repository node that appears in the Node Palette. Duplicate creation is prevented. Node can be placed on canvas with correct ports.

---

## Phase 3: User Story 3 — Repository Node Code Generation (Priority: P3)

**Goal**: Create `RepositoryCodeGenerator` that generates Entity, DAO, Repository, and BaseDao classes from repository node metadata. Integrate into `ModuleSaveService` so that saving a flow graph with repository nodes produces persistence code.

**Independent Test**: Create a flow graph with a "UserRepository" node, trigger save/code generation, verify generated files include UserEntity.kt, UserDao.kt, UserRepository.kt, BaseDao.kt with correct content.

### Implementation for User Story 3

- [ ] T010 [P] [US3] Create `EntityProperty` and `EntityInfo` data classes in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/RepositoryCodeGenerator.kt` — `EntityProperty(name: String, kotlinType: String, isRequired: Boolean)` and `EntityInfo(entityName: String, tableName: String, daoName: String)`
- [ ] T011 [US3] Implement `RepositoryCodeGenerator.generateBaseDao(packageName: String): String` — generates a `BaseDao<T>` interface with `@Insert(onConflict = OnConflictStrategy.REPLACE)`, `@Update`, `@Delete` methods per research.md R6 pattern in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/RepositoryCodeGenerator.kt`
- [ ] T012 [US3] Implement `RepositoryCodeGenerator.generateEntity(entityName, properties, packageName): String` — generates a `@Entity` data class with `@PrimaryKey(autoGenerate = true) val id: Long = 0` plus columns mapped from properties using IP type→Kotlin type mapping from data-model.md in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/RepositoryCodeGenerator.kt`
- [ ] T013 [US3] Implement `RepositoryCodeGenerator.generateDao(entityName, tableName, packageName): String` — generates a `@Dao` interface extending `BaseDao<{Entity}Entity>` with `@Query("SELECT * FROM {tableName}") fun getAllAsFlow(): Flow<List<{Entity}Entity>>` in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/RepositoryCodeGenerator.kt`
- [ ] T014 [US3] Implement `RepositoryCodeGenerator.generateRepository(entityName, packageName): String` — generates a `{Entity}Repository` class wrapping the DAO with `save()`, `update()`, `remove()`, `observeAll()` methods per the Repository interface pattern in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/RepositoryCodeGenerator.kt`
- [ ] T015 [P] [US3] Create `RepositoryCodeGeneratorTest` with tests for each generator method — verify `generateBaseDao()` output contains `@Insert`, `@Update`, `@Delete` annotations; verify `generateEntity()` output contains `@Entity`, `@PrimaryKey`, correct columns; verify `generateDao()` output extends `BaseDao` and has `getAllAsFlow()`; verify `generateRepository()` wraps DAO methods in `kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/RepositoryCodeGeneratorTest.kt`
- [ ] T016 [US3] Compile and run tests: `./gradlew :kotlinCompiler:jvmTest`

**Checkpoint**: `RepositoryCodeGenerator` produces correct Entity, DAO, Repository, and BaseDao code. All generator tests pass.

---

## Phase 4: User Story 4 — Database Module Integration (Priority: P4)

**Goal**: Generate a shared `AppDatabase` class and singleton `DatabaseModule` when repository nodes exist. Integrate `RepositoryCodeGenerator` into `ModuleSaveService` to produce persistence files alongside existing generated code.

**Independent Test**: Save a flow graph with two repository nodes ("UserRepository", "OrderRepository"). Verify generated module contains `persistence/` package with BaseDao, both entities, both DAOs, both repositories, AppDatabase with both entities registered, and platform-specific DatabaseBuilder files.

### Implementation for User Story 4

- [ ] T017 [US4] Implement `RepositoryCodeGenerator.generateDatabase(entities: List<EntityInfo>, packageName): String` — generates `@Database(entities = [...], version = 1)` class with `@ConstructedBy` annotation, `expect object` constructor, and abstract DAO accessor methods per research.md R5 pattern in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/RepositoryCodeGenerator.kt`
- [ ] T018 [US4] Implement `RepositoryCodeGenerator.generateDatabaseModule(packageName): String` — generates singleton `DatabaseModule` object with `getDatabase()` method using lazy initialization and `getRoomDatabase()` helper in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/RepositoryCodeGenerator.kt`
- [ ] T019 [US4] Implement `RepositoryCodeGenerator.generateDatabaseBuilder(platform, packageName, dbFileName): String` — generates platform-specific `getDatabaseBuilder()` and `getRoomDatabase()` functions for JVM (File-based path `~/.codenode/data/{dbFileName}`), Android (context-based), and iOS (NSDocumentDirectory) per research.md R5 in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/RepositoryCodeGenerator.kt`
- [ ] T020 [US4] Integrate `RepositoryCodeGenerator` into `ModuleSaveService.saveModule()` — detect repository nodes in the FlowGraph via `_repository` configuration or `isRepository` flag on the CustomNodeDefinition, extract entity metadata from custom IP type properties, generate all persistence files into `persistence/` subdirectory, generate platform-specific builders in `jvmMain/`/`androidMain/`/`iosMain/` source sets in `graphEditor/src/jvmMain/kotlin/save/ModuleSaveService.kt`
- [ ] T021 [US4] Add repository node configuration to `CustomNodeDefinition.toNodeTypeDefinition()` — when creating a CodeNode from a repository node, ensure `_repository = "true"` and `_sourceIPTypeId = {id}` and `_sourceIPTypeName = {name}` are set in the node's configuration map so code generators can detect repository nodes in `graphEditor/src/jvmMain/kotlin/repository/CustomNodeDefinition.kt`
- [ ] T022 [P] [US4] Add tests for `generateDatabase()` — verify output contains `@Database` annotation with correct entity list, abstract DAO methods, `@ConstructedBy` annotation. Add tests for `generateDatabaseModule()` — verify singleton pattern with lazy init. Add tests for `generateDatabaseBuilder()` — verify JVM/Android/iOS variants in `kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/RepositoryCodeGeneratorTest.kt`
- [ ] T023 [US4] Compile and run all tests: `./gradlew :kotlinCompiler:jvmTest :graphEditor:compileKotlinJvm`

**Checkpoint**: Flow graphs with repository nodes generate a complete persistence layer. All entity/DAO/repository/database files are produced in the correct directories. Tests pass.

---

## Dependencies & Execution Order

### Phase Dependencies

- **User Story 1 (Phase 1)**: No dependencies — can start immediately. Partially done (IPTypePropertiesPanel exists).
- **User Story 2 (Phase 2)**: Depends on US1 T001-T002 (button in Properties Panel). T005 (isRepository field) can start immediately.
- **User Story 3 (Phase 3)**: Independent of US1/US2 (different module: kotlinCompiler). Can run in parallel.
- **User Story 4 (Phase 4)**: Depends on US3 (RepositoryCodeGenerator class) and US2 T005/T021 (repository node configuration).

### Within Each Phase

**US1**: T001 → T002 → T003 → T004
**US2**: T005 → T006 → T007 → T008 → T009
**US3**: T010 → T011 → T012 → T013 → T014 → T015 → T016 (T010 ∥ T015 can start early)
**US4**: T017 → T018 → T019 → T020 → T021 → T022 → T023

### Parallel Opportunities

- US1 and US3 can run in parallel (completely independent: graphEditor vs kotlinCompiler)
- Within US3: T010 and T015 (data classes and test scaffolding) can start in parallel
- Within US4: T022 (tests) can run in parallel with T020/T021 once T017-T019 are complete

---

## Implementation Strategy

### MVP First (User Story 1 + 2)

1. Complete Phase 1: US1 — Button in Properties Panel (T001–T004)
2. Complete Phase 2: US2 — Create Repository Node (T005–T009)
3. **STOP and VALIDATE**: Create a custom IP type, create repository node, verify it appears in palette with correct ports
4. UI feature is complete and visible

### Incremental Delivery

1. US1 (T001–T004): Properties Panel button → Visual foundation
2. US2 (T005–T009): Repository node creation → Users can create repository nodes
3. US3 (T010–T016): Code generators → Persistence code can be generated
4. US4 (T017–T023): Database module → Full save-compile-run lifecycle

### Parallel Team Strategy

With two developers:
1. Developer A: US1 (T001–T004) then US2 (T005–T009) — graphEditor UI
2. Developer B: US3 (T010–T016) — kotlinCompiler generators
3. Both done → either developer: US4 (T017–T023) — integration

---

## Notes

- `IPTypePropertiesPanel` already exists and displays properties — US1 only adds the button
- `CustomNodeDefinition` is `@Serializable` — new fields must have defaults for backward compatibility with existing `custom-nodes.json` files
- Repository nodes use `genericType = "in3out2"` but need special port naming (save/update/remove/result/error) unlike generic nodes which have numbered ports
- The `_repository` configuration key in CodeNode configuration is the bridge between the UI/persistence layer and the code generators
- Room KMP requires KSP — generated modules that include persistence code need KSP plugin in their build.gradle.kts (handled by T020)
- `@Query` methods cannot be in generic BaseDao — each entity-specific DAO must have its own `getAllAsFlow()` method
