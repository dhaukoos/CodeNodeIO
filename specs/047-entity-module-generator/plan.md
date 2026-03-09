# Implementation Plan: Generalize Entity Repository Module Creation

**Branch**: `047-entity-module-generator` | **Date**: 2026-03-09 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/047-entity-module-generator/spec.md`

## Summary

Generalize the existing single-node "Create Repository Node" workflow into a full "Create Repository Module" pipeline. This extends the code-generation infrastructure (kotlinCompiler generators + graphEditor ModuleSaveService) to produce complete entity CRUD modules — including three node types ({Entity}CUD source, {Entity}Repository processor, {Entity}sDisplay sink), their FlowGraph wiring, UI composables (list view, form, row), ViewModel with CRUD methods, processing logic stubs, and Koin DI wiring — all from a single button click in the IP Type Properties panel.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + Compose Desktop 1.7.3
**Primary Dependencies**: kotlinx-coroutines 1.8.0, kotlinx-serialization 1.6.0, lifecycle-viewmodel-compose 2.8.0, Room 2.8.4 (KMP), KSP 2.1.21-2.0.1, Koin 4.0.0
**Storage**: Room (KMP) with BundledSQLiteDriver — all persistence components in shared `persistence` module (feature 046 architecture)
**Testing**: kotlin.test (commonTest), manual validation via graphEditor
**Target Platform**: JVM (graphEditor), Android + iOS (KMPMobileApp)
**Project Type**: KMP multi-module
**Performance Goals**: Module generation completes in < 5 seconds
**Constraints**: Generated code must compile without manual edits; persistence files go to shared `persistence` module
**Scale/Scope**: ~8 generator methods to add/modify, ~15 files touched across kotlinCompiler and graphEditor modules

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality First | PASS | Generators produce well-structured, documented code following UserProfiles patterns |
| II. Test-Driven Development | PASS | Generator unit tests will validate output; GeoLocation used for integration validation |
| III. User Experience Consistency | PASS | Generated UI follows established UserProfiles Material3 patterns |
| IV. Performance Requirements | PASS | Code generation is a one-time operation; < 5s target is easily achievable |
| V. Observability & Debugging | PASS | Generated modules follow existing logging/state patterns |
| Licensing | PASS | No new dependencies; all existing deps are Apache 2.0/MIT |

No violations. No complexity tracking needed.

## Project Structure

### Documentation (this feature)

```text
specs/047-entity-module-generator/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
└── tasks.md             # Phase 2 output (via /speckit.tasks)
```

### Source Code (repository root)

```text
kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/
├── RepositoryCodeGenerator.kt          # MODIFY — add CUD/Display node generators
├── FlowKtGenerator.kt                  # EXISTING — used as-is for .flow.kt generation
├── RuntimeViewModelGenerator.kt        # MODIFY — add CRUD methods to generated ViewModel
├── ProcessingLogicStubGenerator.kt     # EXISTING — already handles all node types
├── UserInterfaceStubGenerator.kt       # MODIFY — replace with entity-aware multi-file generator
├── EntityModuleGenerator.kt            # NEW — orchestrates full module generation
├── EntityCUDGenerator.kt               # NEW — generates {Entity}CUD.kt source node stub
├── EntityDisplayGenerator.kt           # NEW — generates {Entity}sDisplay.kt sink node stub
├── EntityUIGenerator.kt                # NEW — generates all 3 UI files ({Entity}s.kt, AddUpdate{Entity}.kt, {Entity}Row.kt)
└── EntityPersistenceGenerator.kt       # NEW — generates {Entity}sPersistence.kt Koin module

kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/
├── EntityModuleGeneratorTest.kt        # NEW — tests for orchestrator
├── EntityCUDGeneratorTest.kt           # NEW — tests for CUD source generation
├── EntityDisplayGeneratorTest.kt       # NEW — tests for Display sink generation
├── EntityUIGeneratorTest.kt            # NEW — tests for UI file generation
└── EntityPersistenceGeneratorTest.kt   # NEW — tests for Koin module generation

graphEditor/src/jvmMain/kotlin/
├── repository/CustomNodeDefinition.kt  # MODIFY — add createCUD() and createDisplay() factory methods
├── save/ModuleSaveService.kt           # MODIFY — integrate EntityModuleGenerator, move persistence to shared module
└── ui/PropertiesPanel.kt               # MODIFY — rename button, wire to module generation pipeline
```

**Structure Decision**: All new generators go in `kotlinCompiler/generator/` following the established pattern. The graphEditor wires the UI trigger and orchestrates file writing via ModuleSaveService. Persistence files are written to the shared `persistence/` module (not inside the generated module).

## Phase 0: Research

See [research.md](research.md)

## Phase 1: Design

See [data-model.md](data-model.md) and [quickstart.md](quickstart.md)
