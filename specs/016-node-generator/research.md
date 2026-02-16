# Research: Node Generator UI Tool

**Feature**: 016-node-generator
**Date**: 2026-02-15
**Status**: Complete

## Research Tasks

### 1. UI Framework Patterns in graphEditor

**Question**: How are UI panels structured in the existing graphEditor?

**Research Findings**:
- UI components are in `graphEditor/src/jvmMain/kotlin/ui/`
- Uses Compose Desktop 1.7.3 with Material Design components
- Pattern: `@Composable` functions with state hoisting
- Existing examples: `NodePalette.kt`, `PropertiesPanel.kt`, `IPPalette.kt`
- Layout: Main.kt uses Row/Column layout with weighted sections

**Decision**: Follow existing NodePalette.kt pattern for NodeGeneratorPanel
**Rationale**: Consistency with existing codebase, proven patterns
**Alternatives Considered**:
- Custom canvas-based UI (rejected: overkill for form)
- Dialog-based approach (rejected: user wants panel above palette)

---

### 2. NodeTypeDefinition Integration

**Question**: How do new node types get added to the palette?

**Research Findings**:
- `NodeTypeDefinition` is the template class for palette items
- Category `NodeCategory.GENERIC` places nodes in Generic section
- `GenericNodeTypeFactory.createGenericNodeType()` creates generic types
- Parameters: numInputs, numOutputs, customName, customDescription
- genericType string format: "inXoutY" (e.g., "in2out1")

**Decision**: Use `GenericNodeTypeFactory.createGenericNodeType()` for creating custom nodes
**Rationale**: Leverages existing factory with established patterns
**Alternatives Considered**:
- Direct NodeTypeDefinition construction (rejected: factory handles defaults)
- New factory class (rejected: existing factory is sufficient)

---

### 3. State Management Pattern

**Question**: How is UI state managed in the graphEditor?

**Research Findings**:
- `GraphState` class holds application state with `mutableStateOf`
- Pattern: State hoisting with callbacks for mutations
- Example: `PropertiesPanelState` data class with copy() for immutability
- Compose reactivity: `var x by mutableStateOf(...)` triggers recomposition

**Decision**: Create `NodeGeneratorState` data class following PropertiesPanelState pattern
**Rationale**: Consistent with existing state management approach
**Alternatives Considered**:
- ViewModel pattern (rejected: not used in existing codebase)
- Redux-like store (rejected: overcomplicated for this use case)

---

### 4. Persistence Strategy

**Question**: How should CustomNodeRepository persist data across sessions?

**Research Findings**:
- `FlowGraphSerializer` uses .flow.kts DSL format for graphs
- kotlinx-serialization is available (build.gradle.kts includes it)
- File I/O examples in `ModuleSaveService.kt` and `CompilationService.kt`
- Standard app data location: `System.getProperty("user.home")/.codenode/`

**Decision**: JSON serialization via kotlinx-serialization to `~/.codenode/custom-nodes.json`
**Rationale**:
- Simple format for list of node definitions
- Human-readable for debugging
- kotlinx-serialization already in dependencies
**Alternatives Considered**:
- SQLite database (rejected: overkill for simple list)
- DSL format like flow graphs (rejected: JSON simpler for flat list)
- Binary format (rejected: not human-readable)

---

### 5. Validation Rules

**Question**: What validation is needed for the Node Generator form?

**Research Findings**:
- From spec: Name required, not empty/whitespace
- From spec: 0/0 inputs/outputs not allowed
- Valid range: 0-3 for both inputs and outputs
- GenericType format: "in{0-3}out{0-3}"

**Decision**: Validation logic in NodeGeneratorState with `isValid` computed property
**Rationale**: Follows PropertiesPanelState validation pattern
**Alternatives Considered**:
- Separate validator class (rejected: simple validation doesn't need it)
- Validation on button click only (rejected: better UX with live validation)

---

### 6. Integration with NodePalette

**Question**: How to dynamically add custom nodes to the palette?

**Research Findings**:
- NodePalette takes `List<NodeTypeDefinition>` as parameter
- Main.kt creates sample nodes with `createSampleNodeTypes()`
- State can be updated to include custom nodes
- NodePalette re-renders when list changes (Compose reactivity)

**Decision**: Add `customNodeTypes` to application state, concatenate with built-in types
**Rationale**: Clean separation of built-in vs custom nodes
**Alternatives Considered**:
- Modify createSampleNodeTypes() directly (rejected: mixes concerns)
- Separate palette section (rejected: spec says Generic section)

---

## Summary

All research questions resolved. Key decisions:

| Area | Decision |
|------|----------|
| UI Pattern | Follow NodePalette.kt/@Composable pattern |
| Node Creation | Use GenericNodeTypeFactory.createGenericNodeType() |
| State Management | NodeGeneratorState data class with copy() |
| Persistence | JSON file at ~/.codenode/custom-nodes.json |
| Validation | Computed isValid property in state |
| Palette Integration | Concatenate custom nodes with built-in types |

No NEEDS CLARIFICATION items remaining.
