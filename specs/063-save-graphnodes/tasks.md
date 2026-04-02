# Tasks: Save GraphNodes

**Input**: Design documents from `/specs/063-save-graphnodes/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, quickstart.md

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2)
- Include exact file paths in descriptions

---

## Phase 1: Setup

**Purpose**: No external setup needed — this feature modifies the existing graphEditor module only.

*(No tasks — existing project infrastructure is already in place.)*

---

## Phase 2: Foundational (Model, Serialization, Registry)

**Purpose**: Create the core data model, serialization layer, and discovery registry that ALL user stories depend on. MUST be complete before user story work begins.

- [X] T001 Create `GraphNodeTemplateMeta` data class with fields: name, description, inputPortCount, outputPortCount, childNodeCount, filePath, tier (PlacementLevel) in `graphEditor/src/jvmMain/kotlin/model/GraphNodeTemplateMeta.kt`
- [X] T002 Implement `GraphNodeTemplateSerializer` with `saveTemplate(graphNode: GraphNode, filePath: File)` that generates a `.flow.kts` file with metadata comment header (`@GraphNodeTemplate`, `@TemplateName`, `@Description`, `@InputPorts`, `@OutputPorts`, `@ChildNodes`) followed by the GraphNode DSL content via FlowGraphSerializer in `graphEditor/src/jvmMain/kotlin/serialization/GraphNodeTemplateSerializer.kt`
- [X] T003 Implement `GraphNodeTemplateSerializer.parseMetadata(file: File): GraphNodeTemplateMeta?` that reads only the first ~15 lines of a `.flow.kts` file to extract metadata markers via regex, returning null if `@GraphNodeTemplate` marker is absent in `graphEditor/src/jvmMain/kotlin/serialization/GraphNodeTemplateSerializer.kt`
- [X] T004 Implement `GraphNodeTemplateSerializer.loadTemplate(file: File): GraphNode?` that fully deserializes a `.flow.kts` template file via FlowKtParser, returning the reconstructed GraphNode with child nodes, internal connections, and port mappings in `graphEditor/src/jvmMain/kotlin/serialization/GraphNodeTemplateSerializer.kt`
- [X] T005 Implement `GraphNodeTemplateRegistry` with three-tier filesystem scanning: `discoverAll(projectRoot: File?, activeModulePaths: List<File>)` scans Module (`{module}/.../graphnodes/`), Project (`{projectRoot}/graphnodes/`), and Universal (`~/.codenode/graphnodes/`) directories, parsing metadata from each `.flow.kts` file, deduplicating by name with Module > Project > Universal precedence in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/state/GraphNodeTemplateRegistry.kt`
- [X] T006 Add tier path resolution methods to `GraphNodeTemplateRegistry`: `resolveOutputPath(name: String, level: PlacementLevel, activeModulePath: String?): File` returning the correct filesystem path for each tier, and `resolveOutputDir(level: PlacementLevel, activeModulePath: String?): File` that creates the directory if needed in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/state/GraphNodeTemplateRegistry.kt`
- [X] T007 Add `saveGraphNode(graphNode: GraphNode, level: PlacementLevel, activeModulePath: String?): GraphNodeTemplateMeta` to `GraphNodeTemplateRegistry` that serializes via `GraphNodeTemplateSerializer`, writes to the resolved path, and immediately registers the new `GraphNodeTemplateMeta` in the in-memory cache in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/state/GraphNodeTemplateRegistry.kt`
- [X] T008 Add `removeTemplate(name: String, level: PlacementLevel): Boolean` to `GraphNodeTemplateRegistry` that deletes the `.flow.kts` file from disk and removes the entry from the in-memory cache in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/state/GraphNodeTemplateRegistry.kt`
- [X] T009 Add `getAll(): List<GraphNodeTemplateMeta>`, `getByName(name: String): GraphNodeTemplateMeta?`, and `nameExists(name: String): Boolean` query methods to `GraphNodeTemplateRegistry` in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/state/GraphNodeTemplateRegistry.kt`
- [X] T010 Wire `GraphNodeTemplateRegistry` into app initialization in `Main.kt`: instantiate at startup, call `discoverAll()` with project root and active module paths, and pass the registry to palette and properties panel components in `graphEditor/src/jvmMain/kotlin/Main.kt`
- [X] T011 Run `./gradlew :graphEditor:jvmTest` to verify all existing tests pass with the new classes added

**Checkpoint**: Core model, serialization, and registry infrastructure complete. All existing tests pass. User story implementation can begin.

