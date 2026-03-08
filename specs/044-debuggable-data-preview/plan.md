# Implementation Plan: Debuggable Data Runtime Preview

**Branch**: `044-debuggable-data-preview` | **Date**: 2026-03-07 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/044-debuggable-data-preview/spec.md`

## Summary

Add runtime data inspection to the Runtime Preview. When data flow animation is enabled, each channel captures the most recent value that passes through it. When execution is paused, selecting a connection in the flow graph displays the captured value in the Properties panel below the existing connection properties. Uses a value-capturing emission callback on runtimes and a `DataFlowDebugger` to store per-connection snapshots keyed by connection ID.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (Kotlin Multiplatform) + Compose Desktop 1.7.3
**Primary Dependencies**: kotlinx-coroutines 1.8.0 (channels, StateFlow), Compose Material (UI)
**Storage**: N/A (in-memory snapshots, transient per runtime session)
**Testing**: Manual verification via Runtime Preview (quickstart.md scenarios)
**Target Platform**: JVM Desktop (Compose Desktop)
**Project Type**: KMP multiplatform with circuitSimulator and graphEditor modules
**Performance Goals**: Zero overhead when debug mode disabled; snapshot capture adds negligible latency to send()
**Constraints**: Must not change external runtime API behavior; snapshots cleared on stop
**Scale/Scope**: ~6 files modified, 1 new file

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality First | PASS | Clean separation: DataFlowDebugger owns snapshots, runtimes provide values via callback. |
| II. Test-Driven Development | PASS | Verifiable via manual runtime preview scenarios. Existing emission callback pattern is well-tested. |
| III. User Experience Consistency | PASS | Extends existing Properties panel with additional section. Follows established UI patterns. |
| IV. Performance Requirements | PASS | Zero overhead when disabled. When enabled, one StateFlow write per emission (negligible). |
| V. Observability & Debugging | PASS | This feature IS a debugging capability. |
| Licensing & IP | PASS | No new dependencies. |

## Project Structure

### Documentation (this feature)

```text
specs/044-debuggable-data-preview/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (repository root)

```text
circuitSimulator/src/commonMain/kotlin/io/codenode/circuitsimulator/
├── DataFlowDebugger.kt              # NEW: Per-connection snapshot storage
├── RuntimeSession.kt                # MODIFY: Wire debugger into lifecycle
└── DataFlowAnimationController.kt   # READ-ONLY (reference for emission mapping pattern)

fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/
├── NodeRuntime.kt                   # MODIFY: Add onEmitValue callback
└── [All runtime files with emission sites]  # MODIFY: Pass value to onEmitValue

graphEditor/src/jvmMain/kotlin/
├── ui/PropertiesPanel.kt            # MODIFY: Display snapshot for selected connection
└── Main.kt                          # MODIFY: Pass debugger state to PropertiesPanel
```

**Structure Decision**: Changes span the existing circuitSimulator (runtime state), fbpDsl (emission callbacks), and graphEditor (UI display) modules. One new file (`DataFlowDebugger.kt`) in circuitSimulator. No new modules or structural changes.

## Constitution Check (Post-Design)

*Re-evaluated after Phase 1 design completion.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality First | PASS | Single-responsibility DataFlowDebugger. Minimal changes to existing runtimes. |
| II. Test-Driven Development | PASS | Feature verifiable via runtime preview scenarios in quickstart.md. |
| III. User Experience Consistency | PASS | Additive UI section in Properties panel. No existing behavior changed. |
| IV. Performance Requirements | PASS | Conditional capture via callback — no overhead when disabled. |
| V. Observability & Debugging | PASS | Core purpose is debugging support. |
| Licensing & IP | PASS | No new dependencies. |

## Complexity Tracking

No violations to justify.
