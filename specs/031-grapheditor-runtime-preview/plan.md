# Implementation Plan: GraphEditor Runtime Preview

**Branch**: `031-grapheditor-runtime-preview` | **Date**: 2026-02-25 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/031-grapheditor-runtime-preview/spec.md`

## Summary

Add a runtime preview pane to the graphEditor that enables live execution and visualization of flow graph modules. The implementation adds speed attenuation support to the fbpDsl runtime, creates a runtime session orchestrator in the circuitSimulator module, and integrates a collapsible preview panel into the graphEditor UI. The StopWatch module serves as the proof-of-concept, with its controller, view model, and UI composables rendered live in the preview pane.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP - Kotlin Multiplatform)
**Primary Dependencies**: Compose Desktop 1.7.3, kotlinx-coroutines 1.8.0, lifecycle-viewmodel-compose 2.8.0
**Storage**: N/A (in-memory runtime state)
**Testing**: kotlin.test (JVM), manual verification via graphEditor run
**Target Platform**: JVM Desktop (Compose Desktop)
**Project Type**: Multi-module KMP project
**Performance Goals**: Preview renders at 60fps, control actions respond within 100ms
**Constraints**: Speed attenuation is added delay only (0-5000ms), no speed-up beyond nominal
**Scale/Scope**: Single module proof-of-concept (StopWatch), extensible pattern for future modules

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality First | PASS | New classes follow single-responsibility: RuntimeSession for orchestration, preview panel for UI |
| II. Test-Driven Development | PASS | Unit tests for RuntimeSession, attenuation injection. Manual verification for Compose UI. |
| III. User Experience Consistency | PASS | Preview panel follows existing right-side panel pattern (like Properties panel) |
| IV. Performance Requirements | PASS | Compose Desktop renders at 60fps natively. Attenuation adds delay, does not impact render performance |
| V. Observability & Debugging | PASS | ExecutionState exposed as StateFlow for monitoring |
| Licensing & IP | PASS | No new dependencies beyond existing Apache 2.0 stack |

## Project Structure

### Documentation (this feature)

```text
specs/031-grapheditor-runtime-preview/
├── spec.md
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── checklists/
│   └── requirements.md
└── tasks.md             # Phase 2 output (via /speckit.tasks)
```

### Source Code (repository root)

```text
fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/
├── NodeRuntime.kt                    # MODIFY: Add attenuationDelayMs property

fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/
├── CodeNodeFactory.kt                # MODIFY: Use attenuationDelayMs in timed generator loops

fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/runtime/
├── AttenuationDelayTest.kt           # NEW: Tests for attenuation delay behavior

circuitSimulator/
├── build.gradle.kts                  # MODIFY: Update dependencies (remove graphEditor, add StopWatch + Compose)
├── src/commonMain/kotlin/io/codenode/circuitsimulator/
│   ├── RuntimeSession.kt             # NEW: Orchestrates runtime execution with attenuation
│   └── StopWatchPreviewProvider.kt   # NEW: Provides StopWatch composable + view model for preview
├── src/commonTest/kotlin/io/codenode/circuitsimulator/
│   └── RuntimeSessionTest.kt         # NEW: Tests for RuntimeSession lifecycle

graphEditor/
├── build.gradle.kts                  # MODIFY: Add circuitSimulator + StopWatch dependencies
├── src/jvmMain/kotlin/
│   ├── Main.kt                       # MODIFY: Add RuntimePreviewPanel to layout
│   └── ui/
│       └── RuntimePreviewPanel.kt    # NEW: Compose UI for controls + preview pane
```

**Structure Decision**: Follows existing multi-module pattern. Changes span 3 modules: fbpDsl (runtime attenuation support), circuitSimulator (orchestration), graphEditor (UI integration). No new modules created.

## Key Design Decisions

### D1: Speed Attenuation via NodeRuntime Property

Add `var attenuationDelayMs: Long? = null` to `NodeRuntime`. Modify `CodeNodeFactory` timed generator factory methods to use `delay(attenuationDelayMs ?: tickIntervalMs)` in the loop — when null (default), normal `tickIntervalMs` behavior is preserved; when set, the attenuation value **replaces** `tickIntervalMs` entirely as the sole delay. The property reference is captured in the closure, so changes take effect on the next loop iteration.

**Important**: The property is on the runtime instance, not a global. The `RuntimeSession` in circuitSimulator sets it on all generator runtimes when attenuation changes. A value of 0ms means no delay (maximum speed); null means use the original tickIntervalMs.

### D2: Dependency Direction

```
graphEditor → circuitSimulator → fbpDsl
graphEditor → circuitSimulator → StopWatch
graphEditor → fbpDsl (existing)
```

circuitSimulator's existing dependency on graphEditor must be removed (it was there for the stub CircuitSimulator class which can be refactored). The graphEditor gains a dependency on circuitSimulator to access RuntimeSession and preview composables.

### D3: RuntimeSession as Orchestrator

`RuntimeSession` (in circuitSimulator) owns:
- `StopWatchController` creation and lifecycle
- `StopWatchViewModel` creation
- `attenuationDelayMs` state propagation to generator runtimes
- `CoroutineScope` management
- Auto-stop on graph edit detection

The graphEditor's `RuntimePreviewPanel` calls `RuntimeSession` methods and observes its state.

### D4: Collapsible Right-Side Panel

The `RuntimePreviewPanel` is added to the right side of the graphEditor layout (after the existing Properties panel). It has:
- A toggle button to show/hide
- Controls section (top): Start/Stop/Pause/Resume buttons, attenuation slider
- Preview section (bottom): Renders the StopWatch composable

When collapsed, the panel takes zero width and the canvas expands.

### D5: Auto-Stop on Graph Edit

The graphEditor already tracks graph mutations via `GraphEditorState`. The `RuntimePreviewPanel` observes the flow graph state and calls `RuntimeSession.stop()` when it detects a change while running.

## Complexity Tracking

No constitution violations. No complexity tracking needed.
