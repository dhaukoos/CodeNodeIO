# Research: Collapsible Panels

**Feature**: 042-collapsible-panels
**Date**: 2026-03-07

## R1: Existing Collapsible Pattern in RuntimePreviewPanel

**Decision**: Extract the toggle strip pattern from `RuntimePreviewPanel` into a reusable `CollapsiblePanel` composable.

**Rationale**: RuntimePreviewPanel already implements the exact collapse/expand behavior needed:
- A `Row` containing a `Divider`, a 20dp-wide clickable `Box` with a chevron icon, and the content (shown only when expanded)
- Chevron direction indicates the collapse/expand action based on panel side (left vs right)
- State is managed externally via `isExpanded: Boolean` and `onToggle: () -> Unit` parameters

Extracting this into a reusable composable avoids code duplication across 3 new panels and ensures visual consistency (FR-006, SC-002).

**Alternatives considered**:
- Inline the toggle strip in each panel: Rejected — duplicates ~15 lines of UI code per panel, risks inconsistency
- Modify existing panel composables to accept isExpanded: Rejected — adds collapse concerns to panels that should only handle their content
- Use Compose animation (AnimatedVisibility): Considered for future enhancement but not needed for MVP — the existing pattern uses simple if/else toggle without animation

## R2: Panel Edge Placement and Chevron Direction

**Decision**: Follow the convention established by RuntimePreviewPanel:
- **Left-side panels** (Node Generator/Palette, IP Generator/Palette): Toggle strip on the **right edge**, ChevronRight when expanded (collapse rightward → hide), ChevronLeft when collapsed (expand leftward → show)
- **Right-side panels** (Properties, Runtime Preview): Toggle strip on the **left edge**, ChevronRight when expanded (collapse rightward → hide... wait — RuntimePreviewPanel uses ChevronRight to collapse, ChevronLeft to expand)

Actual existing behavior in RuntimePreviewPanel (right-side panel):
- When expanded: `Icons.Default.ChevronRight` → "Collapse runtime panel" (collapse = push right = hide)
- When collapsed: `Icons.Default.ChevronLeft` → "Expand runtime panel" (expand = pull left = show)

For left-side panels, the inverse:
- When expanded: `Icons.Default.ChevronLeft` → collapse = push left = hide
- When collapsed: `Icons.Default.ChevronRight` → expand = pull right = show

**Rationale**: The chevron always points in the direction the panel would move (collapse direction when expanded, expand direction when collapsed). This is the standard IDE panel convention.

**Alternatives considered**: Using +/- icons, hamburger menu, or labeled buttons. Rejected — chevron is the established pattern in the app.

## R3: State Management Approach

**Decision**: Use `remember { mutableStateOf(true) }` for each panel's expanded state in Main.kt, matching the existing `isRuntimePanelExpanded` pattern.

**Rationale**:
- Session-only state (no persistence across restarts per spec assumptions)
- Each panel's state is independent (FR-005)
- Default to expanded (true) on launch per spec assumptions
- Simple boolean state is sufficient — no complex state machine needed

**Alternatives considered**:
- ViewModel-based state: Overkill for simple boolean toggles
- SharedPreferences/DataStore persistence: Out of scope per spec assumptions
- Single combined state object: Over-couples independent panel states

## R4: Canvas Resizing Behavior

**Decision**: The canvas already uses `Modifier.weight(1f)` (or equivalent fill behavior) in the layout Row. When side panels collapse to their narrow toggle strips (~21dp), the canvas automatically expands to fill the freed space. No explicit resize logic needed.

**Rationale**: Compose's Row layout with weight modifiers handles space redistribution automatically. The existing layout already works this way with RuntimePreviewPanel — when it collapses, the canvas gets wider.

**Alternatives considered**: Explicit width calculations — unnecessary with Compose's layout system.

## R5: CollapsiblePanel API Design

**Decision**: Create a composable with this signature:

```
CollapsiblePanel(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    side: PanelSide,           // LEFT or RIGHT — determines chevron direction and strip placement
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
)
```

Where `PanelSide` is an enum: `LEFT` (toggle on right edge, content expands leftward) or `RIGHT` (toggle on left edge, content expands rightward).

**Rationale**: The `side` parameter encapsulates all directional logic (chevron icons, divider placement, content order) in one place. This matches the RuntimePreviewPanel pattern while being reusable for both left and right panels.

**Alternatives considered**:
- Separate LeftCollapsiblePanel/RightCollapsiblePanel composables: More duplication, harder to maintain consistency
- Boolean `isRightSide` parameter: Less readable than an enum
