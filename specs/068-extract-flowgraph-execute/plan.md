# Implementation Plan: Extract flowGraph-execute Module

**Branch**: `068-extract-flowgraph-execute` | **Date**: 2026-04-08 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/068-extract-flowgraph-execute/spec.md`

## Summary

Extract the fourth vertical-slice module (flowGraph-execute) from circuitSimulator and graphEditor as Phase B Step 4 of the migration plan defined in feature 064. Move 5 files from circuitSimulator (RuntimeSession, DataFlowAnimationController, DataFlowDebugger, ConnectionAnimation, CircuitSimulator) and 1 file from graphEditor (ModuleSessionFactory) into a new independently buildable KMP module. Research (R1) confirms all 6 files have clean dependency boundaries — ModuleSessionFactory's imports are fbpDsl (DynamicPipelineController), flowGraph-inspect (NodeDefinitionRegistry), and circuitSimulator (RuntimeSession, which moves together). The module boundary is FBP-native: a coarse-grained CodeNode with 2 inputs (`flowGraphModel`, `nodeDescriptors`) and 3 outputs (`executionState`, `animations`, `debugSnapshots`). Uses `In2AnyOut3Runtime` with `anyInput` mode and `ProcessResult3` for selective multi-output. No service interfaces — data flow through ports only. RuntimePreviewPanel.kt stays in graphEditor per the Compose UI principle. circuitSimulator is fully absorbed. Total: 6 file moves, call site updates across 4 graphEditor files + idePlugin, 1 CodeNode (2-in/3-out, anyInput), architecture wiring.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP - Kotlin Multiplatform)
**Primary Dependencies**: fbpDsl (core FBP domain model, DynamicPipelineController, ModuleController), flowGraph-inspect (NodeDefinitionRegistry for ModuleSessionFactory), kotlinx-coroutines 1.8.0
**Storage**: N/A — in-memory runtime state (StateFlow for execution state, animations, debug snapshots)
**Testing**: `./gradlew :graphEditor:jvmTest :circuitSimulator:jvmTest` (kotlin.test + JUnit 5); new module: `./gradlew :flowGraph-execute:jvmTest`
**Target Platform**: JVM Desktop (macOS/Linux/Windows); KMP module structure for future iOS parity
**Project Type**: KMP multi-module (adding flowGraph-execute as the fourth vertical-slice module)
**Performance Goals**: N/A — structural refactor, no new runtime behavior
**Constraints**: All existing characterization tests must pass at every intermediate step (Strangler Fig invariant); module must depend only on fbpDsl and flowGraph-inspect (no cycles); KMP-first module structure required
**Scale/Scope**: 6 files extracted (5 from circuitSimulator ~582 lines, 1 from graphEditor ~258 lines), call sites migrated in 4 graphEditor files + idePlugin build, 1 CodeNode (2-in/3-out, anyInput) + TDD tests, architecture.flow.kt wiring

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Pre-Research Check

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality First | PASS | Extraction enforces single responsibility per module. FBP-native boundary makes dependencies explicit. |
| II. Test-Driven Development | PASS | CodeNode tests written before implementation (TDD). RuntimeSessionCharacterizationTest provides regression safety. All tests pass at every intermediate step. |
| III. User Experience Consistency | PASS | No user-facing changes. Pure structural refactor preserves all existing behavior. |
| IV. Performance Requirements | PASS | No runtime performance impact. No new code paths, only reorganization. |
| V. Observability & Debugging | PASS | CodeNode ports make data flow visible and inspectable in the architecture FlowGraph. |
| Licensing | PASS | No new dependencies. flowGraph-execute uses only fbpDsl (Apache 2.0) and flowGraph-inspect (Apache 2.0). |
| Refactoring Specs | PASS | Constitution explicitly permits refactoring specs: "acceptance criteria center on behavior unchanged, tests green, architecture improved." |

### Post-Design Re-Check

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality First | PASS | FBP-native data flow boundary gives the module a single, well-defined contract (2 input ports, 3 output ports). 6 files have clear cohesion around runtime execution. |
| II. Test-Driven Development | PASS | CodeNode tests written first (TDD) cover port signatures, runtime type, data flow through channels, and boundary conditions. |
| III. User Experience Consistency | PASS | No UI changes. |
| IV. Performance Requirements | PASS | No runtime changes. CodeNode channel wiring adds negligible overhead. |
| V. Observability & Debugging | PASS | FBP-native boundary makes data flow visible in architecture FlowGraph. |

No gate violations. No complexity tracking needed.

## Project Structure

### Documentation (this feature)

```text
specs/068-extract-flowgraph-execute/
├── plan.md              # This file
├── spec.md              # Feature specification
├── research.md          # Research decisions (R1-R8)
├── quickstart.md        # Validation scenarios
├── checklists/
│   └── requirements.md  # Spec quality checklist
└── tasks.md             # Task list (created by /speckit.tasks)
```

### Source Code (repository root)

```text
# New module: flowGraph-execute
flowGraph-execute/
├── build.gradle.kts                                                        # KMP module config, depends on :fbpDsl, :flowGraph-inspect
├── src/
│   ├── commonMain/kotlin/io/codenode/flowgraphexecute/
│   │   ├── RuntimeSession.kt                                              # Moved from circuitSimulator (kotlinx-coroutines only)
│   │   ├── DataFlowAnimationController.kt                                 # Moved from circuitSimulator (kotlinx-coroutines only)
│   │   ├── DataFlowDebugger.kt                                            # Moved from circuitSimulator (kotlinx-coroutines only)
│   │   ├── ConnectionAnimation.kt                                         # Moved from circuitSimulator (pure data class)
│   │   └── CircuitSimulator.kt                                            # Moved from circuitSimulator (placeholder stub)
│   ├── jvmMain/kotlin/io/codenode/flowgraphexecute/
│   │   ├── ModuleSessionFactory.kt                                        # Moved from graphEditor (java.lang.reflect, java.io.File)
│   │   └── node/
│   │       └── FlowGraphExecuteCodeNode.kt                                # NEW: CodeNode wrapping execute functionality
│   └── jvmTest/kotlin/io/codenode/flowgraphexecute/
│       ├── node/
│       │   └── FlowGraphExecuteCodeNodeTest.kt                            # NEW: TDD tests for CodeNode
│       └── characterization/
│           └── RuntimeSessionCharacterizationTest.kt                      # Migrated from circuitSimulator

# Modified: graphEditor depends on flowGraph-execute (replaces circuitSimulator)
graphEditor/build.gradle.kts                                                # Add implementation(project(":flowGraph-execute")), remove :circuitSimulator

# Modified: idePlugin depends on flowGraph-execute (replaces circuitSimulator)
idePlugin/build.gradle.kts                                                  # Replace :circuitSimulator with :flowGraph-execute

# Modified: settings.gradle.kts
settings.gradle.kts                                                         # Add include("flowGraph-execute")

# Modified: architecture.flow.kt
graphEditor/architecture.flow.kt                                            # Populate execute GraphNode with child codeNode
```

**Structure Decision**: KMP multi-module following the same pattern as flowGraph-inspect (feature 067). New package root `io.codenode.flowgraphexecute`. The 5 circuitSimulator files go in commonMain (they use only kotlinx-coroutines — no JVM-specific APIs). ModuleSessionFactory goes in jvmMain (uses java.lang.reflect and java.io.File). CodeNode and tests in jvmMain/jvmTest.
