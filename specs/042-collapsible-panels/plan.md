# Implementation Plan: Collapsible Panels

**Branch**: `042-collapsible-panels` | **Date**: 2026-03-07 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/042-collapsible-panels/spec.md`

## Summary

Add collapse/expand toggle controls to the Properties Panel (right), IP Generator/IP Types column (second from left), and Node Generator/Node Palette column (far left), matching the existing RuntimePreviewPanel pattern. Each panel gets an `isExpanded` boolean state and a chevron toggle strip on the panel edge. The canvas fills available space as panels collapse.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (Kotlin Multiplatform) + Compose Desktop 1.7.3
**Primary Dependencies**: Compose Material (Icons, Surface, Row, Column, Box), Compose Foundation (clickable, background)
**Storage**: N/A (in-memory panel state, session-only)
**Testing**: Manual visual testing (Compose Desktop UI)
**Target Platform**: JVM Desktop (Compose Desktop)
**Project Type**: KMP multiplatform with graphEditor JVM module
**Performance Goals**: Instant toggle response (<100ms), no canvas flicker on collapse/expand
**Constraints**: Must match existing RuntimePreviewPanel collapsible pattern exactly
**Scale/Scope**: 3 panels to make collapsible, changes scoped to graphEditor module only

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality First | PASS | Extracting reusable collapsible wrapper follows single responsibility. Clear naming. |
| II. Test-Driven Development | WAIVED | UI-only change (Compose toggle state). No business logic to unit test. Visual behavior verified manually. |
| III. User Experience Consistency | PASS | Core goal of this feature — all panels use the same toggle pattern as RuntimePreviewPanel. |
| IV. Performance Requirements | PASS | Simple boolean state toggle; no performance impact. |
| V. Observability & Debugging | N/A | No logging/metrics needed for panel collapse state. |
| Licensing & IP | PASS | No new dependencies. Uses existing Compose Material icons. |

## Project Structure

### Documentation (this feature)

```text
specs/042-collapsible-panels/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (repository root)

```text
graphEditor/src/jvmMain/kotlin/
├── Main.kt                              # Panel state vars, layout Row with collapsible wrappers
├── ui/
│   ├── CollapsiblePanel.kt              # NEW: Reusable collapsible wrapper composable
│   ├── RuntimePreviewPanel.kt           # Existing collapsible panel (reference pattern)
│   ├── CompactPropertiesPanel.kt        # Properties Panel content (wrapped by CollapsiblePanel)
│   ├── NodeGeneratorPanel.kt            # Node Generator content (wrapped by CollapsiblePanel)
│   ├── NodePalette.kt                   # Node Palette content (wrapped by CollapsiblePanel)
│   ├── IPGeneratorPanel.kt              # IP Generator content (wrapped by CollapsiblePanel)
│   └── IPPalette.kt                     # IP Palette content (wrapped by CollapsiblePanel)
```

**Structure Decision**: All changes are within the existing `graphEditor` JVM module. A new `CollapsiblePanel.kt` composable extracts the toggle strip + content pattern from RuntimePreviewPanel into a reusable wrapper. Existing panel composables are not modified — they are wrapped by CollapsiblePanel in Main.kt's layout.

## Constitution Check (Post-Design)

*Re-evaluated after Phase 1 design completion.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality First | PASS | CollapsiblePanel composable has single responsibility. PanelSide enum provides clear API. No complex logic. |
| II. Test-Driven Development | WAIVED | Pure UI composition with no business logic. Boolean state toggle is trivially correct. Manual visual verification sufficient. |
| III. User Experience Consistency | PASS | All 4 panels (including existing RuntimePreviewPanel) will use identical toggle pattern via shared CollapsiblePanel composable. |
| IV. Performance Requirements | PASS | No computational work — just Compose recomposition on boolean state change. |
| V. Observability & Debugging | N/A | No change to system behavior or data flow. |
| Licensing & IP | PASS | No new dependencies added. |

## Complexity Tracking

No violations to justify.
