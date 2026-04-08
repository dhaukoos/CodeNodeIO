# Implementation Plan: Extract flowGraph-inspect Module

**Branch**: `067-extract-flowgraph-inspect` | **Date**: 2026-04-08 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/067-extract-flowgraph-inspect/spec.md`

## Summary

Extract the third vertical-slice module (flowGraph-inspect) from graphEditor as Phase B Step 3 of the migration plan defined in feature 064. Move 7 non-UI files (NodeDefinitionRegistry, CodeEditorViewModel, IPPaletteViewModel, GraphNodePaletteViewModel, NodePaletteViewModel, ComposableDiscovery, DynamicPreviewDiscovery) into a new independently buildable KMP module. Research (R1) confirms all 7 files have zero circular dependency risk — only one internal cross-reference (CodeEditorViewModel→NodeDefinitionRegistry, both in the extraction set). The module boundary is FBP-native: a coarse-grained CodeNode with 2 inputs (`filesystemPaths`, `classpathEntries`) and 1 output (`nodeDescriptors`). Uses `In2AnyOut1Runtime` with `anyInput` mode. No service interfaces — data flow through ports only. Compose UI composables (CodeEditor, ColorEditor, IPPalette, NodePalette, SyntaxHighlighter) stay in graphEditor per the established principle. Total: 7 file moves, call site updates, 1 CodeNode (2-in/1-out, anyInput), architecture wiring.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP - Kotlin Multiplatform)
**Primary Dependencies**: fbpDsl (core FBP domain model), flowGraph-types (IPTypeRegistry for IPPaletteViewModel), flowGraph-persist (GraphNodeTemplateMeta for GraphNodePaletteViewModel), kotlinx-coroutines 1.8.0, androidx.lifecycle-viewmodel
**Storage**: Filesystem (scanning directories for .kt source files and compiled CodeNode definitions)
**Testing**: `./gradlew :graphEditor:jvmTest :kotlinCompiler:jvmTest :circuitSimulator:jvmTest` (kotlin.test + JUnit 5); new module: `./gradlew :flowGraph-inspect:jvmTest`
**Target Platform**: JVM Desktop (macOS/Linux/Windows); KMP module structure for future iOS parity
**Project Type**: KMP multi-module (adding flowGraph-inspect as the third vertical-slice module)
**Performance Goals**: N/A — structural refactor, no new runtime behavior
**Constraints**: All existing characterization tests must pass at every intermediate step (Strangler Fig invariant); module must depend only on fbpDsl, flowGraph-types, and flowGraph-persist (no cycles); KMP-first module structure required
**Scale/Scope**: 7 files extracted from graphEditor (~945 lines), call sites migrated, 1 CodeNode (2-in/1-out, anyInput) + TDD tests, architecture.flow.kt wiring

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
| Licensing | PASS | No new dependencies. flowGraph-inspect uses only fbpDsl (Apache 2.0), flowGraph-types (Apache 2.0), flowGraph-persist (Apache 2.0), and Kotlin stdlib. |
| Refactoring Specs | PASS | Constitution explicitly permits refactoring specs: "acceptance criteria center on behavior unchanged, tests green, architecture improved." |

### Post-Design Re-Check

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality First | PASS | FBP-native data flow boundary gives the module a single, well-defined contract (2 input ports, 1 output port). 7 files have clear cohesion around node discovery/inspection. |
| II. Test-Driven Development | PASS | CodeNode tests written first (TDD) cover port signatures, runtime type, data flow through channels, and boundary conditions. |
| III. User Experience Consistency | PASS | No UI changes. |
| IV. Performance Requirements | PASS | No runtime changes. CodeNode channel wiring adds negligible overhead. |
| V. Observability & Debugging | PASS | FBP-native boundary makes data flow visible in architecture FlowGraph. |

No gate violations. No complexity tracking needed.

## Project Structure

### Documentation (this feature)

```text
specs/067-extract-flowgraph-inspect/
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
# New module: flowGraph-inspect
flowGraph-inspect/
├── build.gradle.kts                                                        # KMP module config, depends on :fbpDsl, :flowGraph-types, :flowGraph-persist
├── src/
│   ├── commonMain/kotlin/io/codenode/flowgraphinspect/
│   │   └── (empty — all files are JVM-only due to java.io.File, ServiceLoader, reflection)
│   ├── jvmMain/kotlin/io/codenode/flowgraphinspect/
│   │   ├── registry/
│   │   │   └── NodeDefinitionRegistry.kt                                   # Moved from graphEditor (ServiceLoader, java.io.File)
│   │   ├── viewmodel/
│   │   │   ├── CodeEditorViewModel.kt                                      # Moved from graphEditor (java.io.File)
│   │   │   ├── IPPaletteViewModel.kt                                       # Moved from graphEditor (flowGraph-types)
│   │   │   ├── GraphNodePaletteViewModel.kt                                # Moved from graphEditor (flowGraph-persist)
│   │   │   └── NodePaletteViewModel.kt                                     # Moved from graphEditor (fbpDsl only)
│   │   ├── discovery/
│   │   │   ├── ComposableDiscovery.kt                                      # Moved from graphEditor (java.io.File only)
│   │   │   └── DynamicPreviewDiscovery.kt                                  # Moved from graphEditor (java.io.File + reflection)
│   │   └── node/
│   │       └── FlowGraphInspectCodeNode.kt                                 # NEW: CodeNode wrapping inspect functionality
│   └── jvmTest/kotlin/io/codenode/flowgraphinspect/
│       └── node/
│           └── FlowGraphInspectCodeNodeTest.kt                             # NEW: TDD tests for CodeNode

# Modified: graphEditor depends on flowGraph-inspect
graphEditor/build.gradle.kts                                                # Add implementation(project(":flowGraph-inspect"))

# Modified: settings.gradle.kts
settings.gradle.kts                                                         # Add include("flowGraph-inspect")

# Modified: architecture.flow.kt
graphEditor/architecture.flow.kt                                            # Populate inspect GraphNode with child codeNode
```

**Structure Decision**: KMP multi-module following the same pattern as flowGraph-persist (feature 066). New package root `io.codenode.flowgraphinspect`. All 7 files in jvmMain (all use JVM APIs: java.io.File, ServiceLoader, reflection). Organized into `registry/`, `viewmodel/`, `discovery/`, `node/` sub-packages.