---

## Phase 3: User Story 1 — Save and Reuse a GraphNode (Priority: P1) MVP

**Goal**: Users can save a GraphNode to the palette with level selection, see it in a "GraphNodes" palette section, drag it onto the canvas to create an independent copy, and have it persist across restarts.

**Independent Test**: Select a GraphNode on canvas → click "Add to Palette" with a level → verify it appears in palette → drag to canvas → verify independent copy created → restart graphEditor → verify palette entry persists.

### Implementation for User Story 1

- [X] T012 [US1] Add "Palette" section to the GraphNode properties view in `PropertiesPanel.kt`: when a GraphNode is selected, render a level selector dropdown (`PlacementLevel.availableLevels(moduleLoaded)`) and an "Add to Palette" button below the existing child nodes section in `graphEditor/src/jvmMain/kotlin/ui/PropertiesPanel.kt`
- [X] T013 [US1] Add save action wired through `CompactPropertiesPanelWithViewModel` callbacks: `onSaveGraphNodeToPalette` calls `GraphNodeTemplateRegistry.saveGraphNode()` and `onRemoveGraphNodeFromPalette` calls `removeTemplate()`, both wired in `Main.kt`
- [X] T014 [US1] Create `GraphNodePaletteSection` composable that renders a collapsible "GraphNodes" dropdown in the Node Palette, listing all `GraphNodeTemplateMeta` entries from the registry as cards with name and port count badges (green "N in", blue "M out") in `graphEditor/src/jvmMain/kotlin/ui/GraphNodePaletteSection.kt`
- [X] T015 [US1] Integrate `GraphNodePaletteSection` into `NodePalette.kt`: add the "GraphNodes" section above the existing CodeNodeType category sections, passing the registry's template list and search filtering in `graphEditor/src/jvmMain/kotlin/ui/NodePalette.kt`
- [X] T016 [US1] Implement instantiation logic: create `GraphNodeTemplateInstantiator` with `instantiate(meta, registry): GraphNode` that loads the full template via `GraphNodeTemplateSerializer.loadTemplate()`, generates fresh IDs for the GraphNode and all child nodes, remaps all internal connection source/target IDs, remaps all port `owningNodeId` references, remaps all `portMapping.childNodeId` references in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/state/GraphNodeTemplateInstantiator.kt`
- [X] T017 [US1] Add click-to-add support for GraphNode palette cards (consistent with existing CodeNode palette pattern): `onGraphNodeTemplateSelected` callback in `NodePalette.kt` wired in `Main.kt` to call `GraphNodeTemplateInstantiator.instantiate()` then `graphState.addNode()`
- [X] T018 [US1] **MANUAL** Verify quickstart Scenario 1: save a GraphNode to the palette at Project level, verify `.flow.kts` file created at `{projectRoot}/graphnodes/`, verify card appears in palette
- [X] T019 [US1] **MANUAL** Verify quickstart Scenario 2: drag saved GraphNode from palette to canvas, verify independent copy with unique IDs, verify child nodes and connections intact
- [X] T020 [US1] **MANUAL** Verify quickstart Scenario 3: restart graphEditor, verify saved GraphNode persists in palette, drag to canvas and verify functional

**Checkpoint**: Users can save GraphNodes to the palette, see them listed, drag them to create copies, and they persist across restarts. MVP complete.

---

## Phase 4: User Story 2 — Remove a Saved GraphNode (Priority: P2)

**Goal**: Users can remove a previously saved GraphNode from the palette via the Properties panel. Existing canvas instances are unaffected.

**Independent Test**: Save a GraphNode → remove it via Properties panel → verify palette entry and file deleted → verify canvas instances still work.

### Implementation for User Story 2

- [X] T021 [US2] Update the "Palette" section in `PropertiesPanel.kt` to detect when the selected GraphNode matches an existing palette entry (by name via `GraphNodeTemplateRegistry.getByName()`), and toggle between "Add to Palette" and "Remove from Palette" buttons accordingly in `graphEditor/src/jvmMain/kotlin/ui/PropertiesPanel.kt`
- [X] T022 [US2] Add removal confirmation dialog: when "Remove from Palette" is clicked, show a dialog with the template name and level, with "Remove" and "Cancel" options in `graphEditor/src/jvmMain/kotlin/ui/PropertiesPanel.kt`
- [X] T023 [US2] Add remove action to `PropertiesPanelViewModel`: implement `removeGraphNodeFromPalette(name: String, level: PlacementLevel)` that calls `GraphNodeTemplateRegistry.removeTemplate()` and updates palette state in `graphEditor/src/jvmMain/kotlin/viewmodel/PropertiesPanelViewModel.kt`
- [X] T024 [US2] **MANUAL** Verify quickstart Scenario 4: save a GraphNode, then remove it, verify palette entry gone and `.flow.kts` file deleted, verify existing canvas instances remain functional

**Checkpoint**: Users can remove saved GraphNodes. Add and remove workflow is complete.

---

## Phase 5: User Story 4 — Heterogeneous Child Node Level Promotion (Priority: P2)

**Goal**: When saving a GraphNode at a more general level than its child nodes, the system checks import compatibility. Nodes with unresolvable dependencies (module-specific types, third-party libraries) block the save with a recommendation. Nodes with only standard dependencies can be promoted (copied with updated packages).

**Independent Test**: Create a GraphNode with mixed-level child nodes → save at Universal → verify blocking dialog for nodes with module-specific deps, or promotion dialog for simple nodes → verify appropriate behavior for each case.

### Implementation for User Story 4

- [X] T025 [US4] Implement `LevelCompatibilityChecker` with `checkCompatibility(graphNode: GraphNode, targetLevel: PlacementLevel, nodeRegistry: NodeDefinitionRegistry): List<PromotionCandidate>` that recursively walks all child nodes (including nested GraphNodes), determines each child's current level via source file path lookup in the registry, analyzes each node's imports for promotability, and returns a list of candidates with `promotable` flag indicating whether their dependencies can be resolved at the target level in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/state/LevelCompatibilityChecker.kt`
- [X] T026 [P] [US4] Implement `NodePromoter` with `promoteNodes(...)` that copies promotable candidate CodeNode `.kt` files to the target level's `nodes/` directory (skipping nodes with unresolvable imports), updates `package` declarations, and handles transitive IP type promotion. Includes `hasUnresolvableImports()` that checks for module-specific or third-party imports not available at the target level in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/state/NodePromoter.kt`
- [X] T027 [US4] Create level compatibility dialog composable: when all candidates are promotable, show promotion dialog with "Continue"/"Cancel"; when any candidates have unresolvable dependencies, show blocking dialog titled "Cannot Save at {Level}" listing incompatible nodes and recommending the appropriate level, with only an "OK" dismiss button in `graphEditor/src/jvmMain/kotlin/ui/PropertiesPanel.kt`
- [X] T028 [US4] Wire `LevelCompatibilityChecker` into the save flow in `Main.kt` and `PropertiesPanelViewModel`: call checker before saving, surface candidates to the UI, block save when unpromotable nodes exist, and on confirmation of promotable-only candidates call `NodePromoter.promoteNodes()` before `GraphNodeTemplateRegistry.saveGraphNode()`
- [X] T029 [US4] **MANUAL** Verify quickstart Scenario 5a (blocked save with module-specific deps) and 5b (promotable simple nodes): verify blocking dialog with level recommendation, verify promotion of simple nodes, verify cancel makes no changes

