# Implementation Plan: State Properties Stubs

**Branch**: `028-state-properties-stubs` | **Date**: 2026-02-21 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/028-state-properties-stubs/spec.md`

## Summary

Generate per-node state property files (`stateProperties/` sub-package) containing `MutableStateFlow`/`StateFlow` pairs derived from each node's ports. Update `ProcessingLogicStubGenerator` to import state properties so tick functions can reference `_portName.value`. Update `RuntimeFlowGenerator` to delegate observable state from state properties objects instead of owning `MutableStateFlow` directly. Integrate into `ModuleSaveService.compileModule()` with same don't-overwrite semantics as processing logic stubs.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP - Kotlin Multiplatform)
**Primary Dependencies**: kotlinx-coroutines 1.8.0 (MutableStateFlow/StateFlow), kotlinx-serialization 1.6.0
**Storage**: N/A (generates source code files to filesystem)
**Testing**: kotlin.test (commonTest), JVM test runner for ModuleSaveService integration tests
**Target Platform**: KMP commonMain (generated code targets all KMP platforms)
**Project Type**: Multi-module KMP project (kotlinCompiler module + graphEditor module)
**Performance Goals**: N/A (code generation, not runtime performance)
**Constraints**: Generated code must compile without errors; existing don't-overwrite patterns must be preserved
**Scale/Scope**: 1 new generator class, 3 modified generators/services, ~6 test files

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality First | PASS | Single responsibility per generator; consistent naming patterns |
| II. Test-Driven Development | PASS | Tests written for new generator and updated generators |
| III. User Experience Consistency | PASS | Generated code follows established StopWatch patterns |
| IV. Performance Requirements | N/A | Code generation, no runtime performance impact |
| V. Observability & Debugging | N/A | Generated code; orphan detection provides operational feedback |
| Licensing & IP | PASS | No new dependencies; uses existing kotlinx-coroutines (Apache 2.0) |

## Project Structure

### Documentation (this feature)

```text
specs/028-state-properties-stubs/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/
├── StatePropertiesGenerator.kt          # NEW: Generates {Node}StateProperties.kt objects
├── ProcessingLogicStubGenerator.kt      # MODIFIED: Add state properties import
├── RuntimeFlowGenerator.kt             # MODIFIED: Delegate observable state from state props
└── ObservableStateResolver.kt           # EXISTING: Still used, but Flow delegates instead of owns

kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/
├── StatePropertiesGeneratorTest.kt      # NEW: Tests for state properties generation
├── ProcessingLogicStubGeneratorTest.kt  # MODIFIED: Verify import statements
├── RuntimeFlowGeneratorTest.kt          # MODIFIED: Verify delegation pattern
└── RuntimeControllerGeneratorTest.kt    # CHECK: May need updates if controller references change

graphEditor/src/jvmMain/kotlin/save/
└── ModuleSaveService.kt                 # MODIFIED: Add stateProperties generation + orphan detection

graphEditor/src/jvmTest/kotlin/save/
└── ModuleSaveServiceTest.kt             # MODIFIED: Add stateProperties integration tests

# Generated output structure (what the generators produce):
{Module}/src/commonMain/kotlin/{basePackage}/
├── stateProperties/                     # NEW directory
│   ├── {NodeName}StateProperties.kt     # Per-node state property objects
│   └── ...
├── processingLogic/                     # EXISTING directory
│   ├── {NodeName}ProcessLogic.kt        # Updated with imports
│   └── ...
└── generated/                           # EXISTING directory
    ├── {Name}Flow.kt                    # Updated: delegates state from stateProperties
    └── ...
```

**Structure Decision**: Follows established multi-module KMP structure. New `StatePropertiesGenerator` lives alongside existing generators in `kotlinCompiler`. Generated `stateProperties/` sub-package parallels the existing `processingLogic/` sub-package pattern — both are user-editable stub directories with don't-overwrite semantics.

## Complexity Tracking

No violations. All changes follow existing patterns established by `ProcessingLogicStubGenerator` and `RuntimeFlowGenerator`.
