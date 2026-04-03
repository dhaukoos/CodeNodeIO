# Implementation Plan: Vertical Slice Refactor

**Branch**: `064-vertical-slice-refactor` | **Date**: 2026-04-02 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/064-vertical-slice-refactor/spec.md`

## Summary

Produce the planning artifacts needed to decompose three tightly-coupled modules — graphEditor (77 files), kotlinCompiler (41 main files), and circuitSimulator (5 files) — into five vertical-slice Gradle modules organized by user workflow. Total: ~123 source files to audit and classify. Deliverables are: (1) a file-by-file audit cataloging every source file across all three modules into responsibility buckets with cross-bucket dependency mapping, (2) characterization tests pinning current behavior at every identified seam, (3) a migration map defining module boundaries, public APIs as Kotlin interfaces, and a safe extraction order, and (4) a meta-FlowGraph `.flow.kts` file visualizing the target architecture in the project's own FBP paradigm. No production code is extracted in this feature — the deliverables are the safety net and roadmap for future extraction work.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP - Kotlin Multiplatform)
**Primary Dependencies**: Compose Desktop 1.7.3 (UI), kotlinx-coroutines 1.8.0, kotlinx-serialization 1.6.0, lifecycle-viewmodel-compose 2.8.0
**Storage**: N/A (deliverables are documentation and test files)
**Testing**: `./gradlew :graphEditor:jvmTest :kotlinCompiler:jvmTest` (kotlin.test + JUnit 5 + kotlinx-coroutines-test)
**Target Platform**: JVM Desktop (macOS/Linux/Windows)
**Project Type**: KMP multi-module (graphEditor, fbpDsl, kotlinCompiler, circuitSimulator modules)
**Performance Goals**: N/A — this feature produces planning artifacts, not runtime code
**Constraints**: Characterization tests must run without Compose UI framework (test ViewModel/state layers directly); all tests must pass on unmodified codebase before any extraction begins
**Scale/Scope**: 77 source files in graphEditor, 41 in kotlinCompiler, 5 in circuitSimulator (~123 total), targeting 5 vertical-slice extraction modules. Three existing modules dissolve into five new ones.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Pre-Research Check

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality First | PASS | Audit and migration map enforce single responsibility per module. Characterization tests ensure maintainability through extraction. |
| II. Test-Driven Development | PASS | Core deliverable is a characterization test suite — tests written before any extraction begins. Aligns with TDD mandate. |
| III. User Experience Consistency | PASS | No user-facing changes. Refactoring preserves all existing behavior. |
| IV. Performance Requirements | PASS | No runtime performance impact. Characterization tests may include performance-sensitive seams. |
| V. Observability & Debugging | PASS | Migration map documents all seams, making the architecture more debuggable. |
| Licensing | PASS | No new dependencies. All deliverables are project documentation and test code. |
| Refactoring Specs | PASS | Constitution explicitly permits refactoring specs: "acceptance criteria center on behavior unchanged, tests green, architecture improved." |

### Post-Design Re-Check

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality First | PASS | Vertical-slice decomposition gives each module a single clear workflow responsibility. |
| II. Test-Driven Development | PASS | Characterization tests cover graph ops, runtime execution, ViewModel integration, code generation, and runtime session management — five distinct seam categories. |
| III. User Experience Consistency | PASS | No UI changes in this feature. |
| IV. Performance Requirements | PASS | No runtime changes. |
| V. Observability & Debugging | PASS | Audit document makes implicit dependencies explicit. |

No gate violations. No complexity tracking needed.

## Project Structure

### Documentation (this feature)

```text
specs/064-vertical-slice-refactor/
├── plan.md              # This file
├── spec.md              # Feature specification
├── research.md          # Phase 0: Bucket definitions, seam analysis strategy
├── quickstart.md        # Phase 1: Validation scenarios for each deliverable
├── checklists/
│   └── requirements.md  # Spec quality checklist
└── tasks.md             # Phase 2 output (created by /speckit.tasks)
```

### Source Code (repository root)

```text
# Audit output (new documentation files)
graphEditor/ARCHITECTURE.md              # graphEditor file audit with bucket assignments and seam map
kotlinCompiler/ARCHITECTURE.md           # kotlinCompiler file audit with bucket assignments
circuitSimulator/ARCHITECTURE.md         # circuitSimulator file audit with bucket assignments

# Characterization tests (new test files)
graphEditor/src/jvmTest/kotlin/characterization/
├── GraphDataOpsCharacterizationTest.kt  # Node creation, connection, validation, serialization
├── RuntimeExecutionCharacterizationTest.kt  # Graph execution, state transitions
├── ViewModelCharacterizationTest.kt     # ViewModel state after graph mutations
└── SerializationRoundTripCharacterizationTest.kt  # .flow.kts round-trip fidelity

