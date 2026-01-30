# Implementation Plan: InformationPacket Palette Support

**Branch**: `003-ip-palette-support` | **Date**: 2026-01-30 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/003-ip-palette-support/spec.md`

## Summary

Add InformationPacket (IP) type visualization and management to the graphEditor. This includes:
- Adding an optional color property to IP definitions with black as default
- Creating an IP Palette UI component with search/filter capability
- Enabling connection selection with IP type display in Properties Panel
- Adding right-click context menu for changing connection IP types

## Technical Context

**Language/Version**: Kotlin 2.1.21 (Kotlin Multiplatform)
**Primary Dependencies**: Compose Desktop, kotlinx.serialization, kotlinx.coroutines
**Storage**: In-memory (graph state), .flow.kts file serialization
**Testing**: JUnit 5 with kotlin-test
**Target Platform**: JVM Desktop (macOS, Windows, Linux via Compose Desktop)
**Project Type**: Multi-module KMP (fbpDsl for models, graphEditor for UI)
**Performance Goals**: UI responsive < 100ms for all interactions
**Constraints**: Must integrate with existing FlowGraph/Connection/InformationPacket models
**Scale/Scope**: 5 default IP types, extensible for custom types

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality First | PASS | UI components follow existing patterns (NodePalette, PropertiesPanel) |
| II. Test-Driven Development | PASS | TDD required - tests before implementation |
| III. User Experience Consistency | PASS | Follows existing graphEditor design system patterns |
| IV. Performance Requirements | PASS | UI interactions < 100ms, no heavy computation |
| V. Observability & Debugging | PASS | Existing logging patterns apply |
| Licensing (Apache 2.0) | PASS | All dependencies (Compose, kotlinx) are Apache 2.0 compatible |

**Quality Gates**:
- [ ] All tests pass in CI
- [ ] Code review approval
- [ ] Linting passes
- [ ] Documentation updated

## Project Structure

### Documentation (this feature)

```text
specs/003-ip-palette-support/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output (internal contracts)
└── tasks.md             # Phase 2 output (created by /speckit.tasks)
```

### Source Code (repository root)

```text
fbpDsl/
├── src/commonMain/kotlin/io/codenode/fbpdsl/
│   └── model/
│       ├── InformationPacket.kt      # MODIFY: Add color property
│       ├── InformationPacketType.kt  # NEW: IP type definition with color
│       └── Connection.kt             # MODIFY: Add ipType reference
└── src/commonTest/kotlin/io/codenode/fbpdsl/
    └── model/
        └── InformationPacketTypeTest.kt  # NEW: Tests for IP types

graphEditor/
├── src/jvmMain/kotlin/io/codenode/grapheditor/
│   ├── ui/
│   │   ├── IPPalette.kt              # NEW: IP type palette component
│   │   ├── ConnectionContextMenu.kt  # NEW: Right-click context menu
│   │   ├── ColorEditor.kt            # NEW: RGB color editor component
│   │   ├── PropertiesPanel.kt        # MODIFY: Add connection properties
│   │   └── FlowGraphCanvas.kt        # MODIFY: Add connection selection/right-click
│   └── state/
│       ├── GraphState.kt             # MODIFY: Add connection selection state
│       └── IPTypeRegistry.kt         # NEW: Registry for available IP types
└── src/jvmTest/kotlin/io/codenode/grapheditor/
    ├── ui/
    │   ├── IPPaletteTest.kt          # NEW: IP palette tests
    │   ├── ConnectionContextMenuTest.kt  # NEW: Context menu tests
    │   └── ColorEditorTest.kt        # NEW: Color editor tests
    └── state/
        └── IPTypeRegistryTest.kt     # NEW: Registry tests
```

**Structure Decision**: Extends existing multi-module structure. Model changes in `fbpDsl`, UI changes in `graphEditor`. This maintains separation of concerns and allows model reuse across modules.

## Complexity Tracking

No constitution violations requiring justification.
