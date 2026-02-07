# Implementation Plan: Node ExecutionState and ControlConfig

**Branch**: `007-node-execution-control` | **Date**: 2026-02-07 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/007-node-execution-control/spec.md`

## Summary

Move `executionState` and `controlConfig` from CodeNode to the base Node sealed class, enabling GraphNode to inherit and propagate execution control to all descendants. Introduce `RootControlNode` as master flow controller and support KMP module generation for the "virtual circuit board" concept.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP - Kotlin Multiplatform)
**Primary Dependencies**: kotlinx-serialization, kotlinx-coroutines, Compose Desktop 1.7.3 (graphEditor)
**Storage**: .flow.kts files (text-based DSL serialization)
**Testing**: kotlin.test (JVM), Compose Desktop testing
**Target Platform**: JVM (Desktop via Compose), multiplatform commonMain for fbpDsl module
**Project Type**: Multi-module KMP (fbpDsl/, graphEditor/, kotlinCompiler/)
**Performance Goals**: State propagation through 5-level hierarchy < 100ms; RootControlNode managing 100+ nodes without degradation
**Constraints**: Backward compatibility with existing CodeNode usage; Apache 2.0 licensing
**Scale/Scope**: Hierarchical node graphs up to 5+ levels deep, 100+ nodes per flowGraph

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality First | ✅ PASS | Refactoring model classes with clear responsibilities, type-safe APIs |
| II. Test-Driven Development | ✅ PASS | TDD mandatory per constitution; tests written before implementation |
| III. User Experience Consistency | ✅ PASS | No UI changes required; control propagation is model-layer feature |
| IV. Performance Requirements | ✅ PASS | Performance targets defined: < 100ms propagation, < 10ms per node operation |
| V. Observability & Debugging | ✅ PASS | Independent control mode enables debugging isolation |
| Licensing & IP | ✅ PASS | All dependencies are Apache 2.0 / MIT compatible; no new dependencies needed |

**Gate Status**: ✅ PASSED - No violations. Proceed to Phase 0.

## Project Structure

### Documentation (this feature)

```text
specs/007-node-execution-control/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output (internal API contracts)
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
fbpDsl/
├── src/
│   ├── commonMain/kotlin/io/codenode/fbpdsl/
│   │   ├── model/
│   │   │   ├── Node.kt                    # MODIFY: Add executionState, controlConfig
│   │   │   ├── CodeNode.kt                # MODIFY: Remove executionState, controlConfig (now inherited)
│   │   │   ├── GraphNode.kt               # MODIFY: Add state propagation methods
│   │   │   ├── ExecutionState.kt          # NEW: Extract enum to own file
│   │   │   ├── ControlConfig.kt           # NEW: Extract class to own file
│   │   │   └── RootControlNode.kt         # NEW: Master flow controller
│   │   └── execution/
│   │       └── StatePropagator.kt         # NEW: State propagation logic
│   └── commonTest/kotlin/io/codenode/fbpdsl/
│       ├── model/
│       │   ├── NodeExecutionStateTest.kt  # NEW: Tests for Node state properties
│       │   ├── StatePropagationTest.kt    # NEW: Tests for hierarchical propagation
│       │   └── RootControlNodeTest.kt     # NEW: Tests for master controller
│       └── execution/
│           └── StatePropagatorTest.kt     # NEW: Tests for propagation logic

graphEditor/
├── src/
│   ├── jvmMain/kotlin/
│   │   ├── state/
│   │   │   └── GraphState.kt              # MODIFY: Add execution control operations
│   │   └── ui/
│   │       └── ExecutionControls.kt       # NEW: UI for execution state control (future)
│   └── jvmTest/kotlin/
│       └── state/
│           └── ExecutionControlTest.kt    # NEW: Integration tests for execution control

kotlinCompiler/
├── src/
│   └── jvmMain/kotlin/io/codenode/kotlincompiler/
│       └── ModuleGenerator.kt             # NEW: KMP module generation (P5)
```

**Structure Decision**: Follows existing multi-module KMP structure. Core model changes in `fbpDsl/`, UI integration in `graphEditor/`, code generation in `kotlinCompiler/`.

## Complexity Tracking

No constitution violations requiring justification.

## Phase 0: Research Summary

### Research Tasks Identified

1. **State Propagation Pattern**: Research optimal pattern for hierarchical state propagation in immutable data structures
2. **Kotlin Sealed Class Extension**: Verify approach for adding abstract properties to existing sealed class
3. **KMP Module Generation**: Research gradle KMP module structure generation patterns
4. **Performance Optimization**: Research efficient tree traversal for large node hierarchies

### Key Decisions Made (See research.md for full details)

| Area | Decision | Rationale |
|------|----------|-----------|
| State Storage | Abstract properties in Node base class | Maintains sealed class hierarchy, enables compile-time type safety |
| Propagation | Explicit propagation via copy() chain | Immutable data pattern; avoids mutable state complications |
| Independent Control | Flag in ControlConfig | Single location for all control-related settings |
| RootControlNode | Separate class, not Node subclass | Different responsibility (controller vs. node), avoids inheritance complexity |

## Phase 1: Design Artifacts

### Data Model

See `data-model.md` for complete entity definitions including:
- ExecutionState enum (IDLE, RUNNING, PAUSED, ERROR)
- ControlConfig data class (pauseBufferSize, speedAttenuation, autoResumeOnError, independentControl)
- Node sealed class modifications (new abstract properties)
- RootControlNode class definition
- FlowExecutionStatus aggregation type

### Contracts

See `contracts/` directory for:
- `execution-control-api.md`: Internal API for state management operations
- `state-propagation-contract.md`: Contract for hierarchical state propagation behavior

### Quickstart

See `quickstart.md` for:
- Developer walkthrough of execution control features
- Example code for controlling subgraphs
- RootControlNode usage patterns
