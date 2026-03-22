# Implementation Plan: Table Header Row for Entity List Views

**Branch**: `056-table-header-row` | **Date**: 2026-03-22 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/056-table-header-row/spec.md`

## Summary

Add a HeaderRow composable to all entity list views so that column labels (property names) appear once at the top, and data rows display values only (no inline "Label: value" format). Update the code generator to produce this pattern for new modules, and refactor the three existing modules (Addresses, GeoLocations, UserProfiles) to match.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP - Kotlin Multiplatform)
**Primary Dependencies**: Compose Multiplatform 1.7.3, Room 2.8.4 (KMP), Koin 4.0.0, kotlinx-coroutines 1.8.0
**Storage**: Room (KMP) with BundledSQLiteDriver — all persistence in shared `persistence` module
**Testing**: Manual runtime preview verification in graphEditor
**Target Platform**: KMP Desktop (JVM) via Compose Desktop
**Project Type**: Multi-module KMP project
**Performance Goals**: N/A (UI cosmetic change, no performance-critical paths)
**Constraints**: HeaderRow must use identical column layout (weights/padding) as data rows for alignment
**Scale/Scope**: 4 files modified per existing module (3 modules), 2 generator methods updated

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality First | PASS | Changes are straightforward UI composables with clear responsibility |
| II. Test-Driven Development | PASS | Verification via runtime preview; no new business logic requiring unit tests |
| III. User Experience Consistency | PASS | This feature improves UX consistency — all entity lists will follow the same table pattern |
| IV. Performance Requirements | PASS | No performance-critical changes — static header row, no new computation |
| V. Observability & Debugging | N/A | UI-only change, no logging/metrics needed |
| Licensing & IP | PASS | No new dependencies; all existing Apache 2.0 |

## Project Structure

### Documentation (this feature)

```text
specs/056-table-header-row/
├── spec.md              # Feature specification
├── plan.md              # This file
├── research.md          # Phase 0 output
└── checklists/
    └── requirements.md  # Spec quality checklist
```

### Source Code (repository root)

```text
# Code Generator (produces HeaderRow pattern for NEW modules)
kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/
├── EntityUIGenerator.kt        # generateListView() + generateRowView() updated

# Existing Modules (refactored to new pattern)
Addresses/src/commonMain/kotlin/io/codenode/addresses/userInterface/
├── Addresses.kt                # Add HeaderRow composable, insert before LazyColumn
├── AddressRow.kt               # Remove "Label: " prefixes from Text values

GeoLocations/src/commonMain/kotlin/io/codenode/geolocations/userInterface/
├── GeoLocations.kt             # Add HeaderRow composable, insert before LazyColumn
├── GeoLocationRow.kt           # Remove "label: " prefixes from Text values

UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/userInterface/
├── UserProfiles.kt             # Add HeaderRow composable, insert before LazyColumn
                                # UserProfileRow is in same file — remove inline labels
```

**Structure Decision**: Existing multi-module KMP project. Changes are scoped to UI composable files in each entity module and the code generator in kotlinCompiler.

## Design Decisions

### HeaderRow Layout Strategy

The HeaderRow composable must use the **exact same layout modifiers** as the data row to ensure column alignment:
- Same `Row` with `fillMaxWidth()`, `padding(horizontal = 16.dp, vertical = 12.dp)`
- Same weight/padding modifiers per column: first column `Modifier.weight(1f)`, subsequent columns `Modifier.padding(horizontal = 8.dp)`
- Bold text style (`MaterialTheme.typography.labelLarge`) to visually distinguish from data rows
- Bottom border via `Divider` composable after the HeaderRow

### HeaderRow Placement

The HeaderRow is placed **outside** the `LazyColumn`, directly above it in the `Column` layout. This ensures:
- FR-002: Header remains visible (not scrolled with content)
- FR-004: Alignment is maintained by identical modifiers
- Edge case: Header displays even when list is empty (above "No items yet" message)

### Null/Empty Value Display

- Required properties: Display the value directly
- Optional/nullable properties: Display `"—"` placeholder when null (FR-008)
- Boolean properties: Display `"Yes"` / `"No"` (no label prefix)

### Code Generator Changes

Two methods in `EntityUIGenerator` need updating:

1. **`generateListView()`**: Insert a `{Entity}HeaderRow()` call between the title and the `Box` containing the list/empty state. The HeaderRow is always rendered regardless of list state.

2. **`generateRowView()`**: Remove `"${prop.name}: "` prefix from all Text values. Display values only. For booleans, display `"Yes"` / `"No"`. For nullable properties, use `"—"` placeholder.

The generator will also produce a `{Entity}HeaderRow` composable function within the `{Entity}s.kt` list view file (not a separate file), keeping it co-located with the list that uses it.

## Complexity Tracking

No constitution violations. All changes are additive UI modifications within existing files and patterns.
