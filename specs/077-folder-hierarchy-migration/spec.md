# Feature Specification: Folder Hierarchy Migration

**Feature Branch**: `077-folder-hierarchy-migration`
**Created**: 2026-04-22
**Status**: Draft
**Input**: User description: "Folder Hierarchy Migration — move generated module files from the current flat layout into the proposed subdirectory structure (flow/, controller/, viewmodel/, persistence/)."

## Context

This is Step 1 of the Code Generation Migration Plan from feature 076. The dependency analysis (076-codegen-decomposition/dependency-analysis.md) established a proposed folder hierarchy that groups related generated files into logical subdirectories. This feature implements that migration.

**Current layout** (flat, with `generated/` subdirectory):
```
{Module}/src/commonMain/kotlin/.../
├── {Name}.flow.kt              (root)
├── {Name}ViewModel.kt          (root)
├── {Name}Persistence.kt        (root)
├── {Entity}Converters.kt       (root)
├── generated/                   (runtime files)
│   ├── {Name}Flow.kt
│   ├── {Name}Controller.kt
│   ├── {Name}ControllerInterface.kt
│   └── {Name}ControllerAdapter.kt
├── nodes/
└── userInterface/
```

**Target layout** (organized by concern):
```
{Module}/src/commonMain/kotlin/.../
├── flow/
│   ├── {Name}.flow.kt
│   └── {Name}Flow.kt
├── controller/
│   ├── {Name}Controller.kt
│   ├── {Name}ControllerInterface.kt
│   └── {Name}ControllerAdapter.kt
├── viewmodel/
│   └── {Name}ViewModel.kt
├── nodes/
├── userInterface/
└── persistence/
    ├── {Name}Persistence.kt
    └── {Entity}Converters.kt
```

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Update Code Generators to Write to New Paths (Priority: P1)

A developer triggers "Generate Module" or "Generate Repository Module" and the system writes all generated files into the new subdirectory structure (flow/, controller/, viewmodel/, persistence/) instead of the current flat layout. The `generated/` subdirectory is no longer used. All generated files compile correctly with updated package declarations and imports.

**Why this priority**: The generators must write to the correct locations before existing modules can be migrated. This is the foundation — if generators still write to the old paths, migration is pointless.

**Independent Test**: Create a new module via "Generate Module". Verify the output directory contains flow/, controller/, viewmodel/, and userInterface/ subdirectories with files in the correct locations. Verify the generated files compile. Verify no files are written to the old `generated/` directory or the package root.

**Acceptance Scenarios**:

1. **Given** a developer triggers "Generate Module", **When** files are written, **Then** the .flow.kt and Flow.kt files are in the `flow/` subdirectory
2. **Given** a developer triggers "Generate Module", **When** files are written, **Then** Controller, ControllerInterface, and ControllerAdapter are in the `controller/` subdirectory
3. **Given** a developer triggers "Generate Module", **When** files are written, **Then** the ViewModel is in the `viewmodel/` subdirectory
4. **Given** a developer triggers "Generate Module", **When** files are written, **Then** no files are written to a `generated/` subdirectory
5. **Given** a developer triggers "Generate Repository Module", **When** files are written, **Then** Persistence.kt and Converters.kt are in the `persistence/` subdirectory
6. **Given** the generated files, **When** compiled together, **Then** all import references resolve correctly across the new subdirectory packages

---

### User Story 2 - Migrate Existing Demo Project Modules (Priority: P2)

The five existing generated modules in the Demo Project (StopWatch, UserProfiles, Addresses, EdgeArtFilter, WeatherForecast) are migrated from the current flat layout to the new folder hierarchy. After migration, all modules compile and the graph editor can load and run them without changes to user-authored code.

**Why this priority**: Migrating existing modules validates the new layout works end-to-end and ensures backward compatibility. Without migrating the demo project, the new layout would be untested on real modules.

**Independent Test**: After migration, run `./gradlew :StopWatch:compileKotlinJvm :UserProfiles:compileKotlinJvm :Addresses:compileKotlinJvm` in the Demo Project. Launch the graph editor, open each module's .flow.kt, and verify it loads. Run the Runtime Preview for at least one module.

**Acceptance Scenarios**:

1. **Given** the StopWatch module, **When** migrated to the new layout, **Then** all files are in their correct subdirectories and the module compiles
2. **Given** each of the 5 demo modules, **When** migrated, **Then** the module compiles without import errors
3. **Given** a migrated module, **When** opened in the graph editor, **Then** the flow graph loads correctly with all nodes and connections intact
4. **Given** a migrated module with a Runtime Preview, **When** the preview is run, **Then** it executes without errors
5. **Given** user-authored files (UI composables, custom code outside generated markers), **When** migration occurs, **Then** no user-authored code is modified

---

### User Story 3 - Update Discovery and Scanning (Priority: P3)

