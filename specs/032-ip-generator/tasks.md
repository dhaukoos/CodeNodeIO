# Tasks: IP Generator Interface

**Input**: Design documents from `/specs/032-ip-generator/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, quickstart.md

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup

**Purpose**: No project initialization needed — existing graphEditor module with kotlinx-serialization already configured.

_(No setup tasks required — all dependencies and build configuration are in place.)_

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Data model classes, serialization DTOs, repository, and IPTypeRegistry modifications that ALL user stories depend on.

**CRITICAL**: No user story work can begin until this phase is complete.

- [X] T001 [P] Create IPProperty data class (name, typeId, isRequired) and CustomIPTypeDefinition data class (id, typeName, properties, color) with auto-color palette constant in `graphEditor/src/jvmMain/kotlin/model/IPProperty.kt`
- [X] T002 [P] Create @Serializable SerializableIPType DTO (id, typeName, payloadTypeName, color, description, properties) and @Serializable SerializableIPProperty DTO (name, typeId, isRequired) with toDefinition/fromDefinition conversion methods in `graphEditor/src/jvmMain/kotlin/model/SerializableIPType.kt`
- [X] T003 Create FileIPTypeRepository with load() and save() methods following FileCustomNodeRepository pattern, persisting to ~/.codenode/custom-ip-types.json using Json { prettyPrint = true; ignoreUnknownKeys = true } in `graphEditor/src/jvmMain/kotlin/repository/FileIPTypeRepository.kt`
- [X] T004 Add custom type property storage (Map<String, List<IPProperty>>) to IPTypeRegistry, add registerCustomType() method that stores both the InformationPacketType and its properties, and add getCustomTypeProperties(id) accessor in `graphEditor/src/jvmMain/kotlin/state/IPTypeRegistry.kt`
- [X] T005 Compile foundational classes: `./gradlew :graphEditor:compileKotlinJvm`

**Checkpoint**: All model, serialization, repository, and registry infrastructure ready for UI development.

---

## Phase 3: User Story 1 - Create a Custom IP Type (Priority: P1) MVP

**Goal**: A user can open the IP Generator panel, enter a type name, and click Create to register a new IP type that appears in the IP Types palette and is persisted to disk.

**Independent Test**: Open IP Generator, enter a name, click Create, verify the new type appears in the IP Types palette with an auto-assigned color. Close and reopen the editor to verify persistence.

### Implementation for User Story 1

- [X] T006 [US1] Create IPGeneratorViewModel with IPGeneratorPanelState data class (typeName, isExpanded, basic isValid = typeName.isNotBlank()), methods: setTypeName(), toggleExpanded(), createType() that registers in IPTypeRegistry + saves via FileIPTypeRepository + resets form, and reset() that clears form preserving isExpanded, in `graphEditor/src/jvmMain/kotlin/viewmodel/IPGeneratorViewModel.kt`
- [X] T007 [US1] Create IPGeneratorPanel composable (stateful wrapper + stateless IPGeneratorPanelContent) with collapsible header (arrow toggle), name OutlinedTextField, Cancel OutlinedButton calling reset(), and Create Button enabled by state.isValid calling createType(), styled per NodeGeneratorPanel (250.dp width, Color(0xFFF5F5F5) background, 1.dp border, 12.dp padding) in `graphEditor/src/jvmMain/kotlin/ui/IPGeneratorPanel.kt`
- [X] T008 [US1] Wire IPGeneratorPanel into Main.kt: create IPGeneratorViewModel and FileIPTypeRepository in remember blocks, call repository.load() + register loaded types in IPTypeRegistry on startup, wrap existing IPPalette in a Column and add IPGeneratorPanel above it, pass onTypeCreated callback to refresh IPPalette, in `graphEditor/src/jvmMain/kotlin/Main.kt`
- [X] T009 [US1] Compile and verify US1: `./gradlew :graphEditor:compileKotlinJvm`

**Checkpoint**: Core IP type creation works — enter name, click Create, type appears in palette and persists across sessions.

---

## Phase 4: User Story 2 - Manage Properties During Creation (Priority: P2)

**Goal**: A user can add, edit, and remove typed properties (with name TextField, type dropdown, required toggle) before creating the IP type.

**Independent Test**: Add multiple property rows via "+" button, edit names/types/required toggles, remove some via "-" button, then Create. Verify the created type includes only the remaining properties with correct names, types, and required/optional flags.

**Depends on**: US1 (extends existing ViewModel and Panel)

### Implementation for User Story 2

- [ ] T010 [US2] Add IPPropertyState data class (id: UUID, name, selectedTypeId defaulting to "ip_any", isRequired defaulting to true) to IPGeneratorPanelState, add properties: List<IPPropertyState> field to state, add ViewModel methods: addProperty(), removeProperty(id), updatePropertyName(id, name), updatePropertyType(id, typeId), updatePropertyRequired(id, isRequired), update createType() to map IPPropertyState list to IPProperty list in created definition, update reset() to clear properties, in `graphEditor/src/jvmMain/kotlin/viewmodel/IPGeneratorViewModel.kt`
- [ ] T011 [US2] Add property management UI to IPGeneratorPanel: "+" IconButton below name field to call addProperty(), for each property in state.properties render a Row with name OutlinedTextField, type OutlinedButton+DropdownMenu populated from IPTypeRegistry.getAllTypes() with color swatches, required Checkbox defaulting checked, and "-" IconButton to call removeProperty(id), in `graphEditor/src/jvmMain/kotlin/ui/IPGeneratorPanel.kt`
- [ ] T012 [US2] Compile and verify US2: `./gradlew :graphEditor:compileKotlinJvm`

**Checkpoint**: Full property management works — add, edit, remove properties with type selection and required/optional toggle.

---

## Phase 5: User Story 3 - Validation Feedback (Priority: P3)

**Goal**: The system prevents invalid type creation by disabling the Create button and showing visual indicators for: empty name, duplicate type name, empty property names, and duplicate property names.

**Independent Test**: Attempt to create types with empty name (Create disabled), duplicate name e.g. "String" (Create disabled + indicator), empty property name (Create disabled), duplicate property names (Create disabled + indicator). Verify all validation clears when issues are fixed.

**Depends on**: US2 (validates property state)

### Implementation for User Story 3

- [ ] T013 [US3] Add validation computed properties to IPGeneratorPanelState: hasNameConflict (check IPTypeRegistry.getByTypeName case-insensitively, requires passing existingTypeNames: Set<String> into state or a validation lambda), hasDuplicatePropertyNames (group by name, any count > 1), hasEmptyPropertyNames (any property.name.isBlank()), update isValid to incorporate all four checks (name not blank AND no name conflict AND no empty property names AND no duplicate property names), in `graphEditor/src/jvmMain/kotlin/viewmodel/IPGeneratorViewModel.kt`
- [ ] T014 [US3] Add validation UI to IPGeneratorPanel: show "Name already exists" error text below name field when hasNameConflict is true with isError on OutlinedTextField, highlight duplicate property name rows with isError on their OutlinedTextFields, show error text below property list when hasDuplicatePropertyNames, style Create button as disabled with visual feedback when !isValid, in `graphEditor/src/jvmMain/kotlin/ui/IPGeneratorPanel.kt`
- [ ] T015 [US3] Compile and verify US3: `./gradlew :graphEditor:compileKotlinJvm`

**Checkpoint**: All validation rules enforced — invalid types cannot be created and clear visual feedback is provided.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Tests and final verification across all user stories.

- [ ] T016 [P] Create FileIPTypeRepositoryTest with tests: save and load round-trip preserves all fields, load from missing file returns empty list, load from corrupt file returns empty list gracefully, save creates parent directories, multiple types with properties serialize correctly, in `graphEditor/src/jvmTest/kotlin/repository/FileIPTypeRepositoryTest.kt`
- [ ] T017 [P] Create IPGeneratorViewModelTest with tests: initial state has empty name and no properties, setTypeName updates state, createType registers type in registry and saves to repository and resets form, createType with properties maps IPPropertyState to IPProperty correctly, reset clears form but preserves isExpanded, addProperty adds row with defaults, removeProperty removes correct row, validation: isValid false when name blank, isValid false when name conflicts, isValid false when property name empty, isValid false when duplicate property names, isValid true when all valid, in `graphEditor/src/jvmTest/kotlin/viewmodel/IPGeneratorViewModelTest.kt`
- [ ] T018 Run full test suite: `./gradlew :graphEditor:jvmTest`
- [ ] T019 Run full compilation across all modules: `./gradlew compileKotlinJvm`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Skipped — existing project
- **Foundational (Phase 2)**: No dependencies — can start immediately
- **US1 (Phase 3)**: Depends on Foundational (Phase 2) completion
- **US2 (Phase 4)**: Depends on US1 (Phase 3) — extends ViewModel and Panel
- **US3 (Phase 5)**: Depends on US2 (Phase 4) — validates property state
- **Polish (Phase 6)**: Depends on US3 (Phase 5) — tests cover all features

### User Story Dependencies

- **User Story 1 (P1)**: Depends on Phase 2 only — creates the core panel, ViewModel, and Main.kt wiring
- **User Story 2 (P2)**: Depends on US1 — extends IPGeneratorViewModel with property management and IPGeneratorPanel with property row UI
- **User Story 3 (P3)**: Depends on US2 — adds validation computed properties and UI indicators that reference property state

### Within Each User Story

- ViewModel before UI (Panel composable reads ViewModel state)
- UI before Main.kt wiring (Main.kt references the Panel composable)
- Compile verification after each story

### Parallel Opportunities

- **Phase 2**: T001 and T002 can run in parallel (independent files)
- **Phase 6**: T016 and T017 can run in parallel (independent test files)

---

## Parallel Example: Foundational Phase

```bash
# Launch model files in parallel:
Task: "Create IPProperty + CustomIPTypeDefinition in model/IPProperty.kt"
Task: "Create SerializableIPType + SerializableIPProperty in model/SerializableIPType.kt"

