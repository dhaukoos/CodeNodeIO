# Implementation Plan: Refine the ViewModel Binding

**Branch**: `034-refine-viewmodel-binding` | **Date**: 2026-02-28 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/034-refine-viewmodel-binding/spec.md`

## Summary

Refactor the code-generated ViewModel from a thin delegation wrapper in `generated/` to a stub file in the base package that owns a consolidated `{ModuleName}State` object. This object replaces per-node `{NodeName}StateProperties` singleton objects, consolidating all observable state (derived from sink node input ports) into a single file. The `stateProperties/` folder and `StatePropertiesGenerator` are eliminated entirely. Validated by renaming the existing StopWatch module to StopWatchV2 and producing a refactored StopWatch from the same `.flow.kt` definition.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (Kotlin Multiplatform)
**Primary Dependencies**: Compose Multiplatform 1.7.3, kotlinx-coroutines 1.8.0, kotlinx-serialization 1.6.0, lifecycle-viewmodel-compose 2.8.0
**Storage**: N/A (in-memory FlowGraph models, generated source code)
**Testing**: Kotlin multiplatform tests (`./gradlew :kotlinCompiler:allTests`, `./gradlew :graphEditor:jvmTest`)
**Target Platform**: KMP (JVM, Android, iOS)
**Project Type**: Multi-module KMP project
**Performance Goals**: N/A (code generation, not runtime performance)
**Constraints**: Must maintain functional equivalence with existing StopWatch module
**Scale/Scope**: ~8 generator files modified, ~6 test files modified, 2 files deleted, 1 module renamed, 1 module regenerated

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality First | PASS | Refactoring simplifies architecture (fewer files, consolidated state). Maintains type safety. |
| II. Test-Driven Development | PARTIAL | Tests exist for all generators. StateProperties tests will be deleted (code deleted). New ViewModel stub generator needs updated tests. No NEW test-first requirement since this is a refactoring of existing tested generators. |
| III. User Experience Consistency | PASS | No UI changes. ViewModel remains the binding interface for Compose UI. |
| IV. Performance Requirements | PASS | No performance-critical changes. Same StateFlow delegation pattern. |
| V. Observability & Debugging | PASS | Observable state consolidation improves debuggability (one file instead of many). |
| Licensing & IP | PASS | No new dependencies. All existing libraries are Apache 2.0/MIT. |

**Gate Result**: PASS — TDD principle is partially met (deleting tests for deleted code, updating tests for changed code). No new untested code paths.

## Project Structure

### Documentation (this feature)

```text
specs/034-refine-viewmodel-binding/
├── spec.md              # Feature specification
├── plan.md              # This file
├── research.md          # Phase 0 research decisions
├── data-model.md        # Entity model changes
├── quickstart.md        # Integration scenarios
├── checklists/
│   └── requirements.md  # Quality checklist
└── tasks.md             # Phase 2 output (from /speckit.tasks)
```

### Source Code (repository root)

```text
# Modules affected
StopWatch/                          # Existing → renamed to StopWatchV2
StopWatchV2/                        # Preserved copy of original StopWatch
StopWatch/                          # New: regenerated with refactored code gen

# Code generators (kotlinCompiler module)
kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/
├── RuntimeViewModelGenerator.kt    # MODIFY: generate stub with ModuleState object
├── RuntimeFlowGenerator.kt         # MODIFY: replace statePropertiesPackage → viewModelPackage
├── RuntimeControllerGenerator.kt   # NO CHANGE (delegates from Flow)
├── RuntimeControllerInterfaceGenerator.kt  # NO CHANGE
├── RuntimeControllerAdapterGenerator.kt    # NO CHANGE
├── ProcessingLogicStubGenerator.kt # MODIFY: remove statePropertiesPackage
├── ObservableStateResolver.kt      # NO CHANGE (still resolves sink input ports)
├── StatePropertiesGenerator.kt     # DELETE
├── ConnectionWiringResolver.kt     # NO CHANGE
└── RuntimeTypeResolver.kt          # NO CHANGE

# Generator tests (kotlinCompiler module)
kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/
├── RuntimeViewModelGeneratorTest.kt         # MODIFY: test new stub structure
├── RuntimeFlowGeneratorTest.kt              # MODIFY: test ModuleState delegation
├── ProcessingLogicStubGeneratorTest.kt      # MODIFY: remove StateProperties import tests
├── ObservableStateResolverTest.kt           # NO CHANGE
├── StatePropertiesGeneratorTest.kt          # DELETE

