# Research: Table Header Row for Entity List Views

**Feature**: 056-table-header-row
**Date**: 2026-03-22

## Research Summary

This feature is a straightforward UI change with no unknowns requiring external research. All technical decisions are derived from analyzing the existing codebase patterns.

## Decision 1: HeaderRow Column Alignment Strategy

**Decision**: Use identical layout modifiers between HeaderRow and data rows (weight/padding matching).

**Rationale**: The existing data rows use `Modifier.weight(1f)` for the first column and `Modifier.padding(horizontal = 8.dp)` for subsequent columns. By reusing these exact modifiers in the HeaderRow, column alignment is guaranteed without any custom measurement or layout logic.

**Alternatives considered**:
- Custom `IntrinsicSize` measurement: Overcomplicated for fixed-weight layouts
- Table composable library: Unnecessary dependency for simple column alignment already achieved by matching modifiers

## Decision 2: HeaderRow Visual Distinction

**Decision**: Use `MaterialTheme.typography.labelLarge` (bold by default in Material3) with a `Divider` composable beneath the header row.

**Rationale**: `labelLarge` is semantically appropriate for column labels in Material3 and is visually distinct from `bodyMedium` used in data rows. A `Divider` provides clear visual separation. No background color change needed — keeps the design clean.

**Alternatives considered**:
- Different background color: Would add visual noise; divider line is cleaner
- Custom font weight: `labelLarge` already provides the right weight distinction

## Decision 3: HeaderRow Placement in Code

**Decision**: Define `{Entity}HeaderRow` as a private composable function within the `{Entity}s.kt` list view file, called above the `Box` that contains the `LazyColumn` / empty state.

**Rationale**: The HeaderRow is tightly coupled to its list view — it uses the same property names and layout. A separate file would add unnecessary file overhead. Placing it above the `Box` ensures it's always visible regardless of scroll position or empty state.

**Alternatives considered**:
- Separate `{Entity}HeaderRow.kt` file: Unnecessary for a small, tightly-coupled composable
- Sticky header in LazyColumn: Would scroll with content; spec requires fixed header (FR-002)

## Decision 4: UserProfiles Special Handling

**Decision**: UserProfiles module uses custom display logic (name without label, "Active"/"Inactive" for boolean). The HeaderRow will use property names directly ("name", "age", "isActive") to match the columns, but capitalized for display ("Name", "Age", "Active").

**Rationale**: The UserProfiles module predates the code generator and has hand-written display logic. The HeaderRow should use human-friendly labels that correspond to what's displayed in the columns.

**Alternatives considered**:
- Use raw property names: "isActive" is not user-friendly as a column header
- Full regeneration: Would lose the custom UserProfileRow formatting
