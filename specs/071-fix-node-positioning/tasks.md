# Tasks: Fix Node and Graph Positioning Errors

**Input**: Design documents from `/specs/071-fix-node-positioning/`
**Prerequisites**: plan.md (required), spec.md (required), research.md

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2)
- Include exact file paths in descriptions

---

## Phase 1: Setup

**Purpose**: Branch verification and existing test baseline

- [ ] T001 Verify on branch `071-fix-node-positioning` and run existing tests with `./gradlew :fbpDsl:jvmTest :graphEditor:jvmTest` to establish green baseline

---

## Phase 2: User Story 1 - Allow Negative Node Positions (Priority: P1) 🎯 MVP

**Goal**: Remove the non-negative constraint from `Node.Position` so the infinite canvas coordinate space supports signed values. Eliminates the crash on negative coordinates.

**Independent Test**: Drag any node past the invisible origin — app remains stable, position reflects actual negative value.

### TDD Tests for User Story 1

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T002 [US1] Write test confirming `Node.Position(-10.0, -20.0)` can be constructed without error in `fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/model/NodePositionTest.kt`
- [ ] T003 [P] [US1] Write test confirming `Node.Position(-0.5, 0.0)` and `Node.Position(0.0, -0.5)` are valid (single-axis negative) in `fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/model/NodePositionTest.kt`
- [ ] T004 [P] [US1] Write test confirming `Node.Position(100.0, 200.0)` still works (positive values remain valid) in `fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/model/NodePositionTest.kt`

### Implementation for User Story 1

- [ ] T005 [US1] Remove `require(x >= 0.0)` and `require(y >= 0.0)` from `Node.Position` init block in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/Node.kt` (line ~69-70)
- [ ] T006 [US1] Run `./gradlew :fbpDsl:jvmTest` to verify all Node.Position tests pass (new and existing)

### Fix GraphNode Undo Position (US1 edge case)

- [ ] T007 [US1] Fix old position lookup in `graphEditor/src/jvmMain/kotlin/Main.kt` (~line 966-970): change the `when` block to use `node.position` for all Node types (CodeNode and GraphNode) instead of `Offset.Zero` for non-CodeNode

**Checkpoint**: Node.Position accepts any signed coordinates. Dragging nodes anywhere works without crash. GraphNode undo restores correct position.

---

## Phase 3: User Story 2 - Preserve Graph View State Across Hierarchy Navigation (Priority: P2)

**Goal**: Save and restore pan offset + zoom when navigating in/out of graphNode hierarchy levels, eliminating position drift.

**Independent Test**: Record node screen positions, navigate into graphNode and back, compare — positions match within 1 pixel.

### TDD Tests for User Story 2

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T008 [US2] Write test: `NavigationContext.pushInto` stores a `ViewState(panOffset, scale)` alongside the graphNodeId in `graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/state/NavigationContextTest.kt`
- [ ] T009 [P] [US2] Write test: `NavigationContext.popOut` returns the stored `ViewState` for the popped level in `graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/state/NavigationContextTest.kt`
- [ ] T010 [P] [US2] Write test: `NavigationContext.reset` clears all stored view states in `graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/state/NavigationContextTest.kt`
- [ ] T011 [US2] Write test: after `navigateIntoGraphNode` + `navigateOut`, `GraphState.panOffset` and scale are restored to pre-navigation values in `graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/state/NavigationContextTest.kt`

### Implementation for User Story 2

- [ ] T012 [US2] Add `ViewState` data class (holding `panOffset: Offset` and `scale: Float`) and update `NavigationContext` to store `viewStates: List<ViewState>` alongside `path` entries, updating `pushInto`/`popOut`/`reset` in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/state/NavigationContext.kt`
- [ ] T013 [US2] Update `GraphState.navigateIntoGraphNode()` to save current `(panOffset, scale)` via the updated `NavigationContext.pushInto` before recalculating view in `graphEditor/src/jvmMain/kotlin/state/GraphState.kt` (~line 1557)
- [ ] T014 [US2] Update `GraphState.navigateOut()` to restore saved `(panOffset, scale)` from `NavigationContext.popOut` return value in `graphEditor/src/jvmMain/kotlin/state/GraphState.kt` (~line 1601)
- [ ] T015 [US2] Update `GraphState.navigateToRoot()` to restore root-level view state in `graphEditor/src/jvmMain/kotlin/state/GraphState.kt` (~line 1613)
- [ ] T016 [US2] Update `GraphState.navigateToDepth()` to restore target-depth view state in `graphEditor/src/jvmMain/kotlin/state/GraphState.kt` (~line 1647)
- [ ] T017 [US2] Run `./gradlew :graphEditor:jvmTest` to verify all navigation tests pass (new and existing)

**Checkpoint**: Pan and zoom are preserved across hierarchy navigation. Zero drift after multiple navigate-in/out cycles.

---

## Phase 4: Polish & Verification

**Purpose**: Full test suite and manual verification

- [ ] T018 Run full test suite `./gradlew :fbpDsl:jvmTest :graphEditor:jvmTest` to verify no regressions
- [ ] T019 Run quickstart.md verification scenarios manually via Runtime Preview

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — establishes baseline
- **US1 (Phase 2)**: Depends on Setup — can start immediately after
- **US2 (Phase 3)**: Depends on Setup — independent of US1, can run in parallel
- **Polish (Phase 4)**: Depends on US1 + US2 completion

### User Story Dependencies

- **User Story 1 (P1)**: Independent — modifies `Node.kt` (fbpDsl) and `Main.kt` (graphEditor)
- **User Story 2 (P2)**: Independent — modifies `NavigationContext.kt` and `GraphState.kt` (graphEditor)
- US1 and US2 touch different files and can be implemented in parallel

### Within Each User Story

- Tests MUST be written and FAIL before implementation
- Implementation tasks are sequential within each story
- Run verification after each story completes

### Parallel Opportunities

- T003, T004 can run in parallel (different test cases, same file but independent)
- T009, T010 can run in parallel (different test aspects)
- US1 and US2 phases can run in parallel (different files)

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (baseline)
2. Complete Phase 2: User Story 1 (remove constraint + fix undo)
3. **STOP and VALIDATE**: Drag nodes freely, verify no crash
4. Proceed to Phase 3 for drift fix

### Incremental Delivery

1. Setup → Baseline green
2. US1 → No more crashes on any position → Validate
3. US2 → No more drift on navigation → Validate
4. Polish → Full verification

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- The `require` removal (T005) is a 2-line deletion — the smallest possible fix for the crash
- NavigationContext changes (T012) are the most complex task — extends an immutable data class
- Commit after each phase completion
