# Tasks: Generalize Entity Repository Module Creation

**Input**: Design documents from `/specs/047-entity-module-generator/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, quickstart.md

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **kotlinCompiler generators**: `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/`
- **kotlinCompiler tests**: `kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/`
- **graphEditor**: `graphEditor/src/jvmMain/kotlin/`
- **persistence**: `persistence/src/commonMain/kotlin/io/codenode/persistence/`

---

## Phase 1: Setup

**Purpose**: Create the EntityModuleSpec data class and extend CustomNodeDefinition with new factory methods

- [ ] T001 Create `EntityModuleSpec` data class in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/EntityModuleSpec.kt` with fields: entityName, entityNameLower, pluralName, pluralNameLower, properties (List<EntityProperty>), sourceIPTypeId, basePackage, persistencePackage. Include a companion `fromIPType(ipTypeName: String, sourceIPTypeId: String, properties: List<EntityProperty>)` factory that derives all naming variants.
- [ ] T002 Add `createCUD(ipTypeName: String, sourceIPTypeId: String)` factory method to `graphEditor/src/jvmMain/kotlin/repository/CustomNodeDefinition.kt` — creates source node (0 inputs, 3 outputs: save, update, remove) with config `_cudSource=true`, `_sourceIPTypeId`, `_sourceIPTypeName`. Also update `toNodeTypeDefinition()` to handle `_cudSource` config with correct output port names.
- [ ] T003 Add `createDisplay(ipTypeName: String, sourceIPTypeId: String)` factory method to `graphEditor/src/jvmMain/kotlin/repository/CustomNodeDefinition.kt` — creates sink node (2 inputs: entities, error; 0 outputs) with config `_display=true`, `_sourceIPTypeId`, `_sourceIPTypeName`. Also update `toNodeTypeDefinition()` to handle `_display` config with correct input port names.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core generator classes that all user stories depend on

**CRITICAL**: No user story work can begin until these generators exist

- [ ] T004 [P] Create `EntityCUDGenerator` class in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/EntityCUDGenerator.kt` with method `generate(spec: EntityModuleSpec): String` that produces a {Entity}CUD.kt source node stub file. Use `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/UserProfileCUD.kt` as the template. The stub should declare reactive source state flows for save, update, remove — parameterized by entity name.
- [ ] T005 [P] Create `EntityDisplayGenerator` class in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/EntityDisplayGenerator.kt` with method `generate(spec: EntityModuleSpec): String` that produces a {Entity}sDisplay.kt sink node stub file. Use `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/UserProfilesDisplay.kt` as the template. The stub should accept 2 inputs (entities list, error) — parameterized by entity name.
- [ ] T006 [P] Create `EntityPersistenceGenerator` class in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/EntityPersistenceGenerator.kt` with method `generate(spec: EntityModuleSpec): String` that produces a {Entity}sPersistence.kt Koin module file. Use `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/UserProfilesPersistence.kt` as the template. Generates a Koin `module {}` providing the Repository and a `KoinComponent` object exposing the DAO — parameterized by entity name.

**Checkpoint**: Foundation generators ready — user story implementation can begin

---

## Phase 3: User Story 2 — Generalized Code-Generation Templates for Nodes (Priority: P2)

**Goal**: Complete node generation templates for CUD source, Repository processor (already exists), and Display sink, plus FlowGraph generation.

**Independent Test**: Invoke each generator with "GeoLocation" entity name and verify output matches expected pattern.

### Implementation for User Story 2

- [ ] T007 [P] [US2] Create `EntityFlowGraphBuilder` class in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/EntityFlowGraphBuilder.kt` with method `buildFlowGraph(spec: EntityModuleSpec): FlowGraph` that programmatically creates a FlowGraph with 3 nodes ({Entity}CUD source, {Entity}Repository processor, {Entity}sDisplay sink) and all connections: CUD.save → Repository.save, CUD.update → Repository.update, CUD.remove → Repository.remove, Repository.result → Display.entities, Repository.error → Display.error. Use `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/UserProfiles.flow.kt` as the reference for node types, port types, and connection patterns.
- [ ] T008 [P] [US2] Write unit test `EntityCUDGeneratorTest` in `kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/EntityCUDGeneratorTest.kt` — verify generated CUD stub for "GeoLocation" contains: correct package, MutableStateFlow declarations for save/update/remove, entity import, and proper naming (GeoLocationCUD).
- [ ] T009 [P] [US2] Write unit test `EntityDisplayGeneratorTest` in `kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/EntityDisplayGeneratorTest.kt` — verify generated Display stub for "GeoLocation" contains: correct package, proper naming (GeoLocationsDisplay), and two input parameters.
- [ ] T010 [US2] Write unit test `EntityFlowGraphBuilderTest` in `kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/EntityFlowGraphBuilderTest.kt` — verify built FlowGraph has 3 nodes with correct names, port counts, and 5 connections (3 CUD→Repository + 2 Repository→Display).

