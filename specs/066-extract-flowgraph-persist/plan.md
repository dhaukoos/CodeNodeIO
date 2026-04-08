# Implementation Plan: Extract flowGraph-persist Module

**Branch**: `066-extract-flowgraph-persist` | **Date**: 2026-04-07 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/066-extract-flowgraph-persist/spec.md`

## Summary

Extract the second vertical-slice module (flowGraph-persist) from graphEditor as Phase B Step 2 of the migration plan defined in feature 064. Move 6 serialization/template files (FlowGraphSerializer, FlowKtParser, GraphNodeTemplateSerializer, GraphNodeTemplateMeta, GraphNodeTemplateRegistry, GraphNodeTemplateInstantiator) into a new independently buildable KMP module. Research (R1) determined that ViewSynchronizer and TextualView belong in the compose slice, not persist — reducing the extraction from 8 to 6 files. The module boundary is FBP-native: a coarse-grained CodeNode with 2 inputs (`flowGraphModel`, `ipTypeMetadata`) and 3 outputs (`serializedOutput`, `loadedFlowGraph`, `graphNodeTemplates`). Uses `In2AnyOut3Runtime` with `anyInput` mode. No service interfaces — data flow through ports only. Update call sites to consume from the new module, TDD-test the CodeNode contract, populate the flowGraph-persist GraphNode in architecture.flow.kt with the live CodeNode. Total: 6 file moves, call site updates, 1 CodeNode (2-in/3-out, anyInput), architecture wiring.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP - Kotlin Multiplatform)
**Primary Dependencies**: fbpDsl (core FBP domain model), flowGraph-types (IPTypeRegistry for template loading), kotlinx-coroutines 1.8.0, kotlinx-serialization 1.6.2
**Storage**: Filesystem (GraphNode template .flow.kts files at three-tier locations)
**Testing**: `./gradlew :graphEditor:jvmTest :kotlinCompiler:jvmTest :circuitSimulator:jvmTest` (kotlin.test + JUnit 5); new module: `./gradlew :flowGraph-persist:jvmTest`
**Target Platform**: JVM Desktop (macOS/Linux/Windows); KMP module structure for future iOS parity
**Project Type**: KMP multi-module (adding flowGraph-persist as the second vertical-slice module)
**Performance Goals**: N/A — structural refactor, no new runtime behavior
**Constraints**: All existing characterization tests must pass at every intermediate step (Strangler Fig invariant); module must depend only on fbpDsl and flowGraph-types (no cycles); KMP-first module structure required
**Scale/Scope**: 6 files extracted from graphEditor (~2,056 lines), call sites migrated to data flow consumption, 1 CodeNode (2-in/3-out, anyInput) + TDD tests, architecture.flow.kt wiring (populate persist GraphNode with child CodeNode)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Pre-Research Check

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality First | PASS | Extraction enforces single responsibility per module. FBP-native boundary makes dependencies explicit. |
| II. Test-Driven Development | PASS | CodeNode tests written before implementation (TDD). Characterization tests provide regression safety net. All tests pass at every intermediate step. |
| III. User Experience Consistency | PASS | No user-facing changes. Pure structural refactor preserves all existing behavior. |
| IV. Performance Requirements | PASS | No runtime performance impact. No new code paths, only reorganization. |
| V. Observability & Debugging | PASS | CodeNode ports make data flow visible and inspectable in the architecture FlowGraph. |
| Licensing | PASS | No new dependencies. flowGraph-persist uses only fbpDsl (Apache 2.0), flowGraph-types (Apache 2.0), and Kotlin stdlib. |
| Refactoring Specs | PASS | Constitution explicitly permits refactoring specs: "acceptance criteria center on behavior unchanged, tests green, architecture improved." |

### Post-Design Re-Check

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality First | PASS | FBP-native data flow boundary gives the module a single, well-defined contract (2 input ports, 3 output ports). Research R1 reduced scope from 8→6 files by correctly excluding UI/compose concerns. |
| II. Test-Driven Development | PASS | CodeNode tests written first (TDD) cover port signatures, runtime type, data flow through channels, command processing, and boundary conditions. |
| III. User Experience Consistency | PASS | No UI changes. |
| IV. Performance Requirements | PASS | No runtime changes. CodeNode channel wiring adds negligible overhead. |
| V. Observability & Debugging | PASS | FBP-native boundary makes data flow visible in architecture FlowGraph. |

No gate violations. No complexity tracking needed.

## Project Structure

### Documentation (this feature)

```text
specs/066-extract-flowgraph-persist/
├── plan.md              # This file
├── spec.md              # Feature specification
├── research.md          # Research decisions (R1-R7)
├── quickstart.md        # Validation scenarios
├── checklists/
│   └── requirements.md  # Spec quality checklist
└── tasks.md             # Task list (created by /speckit.tasks)
```

### Source Code (repository root)

```text
# New module: flowGraph-persist
flowGraph-persist/
├── build.gradle.kts                                                        # KMP module config, depends on :fbpDsl, :flowGraph-types
├── src/
│   ├── commonMain/kotlin/io/codenode/flowgraphpersist/
│   │   └── model/
│   │       └── GraphNodeTemplateMeta.kt                                    # Moved from graphEditor (pure Kotlin data class)
│   ├── jvmMain/kotlin/io/codenode/flowgraphpersist/
│   │   ├── serialization/
│   │   │   ├── FlowGraphSerializer.kt                                      # Moved from graphEditor (java.io.File)
│   │   │   ├── FlowKtParser.kt                                             # Moved from graphEditor (kept in jvmMain with serializer)
│   │   │   └── GraphNodeTemplateSerializer.kt                              # Moved from graphEditor (java.io.File)
│   │   ├── state/
│   │   │   ├── GraphNodeTemplateRegistry.kt                                # Moved from graphEditor (java.io.File)
│   │   │   └── GraphNodeTemplateInstantiator.kt                            # Moved from graphEditor (java.io.File)
│   │   └── node/
│   │       └── FlowGraphPersistCodeNode.kt                                 # NEW: CodeNode wrapping persist functionality
│   └── jvmTest/kotlin/io/codenode/flowgraphpersist/
│       └── node/
│           └── FlowGraphPersistCodeNodeTest.kt                             # NEW: TDD tests for CodeNode

# Modified: graphEditor depends on flowGraph-persist
graphEditor/build.gradle.kts                                                # Add implementation(project(":flowGraph-persist"))

# Modified: settings.gradle.kts
settings.gradle.kts                                                         # Add include("flowGraph-persist")

# Modified: architecture.flow.kt
graphEditor/architecture.flow.kt                                            # Populate persist GraphNode with child codeNode
```

**Structure Decision**: KMP multi-module following the same pattern as flowGraph-types (feature 065). New package root `io.codenode.flowgraphpersist`. One file in commonMain (GraphNodeTemplateMeta — pure data class), remaining 5 + CodeNode in jvmMain (JVM filesystem dependencies).
