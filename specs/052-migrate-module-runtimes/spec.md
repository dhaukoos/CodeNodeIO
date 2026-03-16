# Feature Specification: Migrate Module Runtimes

**Feature Branch**: `052-migrate-module-runtimes`
**Created**: 2026-03-16
**Status**: Draft
**Input**: User description: "The last 2 features were scoped to just the EdgeArtFilter module. Now migrate the remaining modules (StopWatch, Addresses, GeoLocations, UserProfiles) to use these new approaches, Self-contained CodeNodes and Dynamic Runtime."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - StopWatch Runs via Dynamic Pipeline (Priority: P1)

The StopWatch module currently uses generated Controller/Flow code with inline node creation. After migration, each StopWatch node (TimerEmitter, TimeIncrementer, DisplayReceiver) is defined as a self-contained CodeNode registered in the system's node registry. When a developer opens StopWatch and presses Start, the system dynamically builds and runs the pipeline from the canvas FlowGraph — identical behavior to before, but now using the dynamic runtime infrastructure.

**Why this priority**: StopWatch is the simplest module (3 nodes, no persistence, no external dependencies) and serves as the proof-of-concept that the migration pattern works for non-image-processing modules.

**Independent Test**: Open StopWatch module, press Start, verify timer ticks (seconds and minutes increment correctly), pause/resume works, stop returns to idle — all running through the dynamic pipeline.

**Acceptance Scenarios**:

1. **Given** StopWatch is loaded in the editor, **When** the user presses Start, **Then** the timer begins ticking with seconds incrementing each second and minutes rolling over at 60 seconds — identical to pre-migration behavior.
2. **Given** StopWatch is running, **When** the user presses Pause then Resume, **Then** the timer freezes during pause and continues from where it left off on resume.
3. **Given** StopWatch is running, **When** the user presses Stop then Start again, **Then** the timer resets and begins from zero.
4. **Given** StopWatch nodes are registered in the node registry, **When** the system checks whether to use the dynamic pipeline, **Then** the dynamic pipeline path is selected (not the fallback generated controller).

---

### User Story 2 - Entity Modules Run via Dynamic Pipeline (Priority: P2)

The three entity modules (UserProfiles, GeoLocations, Addresses) all follow the same CUD-Repository-Display pattern: a 3-output source for Create/Update/Delete operations, a 3-input/2-output processor for repository logic, and a 2-input sink for displaying results and errors. After migration, each module's nodes are self-contained CodeNodes registered in the node registry. The dynamic pipeline builds and runs these modules from the canvas FlowGraph.

**Why this priority**: These three modules share an identical architecture, so migrating one proves the pattern for all three. They are more complex than StopWatch (persistence layer, DAO dependencies) but the migration approach is the same.

**Independent Test**: Open UserProfiles (or GeoLocations or Addresses), press Start, perform CRUD operations (add, update, remove an entity), verify results appear and errors are handled — all running through the dynamic pipeline.

**Acceptance Scenarios**:

1. **Given** UserProfiles is loaded with its nodes registered, **When** the user presses Start and adds a profile, **Then** the profile is saved to the database and appears in the display list.
2. **Given** GeoLocations is running via dynamic pipeline, **When** the user updates a location, **Then** the updated location is persisted and the display refreshes.
3. **Given** Addresses is running via dynamic pipeline, **When** the user removes an address, **Then** the address is deleted from the database and removed from the display list.
4. **Given** any entity module is running, **When** an invalid operation occurs (e.g., removing a non-existent entity), **Then** an error message appears in the display without crashing the pipeline.

---

### User Story 3 - All Modules Interoperable on Canvas (Priority: P3)

After all modules are migrated, a developer can open any module in the editor and the system consistently uses the dynamic pipeline. Speed attenuation, data flow animation, pause/resume, and node hot-swap all work across every module — not just EdgeArtFilter. The generated Controller/Flow code is no longer needed for any migrated module.

**Why this priority**: This is the integration validation — confirming that the full system works uniformly. It depends on US1 and US2 being complete.

**Independent Test**: Cycle through all 5 modules (StopWatch, UserProfiles, GeoLocations, Addresses, EdgeArtFilter), start each, verify dynamic pipeline is used, enable data flow animation with attenuation, stop and switch modules — no regressions.

**Acceptance Scenarios**:

1. **Given** all modules have their nodes registered, **When** the user loads any module and presses Start, **Then** the dynamic pipeline is used (not the fallback generated controller).
2. **Given** any module is running via dynamic pipeline, **When** the user enables speed attenuation and data flow animation, **Then** the pipeline slows down and dot animations appear on connections.
3. **Given** StopWatch is running, **When** the user switches to UserProfiles and starts it, **Then** the previous module stops cleanly and the new module runs correctly.