# Module save service (graphEditor module)
graphEditor/src/jvmMain/kotlin/save/
└── ModuleSaveService.kt            # MODIFY: remove StateProperties, stub ViewModel

graphEditor/src/jvmTest/kotlin/save/
└── ModuleSaveServiceTest.kt        # MODIFY: remove StateProperties assertions

# Mobile app integration
KMPMobileApp/
├── build.gradle.kts                # MODIFY: dependency swap
└── src/commonMain/.../App.kt       # MODIFY: import swap

# Root
settings.gradle.kts                 # MODIFY: add StopWatchV2, keep StopWatch
```

**Structure Decision**: Multi-module KMP project. Changes span 3 modules (kotlinCompiler, graphEditor, StopWatch/StopWatchV2) plus root settings. No new modules created — StopWatchV2 is a copy of StopWatch, and the new StopWatch replaces the original.

## Architecture: Module State Object Pattern

### Current Architecture (Per-Node StateProperties)

```
stateProperties/TimerEmitterStateProperties.kt
  └── object: _elapsedSeconds, _elapsedMinutes, elapsedSecondsFlow, elapsedMinutesFlow, reset()

stateProperties/DisplayReceiverStateProperties.kt
  └── object: _seconds, _minutes, secondsFlow, minutesFlow, reset()

generated/StopWatchFlow.kt
  └── imports N StateProperties objects
  └── delegates StateFlow from StateProperties
  └── sink consume blocks update StateProperties._port.value

generated/StopWatchViewModel.kt
  └── delegates StateFlow from Controller (which delegates from Flow)
```

### New Architecture (Consolidated Module State)

```
StopWatchViewModel.kt (base package, stub file)
  ├── object StopWatchState
  │     └── _seconds, _minutes, secondsFlow, minutesFlow, reset()
  │     (only sink input ports — no generator output ports)
  └── class StopWatchViewModel
        └── delegates StateFlow from Controller + direct same-file State access

generated/StopWatchFlow.kt
  └── imports 1 {ModuleName}State object from base package
  └── delegates StateFlow from {ModuleName}State
  └── sink consume blocks update {ModuleName}State._port.value
```

### Key Design Decisions

1. **Module State as `object`**: Provides namespace for properties, avoids naming clashes with Flow class properties, and mirrors the existing StateProperties reference pattern (`ObjectName.propertyName`).

2. **Only sink input ports**: Generator output ports are NOT in Module State. Observable state comes from sink inputs (data arriving at "display" nodes). Generator tick functions manage their own computation state locally.

3. **ViewModel file with selective regeneration**: The Module Properties section (the `{ModuleName}State` object) is delineated by marker comments and regenerated on each save to stay in sync with current port definitions. Everything outside the markers (user-written ViewModel class code, custom methods, additional imports) is preserved. This follows the same principle as ProcessingLogicStubGenerator's body preservation, but at a section level.

4. **Flow uses `viewModelPackage` import**: Same delegation pattern as before (`{ModuleName}State.secondsFlow`), just a different object name and import path. Minimal change to generator logic.

### Generated ViewModel Stub Structure

```kotlin
package io.codenode.stopwatch

// Auto-generated imports...

// ===== MODULE PROPERTIES START =====
// Auto-generated from sink node input ports. Do not edit this section manually.
// Changes here will be overwritten on next code generation.

object StopWatchState {

    internal val _seconds = MutableStateFlow(0)
    val secondsFlow: StateFlow<Int> = _seconds.asStateFlow()

    internal val _minutes = MutableStateFlow(0)
    val minutesFlow: StateFlow<Int> = _minutes.asStateFlow()

    fun reset() {
        _seconds.value = 0
        _minutes.value = 0
    }
}

// ===== MODULE PROPERTIES END =====

// ============================================================
// ViewModel
// Binding interface between composable UI and FlowGraph.
// User-editable section below — preserved across regenerations.
// ============================================================

