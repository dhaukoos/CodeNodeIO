# Implementation Plan: Migrate Module Runtimes

**Branch**: `052-migrate-module-runtimes` | **Date**: 2026-03-16 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/052-migrate-module-runtimes/spec.md`

## Summary

Create self-contained `CodeNodeDefinition` objects for all 12 nodes across 4 modules (StopWatch, UserProfiles, GeoLocations, Addresses), register them in the `NodeDefinitionRegistry`, and enable the dynamic pipeline path so these modules run via `DynamicPipelineController` instead of their generated Controller/Flow code. This follows the exact pattern established by EdgeArtFilter in features 050/051 — each node becomes a Kotlin `object` implementing `CodeNodeDefinition` with embedded processing logic and a `createRuntime()` factory method.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP - Kotlin Multiplatform)
**Primary Dependencies**: Compose Desktop 1.7.3, kotlinx-coroutines 1.8.0 (channels, StateFlow, select), kotlinx-serialization 1.6.0, lifecycle-viewmodel-compose 2.8.0, Room 2.8.4 (KMP), KSP 2.1.21-2.0.1, SQLite Bundled 2.6.2, Koin 4.0.0
**Storage**: Room (KMP) with BundledSQLiteDriver for entity modules (UserProfiles, GeoLocations, Addresses); N/A for StopWatch
**Testing**: kotlin.test, kotlinx-coroutines-test (runTest), manual quickstart verification
**Target Platform**: JVM Desktop (Compose Desktop)
**Project Type**: KMP multi-module (fbpDsl, graphEditor, circuitSimulator, StopWatch, UserProfiles, GeoLocations, Addresses, EdgeArtFilter, persistence, nodes)
**Performance Goals**: Pipeline startup within same time envelope as generated controllers (~500ms)
**Constraints**: Must not modify ViewModels or UI composables; behavioral equivalence required (FR-004); DAO dependencies via Koin; entity modules pre-started in factory require lifecycle adaptation
**Scale/Scope**: 12 new CodeNodeDefinitions across 4 modules (3 per module)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Status | Notes |
|------|--------|-------|
| **Licensing (Critical)** | PASS | No new dependencies; all existing deps are Apache 2.0/MIT |
| **I. Code Quality First** | PASS | Self-contained CodeNode pattern consolidates fragmented code (separate Flow + ProcessLogic + Controller) into single-responsibility objects |
| **II. Test-Driven Development** | PASS | Spec does not explicitly request tests; existing test suite must pass (SC-005); manual quickstart verification covers acceptance scenarios |
| **III. User Experience Consistency** | PASS | No UI changes — identical behavior to pre-migration (FR-004, FR-010) |
| **IV. Performance Requirements** | PASS | Dynamic pipeline reuses same runtime classes; startup latency matches generated approach (SC-002) |
| **V. Observability & Debugging** | PASS | DynamicPipelineController already supports emission/value observers; validation errors surface via StateFlow |

No violations. All gates pass.

**Post-Phase 1 Re-check**: All gates still pass. No new external dependencies. Each module's `nodes/` directory follows the EdgeArtFilter pattern. Entity module CodeNodes access DAOs via existing Koin infrastructure.

## Project Structure

### Documentation (this feature)

```text
specs/052-migrate-module-runtimes/
├── plan.md              # This file
├── spec.md              # Feature specification
├── research.md          # Phase 0: migration pattern research
├── data-model.md        # Phase 1: CodeNodeDefinition entities
├── quickstart.md        # Phase 1: step-by-step verification guide
├── contracts/           # Phase 1: node definition contracts
│   └── code-node-migration.md
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (repository root)

```text
# StopWatch module — new CodeNodeDefinition files
StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/nodes/
├── TimerEmitterCodeNode.kt       # SOURCE (0→2): SourceOut2Runtime
├── TimeIncrementerCodeNode.kt    # PROCESSOR (2→2): In2Out2Runtime
└── DisplayReceiverCodeNode.kt    # SINK (2→0): SinkIn2AnyRuntime

# UserProfiles module — new CodeNodeDefinition files
UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/nodes/
├── UserProfileCUDCodeNode.kt     # SOURCE (0→3): SourceOut3Runtime
├── UserProfileRepositoryCodeNode.kt  # PROCESSOR (3→2): In3AnyOut2Runtime
└── UserProfilesDisplayCodeNode.kt    # SINK (2→0): SinkIn2Runtime

# GeoLocations module — new CodeNodeDefinition files
GeoLocations/src/commonMain/kotlin/io/codenode/geolocations/nodes/
├── GeoLocationCUDCodeNode.kt     # SOURCE (0→3): SourceOut3Runtime
├── GeoLocationRepositoryCodeNode.kt  # PROCESSOR (3→2): In3AnyOut2Runtime
└── GeoLocationsDisplayCodeNode.kt    # SINK (2→0): SinkIn2Runtime

# Addresses module — new CodeNodeDefinition files
Addresses/src/commonMain/kotlin/io/codenode/addresses/nodes/
├── AddressCUDCodeNode.kt         # SOURCE (0→3): SourceOut3Runtime
├── AddressRepositoryCodeNode.kt  # PROCESSOR (3→2): In3AnyOut2Runtime
└── AddressesDisplayCodeNode.kt   # SINK (2→0): SinkIn2Runtime

# Modified files
graphEditor/src/jvmMain/kotlin/ui/ModuleSessionFactory.kt  # Dynamic session for all modules
graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/state/NodeDefinitionRegistry.kt  # Register new nodes

# ServiceLoader registration (per module)
StopWatch/src/jvmMain/resources/META-INF/services/io.codenode.fbpdsl.runtime.CodeNodeDefinition
UserProfiles/src/jvmMain/resources/META-INF/services/io.codenode.fbpdsl.runtime.CodeNodeDefinition
GeoLocations/src/jvmMain/resources/META-INF/services/io.codenode.fbpdsl.runtime.CodeNodeDefinition
Addresses/src/jvmMain/resources/META-INF/services/io.codenode.fbpdsl.runtime.CodeNodeDefinition
```

**Structure Decision**: Each module gets a `nodes/` directory containing CodeNodeDefinition objects, mirroring the EdgeArtFilter pattern. ServiceLoader META-INF files enable automatic discovery. No new modules created.

## Complexity Tracking

> No constitution violations. All gates pass without justification needed.
