# Quickstart Verification: Decompose graphEditor Main.kt

**Feature**: 072-decompose-main-kt
**Date**: 2026-04-13

## Prerequisites

- Branch `072-decompose-main-kt` checked out
- Gradle builds successfully: `./gradlew :graphEditor:compileKotlinJvm`

## Verification Scenarios

### VS1: Compilation and Test Baseline

**Steps**:
1. Run `./gradlew :graphEditor:jvmTest`
2. Verify all tests pass

**Expected**: Zero test failures. Same test count as before refactoring.

### VS2: Application Launch

**Steps**:
1. Run `./gradlew :graphEditor:run`
2. Verify the graph editor window opens
3. Verify the toolbar, node palette, IP palette, properties panel, and canvas are all visible

**Expected**: Application launches identically to pre-refactoring state. All panels are present and functional.

### VS3: File Open and Save

**Steps**:
1. Click "Open" in the toolbar
2. Navigate to and open a `.flow.kt` file (e.g., architecture.flow.kt)
3. Verify the graph loads with correct node positions and connections
4. Verify connection types display correctly (not "Any")
5. Click "Save" — verify save completes without error

**Expected**: File operations work identically to before.

### VS4: Node Interaction

**Steps**:
1. Open any flow graph
2. Drag a node to a new position
3. Undo the move (Ctrl+Z)
4. Redo the move (Ctrl+Shift+Z)
5. Select multiple nodes and group them
6. Navigate into a GraphNode (double-click)
7. Navigate back out (breadcrumb or back button)

**Expected**: All interactions behave identically. Undo/redo works. Navigation preserves view state.

### VS5: Runtime Preview

**Steps**:
1. Open a module with runtime preview capability
2. Start the runtime preview
3. Verify data flow animations appear on connections
4. Stop the runtime preview

**Expected**: Runtime preview functions identically.

### VS6: File Size Verification

**Steps**:
1. Run `wc -l graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/Main.kt`
2. Run `find graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ -name "*.kt" -exec wc -l {} + | sort -rn | head -5`

**Expected**:
- Main.kt is under 50 lines
- No single file exceeds 500 lines
