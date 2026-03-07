# Implementation Plan: Animate Data Flow

**Branch**: `041-animate-data-flow` | **Date**: 2026-03-06 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/041-animate-data-flow/spec.md`

## Summary

Add an "Animate Data Flow" toggle to the Runtime Preview panel that, when enabled, shows animated dots traveling along connection curves as IPs are emitted between nodes during runtime execution. The toggle is gated by a minimum attenuation threshold (500ms). Animation logic resides primarily in circuitSimulator, with graphEditor handling only visual rendering.

**Technical Approach**: Add an `onEmit` callback to `NodeRuntime` base class (fbpDsl) to intercept IP emissions. A `DataFlowAnimationController` in circuitSimulator manages animation events and exposes active animations via `StateFlow`. The graphEditor renders dots by interpolating positions along the existing cubic Bezier connection curves using the parametric formula `B(t) = (1-t)^3*P0 + 3(1-t)^2*t*P1 + 3(1-t)*t^2*P2 + t^3*P3`.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP - Kotlin Multiplatform)
**Primary Dependencies**: Compose Desktop 1.7.3, kotlinx-coroutines 1.8.0, kotlinx-serialization 1.6.0
**Storage**: N/A (in-memory animation state)
**Testing**: kotlinx-coroutines-test (runTest, advanceTimeBy)
**Target Platform**: JVM Desktop (Compose Desktop)
**Project Type**: Multi-module KMP (fbpDsl, circuitSimulator, graphEditor)
**Performance Goals**: 60fps rendering with up to 5 concurrent dot animations; no visible stutter
**Constraints**: Animation logic in circuitSimulator; graphEditor handles only rendering. Dot travel duration = 80% of attenuationMs.
**Scale/Scope**: ~15 runtime classes instrumented with emission callback; 1 new controller class; 2-3 UI modifications

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality First | PASS | Single-responsibility: NodeRuntime gets minimal callback, controller owns animation logic, renderer owns visuals |
| II. Test-Driven Development | PASS | Unit tests for DataFlowAnimationController timing and state; rendering tested via manual quickstart |
| III. User Experience Consistency | PASS | Toggle follows existing panel patterns; dots complement connection curves visually |
| IV. Performance Requirements | PASS | Bezier interpolation is O(1) per dot per frame; StateFlow updates are efficient |
| V. Observability & Debugging | PASS | Animation state exposed via StateFlow for inspection |
| Licensing & IP | PASS | No new dependencies; all existing Apache 2.0 / KMP-compatible |

## Project Structure

### Documentation (this feature)

```text
specs/041-animate-data-flow/
├── plan.md              # This file
├── research.md          # Phase 0 output (complete)
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   └── animation-controller-api.md
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/
├── NodeRuntime.kt                    # MODIFY: Add onEmit callback property
├── SourceRuntime.kt                  # MODIFY: Invoke onEmit on send
├── TransformerRuntime.kt             # MODIFY: Invoke onEmit on send
├── FilterRuntime.kt                  # MODIFY: Invoke onEmit on send
├── SourceOut2Runtime.kt              # MODIFY: Invoke onEmit on sends
├── SourceOut3Runtime.kt              # MODIFY: Invoke onEmit on sends
├── In1Out2Runtime.kt                 # MODIFY: Invoke onEmit on sends
├── In1Out3Runtime.kt                 # MODIFY: Invoke onEmit on sends
├── In2Out1Runtime.kt                 # MODIFY: Invoke onEmit on send
├── In2Out2Runtime.kt                 # MODIFY: Invoke onEmit on sends
├── In2Out3Runtime.kt                 # MODIFY: Invoke onEmit on sends
├── In3Out1Runtime.kt                 # MODIFY: Invoke onEmit on send
├── In3Out2Runtime.kt                 # MODIFY: Invoke onEmit on sends
├── In3Out3Runtime.kt                 # MODIFY: Invoke onEmit on sends
├── In2AnyOut1Runtime.kt              # MODIFY: Invoke onEmit on send
├── In2AnyOut2Runtime.kt              # MODIFY: Invoke onEmit on sends
├── In2AnyOut3Runtime.kt              # MODIFY: Invoke onEmit on sends
├── In3AnyOut1Runtime.kt              # MODIFY: Invoke onEmit on send
├── In3AnyOut2Runtime.kt              # MODIFY: Invoke onEmit on sends
├── In3AnyOut3Runtime.kt              # MODIFY: Invoke onEmit on sends
└── ModuleController.kt               # MODIFY: Add setEmissionObserver()

circuitSimulator/src/commonMain/kotlin/io/codenode/circuitsimulator/
├── DataFlowAnimationController.kt    # NEW: Animation state management
└── RuntimeSession.kt                 # MODIFY: Add animation toggle, threshold, observer wiring

graphEditor/src/jvmMain/kotlin/ui/
├── FlowGraphCanvas.kt                # MODIFY: Add animation dot rendering pass
├── RuntimePreviewPanel.kt            # MODIFY: Add toggle button in attenuation section
└── Main.kt                           # MODIFY: Wire animation state to canvas
```

**Structure Decision**: Follows existing multi-module architecture. Emission callbacks in fbpDsl (minimal hook), animation controller in circuitSimulator (business logic), dot rendering in graphEditor (visual only).

## Complexity Tracking

No constitution violations. All changes follow single-responsibility and existing patterns.
