# Tasks: Collapsible Panels

**Input**: Design documents from `/specs/042-collapsible-panels/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md

**Tests**: Not requested — UI-only feature with manual visual verification.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create the reusable CollapsiblePanel composable extracted from RuntimePreviewPanel's pattern

- [x] T001 Create `PanelSide` enum and `CollapsiblePanel` composable in `graphEditor/src/jvmMain/kotlin/ui/CollapsiblePanel.kt` — Extract the toggle strip pattern (Divider + 20dp clickable Box with chevron + conditional content) from RuntimePreviewPanel. PanelSide.LEFT places toggle on right edge (ChevronLeft expanded / ChevronRight collapsed), PanelSide.RIGHT places toggle on left edge (ChevronRight expanded / ChevronLeft collapsed). Parameters: `isExpanded: Boolean`, `onToggle: () -> Unit`, `side: PanelSide`, `modifier: Modifier`, `content: @Composable () -> Unit`.

**Checkpoint**: `./gradlew :graphEditor:compileKotlinJvm` compiles successfully

---

## Phase 2: User Story 1 - Collapse Properties Panel (Priority: P1) 🎯 MVP

**Goal**: Properties Panel on the right side can be collapsed/expanded via a chevron toggle on its left edge

**Independent Test**: Click the chevron on the Properties Panel's left edge — panel content hides, only toggle strip remains. Click again — panel content restores. Select a node while collapsed — panel stays collapsed.

### Implementation for User Story 1

- [x] T002 [US1] Add `isPropertiesPanelExpanded` state variable (default `true`) in `graphEditor/src/jvmMain/kotlin/Main.kt` alongside the existing `isRuntimePanelExpanded`
- [x] T003 [US1] Wrap the `CompactPropertiesPanelWithViewModel(...)` call in Main.kt's layout Row with `CollapsiblePanel(isExpanded = isPropertiesPanelExpanded, onToggle = { isPropertiesPanelExpanded = !isPropertiesPanelExpanded }, side = PanelSide.RIGHT)` in `graphEditor/src/jvmMain/kotlin/Main.kt`

**Checkpoint**: `./gradlew :graphEditor:compileKotlinJvm` compiles. Run app — Properties Panel has a collapsible toggle on its left edge. Clicking toggles content visibility. Canvas resizes. Node selection does not change collapsed state.

---

## Phase 3: User Story 2 - Collapse IP Generator / IP Types Column (Priority: P2)

**Goal**: IP Generator and IP Palette column (second from left) can be collapsed/expanded via a chevron toggle on its right edge

**Independent Test**: Click the chevron on the IP column's right edge — column content hides. Click again — column content restores. Canvas resizes accordingly.

### Implementation for User Story 2

- [x] T004 [US2] Add `isIPPanelExpanded` state variable (default `true`) in `graphEditor/src/jvmMain/kotlin/Main.kt`
- [x] T005 [US2] Wrap the IP Generator + IP Palette `Column { IPGeneratorPanel(...) IPPalette(...) }` in Main.kt's layout Row with `CollapsiblePanel(isExpanded = isIPPanelExpanded, onToggle = { isIPPanelExpanded = !isIPPanelExpanded }, side = PanelSide.LEFT)` in `graphEditor/src/jvmMain/kotlin/Main.kt`

**Checkpoint**: `./gradlew :graphEditor:compileKotlinJvm` compiles. Run app — IP column has a collapsible toggle on its right edge. Canvas resizes when toggled.

---

## Phase 4: User Story 3 - Collapse Node Generator / Node Palette Column (Priority: P3)

**Goal**: Node Generator and Node Palette column (far left) can be collapsed/expanded via a chevron toggle on its right edge

**Independent Test**: Click the chevron on the Node column's right edge — column content hides. Click again — column content restores. Canvas resizes accordingly.

### Implementation for User Story 3

- [x] T006 [US3] Add `isNodePanelExpanded` state variable (default `true`) in `graphEditor/src/jvmMain/kotlin/Main.kt`
- [x] T007 [US3] Wrap the Node Generator + Node Palette `Column { NodeGeneratorPanel(...) NodePalette(...) }` in Main.kt's layout Row with `CollapsiblePanel(isExpanded = isNodePanelExpanded, onToggle = { isNodePanelExpanded = !isNodePanelExpanded }, side = PanelSide.LEFT)` in `graphEditor/src/jvmMain/kotlin/Main.kt`

**Checkpoint**: `./gradlew :graphEditor:compileKotlinJvm` compiles. Run app — Node column has a collapsible toggle on its right edge. Canvas resizes when toggled.

---

## Phase 5: User Story 4 - Consistent Collapse Behavior (Priority: P4)

**Goal**: All four collapsible panels use the same visual pattern and behave independently

**Independent Test**: Visually compare all four toggle strips — same width, same icon style, same click behavior. Collapse/expand panels in various combinations — each is independent.

### Implementation for User Story 4

- [x] T008 [US4] Refactor `RuntimePreviewPanel` in `graphEditor/src/jvmMain/kotlin/ui/RuntimePreviewPanel.kt` to use `CollapsiblePanel` internally, replacing its inline toggle strip implementation. The RuntimePreviewPanel's `isExpanded`/`onToggle` params are passed through to CollapsiblePanel with `side = PanelSide.RIGHT`. Preserve all existing RuntimePreviewPanel content and behavior.
- [x] T009 [US4] Verify all four panels have identical toggle strip appearance and independent state by running the app and testing: collapse all panels (canvas fills full width), expand one at a time (others stay collapsed), window resize preserves state, module switch preserves state.

**Checkpoint**: All four collapsible panels visually match. States are independent. Canvas resizes correctly in all combinations.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **User Stories (Phases 2-4)**: All depend on Phase 1 (CollapsiblePanel composable)
  - US1, US2, US3 can proceed in parallel after Phase 1
  - US4 depends on US1, US2, US3 completion (refactors RuntimePreviewPanel to use shared composable)
- **No Foundational phase needed** — no database, auth, or shared infrastructure beyond CollapsiblePanel

### User Story Dependencies

- **User Story 1 (P1)**: Depends on Phase 1 only — no cross-story dependencies
- **User Story 2 (P2)**: Depends on Phase 1 only — no cross-story dependencies
- **User Story 3 (P3)**: Depends on Phase 1 only — no cross-story dependencies
- **User Story 4 (P4)**: Depends on US1 + US2 + US3 (refactors RuntimePreviewPanel for consistency)

### Parallel Opportunities

- T002 and T004 and T006 can run in parallel (different state variables, same file but independent additions)
- T003, T005, T007 modify the same layout Row in Main.kt, so should be done sequentially
- T008 modifies a different file (RuntimePreviewPanel.kt) and can run in parallel with any Main.kt task

---

## Parallel Example: User Stories 1-3

```bash
# After Phase 1 (CollapsiblePanel created), these state additions can run in parallel:
Task: "T002 — Add isPropertiesPanelExpanded state in Main.kt"
Task: "T004 — Add isIPPanelExpanded state in Main.kt"
Task: "T006 — Add isNodePanelExpanded state in Main.kt"

# Then wrap tasks sequentially (same layout Row):
Task: "T003 — Wrap Properties Panel with CollapsiblePanel"
Task: "T005 — Wrap IP column with CollapsiblePanel"
Task: "T007 — Wrap Node column with CollapsiblePanel"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Create CollapsiblePanel composable
2. Complete Phase 2: Wrap Properties Panel
3. **STOP and VALIDATE**: Properties Panel collapses/expands correctly
4. Continue with US2, US3, US4

### Incremental Delivery

1. Phase 1 → CollapsiblePanel ready
2. Add US1 (Properties Panel) → Test → Functional MVP
3. Add US2 (IP column) → Test → Two collapsible panels
4. Add US3 (Node column) → Test → Three collapsible panels
5. Add US4 (Refactor RuntimePreviewPanel) → Test → All four panels consistent

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story is independently completable and testable
- Commit after each phase checkpoint
- All changes scoped to `graphEditor` module only