class StopWatchViewModel(
    private val controller: StopWatchControllerInterface
) : ViewModel() {

    // Observable state from module properties
    val seconds: StateFlow<Int> = StopWatchState.secondsFlow
    val minutes: StateFlow<Int> = StopWatchState.minutesFlow

    // Execution state from controller
    val executionState: StateFlow<ExecutionState> = controller.executionState

    // Control methods
    fun start(): FlowGraph = controller.start()
    fun stop(): FlowGraph = controller.stop()
    fun reset(): FlowGraph = controller.reset()
    fun pause(): FlowGraph = controller.pause()
    fun resume(): FlowGraph = controller.resume()
}
```

## Implementation Phases

### Phase 1: Rename StopWatch → StopWatchV2 (US1)

**Goal**: Preserve the working prototype as a baseline for comparison.

**Steps**:
1. Copy `StopWatch/` directory to `StopWatchV2/`
2. Update all package declarations, class names, object names, val names
3. Update `settings.gradle.kts` to include `:StopWatchV2`
4. Update `KMPMobileApp` dependencies and imports to use StopWatchV2
5. Compile and verify: `./gradlew :StopWatchV2:compileKotlinJvm :KMPMobileApp:compileKotlinJvm`

### Phase 2: Refactor Code Generators (US2)

**Goal**: Change code generation output to produce the new ViewModel stub with Module State object. Eliminate StateProperties.

**Generator changes**:
1. `RuntimeViewModelGenerator.kt` → Generate ViewModel stub with marker-delineated `{ModuleName}State` object + `{ModuleName}ViewModel` class. Support selective regeneration: on re-save, regenerate Module Properties section between markers while preserving user code outside markers. Change output path to base package (not `generated/`).
2. `RuntimeFlowGenerator.kt` → Replace `statePropertiesPackage` with `viewModelPackage`. Import `{ModuleName}State`. Delegate StateFlow and consume updates from `{ModuleName}State`.
3. `ProcessingLogicStubGenerator.kt` → Remove `statePropertiesPackage` parameter. Stubs no longer import StateProperties.
4. DELETE `StatePropertiesGenerator.kt`

**Orchestrator changes**:
5. `ModuleSaveService.kt` → Remove stateProperties directory creation, file generation, and orphan cleanup. Move ViewModel write to base package. Use selective regeneration: if ViewModel file exists, regenerate only the Module Properties section (between markers) while preserving user code.

**Test changes**:
6. DELETE `StatePropertiesGeneratorTest.kt`
7. Update `RuntimeFlowGeneratorTest.kt` — test `{ModuleName}State` delegation
8. Update `ProcessingLogicStubGeneratorTest.kt` — remove StateProperties import tests
9. Update `RuntimeViewModelGeneratorTest.kt` — test new stub structure
10. Update `ModuleSaveServiceTest.kt` — remove StateProperties assertions, add selective regeneration tests (Module Properties section updated, user code preserved)

### Phase 3: Generate New StopWatch & Validate (US2 + US3)

**Goal**: Produce a new StopWatch module with the refactored code generation and validate equivalence.

**Steps**:
1. Generate new StopWatch module from same `.flow.kt` definition using updated ModuleSaveService
2. Fill in processingLogic stubs with tick logic (TimerEmitter: local state + increment, DisplayReceiver: passthrough)
3. Write userInterface files (copy from StopWatchV2, update imports)
4. Switch KMPMobileApp from StopWatchV2 to new StopWatch
5. Compile and verify: `./gradlew :StopWatch:compileKotlinJvm :KMPMobileApp:compileKotlinJvm`
6. Verify no `stateProperties/` folder exists in new StopWatch
7. Verify ViewModel is in base package, not `generated/`

## Complexity Tracking

No constitution violations requiring justification.

## Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| StopWatch rename misses a reference | Build failure | Compile after each step; grep for stale "stopwatch" package references |
| ViewModel user code gets overwritten | Lost user edits | Selective regeneration with marker comments; only Module Properties section between `MODULE PROPERTIES START/END` markers is overwritten; test coverage for preservation of user code outside markers |
| Marker comments removed by user | Module Properties section not found for regeneration | Treat as fresh generation if markers missing; document in generated file header that markers must be preserved |
| Processing logic stubs need StateProperties | Compilation error | New stubs don't import StateProperties; existing stubs preserved in StopWatchV2 |
| Module State object naming conflicts | Compile error | Use `{ModuleName}State` pattern; test with generators |