# Then sequentially:
Task: "Create FileIPTypeRepository (depends on SerializableIPType)"
Task: "Modify IPTypeRegistry (depends on IPProperty + FileIPTypeRepository)"
```

## Parallel Example: Polish Phase

```bash
# Launch test files in parallel:
Task: "Create FileIPTypeRepositoryTest in repository/FileIPTypeRepositoryTest.kt"
Task: "Create IPGeneratorViewModelTest in viewmodel/IPGeneratorViewModelTest.kt"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 2: Foundational (model, serialization, repository, registry)
2. Complete Phase 3: User Story 1 (ViewModel, Panel, Main.kt wiring)
3. **STOP and VALIDATE**: Create a type with just a name, verify it appears in palette and persists
4. Demo if ready — basic IP type creation works

### Incremental Delivery

1. Phase 2 → Foundation ready
2. Add US1 → Test: create marker type with name only → MVP!
3. Add US2 → Test: create type with properties, edit/remove properties
4. Add US3 → Test: all validation rules prevent invalid creation
5. Add Polish → Tests pass, full verification
6. Each story adds value without breaking previous stories

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- US1 is independently deliverable as MVP (create types with name only)
- US2 and US3 build incrementally on US1
- All compile verification tasks use `./gradlew :graphEditor:compileKotlinJvm`
- FileIPTypeRepository follows FileCustomNodeRepository pattern exactly
- SerializableIPType DTO avoids KClass<*> serialization issue by using payloadTypeName: String