---

### Edge Cases

- What happens when a module's database is unavailable at startup? The pipeline should surface a clear error rather than silently failing.
- What happens when a node's processing logic throws an exception mid-pipeline? The error should be captured and displayed, and the pipeline should stop gracefully.
- What happens when the user swaps a node in StopWatch (e.g., replaces TimeIncrementer with a different processor)? The same hot-swap behavior validated for EdgeArtFilter should work.
- What happens when an entity module's CUD source emits before the repository processor is ready? Buffered channels should handle the timing gracefully.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Each StopWatch node (TimerEmitter, TimeIncrementer, DisplayReceiver) MUST be defined as a self-contained CodeNode with declared input/output ports and a factory method that creates the appropriate runtime.
- **FR-002**: Each entity module node (CUD source, Repository processor, Display sink) MUST be defined as a self-contained CodeNode for UserProfiles, GeoLocations, and Addresses.
- **FR-003**: All new CodeNodes MUST be registered in the system's node registry at application startup so the dynamic pipeline builder can resolve them by name.
- **FR-004**: The dynamic pipeline MUST produce identical observable behavior to the current generated Controller/Flow for each module (same state transitions, same data flow, same UI output).
- **FR-005**: Entity module CodeNodes that require persistence (Repository processors) MUST receive their data access dependencies through the existing dependency injection mechanism, not through hardcoded singletons.
- **FR-006**: The module session factory MUST select the dynamic pipeline path for all migrated modules (StopWatch, UserProfiles, GeoLocations, Addresses) when their nodes are registered in the registry.
- **FR-007**: Processing logic currently in separate tick-block files MUST be incorporated into each CodeNode's runtime factory method, keeping the node self-contained.
- **FR-008**: The generated Controller/Flow files MAY remain in the codebase as fallback but MUST NOT be required for normal operation of migrated modules.
- **FR-009**: Speed attenuation and data flow animation MUST work for all migrated modules through the dynamic pipeline, identical to EdgeArtFilter behavior.
- **FR-010**: Module ViewModels MUST continue to function without modification — the migration only affects node definitions and pipeline construction, not UI state management.

### Key Entities

- **CodeNode Definition**: A self-contained node declaration with name, category (source/transformer/processor/sink), port specifications, and a runtime factory method. One per logical node in each module.
- **Node Registry Entry**: A mapping from node name to its CodeNode definition, enabling the dynamic pipeline builder to resolve and instantiate nodes at runtime.
- **Module State Bridge**: The shared state object (e.g., StopWatchState, UserProfilesState) that connects the ViewModel's UI state to the pipeline's runtime nodes — unchanged by this migration.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: All 4 modules (StopWatch, UserProfiles, GeoLocations, Addresses) run via dynamic pipeline with zero behavioral regressions — every acceptance scenario passes.
- **SC-002**: Each migrated module starts within the same time envelope as before migration (no perceptible delay from dynamic pipeline construction).
- **SC-003**: 100% of module nodes across all 4 modules are registered in the node registry (12 nodes total: 3 per module).
- **SC-004**: Data flow animation and speed attenuation work for all 4 migrated modules, not just EdgeArtFilter.
- **SC-005**: The existing test suite passes with no new failures introduced by the migration.

## Assumptions

- The dynamic pipeline infrastructure (DynamicPipelineBuilder, DynamicPipelineController) from features 050 and 051 is stable and complete.
- The EdgeArtFilter self-contained CodeNode pattern is the authoritative reference for how migrated nodes should be structured.
- Module ViewModels and their State objects do not need modification — the migration is purely at the node/runtime/pipeline level.
- The existing dependency injection setup for DAOs will be accessible from CodeNode factory methods.
- Pre-started modules (UserProfiles, GeoLocations, Addresses — whose controllers are started immediately in the factory) will need their startup behavior preserved or adapted for the dynamic pipeline lifecycle.

## Scope Boundaries

### In Scope
- Creating self-contained CodeNode definitions for all nodes in StopWatch, UserProfiles, GeoLocations, and Addresses
- Registering all new CodeNodes in the node registry
- Ensuring the dynamic pipeline path is selected for all migrated modules
- Verifying behavioral equivalence with generated Controller/Flow code

### Out of Scope
- Modifying the dynamic pipeline infrastructure itself (features 050/051)
- Changing module ViewModels or UI composables
- Removing generated Controller/Flow files (they remain as fallback)
- Migrating any modules beyond the four listed
- Adding new functionality to any module — this is a pure migration