The graph editor's runtime discovery systems — NodeDefinitionRegistry, ModuleSessionFactory, and DynamicPreviewDiscovery — are updated to scan the new subdirectory paths. CodeNode definitions in `nodes/` continue to be discovered. Runtime preview composables in `userInterface/` continue to be found. Controller and Flow classes in their new locations are resolved correctly at runtime.

**Why this priority**: Without updating the scanning paths, the graph editor cannot discover CodeNodes, load modules, or run previews from the new layout.

**Independent Test**: Launch the graph editor with migrated modules. Verify the node palette shows all CodeNode definitions. Open a module's flow graph and verify it loads. Start the Runtime Preview and verify it finds and renders the preview composable.

**Acceptance Scenarios**:

1. **Given** a migrated module with CodeNodes in `nodes/`, **When** the graph editor starts, **Then** all CodeNode definitions appear in the node palette
2. **Given** a migrated module, **When** a flow graph is loaded, **Then** the runtime can locate Controller, ControllerInterface, and ControllerAdapter classes in the `controller/` subdirectory
3. **Given** a migrated module with a preview composable, **When** the Runtime Preview scans for composables, **Then** it discovers and renders them from `userInterface/`
4. **Given** the old `generated/` path no longer exists in migrated modules, **When** the system scans for runtime classes, **Then** it does not fail or produce errors about missing `generated/` directories

---

### Edge Cases

- What happens when a module has both old-layout and new-layout files? The system should handle mixed layouts gracefully during transition — scan both `generated/` and `controller/`/`flow/` paths until all modules are migrated.
- What happens when a user re-generates a module that was already migrated? The generator writes to the new paths. If old `generated/` files still exist, they become orphaned but do not cause errors.
- What happens to the TestModule (DemoUI) which was recently created with a partial layout? It should be migrated to the new layout alongside the other modules.
- What happens to hand-written files in module roots? Files that are not generated (e.g., custom helper classes placed in the module root) remain untouched.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: All code generators MUST write output files to the new subdirectory paths: flow/, controller/, viewmodel/, userInterface/, persistence/, nodes/
- **FR-002**: The `generated/` subdirectory MUST no longer be used as an output target for any generator
- **FR-003**: All generated file package declarations MUST reflect the new subdirectory paths (e.g., `package io.codenode.stopwatch.controller` instead of `package io.codenode.stopwatch.generated`)
- **FR-004**: All generated file import statements MUST reference the correct new packages for cross-subdirectory references
- **FR-005**: The five existing demo modules (StopWatch, UserProfiles, Addresses, EdgeArtFilter, WeatherForecast) MUST be migrated to the new layout and compile without errors
- **FR-006**: NodeDefinitionRegistry MUST discover CodeNode definitions from the `nodes/` subdirectory in migrated modules
- **FR-007**: ModuleSessionFactory and runtime class loading MUST locate controller and flow classes in their new `controller/` and `flow/` subdirectory packages
- **FR-008**: DynamicPreviewDiscovery MUST continue to discover preview composables from the `userInterface/` subdirectory
- **FR-009**: User-authored code (UI composables, custom logic outside generated markers) MUST NOT be modified during migration
- **FR-010**: The system MUST gracefully handle modules in either the old or new layout during the transition period — scanning both `generated/` and `controller/`/`flow/` paths

### Key Entities

- **ModuleLayout**: The organizational structure of files within a generated module — either the legacy flat layout (with `generated/`) or the new hierarchical layout (with flow/, controller/, viewmodel/, persistence/).
- **GeneratorOutputPath**: The target subdirectory for each generator's output, mapping generator class to the correct folder in the new hierarchy.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of generators write to the new subdirectory paths — zero files are written to `generated/` or the package root
- **SC-002**: All 5 demo project modules compile after migration with zero import errors
- **SC-003**: The graph editor loads migrated modules and displays all nodes in the palette without errors
- **SC-004**: Runtime Preview executes successfully on at least one migrated module
- **SC-005**: A newly generated module (via "Generate Module") uses the new layout exclusively — no `generated/` directory is created
- **SC-006**: User-authored files in migrated modules are byte-identical before and after migration

## Assumptions

- The `nodes/` and `userInterface/` subdirectories already exist in the current layout and do not need to be moved — only their scanning paths may need verification
- The `generated/` subdirectory in the current layout maps to two new subdirectories: `flow/` (for Flow.kt) and `controller/` (for Controller*.kt)
- Package names follow the subdirectory structure (e.g., `io.codenode.stopwatch.controller` for files in controller/)
- The migration of demo project modules is performed as file moves + package/import updates — not by re-generating from scratch
- This feature changes only the CodeNodeIO tool and demo project — no changes to the fbpDsl library or other infrastructure modules
- The old `generated/` directories in migrated modules can be deleted after migration, but the system should not break if they still exist
