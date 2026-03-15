# Implementation Plan: Dynamic Runtime Pipeline

**Branch**: `051-dynamic-runtime-pipeline` | **Date**: 2026-03-15 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/051-dynamic-runtime-pipeline/spec.md`

## Summary

Replace the pre-compiled Flow/Controller pattern with a dynamic pipeline builder that reads the editor's FlowGraph at start time, resolves each node name to a `CodeNodeDefinition` via `NodeDefinitionRegistry`, creates `NodeRuntime` instances, and wires channels based on the FlowGraph's connections. This enables hot-swapping nodes on the canvas without recompilation. Modules without full registry coverage fall back to their existing generated Controller/Flow.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP - Kotlin Multiplatform)
**Primary Dependencies**: Compose Desktop 1.7.3, kotlinx-coroutines 1.8.0 (channels, StateFlow, CoroutineScope), lifecycle-viewmodel-compose 2.8.0
**Storage**: N/A (in-memory pipeline state only)
**Testing**: kotlin.test + kotlinx-coroutines-test (runTest, advanceUntilIdle)
**Target Platform**: JVM Desktop (Compose Desktop)
**Project Type**: KMP multi-module (fbpDsl, graphEditor, circuitSimulator, EdgeArtFilter, nodes)
**Performance Goals**: Pipeline startup within 500ms of the existing generated approach
**Constraints**: Must not break existing modules (StopWatch, UserProfiles, GeoLocations, Addresses); must support all existing lifecycle operations (start/stop/pause/resume/attenuation/animation)
**Scale/Scope**: ~5-10 node pipelines, single-user desktop application

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Status | Notes |
|------|--------|-------|
| Licensing (Apache 2.0 / MIT / BSD only) | PASS | No new dependencies added |
| Code Quality (readability, maintainability, type safety) | PASS | New classes follow existing patterns; strong typing via CodeNodeDefinition interface |
| Test-Driven Development | PASS | Unit tests for pipeline builder, validation, and wiring planned |
| UX Consistency | PASS | No UI changes — Start/Stop/Pause/Resume behavior unchanged |
| Performance Requirements | PASS | Dynamic pipeline must match existing startup latency within 500ms |
| Observability | PASS | Emission/value observers wired to dynamic runtimes; execution state exposed via StateFlow |

No violations. All gates pass.

## Project Structure

### Documentation (this feature)

```text
specs/051-dynamic-runtime-pipeline/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output (internal contracts)
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/
├── DynamicPipelineBuilder.kt    # Reads FlowGraph, resolves nodes, wires channels
├── DynamicPipelineController.kt # Implements ModuleController for dynamic pipelines
├── PipelineValidation.kt        # Pre-start validation (resolve, ports, cycles)
└── CodeNodeDefinition.kt        # (existing) Self-contained node interface

fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/runtime/
├── DynamicPipelineBuilderTest.kt
├── DynamicPipelineControllerTest.kt
└── PipelineValidationTest.kt

graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/
└── ModuleSessionFactory.kt      # (existing) Modified to use dynamic pipeline when possible
```

**Structure Decision**: New runtime classes go in `fbpDsl/runtime/` alongside existing runtime types (`NodeRuntime`, `ModuleController`, `CodeNodeDefinition`). This keeps the dynamic pipeline builder in the same module as the FlowGraph model and runtime types it depends on. The graphEditor's `ModuleSessionFactory` is modified to prefer the dynamic approach when registry coverage is complete.
