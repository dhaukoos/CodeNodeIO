# Research: Generalize Entity Repository Module Creation

## R1: How to Generate Entity-Parameterized Node Stubs (CUD + Display)

**Decision**: Create dedicated generator classes (`EntityCUDGenerator`, `EntityDisplayGenerator`) that produce source/sink node stub files parameterized by entity name, following the existing UserProfileCUD.kt and UserProfilesDisplay.kt as templates.

**Rationale**: The existing `ProcessingLogicStubGenerator` generates generic tick functions but doesn't handle entity-specific imports, reactive source wiring, or display logic. Dedicated generators produce more usable output while keeping the existing generator untouched.

**Alternatives considered**:
- Extending ProcessingLogicStubGenerator with entity awareness — rejected because it would add conditional complexity to a generic generator.
- Using string templates (Mustache/FreeMarker) — rejected because all other generators use direct string building in Kotlin, maintaining consistency.

## R2: How to Generate Entity-Parameterized UI Composables

**Decision**: Create a new `EntityUIGenerator` class with three methods (`generateListView`, `generateFormView`, `generateRowView`) that map entity properties to Compose form fields and display columns. Property type mapping: String → OutlinedTextField, Int/Long/Double/Float → numeric OutlinedTextField with KeyboardType.Number, Boolean → Checkbox.

**Rationale**: The existing `UserInterfaceStubGenerator` generates a single generic stub with placeholder content. Entity modules need three specific composable files with property-aware form fields. A dedicated generator produces immediately functional UI without manual coding.

**Alternatives considered**:
- Extending UserInterfaceStubGenerator — rejected because the existing generator's purpose is one generic stub; entity UI needs three specialized files.
- Generating a single combined UI file — rejected because the UserProfiles pattern uses separate files for list/form/row, and this is a good separation of concerns.

## R3: How to Handle ViewModel CRUD Method Generation

**Decision**: Modify `RuntimeViewModelGenerator` to detect entity-module FlowGraphs (presence of CUD + Repository + Display nodes) and add entity-specific CRUD methods (add{Entity}, update{Entity}, remove{Entity}) plus a DAO constructor parameter and repository observation in init{}.

**Rationale**: The ViewModel is the bridge between UI and FlowGraph. The existing generator produces control methods (start/stop/pause/resume) but not entity-specific CRUD methods. The UserProfilesViewModel shows the exact pattern needed.

**Alternatives considered**:
- Generating ViewModel entirely in a new generator — rejected because RuntimeViewModelGenerator already handles Module Properties section regeneration and control methods; duplicating this would create maintenance burden.
- Adding CRUD methods as user-editable code outside markers — rejected because then re-save would not update them if the entity changes.

## R4: Where to Write Persistence Files (Entity, DAO, Repository)

**Decision**: Write persistence files to the shared `persistence/` module (not inside the generated entity module), following the architecture established in feature 046. The `RepositoryCodeGenerator` already produces these files; `ModuleSaveService.generatePersistenceFiles()` needs to be updated to target `persistence/src/commonMain/kotlin/io/codenode/persistence/` instead of the entity module's own persistence subdirectory.

**Rationale**: Room KSP in KMP requires all `@Entity`, `@Dao`, and `@Database` classes to be in the same module for annotation processing. Feature 046 confirmed this constraint through extensive testing. The shared persistence module is the established location.

**Alternatives considered**:
- Generating persistence inside each entity module — rejected because Room KSP cannot resolve entity types across KMP module boundaries (confirmed in feature 046).
- Creating a separate persistence-per-entity module — rejected for the same KSP limitation.

## R5: How to Register New Entities in AppDatabase

**Decision**: When generating a new entity module, update the existing `AppDatabase.kt` in the persistence module to add the new entity to the `@Database(entities = [...])` annotation and add the corresponding DAO accessor method. Also update `DatabaseModule.kt` if needed.

**Rationale**: Room requires all entities to be listed in the `@Database` annotation. This is a one-time modification per entity type.

**Alternatives considered**:
- Regenerating AppDatabase entirely each time — feasible and simpler; track all known entities and regenerate. This is the better approach since RepositoryCodeGenerator.generateDatabase() already accepts an entities list.

**Final decision**: Use the regeneration approach — collect all entity types from the persistence module and regenerate AppDatabase.kt with the complete entity list.

## R6: How to Create All Three Node Definitions

**Decision**: Extend `CustomNodeDefinition` with two new factory methods: `createCUD(ipTypeName, sourceIPTypeId)` for the source node (0 inputs, 3 outputs: save, update, remove) and `createDisplay(ipTypeName, sourceIPTypeId)` for the sink node (2 inputs: entities, error; 0 outputs). The "Create Repository Module" button creates all three definitions at once and adds them to the CustomNodeRepository.

**Rationale**: The existing `createRepository()` factory creates one node. The same pattern extends cleanly to CUD and Display nodes.

**Alternatives considered**:
- Creating nodes dynamically without CustomNodeDefinition persistence — rejected because custom nodes must be persisted for the graph editor to reload them.

## R7: How to Generate the FlowGraph (.flow.kt)

**Decision**: Create a FlowGraph programmatically with the three entity nodes (CUD source → Repository processor → Display sink) and all connections, then pass it through the existing `FlowKtGenerator.generateFlowKt()` to produce the .flow.kt file.

**Rationale**: Reuses the existing, well-tested FlowKtGenerator rather than duplicating its DSL output logic.

**Alternatives considered**:
- String-template the .flow.kt directly — rejected because FlowKtGenerator handles escaping, variable naming, port typing, and connection serialization correctly.

## R8: Koin DI Wiring for Generated Modules

**Decision**: Create `EntityPersistenceGenerator` that generates a `{Entity}sPersistence.kt` file following the `UserProfilesPersistence.kt` pattern — a Koin module providing the Repository, and a KoinComponent object exposing the DAO. App entry points (graphEditor Main.kt, KMPMobileApp MainActivity.kt) need manual wiring of the new DAO singleton.

**Rationale**: Feature 046 established this pattern. Each entity module needs its own Koin module for DI wiring.

**Alternatives considered**:
- Auto-modifying app entry points — rejected because it's fragile and surprising; a generated comment/README noting the required wiring is safer.
