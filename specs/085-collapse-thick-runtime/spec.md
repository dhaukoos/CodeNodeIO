# Feature Specification: Collapse the Entity-Module Thick Runtime onto DynamicPipelineController

**Feature Branch**: `085-collapse-thick-runtime`
**Created**: 2026-04-27
**Status**: Draft
**Input**: User description: "Collapse the entity-module thick runtime onto DynamicPipelineController. Since preview-only runtime support is largely supported by a universal class in fbpDsl, simplify the extra per-module files to support deployable runtime by a similar (or part of the same) universal class in fbpDsl. DynamicPipelineController is already 90% of that universal class. The per-module AddressesController/StopWatchController are essentially the same boilerplate, just with hand-wired references to a generated AddressesFlow/StopWatchFlow runtime. If a deployable production app could call DynamicPipelineController directly (with a flowGraphProvider returning the module's compiled FlowGraph and a lookup populated by a generated module-local registry), then Controller.kt collapses to a few lines — or disappears entirely. AddressesFlow.kt (and the wireGraphFromCanvas-style hand-coded pipeline) likewise becomes redundant once the dynamic builder is the universal runtime. This touches the entity-module generators (RuntimeControllerGenerator, RuntimeControllerAdapterGenerator, RuntimeFlowGenerator), regenerates Addresses/UserProfiles/EdgeArtFilter, and requires the production-app integration tests for those modules to be re-validated."

## Overview

Today, every generated CodeNodeIO module emits a "thick runtime" stack of three per-module files: `{Module}Controller.kt` (~200 lines of boilerplate), `{Module}ControllerAdapter.kt` (~50 lines of pure delegation), and `{Module}Flow.kt` (~80–140 lines of hand-wired node-runtime instantiation and channel wiring). Across the four entity-module reference implementations in the DemoProject (Addresses, UserProfiles, EdgeArtFilter, StopWatch), this is roughly **1,400 lines of generated per-module boilerplate** plus the **~900 lines of generator code** in `flowGraph-generate` that emits it (`RuntimeControllerGenerator`, `RuntimeControllerAdapterGenerator`, `RuntimeFlowGenerator`).

Inside the GraphEditor's Runtime Preview, none of this thick stack is actually exercised — `ModuleSessionFactory` builds a `DynamicPipelineController` from `fbpDsl` against the module's `ControllerInterface` via a reflection proxy. The thick files exist solely to support a production-deployment path (a built Android/iOS/Desktop app that imports the module as a library and instantiates `{Module}Controller` directly). KMPMobileApp in the DemoProject is the canonical consumer of that path.

`DynamicPipelineController` is already ~90% of a universal runtime that could serve **both** paths. This feature finishes that universalization: it makes `DynamicPipelineController` (or a small companion) sufficient for production deployment as well, eliminates the per-module thick files, regenerates all five reference modules onto the new minimal shape, and migrates KMPMobileApp's consumer code to whatever the new instantiation pattern is. After this feature, generating a module produces only the artifacts genuinely specific to that module (State, ViewModel, ControllerInterface, CodeNodes, .flow.kt, PreviewProvider) — no more per-module Controller, Adapter, or Flow runtime.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Generated modules contain only module-specific files (Priority: P1) 🎯 MVP

A developer regenerates one of the reference modules (e.g., Addresses) and observes that the resulting module directory no longer contains a `Controller.kt`, a `ControllerAdapter.kt`, or a `Flow.kt` runtime file. The generated file count is materially smaller. The remaining files are exactly those that genuinely vary by module: `State.kt`, `ViewModel.kt`, `ControllerInterface.kt`, the per-port CodeNodes in `nodes/`, the `.flow.kt` graph, and (in `jvmMain`) `PreviewProvider.kt`. Compiling the regenerated module succeeds without any manual file edits.

