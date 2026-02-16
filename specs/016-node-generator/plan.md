# Implementation Plan: Node Generator UI Tool

**Branch**: `016-node-generator` | **Date**: 2026-02-15 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/016-node-generator/spec.md`

## Summary

Create a Node Generator UI panel for the graphEditor that allows users to define custom node types with configurable input/output port counts (0-3 each). The panel integrates with the existing Node Palette's Generic section and persists user-created node types via a CustomNodeRepository for session persistence.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP - Kotlin Multiplatform)
**Primary Dependencies**: Compose Desktop 1.7.3, kotlinx-serialization 1.6.0
**Storage**: JSON file via kotlinx-serialization for CustomNodeRepository persistence
**Testing**: kotlin.test with kotlinx-coroutines-test
**Target Platform**: JVM Desktop (Compose Desktop)
**Project Type**: Existing multi-module project (graphEditor module)
**Performance Goals**: UI responsiveness (<100ms for node creation), instant palette updates
**Constraints**: Must integrate with existing NodePalette and NodeTypeDefinition infrastructure
**Scale/Scope**: 15 valid input/output combinations (0-3 × 0-3, excluding 0/0)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Pre-Design Check (Phase 0)

| Principle | Status | Notes |
|-----------|--------|-------|
| **I. Code Quality First** | ✅ PASS | Will follow existing patterns in NodePalette.kt and PropertiesPanel.kt |
| **II. Test-Driven Development** | ✅ PASS | Unit tests for validation logic, integration tests for persistence |
| **III. User Experience Consistency** | ✅ PASS | Using Material Design components consistent with existing UI |
| **IV. Performance Requirements** | ✅ PASS | Simple UI operations, no complex algorithms needed |
| **V. Observability & Debugging** | ✅ PASS | Structured logging for persistence operations |
| **Licensing (Apache 2.0)** | ✅ PASS | Using only KMP-compatible libraries (kotlinx-serialization is Apache 2.0) |

**Gate Status**: ✅ PASSED - No violations requiring justification

### Post-Design Check (Phase 1)

| Principle | Status | Notes |
|-----------|--------|-------|
| **I. Code Quality First** | ✅ PASS | Data model uses immutable data classes with copy(); clear separation of concerns |
| **II. Test-Driven Development** | ✅ PASS | Test files specified for each new component; validation logic is testable |
| **III. User Experience Consistency** | ✅ PASS | Panel design follows PropertiesPanel pattern; Material Design components |
| **IV. Performance Requirements** | ✅ PASS | O(1) validation, O(n) file operations where n = number of custom nodes |
| **V. Observability & Debugging** | ✅ PASS | Error handling for file operations with logging |
| **Licensing (Apache 2.0)** | ✅ PASS | No new dependencies added beyond existing kotlinx-serialization |

**Final Gate Status**: ✅ PASSED - Design adheres to all constitution principles

## Project Structure

### Documentation (this feature)

```text
specs/016-node-generator/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output (internal APIs)
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
graphEditor/src/jvmMain/kotlin/
├── ui/
│   ├── NodePalette.kt           # Existing - will be modified to accept dynamic nodes
│   ├── NodeGeneratorPanel.kt    # NEW - Node Generator UI component
│   └── ...
├── state/
│   ├── GraphState.kt            # Existing - will add custom node type list
│   └── NodeGeneratorState.kt    # NEW - Form state management
├── repository/
│   └── CustomNodeRepository.kt  # NEW - Persistence for custom node types
└── Main.kt                      # Existing - will integrate NodeGeneratorPanel

graphEditor/src/jvmTest/kotlin/
├── ui/
│   └── NodeGeneratorPanelTest.kt  # NEW - UI component tests
├── state/
│   └── NodeGeneratorStateTest.kt  # NEW - State management tests
└── repository/
    └── CustomNodeRepositoryTest.kt # NEW - Persistence tests
```

**Structure Decision**: Integrating into existing graphEditor module structure following established patterns (ui/, state/, repository/).

## Complexity Tracking

> No violations requiring justification - Constitution Check passed.

## Phase 1 Artifacts

| Artifact | Status | Location |
|----------|--------|----------|
| research.md | ✅ Complete | [research.md](research.md) |
| data-model.md | ✅ Complete | [data-model.md](data-model.md) |
| contracts/ | ✅ Complete | [contracts/internal-api.md](contracts/internal-api.md) |
| quickstart.md | ✅ Complete | [quickstart.md](quickstart.md) |
| Agent context | ✅ Updated | CLAUDE.md updated with feature context |

## Next Steps

Run `/speckit.tasks` to generate the implementation task list (tasks.md).