**Checkpoint**: Node templates generate correct output for any entity name

---

## Phase 4: User Story 3 — Generalized UI Composable Templates (Priority: P3)

**Goal**: Generate three UI composable files ({Entity}s.kt, AddUpdate{Entity}.kt, {Entity}Row.kt) with property-aware form fields and display columns.

**Independent Test**: Generate UI files for "GeoLocation" and verify each contains correct form fields mapped from entity properties.

### Implementation for User Story 3

- [ ] T011 [US3] Create `EntityUIGenerator` class in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/EntityUIGenerator.kt` with three methods:
  - `generateListView(spec: EntityModuleSpec): String` — generates {Entity}s.kt composable with scrollable entity list, add/update/remove buttons, selection state, and remove confirmation dialog. Use `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/userInterface/UserProfiles.kt` as template.
  - `generateFormView(spec: EntityModuleSpec): String` — generates AddUpdate{Entity}.kt composable with form fields mapped to entity properties per the property-to-UI mapping table (String→OutlinedTextField, Int/Long/Double/Float→numeric OutlinedTextField, Boolean→Checkbox). Optional properties show "(optional)" in label. Use `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/userInterface/AddUpdateUserProfile.kt` as template.
  - `generateRowView(spec: EntityModuleSpec): String` — generates {Entity}Row.kt composable displaying all entity properties in a single clickable row. Use the `UserProfileRow` composable in `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/userInterface/UserProfiles.kt` as template (extract the row pattern into a standalone file).
- [ ] T012 [P] [US3] Write unit test `EntityUIGeneratorTest` in `kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/EntityUIGeneratorTest.kt` — verify for "GeoLocation" entity with properties (latitude: Double, longitude: Double, label: String, altitude: Double?, isActive: Boolean):
  - `generateListView` output contains: `@Composable fun GeoLocations(`, add/update/remove buttons, `LazyColumn`
  - `generateFormView` output contains: `@Composable fun AddUpdateGeoLocation(`, `OutlinedTextField` for latitude/longitude/label/altitude, `Checkbox` for isActive, `KeyboardType.Number` for numeric fields, "(optional)" label for altitude
  - `generateRowView` output contains: `@Composable fun GeoLocationRow(`, property display text for all fields

**Checkpoint**: UI templates generate property-aware composables for any entity

---

## Phase 5: User Story 4 — ViewModel with CRUD Methods (Priority: P4)

**Goal**: Modify RuntimeViewModelGenerator to produce entity-specific CRUD methods, DAO constructor parameter, and repository observation.

**Independent Test**: Generate ViewModel for "GeoLocations" FlowGraph and verify it contains addGeoLocation/updateGeoLocation/removeGeoLocation methods.

### Implementation for User Story 4

- [ ] T013 [US4] Modify `RuntimeViewModelGenerator` in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/RuntimeViewModelGenerator.kt` to detect entity-module FlowGraphs (presence of nodes with `_cudSource`, `_repository`, `_display` configurations) and when detected:
  - Add `{entity}Dao: {Entity}Dao` constructor parameter to the ViewModel class
  - Add `val profiles: StateFlow<List<{Entity}Entity>>` backed by `{Entity}sState._profiles`
  - Add `init {}` block that launches repository observation via `{Entity}Repository({entity}Dao).observeAll().collect { ... }`
  - Add CRUD methods: `fun add{Entity}(item: {Entity}Entity)`, `fun update{Entity}(item: {Entity}Entity)`, `fun remove{Entity}(item: {Entity}Entity)` that write to the corresponding `{Entity}sState._save/update/remove` flows
  - Add necessary imports for the entity, DAO, repository classes from `io.codenode.persistence`
  - Use `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/UserProfilesViewModel.kt` as the exact reference template
- [ ] T014 [P] [US4] Write unit test for entity ViewModel generation in `kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/RuntimeViewModelGeneratorTest.kt` (add to existing test file or create new) — verify generated ViewModel for a "GeoLocations" entity FlowGraph contains: `geoLocationDao: GeoLocationDao` parameter, `addGeoLocation`, `updateGeoLocation`, `removeGeoLocation` methods, repository observation in init block, import of `io.codenode.persistence.GeoLocationDao`.

**Checkpoint**: ViewModel generation produces entity-specific CRUD methods

---

## Phase 6: User Story 5 — Button Replacement and Module Generation Pipeline (Priority: P5)

**Goal**: Replace "Create Repository Node" button with "Create Repository Module" and wire the full generation pipeline.

**Independent Test**: Click "Create Repository Module" for a custom IP type and verify all three node definitions are created and the module directory is generated.

### Implementation for User Story 5

- [ ] T015 [US5] Create `EntityModuleGenerator` orchestrator class in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/EntityModuleGenerator.kt` with method `generateModule(spec: EntityModuleSpec): EntityModuleOutput` that:
  - Uses `EntityFlowGraphBuilder` to create the FlowGraph
  - Uses `FlowKtGenerator` to produce the .flow.kt file content
  - Uses `EntityCUDGenerator` to produce the CUD stub content
  - Uses `EntityDisplayGenerator` to produce the Display stub content
  - Uses `EntityUIGenerator` to produce all 3 UI file contents
  - Uses `EntityPersistenceGenerator` to produce the Koin module content
  - Uses `RuntimeViewModelGenerator` to produce the ViewModel content
  - Uses `RuntimeFlowGenerator`, `RuntimeControllerGenerator`, `RuntimeControllerInterfaceGenerator`, `RuntimeControllerAdapterGenerator` for the 4 generated/ files
  - Uses `ProcessingLogicStubGenerator` for the Repository processing logic stub
  - Uses `RepositoryCodeGenerator` to produce Entity, DAO, Repository files for the persistence module
  - Returns an `EntityModuleOutput` data class containing all generated file contents (map of relative path → content string)
- [ ] T016 [US5] Modify `ModuleSaveService` in `graphEditor/src/jvmMain/kotlin/save/ModuleSaveService.kt` to add a new method `saveEntityModule(spec: EntityModuleSpec, moduleOutputDir: File, persistenceDir: File): ModuleSaveResult` that:
  - Calls `EntityModuleGenerator.generateModule(spec)` to get all file contents
  - Creates the module directory structure (src/commonMain/kotlin/{package}/, generated/, processingLogic/, userInterface/)
  - Writes all entity module files to the module directory
  - Writes Entity, DAO, Repository files to the persistence module directory
  - Regenerates `AppDatabase.kt` in the persistence module with the new entity added (scan existing *Entity.kt files in persistence to collect all entities, then call `RepositoryCodeGenerator.generateDatabase()`)
  - Returns ModuleSaveResult with created/overwritten file counts
- [ ] T017 [US5] Modify `IPTypePropertiesPanel` in `graphEditor/src/jvmMain/kotlin/ui/PropertiesPanel.kt`:
  - Change button label from "Create Repository Node" to "Create Repository Module"
  - Change disabled label from "Repository exists" to "Module exists"
  - Update the `onCreateRepositoryNode` callback parameter name to `onCreateRepositoryModule`
  - The callback now creates all 3 `CustomNodeDefinition` instances (CUD, Repository, Display) via the new factory methods and adds them to `customNodeRepository`
  - After creating node definitions, calls `ModuleSaveService.saveEntityModule()` to generate the full module
- [ ] T018 [US5] Update `Main.kt` in `graphEditor/src/jvmMain/kotlin/Main.kt` to wire the new `onCreateRepositoryModule` callback:
  - Build `EntityModuleSpec` from the selected IP type (name, ID, properties from IP type registry)
  - Create 3 CustomNodeDefinitions (CUD, Repository, Display) and add to customNodeRepository
  - Call `ModuleSaveService.saveEntityModule()` with the spec, module output directory (project root / {PluralName}), and persistence directory (project root / persistence/src/commonMain/kotlin/io/codenode/persistence/)
  - Update `customNodes` state after adding the new definitions
- [ ] T019 [US5] Add module existence detection: in `graphEditor/src/jvmMain/kotlin/ui/PropertiesPanel.kt`, check if all 3 node definitions ({Entity}CUD, {Entity}Repository, {Entity}sDisplay) already exist in the customNodeRepository for the selected IP type — if so, disable the button and show "Module exists"

**Checkpoint**: Button triggers full module generation pipeline

---

## Phase 7: User Story 1 — End-to-End Validation with GeoLocation (Priority: P1)

**Goal**: Validate the complete pipeline by generating a GeoLocations module from a GeoLocation IP Type and verifying it compiles and functions correctly.

**Independent Test**: Follow the quickstart.md validation scenario end-to-end.

### Implementation for User Story 1

- [ ] T020 [US1] Run `./gradlew :graphEditor:run`, create "GeoLocation" IP Type with properties (latitude: Double required, longitude: Double required, label: String required, altitude: Double optional, isActive: Boolean required), click "Create Repository Module", and verify all files are generated per quickstart.md steps 1-4
- [ ] T021 [US1] Add `include(":GeoLocations")` to `settings.gradle.kts` and verify `./gradlew :GeoLocations:compileKotlinJvm` succeeds — fix any compilation errors in generated code
- [ ] T022 [US1] Verify `AppDatabase.kt` in `persistence/src/commonMain/kotlin/io/codenode/persistence/AppDatabase.kt` lists both `UserProfileEntity` and `GeoLocationEntity` in the `@Database` annotation, and has both `userProfileDao()` and `geoLocationDao()` accessor methods
- [ ] T023 [US1] Wire GeoLocations Koin module in `graphEditor/src/jvmMain/kotlin/Main.kt`: add `single { DatabaseModule.getDatabase().geoLocationDao() }` and `geoLocationsModule` to the Koin modules list. Verify `./gradlew :graphEditor:compileKotlinJvm` succeeds.
- [ ] T024 [US1] Verify generated module file contents match UserProfiles patterns: compare GeoLocationsViewModel.kt with UserProfilesViewModel.kt for structural equivalence, compare GeoLocations.flow.kt with UserProfiles.flow.kt for node/connection patterns, compare userInterface files for composable patterns

**Checkpoint**: GeoLocations module compiles and follows UserProfiles patterns exactly

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Cleanup and edge case handling

- [ ] T025 Add build.gradle.kts generation to `EntityModuleGenerator` or `ModuleSaveService.saveEntityModule()` — generate the module's build.gradle.kts with correct dependencies (project(":fbpDsl"), project(":persistence"), koin-core, coroutines, compose, lifecycle-viewmodel) following the UserProfiles/build.gradle.kts pattern
- [ ] T026 Handle edge case: entity with no properties (only auto-generated id) — ensure all generators produce valid output with an empty properties list
- [ ] T027 [P] Run full quickstart.md validation per `specs/047-entity-module-generator/quickstart.md` — verify all 8 steps pass

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on T001 (EntityModuleSpec) — BLOCKS all user stories
- **US2 (Phase 3)**: Depends on T004, T005 (CUD + Display generators)
- **US3 (Phase 4)**: Depends on T001 (EntityModuleSpec) — can run in parallel with US2
- **US4 (Phase 5)**: Depends on T001 (EntityModuleSpec) — can run in parallel with US2, US3
- **US5 (Phase 6)**: Depends on US2, US3, US4 completion (all generators must exist)
- **US1 (Phase 7)**: Depends on US5 completion (full pipeline must be wired)
- **Polish (Phase 8)**: Depends on US1 completion

### User Story Dependencies

- **US2 (Nodes)**: Independent after Phase 2 — no dependency on other stories
- **US3 (UI)**: Independent after Phase 2 — can run in parallel with US2
- **US4 (ViewModel)**: Independent after Phase 2 — can run in parallel with US2, US3
- **US5 (Button + Pipeline)**: Depends on US2, US3, US4 — integration layer
- **US1 (Validation)**: Depends on US5 — end-to-end test

### Parallel Opportunities

```
Phase 2 (parallel):
  T004 EntityCUDGenerator  |  T005 EntityDisplayGenerator  |  T006 EntityPersistenceGenerator

Phase 3-5 (parallel after Phase 2):
  US2: T007-T010 (nodes)  |  US3: T011-T012 (UI)  |  US4: T013-T014 (ViewModel)
```

---

## Implementation Strategy

### MVP First (US2 + US5 → Generate Nodes + Pipeline)

1. Complete Phase 1: Setup (EntityModuleSpec, CustomNodeDefinition extensions)
2. Complete Phase 2: Foundational generators
3. Complete US2: Node templates verified
4. Complete US5: Pipeline wired end-to-end
5. **STOP and VALIDATE**: Generate a module and inspect output

### Incremental Delivery

1. Setup + Foundational → Core generators ready
2. US2 (Nodes) → Node generation verified
3. US3 (UI) → UI generation verified → can run in parallel with US2
4. US4 (ViewModel) → ViewModel generation verified → can run in parallel
5. US5 (Pipeline) → Button wired, full pipeline works
6. US1 (Validation) → GeoLocation end-to-end test passes
7. Polish → Edge cases, build.gradle.kts generation

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- US2, US3, US4 can proceed in parallel after Phase 2
- US5 integrates all generators — must wait for US2, US3, US4
- US1 is pure validation — tests the complete pipeline
- All generators use UserProfiles module files as reference templates
- Persistence files go to shared `persistence/` module per feature 046 architecture
