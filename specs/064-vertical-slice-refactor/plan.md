# Implementation Plan: Vertical Slice Refactor

**Branch**: `064-vertical-slice-refactor` | **Date**: 2026-04-04 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/064-vertical-slice-refactor/spec.md`

## Summary

Produce the Phase A planning artifacts needed to decompose three tightly-coupled modules — graphEditor (77 files), kotlinCompiler (41 main files), and circuitSimulator (5 files) — into six vertical-slice Gradle modules plus the graphEditor composition root (source + sink). Total: ~120 source files to audit and classify across seven responsibility buckets (types, compose, persist, execute, generate, inspect, root). Deliverables are: (1) a file-by-file audit cataloging every source file across all three modules into responsibility buckets with cross-bucket dependency mapping, (2) characterization tests pinning current behavior at every identified seam, (3) a migration map defining module boundaries, public APIs as Kotlin interfaces, and a safe extraction order, and (4) `architecture.flow.kt` — an architecture FlowGraph with eight GraphNode containers (six workflow modules + graphEditor-source + graphEditor-sink) and 19 connections forming a validated DAG. This FlowGraph serves as both the target blueprint and the scaffold that Phase B features will progressively populate with real executable CodeNodes. No production code is extracted in this feature.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP - Kotlin Multiplatform)
**Primary Dependencies**: Compose Desktop 1.7.3 (UI), kotlinx-coroutines 1.8.0, kotlinx-serialization 1.6.0, lifecycle-viewmodel-compose 2.8.0
**Storage**: N/A (deliverables are documentation and test files)
**Testing**: `./gradlew :graphEditor:jvmTest :kotlinCompiler:jvmTest` (kotlin.test + JUnit 5 + kotlinx-coroutines-test)
**Target Platform**: JVM Desktop (macOS/Linux/Windows)
**Project Type**: KMP multi-module (graphEditor, fbpDsl, kotlinCompiler, circuitSimulator modules)
**Performance Goals**: N/A — this feature produces planning artifacts, not runtime code
**Constraints**: Characterization tests must run without Compose UI framework (test ViewModel/state layers directly); all tests must pass on unmodified codebase before any extraction begins
**Scale/Scope**: 77 source files in graphEditor, 41 in kotlinCompiler, 5 in circuitSimulator (~120 total), targeting 6 vertical-slice modules + composition root (source/sink). Three existing modules dissolve into six new ones; graphEditor becomes a thin shell.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Pre-Research Check

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality First | PASS | Audit and migration map enforce single responsibility per module. Characterization tests ensure maintainability through extraction. |
| II. Test-Driven Development | PASS | Core deliverable is a characterization test suite — tests written before any extraction begins. Aligns with TDD mandate. |
| III. User Experience Consistency | PASS | No user-facing changes. Refactoring preserves all existing behavior. |
| IV. Performance Requirements | PASS | No runtime performance impact. Characterization tests may include performance-sensitive seams. |
| V. Observability & Debugging | PASS | Migration map documents all seams, making the architecture more debuggable. Architecture FlowGraph makes module dependencies visually inspectable. |
| Licensing | PASS | No new dependencies. All deliverables are project documentation and test code. |
| Refactoring Specs | PASS | Constitution explicitly permits refactoring specs: "acceptance criteria center on behavior unchanged, tests green, architecture improved." |

### Post-Design Re-Check

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality First | PASS | Six-module vertical-slice decomposition gives each module a single clear workflow responsibility. flowGraph-types eliminates cyclic dependencies found in the original 5-module partition. |
| II. Test-Driven Development | PASS | Characterization tests cover graph ops, runtime execution, ViewModel integration, code generation, runtime session management, and architecture FlowGraph structural invariants — seven distinct test categories. |
| III. User Experience Consistency | PASS | No UI changes in this feature. |
| IV. Performance Requirements | PASS | No runtime changes. |
| V. Observability & Debugging | PASS | Audit documents make implicit dependencies explicit. Architecture FlowGraph provides visual architecture representation. |

No gate violations. No complexity tracking needed.

## Project Structure

### Documentation (this feature)

```text
specs/064-vertical-slice-refactor/
├── plan.md              # This file
├── spec.md              # Feature specification
├── research.md          # Phase 0: Module boundaries, seam analysis, extraction order
├── quickstart.md        # Phase 1: Validation scenarios for each deliverable
├── checklists/
│   └── requirements.md  # Spec quality checklist
└── tasks.md             # Phase 2 output (created by /speckit.tasks)
```

### Source Code (repository root)

```text
# Audit output (documentation files)
graphEditor/ARCHITECTURE.md              # graphEditor file audit with bucket assignments and seam map
kotlinCompiler/ARCHITECTURE.md           # kotlinCompiler file audit with bucket assignments
circuitSimulator/ARCHITECTURE.md         # circuitSimulator file audit with bucket assignments

# Characterization tests (test files)
graphEditor/src/jvmTest/kotlin/characterization/
├── GraphDataOpsCharacterizationTest.kt       # Node creation, connection, validation, serialization
├── RuntimeExecutionCharacterizationTest.kt   # Graph execution, state transitions
├── ViewModelCharacterizationTest.kt          # ViewModel state after graph mutations
├── SerializationRoundTripCharacterizationTest.kt  # .flow.kt round-trip fidelity
└── ArchitectureFlowKtsTest.kt                # Architecture FlowGraph structural invariants

kotlinCompiler/src/jvmTest/kotlin/characterization/
├── CodeGenerationCharacterizationTest.kt     # Generator output for known FlowGraph inputs
└── FlowKtGeneratorCharacterizationTest.kt    # .flow.kt generation round-trip