**Checkpoint**: Level compatibility checking blocks saves with unresolvable dependencies and promotes simple nodes. Users cannot create broken palette entries.

---

## Phase 6: User Story 3 — Browse and Organize GraphNodes in Palette (Priority: P3)

**Goal**: GraphNode cards in the palette have a visually-distinct design, show level indicators, and are included in search results.

**Independent Test**: Save multiple GraphNodes at different levels → verify distinct card design → verify level indicators → search by name and confirm results include GraphNodes.

### Implementation for User Story 3

- [X] T030 [US3] Create `GraphNodePaletteViewModel` with state for expanded/collapsed "GraphNodes" section and search integration: expose `filteredTemplates(query: String): List<GraphNodeTemplateMeta>` that filters by name and description matching the palette search query in `graphEditor/src/jvmMain/kotlin/viewmodel/GraphNodePaletteViewModel.kt`
- [X] T031 [US3] Update `GraphNodePaletteSection` card design to be visually distinct: use blue-tinted background (#E3F2FD), blue border (#1565C0) matching GraphNodeRenderer, add a composition icon (nested squares), add child node count badge, and add a level indicator pill (Module/Project/Universal text with tier color) in `graphEditor/src/jvmMain/kotlin/ui/GraphNodePaletteSection.kt`
- [X] T032 [US3] Integrate GraphNode search into `NodePaletteViewModel`: update `setSearchQuery()` to also filter GraphNode templates via `GraphNodePaletteViewModel.filteredTemplates()`, and pass filtered results to `GraphNodePaletteSection` in `graphEditor/src/jvmMain/kotlin/viewmodel/NodePaletteViewModel.kt` and `graphEditor/src/jvmMain/kotlin/ui/NodePalette.kt`
- [X] T033 [US3] **MANUAL** Verify quickstart Scenario 6: save multiple GraphNodes at different levels, verify distinct card design with blue tint, verify level indicators, verify search filters both CodeNodes and GraphNodes

**Checkpoint**: GraphNodes are visually distinct in the palette, show tier indicators, and are searchable.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Edge cases, validation, and final integration testing.

- [X] T034 Implement duplicate name handling: when saving a GraphNode with a name that already exists at the same tier, show a dialog with "Overwrite," "Rename," and "Cancel" options; integrate into the save flow in `graphEditor/src/jvmMain/kotlin/ui/PropertiesPanel.kt`
- [X] T035 Handle edge case: allow saving empty GraphNodes (no child nodes) — ensure serialization and instantiation work correctly with zero children in `graphEditor/src/jvmMain/kotlin/serialization/GraphNodeTemplateSerializer.kt`
- [X] T036 Handle edge case: saving GraphNodes containing nested GraphNodes — ensure recursive serialization captures full hierarchy and instantiation remaps IDs at all nesting levels in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/state/GraphNodeTemplateInstantiator.kt`
- [X] T037 Run `./gradlew :graphEditor:jvmTest` to verify all tests pass
- [X] T038 **MANUAL** Verify quickstart Scenario 7: duplicate name handling with overwrite/rename/cancel
- [X] T039 **MANUAL** End-to-end validation: save, instantiate, restart, remove, promotion — all scenarios from quickstart.md

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 2 (Foundational)**: No dependencies — can start immediately. BLOCKS all user stories.
- **Phase 3 (US1)**: Depends on Phase 2 completion (registry, serializer, model must exist)
- **Phase 4 (US2)**: Depends on Phase 3 completion (requires save to exist before remove)
- **Phase 5 (US4)**: Depends on Phase 3 completion (requires save flow to exist for promotion integration). Can run in parallel with Phase 4.
- **Phase 6 (US3)**: Depends on Phase 3 completion (requires palette section to exist). Can run in parallel with Phases 4 and 5.
- **Phase 7 (Polish)**: Depends on Phases 3-6 completion.

### User Story Dependencies

- **User Story 1 (P1)**: Depends only on Phase 2. Core save/instantiate/persist.
- **User Story 2 (P2)**: Depends on US1 (needs save to exist before remove).
- **User Story 4 (P2)**: Depends on US1 (augments the save flow with promotion). Can run in parallel with US2.
- **User Story 3 (P3)**: Depends on US1 (needs palette section to exist). Can run in parallel with US2 and US4.

### Within Each User Story

- US1: Properties panel button → save action → palette section → instantiation → drag-and-drop → manual test
- US2: Toggle button detection → confirmation dialog → remove action → manual test
- US4: Compatibility checker → promoter → dialog → save flow integration → manual test
- US3: ViewModel → card design → search integration → manual test

### Parallel Opportunities

- T025 and T026 can run in parallel (different files: LevelCompatibilityChecker vs NodePromoter)
- US2 (Phase 4) and US4 (Phase 5) can run in parallel after US1 completes
- US3 (Phase 6) can run in parallel with US2 and US4 after US1 completes
- T034, T035, T036 can run in parallel (different edge cases, different files)

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 2: Foundational (model, serializer, registry)
2. Complete Phase 3: User Story 1 — save, palette display, instantiation, persistence
3. **STOP and VALIDATE**: Save a GraphNode, drag it to canvas, restart and verify
4. This delivers the primary value: GraphNode compositions can be saved and reused

### Incremental Delivery

1. Phase 2 → Core infrastructure ready
2. Phase 3 (US1) → Save and reuse works → **MVP ready**
3. Phase 4 (US2) → Removal works → Full add/remove lifecycle
4. Phase 5 (US4) → Promotion works → Data integrity guaranteed
5. Phase 6 (US3) → Visual polish and search → Full palette experience
6. Phase 7 → Edge cases and final validation → Feature complete

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Commit after each phase or logical group
- The `fbpDsl` module requires no changes — GraphNode model and FlowGraphSerializer already exist
- All new code is in the `graphEditor` module (JVM Desktop only)
- The drag-and-drop extension (T017) is the most architecturally complex task — it bridges the palette, registry, instantiator, and graph state