**Why this priority**: This is the visible outcome of the collapse. If this fails, the feature has not delivered its premise. Everything else (KMPMobileApp migration, Runtime Preview parity) builds on the modules first being regeneratable to the new shape.

**Independent Test**: Run module generation against each of the five reference modules. Verify that for every module, no `*Controller.kt`, `*ControllerAdapter.kt`, or `*Flow.kt` (in the runtime sense — the user's authored `.flow.kt` is unaffected) is emitted. Compile each module under its full target set and confirm a clean build.

**Acceptance Scenarios**:

1. **Given** the Addresses module specification, **When** module generation is run, **Then** the resulting `Addresses/src/commonMain/kotlin/io/codenode/addresses/controller/` directory contains only `AddressesControllerInterface.kt` (no `AddressesController.kt`, no `AddressesControllerAdapter.kt`), and the `flow/` directory contains only the user's `Addresses.flow.kt` (no `AddressesFlow.kt` runtime file).
2. **Given** the same regenerated Addresses module, **When** the project is built, **Then** all configured targets compile successfully without manual file edits, missing-import errors, or unresolved-symbol errors.
3. **Given** the four other reference modules (UserProfiles, EdgeArtFilter, StopWatch, WeatherForecast), **When** each is regenerated, **Then** the same file-set reduction is observed and each compiles cleanly.

---

### User Story 2 - Production-app consumers (KMPMobileApp) work end-to-end against the collapsed modules (Priority: P1)

The KMPMobileApp in the DemoProject — which today instantiates `StopWatchController` and `UserProfilesController` directly to drive its UI — continues to function end-to-end after the collapse. The user opens the running app, switches between the StopWatch and UserProfiles tabs, exercises every interactive feature, and observes that behavior is unchanged from before the collapse: timer ticks, profile creation/listing/editing/deletion, and lifecycle pause/resume all work. The only consumer-visible change is in the imports the app declares — KMPMobileApp's source has been updated to use whatever new instantiation pattern replaces the old per-module Controller.

**Why this priority**: Production deployability is the entire reason the thick stack existed. If KMPMobileApp regresses, the collapse has destroyed real functionality. This story is the production-side validation that the universal runtime is genuinely sufficient, not just preview-side sufficient. It is gated on US1 (modules must be regeneratable) but blocks shipping if it fails.

**Independent Test**: Update KMPMobileApp's imports and instantiation code to the new pattern. Build and run the app on at least one platform target (Android JVM, since that's where KMPMobileApp's existing tests live). Manually exercise every primary user journey already covered by KMPMobileApp's existing integration tests and confirm parity.

**Acceptance Scenarios**:

1. **Given** the collapsed reference modules and an updated KMPMobileApp, **When** the app is built and launched, **Then** the StopWatch tab displays a running stopwatch that advances seconds and minutes when started, and pauses/resumes correctly.
2. **Given** the same app, **When** the user switches to the UserProfiles tab, **Then** the profile list renders, the user can add/update/delete a profile, and the displayed list reflects each change without restart.
3. **Given** KMPMobileApp's existing integration test suite (e.g., `StopWatchIntegrationTest`), **When** the suite is updated for the new instantiation pattern and run, **Then** every test passes with no test logic changes beyond instantiation-site updates.

---

### User Story 3 - Runtime Preview behavior is unchanged after the collapse (Priority: P1)

A user opens the GraphEditor, selects any of the five reference modules, opens its `.flow.kt` graph, and clicks Runtime Preview. The behavior is identical to today: the UI renders, the runtime starts/stops/pauses/resumes, data flows from inputs through user-authored CodeNodes to outputs, attenuation/animation/observers all function. No regressions introduced by the collapse.

**Why this priority**: Runtime Preview is the GraphEditor's primary interactive surface. Even though it was already using `DynamicPipelineController` and never exercised the thick files, regressing it during the collapse (e.g., by accidentally breaking the `ControllerInterface` shape that the proxy depends on) would be highly visible. This story acts as a regression-prevention checkpoint.

**Independent Test**: Run the existing Runtime Preview verification scenarios (the quickstart for feature 040 / 031 / 044, or the equivalent in the GraphEditor's test suite) against each of the five reference modules. Confirm no behavioral regression.

**Acceptance Scenarios**:

1. **Given** the collapsed reference modules and a running GraphEditor, **When** the user opens StopWatch in Runtime Preview and clicks Start, **Then** the stopwatch UI renders, advances time, and responds to Pause/Resume/Stop identically to pre-collapse behavior.
2. **Given** any of the five collapsed modules in Runtime Preview, **When** the user enables data-flow animation and observers, **Then** observed values, animation, and attenuation behave identically to pre-collapse.
3. **Given** any of the five collapsed modules, **When** the user resets the runtime, **Then** the module's State is fully cleared and a subsequent Start produces the initial behavior, identical to pre-collapse.

---

### User Story 4 - Generator output for new modules is materially simpler (Priority: P2)

A developer creates a fresh module via the existing module-creation flow. The set of files generated is materially smaller than today — by at least three files per module — and consists only of artifacts whose contents genuinely vary by module. The developer can read the entire generated output of a new module in a single sitting without losing context. Inspecting the new module's structure makes the runtime universalization architecturally legible: the developer sees that runtime execution is delegated to a single class in `fbpDsl` (or to a small generated wrapper around it), not duplicated per module.

**Why this priority**: The collapse's long-term value is partly aesthetic — making the generator output match the actual abstraction. This is a developer-experience win that becomes important as the project accumulates more modules. It depends on US1 but is not gating for ship.

**Independent Test**: Create a new module of each kind (entity-style with persistence; non-entity like StopWatch). Count the generated files; confirm the count is at least three lower than today. Confirm a developer unfamiliar with the generator pipeline can read every emitted file in under 15 minutes total.

**Acceptance Scenarios**:

1. **Given** a fresh module specification, **When** the generator runs, **Then** the generated file count is at least three lower than today's equivalent module (the eliminated trio: Controller.kt, ControllerAdapter.kt, Flow.kt runtime).
2. **Given** the generated output for a new module, **When** a developer reviews each emitted file, **Then** every file's contents are either trivially small (≤ ~30 lines of declarative wiring) or genuinely module-specific (no boilerplate that would also appear unchanged in a different module's generated output).

---

### User Story 5 - The deprecated generator surface is removed cleanly from the codebase (Priority: P3)

A developer searches the `flowGraph-generate` source tree for `RuntimeControllerGenerator`, `RuntimeControllerAdapterGenerator`, and `RuntimeFlowGenerator` and finds either no matches or only references in test fixtures kept for historical comparison. The build's generator wiring (whatever orchestrates which generators run for which module type) no longer invokes the deprecated generators. Test files for the removed generators are deleted (or migrated to test the new universal-runtime emission, where one exists).

**Why this priority**: Leaving deprecated generator code in place creates ambiguity for future contributors ("which generator path is canonical?"). Removing it is a one-time cleanup that prevents confusion. Lower priority than US1–US3 because the rest of the system works either way; this is a hygiene concern.

**Independent Test**: Grep the repo for the three deprecated generator class names. Confirm absence (or that any remaining references are clearly archival). Run the full test suite and confirm no test depends on the removed generators.

**Acceptance Scenarios**:

1. **Given** the post-feature codebase, **When** a developer searches for `class RuntimeControllerGenerator`, `class RuntimeControllerAdapterGenerator`, or `class RuntimeFlowGenerator`, **Then** the search returns no live source files (test fixtures kept for documentation purposes are clearly marked as historical).
2. **Given** the project's build configuration, **When** module generation runs end-to-end, **Then** none of the deprecated generators is invoked anywhere in the call chain.

---

### Edge Cases

- **Module with persistence (Addresses, UserProfiles)**: The thick `Controller.kt` today carries no persistence wiring (persistence is in a separate `Persistence` module + ViewModel-level DAO injection), so the collapse is straightforward — but the spec must verify that the new universal runtime correctly observes repository-driven flows that mutate State (e.g., Addresses' `_addresses` list updated by `AddressRepository.observeAll()`).
- **Module without persistence (StopWatch, EdgeArtFilter, WeatherForecast)**: The collapse path is simpler (no DAO injection). The spec must verify these still work after their `Controller.kt`/`Flow.kt` are removed.
- **Module with hand-edited generated files**: If a developer has manually customized `{Module}Controller.kt` or `{Module}Flow.kt` (against project conventions), the collapse will discard those edits. The spec must require detection and a clear warning before deletion, not silent removal.
- **A module is regenerated mid-feature (after collapse, before KMPMobileApp is updated)**: KMPMobileApp will not compile because its imports reference the now-deleted classes. The spec must require KMPMobileApp's update to land atomically with (or before) the modules' regeneration to avoid a broken intermediate state on the main branch.
- **DynamicPipelineController missing capability needed by production-app path**: Today's `DynamicPipelineController` is implemented for the GraphEditor's needs (in-memory FlowGraph from canvas). The spec must require an explicit gap audit — every operation the per-module `Controller` exposes today (e.g., `bindToLifecycle(Lifecycle)`, `setNodeState(...)`, `setNodeConfig(...)`, `getStatus()`) must either be (a) provided by the universal runtime, (b) provided by a small generated wrapper, or (c) explicitly declared not-needed-for-production with justification.
- **Production app's DI / ViewModel construction**: Today, KMPMobileApp builds a `Controller` then wraps it in a `ControllerAdapter` then passes the adapter to a `ViewModel`. The collapse changes that pattern. The spec must define the new pattern unambiguously so the migration is mechanical, not ad-hoc per consumer.
- **Re-generation of a module where the user has edited `.flow.kt`**: The user's authored `.flow.kt` graph must be preserved across regeneration (already a requirement for individual module-save, must continue to hold).

## Requirements *(mandatory)*

### Functional Requirements

#### Universal Runtime Sufficiency

- **FR-001**: A single class (or small set of cooperating classes) in `fbpDsl` MUST be sufficient to drive both Runtime Preview AND production-app deployment for every module that today uses the thick runtime stack.
- **FR-002**: That universal runtime MUST support every behavior currently exposed by per-module `{Module}Controller` to its consumers, including: start/stop/pause/resume/reset; attenuation delay configuration; emission and value observers; lifecycle binding (where applicable); status query. Any behavior the universal runtime cannot subsume MUST be explicitly enumerated and either ported into the universal runtime or replaced with an equivalent generated companion.
- **FR-003**: The universal runtime MUST work without any GraphEditor classes on the classpath — i.e., a built Android/iOS/Desktop app MUST be able to consume any module by importing only the module itself and `fbpDsl` (plus the module's own dependencies such as `persistence`).

#### Per-Module Generator Output

- **FR-004**: Module generation MUST NOT emit `{Module}Controller.kt`, `{Module}ControllerAdapter.kt`, or `{Module}Flow.kt` runtime files for any module.
- **FR-005**: Module generation MAY emit one new small per-module file (e.g., a `{Module}NodeRegistry` or equivalent factory), provided that file's contents are short (target: ≤ ~30 lines of declarative wiring), genuinely module-specific (no copy-pasta-able boilerplate), and necessary for the universal runtime to resolve module-local CodeNode definitions.
- **FR-006**: Module generation MUST continue to emit `{Module}State.kt`, `{Module}ViewModel.kt`, `{Module}ControllerInterface.kt`, `nodes/{NodeName}CodeNode.kt`, the user's `.flow.kt`, and (in `jvmMain`) `{Module}PreviewProvider.kt` — these remain genuinely module-specific.
- **FR-007**: The shape of `{Module}ControllerInterface` (its FQCN, its method signatures, its property signatures) MUST continue to satisfy `flowGraph-execute/ModuleSessionFactory`'s reflection contract so that the GraphEditor's Runtime Preview path continues to work without changes to `flowGraph-execute` or `fbpDsl/runtime/proxy` code.

#### Reference-Module Migration

- **FR-008**: All five reference modules in the DemoProject (Addresses, UserProfiles, EdgeArtFilter, StopWatch, WeatherForecast) MUST be regenerated to the new minimal shape and MUST compile cleanly under their full configured target sets.
- **FR-009**: Each reference module's behavior in the GraphEditor's Runtime Preview MUST remain identical to its pre-collapse behavior (no regressions in animation, attenuation, observers, reset behavior, or data flow).
- **FR-010**: KMPMobileApp's source MUST be updated to consume the collapsed modules; its existing primary user journeys (StopWatch tab, UserProfiles tab) MUST continue to work end-to-end; its existing integration tests (e.g., `StopWatchIntegrationTest`) MUST continue to pass after instantiation-site updates.

#### Generator Codebase Cleanup

- **FR-011**: The deprecated generators (`RuntimeControllerGenerator`, `RuntimeControllerAdapterGenerator`, `RuntimeFlowGenerator`) and their unit tests MUST be removed from the `flowGraph-generate` source tree, OR clearly marked as archival/historical with no live invocation path.
- **FR-012**: The orchestration that decides which generators to invoke for a module MUST no longer reference the deprecated generators in any live code path.

#### Migration Safety

- **FR-013**: If module regeneration would delete a file at a generator-target path whose content does NOT carry the standard `Generated by CodeNodeIO` marker comment (i.e., the file is hand-written or hand-edited), the generator MUST refuse to delete it, MUST surface a clear warning naming the file and the reason, and MUST require explicit user confirmation before proceeding.
- **FR-014**: KMPMobileApp's source updates MUST land atomically with the reference-module regeneration in the same change set, so that the main branch is never in a state where modules are collapsed but KMPMobileApp still references the eliminated classes.

#### Backward Compatibility (or Lack Thereof)

- **FR-015**: This feature explicitly breaks the per-module `{Module}Controller` and `{Module}ControllerAdapter` import surface. The only consumers under the project's control are inside DemoProject (KMPMobileApp + the modules' own internal use). External or unknown consumers, if any, are out of scope.
- **FR-016**: The migration MUST document the new instantiation pattern in a single, consumer-facing reference (e.g., a section of `quickstart.md`) so any future production-app consumer has a clear template to follow.

### Key Entities

- **Universal Runtime**: The single class (or small set of cooperating classes) in `fbpDsl` that, after this feature, is sufficient to execute any generated module's FlowGraph. `DynamicPipelineController` is the starting point and is expected to be ~90% of the final shape.
- **Module-Local Node Registry** (new, possibly): A small generated per-module artifact that maps node names (as they appear in the user's `.flow.kt`) to the module's compiled `CodeNodeDefinition` instances. Only needed if the universal runtime cannot resolve nodes via global discovery in the production-app path. May or may not exist depending on how the universal runtime is designed.
- **Per-Module Controller** (eliminated): The `{Module}Controller.kt` file that today instantiates `{Module}Flow()`, holds it as a field, and exposes per-port flows by delegating to `flow.{port}Flow`. Disappears.
- **Per-Module Controller Adapter** (eliminated): The `{Module}ControllerAdapter.kt` file that today wraps `{Module}Controller` to satisfy `{Module}ControllerInterface`. Disappears (the universal runtime, or its small companion, satisfies the interface directly).
- **Per-Module Flow Runtime** (eliminated): The `{Module}Flow.kt` file that today hand-wires node-runtime instances and channel connections. Disappears (the universal runtime builds the equivalent dynamically from the FlowGraph + node lookup).
- **Production-App Consumer**: A built application (today: KMPMobileApp) that imports a module as a library and instantiates its runtime to drive UI. After this feature, the consumer instantiates the universal runtime (or a tiny generated factory) instead of a per-module Controller.

## Assumptions

- The five reference modules in the DemoProject (Addresses, UserProfiles, EdgeArtFilter, StopWatch, WeatherForecast) are the complete set of modules that today use the thick runtime stack. There are no other consumers of the deprecated generators outside the project tree.
- The only production-app consumer that imports per-module `{Module}Controller` directly is KMPMobileApp, located inside the DemoProject. Migrating it is in scope; migrating any external consumer is not.
- `{Module}ControllerInterface` is retained (not eliminated) because the GraphEditor's `ModuleSessionFactory` depends on it as a reflection target, and because the typed surface is the cleanest way for ViewModels to express their controller dependency. The interface FQCN and method shapes are preserved at their current canonical location (`io.codenode.{modulename}.controller.{Module}ControllerInterface`).
- Every behavior currently provided by per-module `{Module}Controller` either is, or becomes, available through the universal runtime in `fbpDsl`. If any specific operation (e.g., `bindToLifecycle`, `setNodeState`, `setNodeConfig`, `getStatus`) genuinely belongs at the per-module level rather than in fbpDsl, it may be ported into a tiny generated companion file — but the default expectation is that the universal runtime in fbpDsl absorbs all of it.
- WeatherForecast already lacks the thick-stack files today and works in Runtime Preview; this feature additionally makes it production-deployable through the same universal runtime path that the other four modules use.
- The new instantiation pattern is acceptable to break across `KMPMobileApp`'s import surface; no source-compat shim is required as long as the migration in `KMPMobileApp` is mechanical and lands atomically with the module regeneration (per FR-014).
- Test fixtures and unit tests inside `flowGraph-generate` that exercised the deprecated generators are deleted or rewritten; no need to maintain dual test coverage during migration.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: After this feature ships, the cumulative line count of generated per-module Controller / ControllerAdapter / Flow runtime files across the five reference modules drops from ~1,400 lines to zero.
- **SC-002**: After this feature ships, the line count of generator code in `flowGraph-generate` dedicated to producing the eliminated thick-stack files (currently ~900 lines across `RuntimeControllerGenerator`, `RuntimeControllerAdapterGenerator`, `RuntimeFlowGenerator`) drops to zero, with any necessary universal-runtime-supporting generator code introduced being substantially smaller than what it replaces.
- **SC-003**: All five reference modules in the DemoProject compile cleanly on the first attempt after regeneration, with zero compile errors attributable to the collapse.
- **SC-004**: KMPMobileApp builds and runs end-to-end on Android with the collapsed modules; every primary user journey already exercised by its existing manual / automated test surface continues to work without behavioral change.
- **SC-005**: The GraphEditor's Runtime Preview behavior is identical for every reference module before and after the collapse, as measured by walking each module through Start, Stop, Pause, Resume, Reset, attenuation, observers, and a representative data-flow scenario.
- **SC-006**: The number of files generated for a fresh new entity-style module is at least three lower than today (the eliminated trio).
- **SC-007**: After this feature ships, a developer can find no live source-code references to `RuntimeControllerGenerator`, `RuntimeControllerAdapterGenerator`, or `RuntimeFlowGenerator` outside of clearly-marked archival material.
- **SC-008**: The new instantiation pattern for production-app consumers is documented in one consumer-facing reference, and a developer unfamiliar with the project can use that reference alone to write a working production-app `main()` consuming any reference module in under 30 minutes.
- **SC-009**: After this feature ships, feature 084 (UI-FBP Runtime Preview) can be unblocked and its plan / research / data-model / contracts / quickstart can be regenerated against the universal runtime with no further design discoveries required.
