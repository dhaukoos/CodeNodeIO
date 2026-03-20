# Tasks: Filesystem-Driven Node Palette

**Input**: Design documents from `/specs/055-filesystem-node-palette/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, quickstart.md

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup

**Purpose**: No new project structure needed — this is a refactor of existing files.

- [X] T001 Verify all existing tests pass before refactoring by running `./gradlew :fbpDsl:jvmTest :graphEditor:jvmTest`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Unify the category enum system. MUST complete before any user story work.

**⚠️ CRITICAL**: All three enum systems must be consolidated before palette/generator changes.

### CodeNodeType Enum Cleanup

- [X] T002 Remove CUSTOM and GENERIC values from `CodeNodeType` enum in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNode.kt`. Keep 9 values: SOURCE, SINK, TRANSFORMER, FILTER, SPLITTER, MERGER, VALIDATOR, API_ENDPOINT, DATABASE.

- [X] T003 Add backward-compatible deserialization for removed enum values in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/dsl/FlowGraphDsl.kt` — map "CUSTOM" and "GENERIC" strings to `CodeNodeType.TRANSFORMER` in the `valueOf` catch block.

### Remove NodeCategory Enum (4-value)

- [X] T004 Remove `NodeCategory` enum from `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/CodeNodeDefinition.kt`. Change the `category` property type from `NodeCategory` to `CodeNodeType` (import from `io.codenode.fbpdsl.model.CodeNodeType`). Update `toNodeTypeDefinition()` to pass `CodeNodeType` directly instead of mapping through `paletteCategory`.

### Remove NodeTypeDefinition.NodeCategory Enum (7-value)

- [X] T005 Remove `NodeCategory` nested enum from `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/NodeTypeDefinition.kt`. Change the `category` field type from `NodeTypeDefinition.NodeCategory` to `CodeNodeType`. Update the `validate()` method if it references NodeCategory.

### Update CodeNodeDefinition Implementations (all parallel — different files)

- [X] T006 [P] Update `StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/nodes/TimerEmitterCodeNode.kt` — replace `import NodeCategory` with `import CodeNodeType`, change `category = NodeCategory.SOURCE` to `category = CodeNodeType.SOURCE`
- [X] T007 [P] Update `StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/nodes/TimeIncrementerCodeNode.kt` — replace NodeCategory.PROCESSOR with `CodeNodeType.TRANSFORMER` (2 in, 2 out processor)
- [X] T008 [P] Update `StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/nodes/DisplayReceiverCodeNode.kt` — replace NodeCategory.SINK with `CodeNodeType.SINK`
- [X] T009 [P] Update `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/nodes/UserProfileCUDCodeNode.kt` — replace NodeCategory with CodeNodeType, map PROCESSOR to appropriate type based on ports
- [X] T010 [P] Update `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/nodes/UserProfileRepositoryCodeNode.kt` — replace NodeCategory with CodeNodeType, map SINK (2 in, 0 out)
- [X] T011 [P] Update `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/nodes/UserProfilesDisplayCodeNode.kt` — replace NodeCategory with CodeNodeType
- [X] T012 [P] Update `GeoLocations/src/commonMain/kotlin/io/codenode/geolocations/nodes/GeoLocationCUDCodeNode.kt` — replace NodeCategory with CodeNodeType
- [X] T013 [P] Update `GeoLocations/src/commonMain/kotlin/io/codenode/geolocations/nodes/GeoLocationRepositoryCodeNode.kt` — replace NodeCategory with CodeNodeType
- [X] T014 [P] Update `GeoLocations/src/commonMain/kotlin/io/codenode/geolocations/nodes/GeoLocationsDisplayCodeNode.kt` — replace NodeCategory with CodeNodeType
- [X] T015 [P] Update `Addresses/src/commonMain/kotlin/io/codenode/addresses/nodes/AddressCUDCodeNode.kt` — replace NodeCategory with CodeNodeType
- [X] T016 [P] Update `Addresses/src/commonMain/kotlin/io/codenode/addresses/nodes/AddressRepositoryCodeNode.kt` — replace NodeCategory with CodeNodeType
- [X] T017 [P] Update `Addresses/src/commonMain/kotlin/io/codenode/addresses/nodes/AddressesDisplayCodeNode.kt` — replace NodeCategory with CodeNodeType
- [X] T018 [P] Update `EdgeArtFilter/src/commonMain/kotlin/io/codenode/edgeartfilter/nodes/ImagePickerCodeNode.kt` — replace NodeCategory with CodeNodeType
- [X] T019 [P] Update `EdgeArtFilter/src/commonMain/kotlin/io/codenode/edgeartfilter/nodes/EdgeDetectorCodeNode.kt` — replace NodeCategory with CodeNodeType
- [X] T020 [P] Update `EdgeArtFilter/src/commonMain/kotlin/io/codenode/edgeartfilter/nodes/SepiaTransformerCodeNode.kt` — replace NodeCategory with CodeNodeType
- [X] T021 [P] Update `EdgeArtFilter/src/commonMain/kotlin/io/codenode/edgeartfilter/nodes/GrayscaleTransformerCodeNode.kt` — replace NodeCategory with CodeNodeType
- [X] T022 [P] Update `EdgeArtFilter/src/commonMain/kotlin/io/codenode/edgeartfilter/nodes/ColorOverlayCodeNode.kt` — replace NodeCategory with CodeNodeType
- [X] T023 [P] Update `EdgeArtFilter/src/commonMain/kotlin/io/codenode/edgeartfilter/nodes/ImageViewerCodeNode.kt` — replace NodeCategory with CodeNodeType
- [X] T024 [P] Update `nodes/src/commonMain/kotlin/io/codenode/nodes/TestNode1CodeNode.kt` — replace NodeCategory with CodeNodeType
- [X] T025 [P] Update `nodes/src/commonMain/kotlin/io/codenode/nodes/Test3TransformerCodeNode.kt` — replace NodeCategory with CodeNodeType

### Update Supporting Infrastructure

- [X] T026 Update `NodeDefinitionRegistry` in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/state/NodeDefinitionRegistry.kt` — change `NodeTemplateMeta.category` from `NodeCategory` to `CodeNodeType`, update `parseTemplateMetadata()` regex to match `CodeNodeType.XXXXX`, update `templateToNodeTypeDefinition()` to use CodeNodeType directly (remove category mapping), update all imports

