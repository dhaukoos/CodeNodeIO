# Tasks: Generalize Runtime Preview

**Input**: Design documents from `/specs/040-generalize-runtime-preview/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/module-controller-api.md, contracts/preview-registry-api.md, contracts/runtime-session-api.md, quickstart.md

**Tests**: Not explicitly requested — test tasks omitted.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Build Configuration)

**Purpose**: Add the UserProfiles module dependency to graphEditor and verify circuitSimulator can be decoupled from StopWatch.

- [X] T001 Add `implementation(project(":UserProfiles"))` to `graphEditor/build.gradle.kts` commonMain dependencies
- [X] T002 Remove `implementation(project(":StopWatch"))` from `circuitSimulator/build.gradle.kts` if present (RuntimeSession will no longer import StopWatch directly)

**Checkpoint**: `./gradlew :graphEditor:compileKotlinJvm` compiles (may have errors until RuntimeSession is refactored — that's expected)

---

## Phase 2: Foundational (ModuleController Interface + RuntimeSession Refactoring)

**Purpose**: Create the shared ModuleController interface and refactor RuntimeSession to be module-agnostic. This BLOCKS all user story work.

**CRITICAL**: No user story UI/preview work can begin until this phase is complete.

- [X] T003 Create `ModuleController` interface in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/ModuleController.kt` — define interface with `executionState: StateFlow<ExecutionState>`, `start(): FlowGraph`, `stop(): FlowGraph`, `pause(): FlowGraph`, `resume(): FlowGraph`, `reset(): FlowGraph`, and `setAttenuationDelay(ms: Long?)`. Import `ExecutionState` and `FlowGraph` from `io.codenode.fbpdsl.model` and `StateFlow` from `kotlinx.coroutines.flow`.
- [X] T004 [P] Add `: ModuleController` to `StopWatchController` class declaration in `StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/generated/StopWatchController.kt` — add `import io.codenode.fbpdsl.runtime.ModuleController` and add `: ModuleController` after the class name. All required methods already exist; this is a pure interface addition.
- [X] T005 [P] Add `: ModuleController` to `UserProfilesController` class declaration in `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/generated/UserProfilesController.kt` — add `import io.codenode.fbpdsl.runtime.ModuleController` and add `: ModuleController` after the class name. All required methods already exist; this is a pure interface addition.
- [X] T006 Refactor `RuntimeSession` in `circuitSimulator/src/commonMain/kotlin/io/codenode/circuitsimulator/RuntimeSession.kt` — change from no-arg constructor with hardcoded StopWatch to `class RuntimeSession(private val controller: ModuleController, val viewModel: Any)`. Remove all StopWatch imports (`StopWatchController`, `StopWatchControllerAdapter`, `StopWatchViewModel`, `stopWatchFlowGraph`). Delegate `executionState` to `controller.executionState`. Keep `attenuationDelayMs: MutableStateFlow<Long>`, `start()`, `stop()`, `pause()`, `resume()`, and `setAttenuation(ms)` methods, delegating lifecycle calls to `controller` instead of the hardcoded StopWatch controller. Import `ModuleController` from `io.codenode.fbpdsl.runtime`.

**Checkpoint**: `./gradlew :fbpDsl:compileKotlinJvm :StopWatch:compileKotlinJvm :UserProfiles:compileKotlinJvm :circuitSimulator:compileKotlinJvm` all compile without errors.

---

## Phase 3: User Story 1 + 3 — Module-Agnostic Runtime Session + Backward Compatibility (Priority: P1 + P2)

**Goal**: The graphEditor creates RuntimeSession instances dynamically based on the loaded module. StopWatch preview continues to work identically (backward compatibility).

**Independent Test**: Load StopWatch module in graphEditor → open Runtime Preview → start/stop/pause/resume/attenuation all work as before. Load UserProfiles module → runtime session initializes and execution controls work.

### Implementation for User Stories 1 + 3

- [X] T007 Create `ModuleSessionFactory` in `graphEditor/src/jvmMain/kotlin/ui/ModuleSessionFactory.kt` — create an `object ModuleSessionFactory` with `fun createSession(moduleName: String): RuntimeSession?` that dispatches on `moduleName`. For `"StopWatch"`: create `StopWatchController(stopWatchFlowGraph)`, wrap in `StopWatchControllerAdapter`, create `StopWatchViewModel(adapter)`, return `RuntimeSession(controller, viewModel)`. For `"UserProfiles"`: create `UserProfilesController(userProfilesFlowGraph)`, call `controller.start()`, wrap in `UserProfilesControllerAdapter`, create `UserProfilesViewModel(adapter)`, return `RuntimeSession(controller, viewModel)`. Return `null` for unknown modules.
- [X] T008 Update `Main.kt` in `graphEditor/src/jvmMain/kotlin/Main.kt` — replace `val runtimeSession = remember { RuntimeSession() }` with factory-based creation using `ModuleSessionFactory.createSession(moduleName)`. Determine the module name from `moduleRootDir` (directory name). Recreate the RuntimeSession when the loaded module changes (use `remember(moduleRootDir)`). Stop the previous session before creating a new one. Handle `null` return (unknown module) gracefully by leaving session null and disabling controls.

**Checkpoint**: graphEditor launches, StopWatch runtime preview works identically to before. Module switching creates correct session.

---

## Phase 4: User Story 2 — Dynamic Preview Rendering (Priority: P1)

**Goal**: The Runtime Preview panel dynamically renders any module's composables using a registry instead of hardcoded `when` block. UserProfiles composables render live.

**Independent Test**: Load UserProfiles module → select "UserProfiles" from dropdown → live preview renders showing profile list and buttons. Load StopWatch → "StopWatch" and "StopWatchScreen" previews render as before.

