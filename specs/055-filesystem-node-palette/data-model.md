# Data Model: Filesystem-Driven Node Palette

**Feature**: 055-filesystem-node-palette
**Date**: 2026-03-19

## Entities

### CodeNodeType (Modified Enum)

The single category classification used across the entire system.

**Before** (11 values):
SOURCE, SINK, TRANSFORMER, FILTER, SPLITTER, MERGER, VALIDATOR, API_ENDPOINT, DATABASE, ~~CUSTOM~~, ~~GENERIC~~

**After** (9 values):
SOURCE, SINK, TRANSFORMER, FILTER, SPLITTER, MERGER, VALIDATOR, API_ENDPOINT, DATABASE

**Used by**: CodeNode.codeNodeType, NodeTypeDefinition.category, CodeNodeDefinition.category, NodePaletteViewModel.expandedCategories, NodeGeneratorViewModel.category

### NodeTypeDefinition (Modified)

Represents a node type available in the palette for drag-and-drop placement.

| Field | Type (Before) | Type (After) | Notes |
|-------|---------------|--------------|-------|
| id | String | String | No change |
| name | String | String | No change |
| category | NodeTypeDefinition.NodeCategory | CodeNodeType | Enum type change |
| description | String | String | No change |
| portTemplates | List\<PortTemplate\> | List\<PortTemplate\> | No change |
| defaultConfiguration | Map\<String, String\> | Map\<String, String\> | No change |
| configurationSchema | String? | String? | No change |
| codeTemplates | Map\<String, String\> | Map\<String, String\> | No change |

**Nested enum deleted**: `NodeTypeDefinition.NodeCategory` (UI_COMPONENT, SERVICE, TRANSFORMER, VALIDATOR, API_ENDPOINT, DATABASE, GENERIC) — replaced by CodeNodeType.

### CodeNodeDefinition (Modified Interface)

Self-contained node definition interface.

| Property | Type (Before) | Type (After) | Notes |
|----------|---------------|--------------|-------|
| category | NodeCategory | CodeNodeType | Enum type change |

**Top-level enum deleted**: `NodeCategory` (SOURCE, TRANSFORMER, PROCESSOR, SINK) — replaced by CodeNodeType.

**Method updated**: `toNodeTypeDefinition()` — removes palette category mapping, passes CodeNodeType directly.

### NodeTemplateMeta (Modified, in NodeDefinitionRegistry)

Metadata parsed from template .kt files on the filesystem.

| Field | Type (Before) | Type (After) | Notes |
|-------|---------------|--------------|-------|
| category | NodeCategory | CodeNodeType | Enum type change |

### NodePaletteState (Modified)

| Field | Type (Before) | Type (After) | Notes |
|-------|---------------|--------------|-------|
| expandedCategories | Set\<NodeTypeDefinition.NodeCategory\> | Set\<CodeNodeType\> | Enum type change |

### NodeGeneratorState (Modified)

| Field | Type (Before) | Type (After) | Notes |
|-------|---------------|--------------|-------|
| category | NodeCategory | CodeNodeType | Enum type change |

## Removed Entities

### NodeTypeDefinition.NodeCategory (Deleted)
7-value enum: UI_COMPONENT, SERVICE, TRANSFORMER, VALIDATOR, API_ENDPOINT, DATABASE, GENERIC

### NodeCategory (Deleted)
4-value enum: SOURCE, TRANSFORMER, PROCESSOR, SINK

## Migration Mapping

### NodeCategory → CodeNodeType

| NodeCategory | CodeNodeType | Condition |
|-------------|-------------|-----------|
| SOURCE | SOURCE | Direct |
| TRANSFORMER | TRANSFORMER | Direct |
| PROCESSOR | TRANSFORMER | Default for 2+ in, 2+ out |
| PROCESSOR | MERGER | When 2+ in, 1 out |
| PROCESSOR | SPLITTER | When 1 in, 2+ out |
| PROCESSOR | SINK | When 2+ in, 0 out (repository pattern) |
| SINK | SINK | Direct |

### NodeTypeDefinition.NodeCategory → CodeNodeType

| NodeTypeDefinition.NodeCategory | CodeNodeType |
|--------------------------------|-------------|
| UI_COMPONENT | SOURCE |
| SERVICE | TRANSFORMER |
| TRANSFORMER | TRANSFORMER |
| VALIDATOR | VALIDATOR |
| API_ENDPOINT | API_ENDPOINT |
| DATABASE | DATABASE |
| GENERIC | TRANSFORMER |