circuitSimulator/src/commonTest/kotlin/characterization/
└── RuntimeSessionCharacterizationTest.kt     # Session lifecycle, animation, debugging

# Migration map (documentation file)
MIGRATION.md                             # Module boundaries, APIs, extraction order (repo root — spans all modules)

# Architecture FlowGraph (flow file)
graphEditor/architecture.flow.kt         # Target architecture as a FlowGraph — 8 GraphNode containers, 19 connections, DAG

# Existing files (read-only, referenced for audit)
graphEditor/src/jvmMain/kotlin/          # 77 files across 12 subdirectories
kotlinCompiler/src/commonMain/kotlin/    # 33 generator/template/validator files
kotlinCompiler/src/jvmMain/kotlin/       # 8 JVM-specific files (tools, contract tests)
circuitSimulator/src/commonMain/kotlin/  # 5 runtime session/animation/debug files
```

**Structure Decision**: This feature adds documentation files and characterization tests across three modules. No new Gradle modules are created — that is Phase B work. Each module gets its own ARCHITECTURE.md for the file audit. The migration map (MIGRATION.md) lives at the repo root since it spans all three source modules. Characterization tests go in dedicated `characterization/` test packages.

### Target Architecture (documented in MIGRATION.md and architecture.flow.kt)

```text
# Future state — six vertical-slice modules + composition root
fbpDsl/                    # Shared vocabulary (FlowGraph, Node, Port, Connection, IP) — unchanged
flowGraph-types/           # IP type lifecycle: discovery, registry, repository, file generation, migration (9 files)
flowGraph-compose/         # Canvas interaction: add/connect/validate/configure nodes (10 files)
flowGraph-persist/         # Save/load: FlowGraphSerializer, FlowKtParser, file I/O (8 files)
flowGraph-execute/         # Runtime: dynamic pipeline, execution control, data flow animation (7 files)
flowGraph-generate/        # Code generation: all kotlinCompiler generators, module save, CodeNode codegen (46 files)
flowGraph-inspect/         # Discovery: node palette, filesystem scanning, CodeNode text editor (13 files)
graphEditor/               # Composition root: Compose UI, ViewModels, DI wiring (27 files)
                           #   source = ViewModel command actions (user intent → workflow modules)
                           #   sink = reactive Compose UI state (workflow modules → display)
```

## Key Technical Decisions

### 1. Six Vertical Slices + Source/Sink Composition Root

The decomposition follows user workflows (types, compose, persist, execute, generate, inspect) rather than technical layers. The original 5-module partition had cyclic dependencies (inspect↔persist, inspect↔generate) caused by IP type concerns scattered across three modules. Extracting flowGraph-types (9 files) consolidated IP type lifecycle into one module and eliminated both cycles, producing a validated DAG.

The graphEditor composition root is modeled as two FBP-aligned nodes: graphEditor-source (ViewModel command actions dispatching user intent) and graphEditor-sink (reactive Compose UI state flowing in from workflow modules). This reflects pure FBP bidirectional data flow while maintaining the DAG property.

### 2. Architecture FlowGraph as Executable Scaffold

`architecture.flow.kt` is not just documentation — it is the scaffold for Phase B extraction. It contains eight GraphNode containers with typed input/output ports and 19 connections. During Phase B, each container will be populated with a coarse-grained CodeNode whose port signatures match the container's ports. After all Phase B features, the FlowGraph becomes fully executable — the actual wiring that runs the application.

### 3. Characterization Tests Target ViewModel/State Layer

Tests pin behavior at the ViewModel and state management layer — not at the Compose UI layer (which requires a Compose test framework) and not at the raw function level (too granular). ViewModels are the public API that the UI consumes, so if ViewModel behavior is preserved, UI behavior is preserved. An additional test suite (ArchitectureFlowKtsTest) validates the architecture FlowGraph's structural invariants.

### 4. Data-Oriented Port Naming

All ports in architecture.flow.kt use names describing the data that flows through them (nodeDescriptors, ipTypeMetadata, flowGraphModel, graphState, etc.) rather than service-oriented names (nodeRegistry, ipTypeRegistry, etc.). This makes the FlowGraph self-documenting about what information moves between modules.

### 5. Extraction Order: types → persist → inspect → execute → generate → compose

Each extraction step leaves the application fully functional. The order prioritizes independence:
1. **flowGraph-types** (first): Consolidates IP type lifecycle. No dependencies on other workflow modules.
2. **flowGraph-persist** (second): Serialization is self-contained. Depends on types (already extracted).
3. **flowGraph-inspect** (third): Discovery/registry is read-only. Depends on types (already extracted).
4. **flowGraph-execute** (fourth): Runtime pipeline has clear boundaries. Absorbs circuitSimulator.
5. **flowGraph-generate** (fifth): Code generation depends on persist and inspect (both extracted). Absorbs kotlinCompiler.
6. **flowGraph-compose** (last): Most tightly coupled to UI. Extract after all dependencies are stable.

### 6. Strangler Fig Pattern with Phase A/B/C

Phase A (this feature) produces planning artifacts. Phase B (seven subsequent features) extracts modules one at a time, each wrapping as a coarse-grained CodeNode. Phase C (opportunistic) deepens granularity within modules. The invariant: the application runs correctly after every step.

### 7. fbpDsl as Shared Vocabulary, Not Shared Layer

The fbpDsl module contains the core domain types (FlowGraph, Node, Port, Connection, InformationPacket). All vertical slices depend on fbpDsl for type definitions but own their own behavior. No vertical slice depends on another — they communicate through the composition root.