kotlinCompiler/src/jvmTest/kotlin/characterization/
├── CodeGenerationCharacterizationTest.kt  # Generator output for known FlowGraph inputs
└── FlowKtGeneratorCharacterizationTest.kt # .flow.kt generation round-trip

circuitSimulator/src/commonTest/kotlin/characterization/
└── RuntimeSessionCharacterizationTest.kt  # Session lifecycle, animation, debugging

# Migration map (new documentation file)
MIGRATION.md                             # Module boundaries, APIs, extraction order (repo root — spans all modules)

# Meta-FlowGraph (new flow file)
graphEditor/architecture.flow.kts        # Target architecture as a FlowGraph

# Existing files (read-only, referenced for audit)
graphEditor/src/jvmMain/kotlin/          # 77 files across 12 subdirectories
kotlinCompiler/src/commonMain/kotlin/    # 33 generator/template/validator files
kotlinCompiler/src/jvmMain/kotlin/       # 8 JVM-specific files (tools, contract tests)
circuitSimulator/src/commonMain/kotlin/  # 5 runtime session/animation/debug files
```

**Structure Decision**: This feature adds documentation files and characterization tests across three modules. No new Gradle modules are created — that is the work of a future extraction feature. Each module gets its own ARCHITECTURE.md for the file audit. The migration map (MIGRATION.md) lives at the repo root since it spans all three source modules. Characterization tests go in dedicated `characterization/` test packages.

### Target Architecture (documented in MIGRATION.md, not built in this feature)

```text
# Future state — what the migration map will describe
# Three existing modules (graphEditor, kotlinCompiler, circuitSimulator) dissolve into:
fbpDsl/                    # Shared vocabulary (FlowGraph, Node, Port, Connection, IP) — unchanged
flowGraph-compose/         # Canvas interaction: add/connect/validate/configure nodes
flowGraph-persist/         # Save/load: FlowGraphSerializer, FlowKtParser, file I/O
flowGraph-execute/         # Runtime: dynamic pipeline, execution control, data flow animation, runtime session
flowGraph-generate/        # Code generation: all kotlinCompiler generators, module save, CodeNode definition codegen
flowGraph-inspect/         # Discovery: node palette, IP type registry, filesystem scanning
graphEditor/               # Composition root: Compose UI, ViewModels, DI wiring (thin shell)
```

## Key Technical Decisions

### 1. Vertical Slices over Horizontal Layers

The decomposition follows user workflows (compose, persist, execute, generate, inspect) rather than technical layers (model, service, repository). Each slice owns its complete workflow end-to-end: data enters through input ports, passes through processing, exits through output ports. This means serialization lives in flowGraph-persist (not a shared "serialization layer"), runtime lives in flowGraph-execute, and the palette/discovery lives in flowGraph-inspect.

### 2. Characterization Tests Target ViewModel/State Layer

Tests pin behavior at the ViewModel and state management layer — not at the Compose UI layer (which requires a Compose test framework) and not at the raw function level (too granular). This provides the right abstraction level: ViewModels are the public API that the UI consumes, so if ViewModel behavior is preserved, UI behavior is preserved.

### 3. Audit Uses Import Analysis for Bucket Assignment

Each file's primary bucket is determined by analyzing its imports and the types it operates on. Files importing `FlowGraphSerializer`/`FlowKtParser` → persist. Files importing runtime types (`NodeRuntime`, `Channel`, `CoroutineScope`) → execute. Files importing compilation/codegen types → generate. Files importing discovery/registry types → inspect. Files that are `@Composable` or extend `ViewModel` → assess by which slice's data they render/manage.

### 4. Strangler Fig Extraction Pattern

The migration map documents a copy-delegate-delete extraction for each module. Step 1: Create the new module with interfaces. Step 2: Copy implementation, make graphEditor delegate. Step 3: Delete the copy from graphEditor. Each step must leave all characterization tests green.

### 5. fbpDsl as Shared Vocabulary, Not Shared Layer

The fbpDsl module contains the core domain types (FlowGraph, Node, Port, Connection, InformationPacket). All vertical slices depend on fbpDsl for type definitions but own their own behavior. No vertical slice depends on another — they communicate through the composition root (graphEditor).

### 6. Three Source Modules Dissolve into Five Target Modules

The refactoring scope includes all three tightly-coupled modules: graphEditor (77 files), kotlinCompiler (41 main files), and circuitSimulator (5 files). kotlinCompiler maps almost entirely to flowGraph-generate — it contains 25 generators, 8 templates, and supporting utilities for code production. circuitSimulator maps to flowGraph-execute — it contains the RuntimeSession orchestrator, DataFlowAnimationController, and DataFlowDebugger. After extraction, the kotlinCompiler and circuitSimulator Gradle modules cease to exist; their code lives in the appropriate vertical slices.