### Implementation for User Story 2

- [ ] T009 Create `PreviewRegistry` in `graphEditor/src/jvmMain/kotlin/ui/PreviewRegistry.kt` — create `typealias PreviewComposable = @Composable (viewModel: Any, modifier: Modifier) -> Unit` and `object PreviewRegistry` with `private val registry = mutableMapOf<String, PreviewComposable>()`, `fun register(composableName: String, preview: PreviewComposable)`, `fun get(composableName: String): PreviewComposable?`, `fun hasPreview(composableName: String): Boolean`, and `fun registeredNames(): Set<String>`.
- [ ] T010 Refactor `StopWatchPreviewProvider` in `graphEditor/src/jvmMain/kotlin/ui/StopWatchPreviewProvider.kt` — add a `fun register()` method that calls `PreviewRegistry.register("StopWatch") { viewModel, modifier -> ... }` and `PreviewRegistry.register("StopWatchScreen") { viewModel, modifier -> ... }` with the existing preview composable logic. Cast `viewModel as StopWatchViewModel` inside each lambda. Keep the existing `Preview()` and `ScreenPreview()` methods for backward compatibility or remove if no longer referenced.
- [ ] T011 Create `UserProfilesPreviewProvider` in `graphEditor/src/jvmMain/kotlin/ui/UserProfilesPreviewProvider.kt` — create `object UserProfilesPreviewProvider` with `fun register()` that calls `PreviewRegistry.register("UserProfiles") { viewModel, modifier -> val vm = viewModel as UserProfilesViewModel; UserProfiles(viewModel = vm, modifier = modifier) }`. Import `UserProfiles` from `io.codenode.userprofiles.userInterface` and `UserProfilesViewModel` from `io.codenode.userprofiles`.
- [ ] T012 Refactor `RuntimePreviewPanel` in `graphEditor/src/jvmMain/kotlin/ui/RuntimePreviewPanel.kt` — replace the hardcoded `when (selectedComposable)` block (lines 332-349) with a `PreviewRegistry.get(selectedComposable)` lookup. If a preview function is found, call it with `runtimeSession.viewModel` and modifier. If `selectedComposable` is null, show "Select a composable to preview". If no preview is registered, show "Preview not available for: $selectedComposable". Remove the direct `StopWatchPreviewProvider` import/references.
- [ ] T013 Initialize preview providers in `Main.kt` in `graphEditor/src/jvmMain/kotlin/Main.kt` — at app startup (before any RuntimePreviewPanel rendering), call `StopWatchPreviewProvider.register()` and `UserProfilesPreviewProvider.register()` to populate the PreviewRegistry.

**Checkpoint**: StopWatch previews work via registry. UserProfiles preview renders live in the panel. No "Preview not available" for registered composables.

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Final validation, edge case handling, and cleanup.

- [ ] T014 Handle null RuntimeSession in `RuntimePreviewPanel` in `graphEditor/src/jvmMain/kotlin/ui/RuntimePreviewPanel.kt` — if `runtimeSession` is null (unknown module), disable all execution controls and show "No runtime available for this module" in the preview area. Update the parameter type to `RuntimeSession?` if needed and guard all control button `onClick` handlers.
- [ ] T015 Run quickstart.md end-to-end validation — follow the manual test flow in `specs/040-generalize-runtime-preview/quickstart.md`: launch graphEditor, load StopWatch, verify preview works, load UserProfiles, verify preview works, switch between modules, verify clean transitions.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 for build config; T003 must complete before T004/T005; T003-T005 must complete before T006
- **US1+US3 (Phase 3)**: Depends on Phase 2 completion (RuntimeSession must be refactored first)
- **US2 (Phase 4)**: Depends on Phase 3 (needs factory-created RuntimeSession to pass viewModel to preview providers)
- **Polish (Phase 5)**: Depends on all user stories being complete

### User Story Dependencies

- **US1+US3 (P1+P2)**: Can start after Foundational — no dependencies on US2
- **US2 (P1)**: Depends on US1 (needs RuntimeSession with viewModel to render previews)

### Within Each Phase

- T003 (ModuleController interface) before T004/T005 (controller implementations)
- T004/T005 can run in parallel (different modules)
- T006 (RuntimeSession) after T003 (needs ModuleController import)
- T009 (PreviewRegistry) before T010/T011 (providers register with it)
- T010/T011 can run in parallel (different files)
- T012 (RuntimePreviewPanel) after T009/T010/T011 (needs registry populated)
- T013 (Main.kt init) after T010/T011 (needs provider register() methods)

### Parallel Opportunities

- T001 and T002 can run in parallel (different build.gradle.kts files)
- T004 and T005 can run in parallel (different controller files)
- T010 and T011 can run in parallel (different preview provider files)

---

## Implementation Strategy

### MVP First (Phase 1 → 2 → 3)

1. Complete Phase 1: Build configuration
2. Complete Phase 2: ModuleController interface + RuntimeSession refactoring
3. Complete Phase 3: Module session factory + Main.kt integration
4. **STOP and VALIDATE**: StopWatch preview works identically, UserProfiles session initializes

### Incremental Delivery

1. Setup + Foundational → Interface defined, controllers implement it, RuntimeSession refactored
2. Add US1+US3 → Factory creates sessions, StopWatch backward-compatible → Demo
3. Add US2 → PreviewRegistry, providers, dynamic rendering → Demo (full feature)
4. Polish → Edge cases, validation

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- US1 and US3 are combined because backward compatibility (US3) is inherently validated by US1's StopWatch session factory
- US2 is separated because preview rendering is independent from session lifecycle
- The existing `discoverComposables()` function requires NO changes
- Commit after each task or logical group