- [X] T027 Update `GenericNodeTypeFactory` in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/factory/GenericNodeTypeFactory.kt` — replace `NodeTypeDefinition.NodeCategory.GENERIC` references with appropriate `CodeNodeType` value, update function signatures

### Update Test Files (all parallel — different files)

- [X] T028 [P] Update `fbpDsl/src/commonTest/kotlin/model/PropertyConfigurationTest.kt` — replace all `NodeTypeDefinition.NodeCategory.*` with `CodeNodeType.*`
- [X] T029 [P] Update `fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/factory/GenericNodeTypeFactoryTest.kt` — replace category references with CodeNodeType
- [X] T030 [P] Update `graphEditor/src/jvmTest/kotlin/ui/GenericNodePaletteTest.kt` — replace all `NodeTypeDefinition.NodeCategory.*` with `CodeNodeType.*`
- [X] T031 [P] Update `graphEditor/src/jvmTest/kotlin/ui/NodePaletteTest.kt` — replace all `NodeTypeDefinition.NodeCategory.*` with `CodeNodeType.*`
- [X] T032 [P] Update `graphEditor/src/jvmTest/kotlin/ui/PropertiesPanelTest.kt` — replace category references with CodeNodeType
- [X] T033 [P] Update `graphEditor/src/jvmTest/kotlin/viewmodel/NodePaletteViewModelTest.kt` — replace `NodeTypeDefinition.NodeCategory.*` with `CodeNodeType.*`
- [X] T034 [P] Update `graphEditor/src/jvmTest/kotlin/viewmodel/NodeGeneratorViewModelTest.kt` — replace `NodeCategory.*` with `CodeNodeType.*`

### Foundational Verification

- [X] T035 Run `./gradlew :fbpDsl:jvmTest :graphEditor:jvmTest` and verify all tests pass with the unified enum system

**Checkpoint**: All three enums consolidated into CodeNodeType (9 values). All tests pass. User story work can begin.

---

## Phase 3: User Story 1 — Browse Discovered CodeNodes by CodeNodeType (Priority: P1) 🎯 MVP

**Goal**: Palette displays only filesystem-discovered CodeNodes grouped by CodeNodeType, showing only populated categories.

**Independent Test**: Place CodeNode files in Module/Project/Universal directories, launch graphEditor, confirm palette shows exactly those nodes grouped by their CodeNodeType.

### Implementation for User Story 1

- [X] T036 [US1] Remove `createSampleNodeTypes()` function and `builtInNodeTypes` variable from `graphEditor/src/jvmMain/kotlin/Main.kt`. Update palette data source to use only `registry.getAllForPalette()` — no hardcoded nodes.

- [X] T037 [US1] Update `NodePaletteViewModel` in `graphEditor/src/jvmMain/kotlin/viewmodel/NodePaletteViewModel.kt` — change `expandedCategories: Set<NodeTypeDefinition.NodeCategory>` to `Set<CodeNodeType>`, update all method signatures (`toggleCategory`, `expandCategory`, `collapseCategory`) to use `CodeNodeType`.

- [X] T038 [US1] Update `NodePalette` composable in `graphEditor/src/jvmMain/kotlin/ui/NodePalette.kt` — group nodes by `CodeNodeType` instead of `NodeTypeDefinition.NodeCategory`. Only render category headers for categories that have at least one node (filter out empty categories). Display category names using `CodeNodeType.typeName`.

- [X] T039 [US1] Update `DragAndDropHandler` in `graphEditor/src/jvmMain/kotlin/ui/DragAndDropHandler.kt` — remove the `when (nodeType.category)` mapping from `NodeTypeDefinition.NodeCategory` to `CodeNodeType`. Use `nodeType.category` directly since it is now `CodeNodeType`.

- [X] T040 [US1] Update drag-drop node creation in `graphEditor/src/jvmMain/kotlin/Main.kt` — remove the `mappedCodeNodeType` when-expression mapping `NodeTypeDefinition.NodeCategory` to `CodeNodeType`. Use `nodeType.category` directly as `codeNodeType`.

- [X] T041 [US1] Verify palette displays correctly by running `./gradlew :graphEditor:jvmTest` and manually launching graphEditor to confirm only discovered nodes appear, grouped by CodeNodeType, with only populated category sections shown.

**Checkpoint**: Palette shows only filesystem-discovered nodes grouped by CodeNodeType. No hardcoded samples. Empty categories hidden.

---

## Phase 4: User Story 2 — Search and Filter Palette Nodes (Priority: P2)

**Goal**: Text search filters palette nodes by name/description while preserving category grouping.

**Independent Test**: Type a partial name in search box, confirm only matching nodes appear under their category headers.

### Implementation for User Story 2

- [ ] T042 [US2] Verify search filtering in `graphEditor/src/jvmMain/kotlin/ui/NodePalette.kt` still works correctly with CodeNodeType grouping — the existing search logic should work unchanged since it filters on `NodeTypeDefinition.name` and `description` before grouping. Confirm that empty categories after filtering are hidden (only show categories with matching nodes).

**Checkpoint**: Search filtering works with the new CodeNodeType-based grouping.

---

## Phase 5: User Story 5 — Node Generator Uses CodeNodeType Categories (Priority: P2)

**Goal**: Generator dropdown shows all 9 CodeNodeType values. Generated nodes carry the selected CodeNodeType.

**Independent Test**: Open Node Generator, confirm all 9 types in dropdown, generate a node with a specific type, see it in palette under correct category.

### Implementation for User Story 5

- [ ] T043 [US5] Update `NodeGeneratorViewModel` in `graphEditor/src/jvmMain/kotlin/viewmodel/NodeGeneratorViewModel.kt` — change `category` state field from `NodeCategory` to `CodeNodeType`, default to `CodeNodeType.TRANSFORMER`. Update `generateCodeNodeContent()` to emit `override val category = CodeNodeType.${category.name}` instead of `NodeCategory.${category.name}`. Update `generateRuntimeBlock()` parameter from `NodeCategory` to `CodeNodeType` and update the when-expression mapping for all 9 types.

- [ ] T044 [US5] Update `NodeGeneratorPanel` composable in `graphEditor/src/jvmMain/kotlin/ui/NodeGeneratorPanel.kt` — change category dropdown to iterate `CodeNodeType.entries` (9 values, always all shown). Update display formatting to use `CodeNodeType.typeName`. Update any `NodeCategory` references in the panel.

- [ ] T045 [US5] Verify generator creates nodes with correct CodeNodeType by manually testing: create a Filter node, confirm it appears under "Filter" category in palette.

**Checkpoint**: Generator shows all 9 CodeNodeType options. Generated nodes carry correct type and appear in palette.

---

## Phase 6: User Story 3 — Filesystem Removal Reflected in Palette (Priority: P3)

**Goal**: Deleting a CodeNode file from the filesystem removes it from the palette on next launch.

**Independent Test**: Delete a CodeNode file, relaunch graphEditor, confirm it's gone from palette.

### Implementation for User Story 3

- [ ] T046 [US3] Verify filesystem-palette sync works — the existing discovery mechanism (`registry.discoverAll()` on launch) already rescans the filesystem each time. Removing a file means it won't be discovered on next launch. Test by deleting a template file from `nodes/` directory, relaunching graphEditor, and confirming the node is absent from the palette. No code changes expected — this is a verification task.

**Checkpoint**: Filesystem removal correctly reflected in palette on relaunch.

---

## Phase 7: User Story 4 — Templates Excluded from Palette (Priority: P3)

**Goal**: No template entries or hardcoded sample nodes appear in the palette.

**Independent Test**: Launch graphEditor, confirm no sample nodes (Data Source, Transform, Filter, API Call, Database Query) appear.

### Implementation for User Story 4

- [ ] T047 [US4] Verify templates are excluded — this is satisfied by T036 (removing `createSampleNodeTypes()` and `builtInNodeTypes`). Confirm the palette shows only nodes backed by actual CodeNodeDefinition files on disk. No additional code changes expected.

**Checkpoint**: Palette is a pure reflection of filesystem CodeNodes. No templates or samples.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Final verification and cleanup across all stories.

- [ ] T048 Run full test suite: `./gradlew :fbpDsl:jvmTest :graphEditor:jvmTest` and fix any remaining failures
- [ ] T049 Search codebase for any remaining references to `NodeCategory` (excluding test comments/documentation) and remove: `grep -r "NodeCategory" --include="*.kt" fbpDsl/src/commonMain/ graphEditor/src/jvmMain/ StopWatch/ UserProfiles/ GeoLocations/ Addresses/ EdgeArtFilter/ nodes/`
- [ ] T050 Search codebase for any remaining references to `NodeTypeDefinition.NodeCategory` and remove: `grep -r "NodeTypeDefinition.NodeCategory" --include="*.kt"`
- [ ] T051 Search for references to `CodeNodeType.CUSTOM` or `CodeNodeType.GENERIC` and remove/update: `grep -r "CodeNodeType\.\(CUSTOM\|GENERIC\)" --include="*.kt"`
- [ ] T052 Run quickstart.md validation steps 1-10 to verify all acceptance criteria
- [ ] T053 Update any existing .flow.kts files that contain `nodeType = "GENERIC"` to use appropriate CodeNodeType value (check GeoLocations, Addresses, UserProfiles modules)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies — verify baseline
- **Phase 2 (Foundational)**: Depends on Phase 1 — BLOCKS all user stories
- **Phase 3 (US1)**: Depends on Phase 2 — core palette restructure
- **Phase 4 (US2)**: Depends on Phase 3 — search works on new grouping
- **Phase 5 (US5)**: Depends on Phase 2 — generator can start after foundational
- **Phase 6 (US3)**: Depends on Phase 3 — verification of discovery sync
- **Phase 7 (US4)**: Depends on Phase 3 (T036) — verification of no samples
- **Phase 8 (Polish)**: Depends on all phases complete

### User Story Dependencies

- **US1 (P1)**: Depends on Foundational (Phase 2) — no other story dependencies
- **US2 (P2)**: Depends on US1 (palette must be restructured before search is meaningful)
- **US5 (P2)**: Depends on Foundational (Phase 2) — independent of US1
- **US3 (P3)**: Depends on US1 (palette must show discovered nodes to verify removal)
- **US4 (P3)**: Depends on US1 (T036 removes samples)

### Within Foundational Phase

- T002 (CodeNodeType cleanup) → T003 (backward compat) must be sequential
- T004 (remove NodeCategory) depends on T002
- T005 (remove NodeTypeDefinition.NodeCategory) depends on T002
- T006-T025 (CodeNode implementations) depend on T004 — all parallel with each other
- T026-T027 (infrastructure) depend on T004 and T005
- T028-T034 (test files) depend on T004 and T005 — all parallel with each other
- T035 (verification) depends on all above

### Parallel Opportunities

```
# All CodeNode implementation updates can run in parallel (T006-T025):
T006, T007, T008, T009, T010, T011, T012, T013, T014, T015,
T016, T017, T018, T019, T020, T021, T022, T023, T024, T025

# All test file updates can run in parallel (T028-T034):
T028, T029, T030, T031, T032, T033, T034

# US1 and US5 can run in parallel after Phase 2:
Phase 3 (US1) || Phase 5 (US5)
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Verify baseline
2. Complete Phase 2: Consolidate enums (CRITICAL)
3. Complete Phase 3: Palette shows filesystem-discovered nodes
4. **STOP and VALIDATE**: Launch graphEditor, verify palette works
5. Demo: Palette shows only real CodeNodes, grouped by CodeNodeType

### Incremental Delivery

1. Phase 1 + Phase 2 → Unified enum system, all tests pass
2. Phase 3 (US1) → Palette restructured → Demo
3. Phase 4 (US2) → Search works with new categories → Demo
4. Phase 5 (US5) → Generator uses 9 CodeNodeTypes → Demo
5. Phase 6-7 (US3, US4) → Verification tasks → Complete
6. Phase 8 → Polish and final validation

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story
- Phase 2 is the heaviest phase (~30 tasks) but most are parallel file updates
- US3 and US4 are primarily verification — may not need code changes
- Commit after each logical group of tasks
