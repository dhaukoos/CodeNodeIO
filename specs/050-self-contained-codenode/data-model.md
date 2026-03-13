# Data Model: Self-Contained CodeNode Definition

**Feature**: 050-self-contained-codenode | **Date**: 2026-03-13

## Entities

### CodeNodeDefinition (Interface)

The core interface that all self-contained node definitions implement. Provides metadata for palette display AND runtime creation capability.

| Field | Type | Description |
|-------|------|-------------|
| name | String | Unique display name (e.g., "SepiaTransformer") |
| category | NodeCategory | Source, Transformer, Processor, Sink — determines palette grouping and color |
| description | String? | Optional human-readable description |
| inputPorts | List\<PortSpec\> | Input port definitions (name, data type) |
| outputPorts | List\<PortSpec\> | Output port definitions (name, data type) |

**Key Method**:
- `createRuntime(name: String): NodeRuntime<*>` — Creates a NodeRuntime instance with the processing logic already embedded. The runtime type (SourceRuntime, TransformerRuntime, In2Out1Runtime, etc.) is determined by the port configuration.

**Validation Rules**:
- `name` must be non-blank and unique within the registry scope
- `inputPorts` and `outputPorts` cannot both be empty
- Port names must be unique within their direction (input vs output)

### PortSpec

Describes a single port on a node definition.

| Field | Type | Description |
|-------|------|-------------|
| name | String | Port display name (e.g., "image", "result") |
| dataType | KClass<*> | The data type flowing through this port |

### NodeCategory (Enum)

Determines palette grouping and visual color coding.

| Value | Color | Runtime Pattern |
|-------|-------|-----------------|
| SOURCE | Green | 0 inputs, 1+ outputs — emits data |
| TRANSFORMER | Blue | 1 input, 1 output — transforms data |
| PROCESSOR | Blue | 2+ inputs and/or 2+ outputs — processes data |
| SINK | Red | 1+ inputs, 0 outputs — consumes data |

### NodeDefinitionRegistry

Central registry that discovers and manages all available node definitions.

| Field | Type | Description |
|-------|------|-------------|
| compiledNodes | Map\<String, CodeNodeDefinition\> | Nodes discovered from classpath (Module + Project levels) |
| templateNodes | Map\<String, NodeTemplateMeta\> | Nodes discovered from Universal level (metadata only) |
| legacyNodes | List\<CustomNodeDefinition\> | Backward-compatible legacy custom nodes |

**Key Methods**:
- `discoverAll()` — Scans classpath and filesystem for node definitions
- `getByName(name: String): CodeNodeDefinition?` — Looks up a compiled node by name
- `getAllForPalette(): List<NodeTypeDefinition>` — Returns palette-ready entries from all sources
- `isCompiled(name: String): Boolean` — Whether the node is on the classpath (executable) or template-only

### NodeTemplateMeta

Metadata parsed from Universal-level node source files (not compiled).

| Field | Type | Description |
|-------|------|-------------|
| name | String | Node name parsed from file |
| category | NodeCategory | Category parsed from file |
| inputCount | Int | Number of input ports |
| outputCount | Int | Number of output ports |
| filePath | String | Absolute path to the source file |

## Relationships

```
CodeNodeDefinition (interface)
    ├── implements → {NodeName}CodeNode (object, one per node)
    ├── has many → PortSpec (input and output ports)
    ├── has one → NodeCategory
    └── creates → NodeRuntime<*> (via createRuntime())

NodeDefinitionRegistry
    ├── contains → CodeNodeDefinition (compiled, from classpath)
    ├── contains → NodeTemplateMeta (templates, from filesystem)
    ├── contains → CustomNodeDefinition (legacy, from JSON)
    └── provides → NodeTypeDefinition (for palette display)

FlowGraph
    └── contains → CodeNode instances (references NodeDefinition by name)
```

## State Transitions

### Node Lifecycle

```
Generated (file created by Node Generator)
    ↓ user edits processing logic
Defined (file has valid processing logic)
    ↓ Gradle compilation
Compiled (on classpath, discoverable by registry)
    ↓ user drags to canvas
Instantiated (CodeNode instance in FlowGraph)
    ↓ runtime preview start
Running (NodeRuntime executing processing logic)
```

### Registry Discovery Flow

```
Startup
    ├── Scan classpath for CodeNodeDefinition implementations → compiledNodes
    ├── Scan ~/.codenode/nodes/ for .kt files → templateNodes (metadata only)
    └── Load ~/.codenode/custom-nodes.json → legacyNodes
```
