# Research: Filesystem-Driven Node Palette

**Feature**: 055-filesystem-node-palette
**Date**: 2026-03-19

## Decision 1: Single Category Enum

**Decision**: Use `CodeNodeType` (9 values) as the sole category system, removing `NodeCategory` and `NodeTypeDefinition.NodeCategory`.

**Rationale**: Three separate enums with mapping layers between them creates confusion and bugs. CodeNodeType is already serialized in .flow.kts files, already has distinct canvas colors per type, and already covers all meaningful classifications. The 4-value NodeCategory (SOURCE/TRANSFORMER/PROCESSOR/SINK) is too coarse — it collapses FILTER, SPLITTER, MERGER, VALIDATOR into TRANSFORMER/PROCESSOR. The 7-value NodeTypeDefinition.NodeCategory mixes UI concerns (UI_COMPONENT, SERVICE) with domain concepts.

**Alternatives considered**:
- Keep all three enums with mapping layers — rejected: too complex, confusing, source of bugs
- Collapse to 4 values (NodeCategory) — rejected: user wants all 9 CodeNodeType values visible
- Keep CodeNodeType at 11, only change palette grouping — rejected: CUSTOM and GENERIC are legacy artifacts

## Decision 2: PROCESSOR Category Migration

**Decision**: Map existing `NodeCategory.PROCESSOR` usages to specific CodeNodeType values based on port patterns.

**Rationale**: PROCESSOR is a catch-all for multi-input/multi-output nodes. CodeNodeType already has more specific types (MERGER, SPLITTER, TRANSFORMER) that better describe the node's function. Mapping by port pattern gives the most accurate categorization.

**Alternatives considered**:
- Add PROCESSOR to CodeNodeType — rejected: user explicitly wants 9 values, PROCESSOR is too vague
- Map all PROCESSOR to TRANSFORMER — rejected: loses semantic information for merger/splitter patterns

## Decision 3: Backward-Compatible Deserialization

**Decision**: Update FlowGraphDsl.kt to map removed enum names ("CUSTOM", "GENERIC") to TRANSFORMER as fallback.

**Rationale**: Existing .flow.kts files contain serialized `nodeType = "GENERIC"` strings. Rather than breaking deserialization, catch these in the existing try/catch and map to TRANSFORMER (the most common and safest default). This preserves backward compatibility without keeping dead enum values.

**Alternatives considered**:
- Force migration of all .flow.kts files — rejected: high risk, many files across modules
- Keep CUSTOM/GENERIC in enum but hide from UI — rejected: contradicts spec FR-012

## Decision 4: Palette Display Strategy

**Decision**: Show only populated categories in palette; show all 9 in generator dropdown.

**Rationale**: A typical project uses 3-4 of the 9 types. Showing 5-6 empty category headers clutters the palette. The generator needs all 9 visible since the user is choosing what to create.

**Alternatives considered**:
- Always show all 9 in both — rejected: palette becomes cluttered
- Show only populated in both — rejected: generator should expose full taxonomy

## Decision 5: CodeNodeDefinition.category Property Type

**Decision**: Change `CodeNodeDefinition.category` from `NodeCategory` to `CodeNodeType`.

**Rationale**: Since NodeCategory is being deleted, all CodeNodeDefinition implementations must use CodeNodeType directly. The property name `category` remains the same, only the type changes. This requires updating all 20 CodeNodeDefinition implementation files (import change + enum value change).

**Alternatives considered**:
- Rename property to `codeNodeType` — rejected: unnecessary churn, `category` is semantically correct
- Add typealias NodeCategory = CodeNodeType — rejected: adds confusion, doesn't clean up

## Decision 6: NodeDefinitionRegistry Template Parsing

**Decision**: Update regex in `parseTemplateMetadata()` to match `CodeNodeType.XXXXX` instead of `NodeCategory.XXXXX`.

**Rationale**: Template .kt files in `~/.codenode/nodes/` contain `override val category = NodeCategory.SOURCE`. After migration, these will contain `override val category = CodeNodeType.SOURCE`. The regex must match the new pattern. Existing template files on disk will need the same update.

**Alternatives considered**:
- Support both patterns in regex — rejected: adds complexity for temporary backward compatibility
- Skip template parsing, rely on compiled discovery only — rejected: Universal-level nodes need parsing
