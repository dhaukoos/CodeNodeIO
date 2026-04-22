# Quickstart Verification: Unified Configurable Code Generation

**Feature**: 076-codegen-decomposition
**Date**: 2026-04-21

## Prerequisites

- Branch `076-codegen-decomposition` checked out
- Gradle builds successfully: `./gradlew :graphEditor:compileKotlinJvm`
- `subscription.tier=SIM` or unset (defaults to SIM)

## Verification Scenarios

### VS1: Dependency Analysis Document

**Steps**:
1. Open `specs/076-codegen-decomposition/dependency-analysis.md`
2. Verify every generator class from flowGraph-generate is catalogued
3. Verify each entry has: inputs, outputs (files), dependencies

**Expected**: 22+ generator classes listed. Dependency edges form a directed acyclic graph. Module scaffolding identified as the root prerequisite.

### VS2: Proposed Folder Hierarchy

**Steps**:
1. In the dependency analysis, find the "Proposed Folder Hierarchy" section
2. Map every file from the Addresses module to the new structure
3. Verify no file is unaccounted for

**Expected**: Every file from Addresses maps to a logical subdirectory (flow/, controller/, viewmodel/, nodes/, userInterface/, persistence/).

### VS3: Code Generator Panel Visible on Pro Tier

**Steps**:
1. Set `subscription.tier=PRO` (or SIM) in `~/.codenode/config.properties`
2. Launch `./gradlew :graphEditor:run`
3. Look for "Code Generator" panel on the left side

**Expected**: Panel appears alongside Node Generator and IP Generator, with a collapsible toggle.

### VS4: Code Generator Panel Hidden on Free Tier

**Steps**:
1. Set `subscription.tier=FREE` in `~/.codenode/config.properties`
2. Launch the graph editor
3. Check left panel area

**Expected**: Code Generator panel is not visible. Node Generator and IP Generator remain.

### VS5: Generate Module Path — File Tree

**Steps**:
1. On Pro/Sim tier, expand the Code Generator panel
2. Select "Generate Module" from the path dropdown
3. Observe the file tree below

**Expected**: File tree shows: flow/ (flow.kt, Flow.kt), controller/ (Controller.kt, ControllerInterface.kt, ControllerAdapter.kt), viewmodel/ (ViewModel.kt), userInterface/ (stub.kt). All checkboxes default to selected.

### VS6: Repository Path — File Tree

**Steps**:
1. Select "Repository" from the path dropdown
2. Pick a custom IP Type from the secondary dropdown
3. Observe the file tree

**Expected**: File tree shows all Repository module files organized under flow/, controller/, viewmodel/, nodes/, userInterface/, persistence/. Folder and file checkboxes are interactive.

### VS7: UI-FBP Path — File Tree

**Steps**:
1. Select "UI-FBP" from the path dropdown
2. Select a Compose UI .kt file
3. Observe the file tree

**Expected**: File tree shows ViewModel, State, Source CodeNode, Sink CodeNode, and bootstrap flow.kt organized by folder.

### VS8: Folder Checkbox Toggles Children

**Steps**:
1. With a populated file tree, click a folder checkbox (e.g., "controller/")
2. Observe child file checkboxes

**Expected**: All child checkboxes toggle to match. Unchecking the folder deselects all children. Checking selects all.

### VS9: Individual File Checkbox Updates Parent

**Steps**:
1. Deselect one file under a fully-selected folder
2. Observe the folder checkbox

**Expected**: Folder checkbox shows indeterminate state (partial selection).

### VS10: Repository Buttons Relocated

**Steps**:
1. Select an IP Type in the IP Palette
2. Open the Properties Panel on the right
3. Verify "Create Repository Module" button is NOT there
4. Open the Code Generator panel, select "Repository" path, pick the same IP Type
5. Verify the repository action buttons are here

**Expected**: Buttons exclusively in Code Generator panel, removed from Properties Panel.

### VS11: Migration Plan Document

**Steps**:
1. Open `specs/076-codegen-decomposition/migration-plan.md`
2. Verify at least 3 future feature steps are described
3. Each step has scope, prerequisites, and deliverables

**Expected**: Numbered sequence of features forming a clear evolution path from monolithic to configurable generation.
