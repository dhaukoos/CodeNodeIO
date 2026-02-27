# Implementation Plan: IP Generator Interface

**Branch**: `032-ip-generator` | **Date**: 2026-02-27 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/032-ip-generator/spec.md`

## Summary

Create an IP Generator panel in the graphEditor positioned above the IP Types palette (mirroring the NodeGenerator/NodePalette pattern). The panel provides a form for defining custom IP types with a name, optional typed properties (each with a name, type dropdown, and required/optional toggle), and Cancel/Create buttons. Uses the existing `IPTypeRegistry` for registration and duplicate checking, and follows the established ViewModel + immutable state pattern for form management.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + Compose Desktop 1.7.3
**Primary Dependencies**: kotlinx-coroutines 1.8.0, Compose Material (OutlinedTextField, DropdownMenu, Checkbox, Button)
**Storage**: JSON file via kotlinx-serialization (`~/.codenode/custom-ip-types.json`) for custom IP type persistence
**Testing**: kotlin.test with JUnit runner (`./gradlew :graphEditor:jvmTest`)
**Target Platform**: JVM Desktop (Compose Desktop)
**Project Type**: Existing KMP multi-module project (graphEditor module)
**Performance Goals**: UI feedback within 100ms per constitution, type creation < 1 second
**Constraints**: Panel width ~250.dp consistent with NodeGeneratorPanel, must integrate with existing Main.kt layout
**Scale/Scope**: Single panel UI + ViewModel + data model + repository classes, ~6-8 new/modified files

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality First | PASS | Public APIs documented, single responsibility per class, type-safe state management |
| II. Test-Driven Development | PASS | ViewModel unit tests for validation logic, integration test for registry |
| III. User Experience Consistency | PASS | Follows existing NodeGeneratorPanel pattern exactly, keyboard navigable form elements |
| IV. Performance Requirements | PASS | Simple UI operations, no expensive computations, instant type registration |
| V. Observability & Debugging | PASS | Minimal scope (UI panel), no production service monitoring needed |
| Licensing & IP | PASS | No new dependencies, uses existing Compose Desktop (Apache 2.0) |

**Post-Design Re-check**: All gates still pass. No new dependencies introduced. Test coverage planned for ViewModel validation and type creation flow.

## Project Structure

### Documentation (this feature)

```text
specs/032-ip-generator/
├── plan.md              # This file
├── research.md          # Phase 0: Existing pattern analysis
├── data-model.md        # Phase 1: Entity definitions
├── quickstart.md        # Phase 1: Integration scenarios
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
graphEditor/src/jvmMain/kotlin/
├── model/
│   ├── IPProperty.kt                    # NEW: IPProperty and CustomIPTypeDefinition data classes
│   └── SerializableIPType.kt            # NEW: @Serializable DTOs for JSON persistence
├── repository/
│   └── FileIPTypeRepository.kt          # NEW: JSON persistence (mirrors FileCustomNodeRepository)
├── viewmodel/
│   └── IPGeneratorViewModel.kt          # NEW: ViewModel with IPGeneratorPanelState
├── ui/
│   └── IPGeneratorPanel.kt              # NEW: Composable UI panel
├── state/
│   └── IPTypeRegistry.kt                # MODIFY: Add custom type property storage + load from repository
└── Main.kt                              # MODIFY: Wire IPGeneratorPanel above IPPalette, load custom types on startup

graphEditor/src/jvmTest/kotlin/
├── viewmodel/
│   └── IPGeneratorViewModelTest.kt      # NEW: ViewModel validation tests
└── repository/
    └── FileIPTypeRepositoryTest.kt      # NEW: Serialization round-trip tests
```

**Structure Decision**: All changes are within the existing `graphEditor` module. New files follow established directory conventions (`model/`, `viewmodel/`, `ui/`, `repository/`). The serializable DTOs go in `model/` alongside the domain classes. The `FileIPTypeRepository` goes in `repository/` mirroring the existing `FileCustomNodeRepository` pattern. The UI composable follows the two-level pattern (stateful + stateless content) established by `NodeGeneratorPanel.kt`.

## Key Design Decisions

### 1. Custom IP Type Persistence via kotlinx.serialization

Custom IP types are persisted to `~/.codenode/custom-ip-types.json` using `@Serializable` DTOs, following the `FileCustomNodeRepository` pattern. A `SerializableIPType` DTO stores `payloadTypeName: String` instead of `KClass<*>` (which is not serializable), mapping to `Any::class` on deserialization. Property definitions are embedded as `List<SerializableIPProperty>` within the DTO. On startup, custom types are loaded from JSON and registered in the `IPTypeRegistry`; on Create, the new type is saved immediately.

### 2. Auto-Color Assignment

A palette of 8 visually distinct colors (avoiding the 5 built-in type colors) is defined. New custom types cycle through this palette based on the count of existing custom types.

### 3. Validation Strategy

All validation is computed properties on `IPGeneratorPanelState`:
- `isValid` — composite check enabling/disabling Create button
- `hasNameConflict` — checks `IPTypeRegistry.getByTypeName()` case-insensitively
- `hasDuplicatePropertyNames` — checks for duplicate property names in the form
- `hasEmptyPropertyNames` — checks for blank property names

### 4. Layout Integration

Main.kt wraps the existing `IPPalette` call in a `Column` and adds `IPGeneratorPanel` above it, mirroring the `NodeGeneratorPanel`/`NodePalette` column pattern on the left side.

## Complexity Tracking

> No constitution violations to justify.
