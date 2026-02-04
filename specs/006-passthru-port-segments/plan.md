# Implementation Plan: PassThruPort and ConnectionSegment

**Branch**: `006-passthru-port-segments` | **Date**: 2026-02-04 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/006-passthru-port-segments/spec.md`

## Summary

This feature introduces PassThruPort and ConnectionSegment model elements to support connections that cross GraphNode boundaries. PassThruPort is a specialized Port subtype that sits on GraphNode boundaries with references to upstream and downstream ports. ConnectionSegment represents visual portions of connections, enabling proper rendering across hierarchical view contexts.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP - Kotlin Multiplatform)
**Primary Dependencies**: Compose Desktop 1.7.3, kotlinx-serialization, kotlinx-coroutines
**Storage**: .flow.kts DSL files (text-based serialization)
**Testing**: kotlin.test with JVM test runner
**Target Platform**: JVM Desktop (via Compose Desktop)
**Project Type**: Multi-module KMP project (fbpDsl/, graphEditor/)
**Performance Goals**: PassThruPort creation during grouping < 500ms, segment visibility switching < 100ms
**Constraints**: Maintain backward compatibility with existing FlowGraph serialization
**Scale/Scope**: Support nested GraphNodes up to 5+ levels with multiple boundary-crossing connections

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| **I. Code Quality First** | ✅ PASS | Port inheritance follows SRP; segments decompose connections cleanly |
| **II. Test-Driven Development** | ✅ PASS | TDD required per constitution; tests written before implementation |
| **III. User Experience Consistency** | ✅ PASS | Square vs circle port distinction follows established visual patterns |
| **IV. Performance Requirements** | ✅ PASS | 500ms grouping target achievable; segment switching is O(1) filter |
| **V. Observability & Debugging** | ✅ PASS | Segment visibility tied to navigation context; clear boundary visualization |
| **Licensing** | ✅ PASS | No new dependencies required; uses existing Apache 2.0 codebase |

## Project Structure

### Documentation (this feature)

```text
specs/006-passthru-port-segments/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output (internal APIs)
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
fbpDsl/
├── src/commonMain/kotlin/io/codenode/fbpdsl/model/
│   ├── Port.kt                    # Existing - base class for PassThruPort
│   ├── PassThruPort.kt            # NEW - PassThruPort model
│   ├── Connection.kt              # MODIFY - add segments property
│   ├── ConnectionSegment.kt       # NEW - ConnectionSegment model
│   └── GraphNode.kt               # MODIFY - update port handling
├── src/commonMain/kotlin/io/codenode/fbpdsl/factory/
│   ├── PassThruPortFactory.kt     # NEW - factory for creating PassThruPorts
│   └── GraphNodeFactory.kt        # MODIFY - integrate PassThruPort creation
└── src/commonTest/kotlin/
    ├── PassThruPortTest.kt        # NEW - unit tests
    └── ConnectionSegmentTest.kt   # NEW - unit tests

graphEditor/
├── src/jvmMain/kotlin/
│   ├── rendering/
│   │   ├── ConnectionRenderer.kt  # MODIFY - render segments as bezier curves
│   │   └── PortRenderer.kt        # NEW - distinguish circle vs square ports
│   ├── state/
│   │   └── GraphState.kt          # MODIFY - segment visibility by context
│   ├── serialization/
│   │   ├── FlowGraphSerializer.kt    # MODIFY - serialize PassThruPorts and segments
│   │   └── FlowGraphDeserializer.kt  # MODIFY - deserialize PassThruPorts and segments
│   └── ui/
│       └── FlowGraphCanvas.kt     # MODIFY - render appropriate segments per context
└── src/jvmTest/kotlin/
    ├── PassThruPortRenderingTest.kt  # NEW - UI tests
    └── SegmentVisibilityTest.kt      # NEW - navigation context tests
```

**Structure Decision**: Multi-module KMP project with model elements in `fbpDsl/` and UI/rendering in `graphEditor/`. This follows the established pattern from features 001-005.

## Complexity Tracking

No constitution violations requiring justification. The feature:
- Adds 2 new model classes (PassThruPort, ConnectionSegment) - within reasonable complexity
- Modifies existing classes (Connection, GraphNode, GraphNodeFactory) - incremental changes
- Follows established inheritance pattern (PassThruPort extends Port concept)
