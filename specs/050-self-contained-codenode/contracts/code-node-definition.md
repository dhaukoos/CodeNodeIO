# Contract: CodeNodeDefinition Interface

**Feature**: 050-self-contained-codenode | **Date**: 2026-03-13

## Interface: CodeNodeDefinition

The contract that all self-contained node definitions must implement.

### Properties

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| name | String | Yes | Unique node name for palette and registry lookup |
| category | NodeCategory | Yes | Determines palette group, color coding, and runtime type |
| description | String? | No | Human-readable description shown in palette tooltip |
| inputPorts | List\<PortSpec\> | Yes | Input port definitions (may be empty for sources) |
| outputPorts | List\<PortSpec\> | Yes | Output port definitions (may be empty for sinks) |

### Methods

#### createRuntime

```
createRuntime(name: String): NodeRuntime<*>
```

Creates a fully configured NodeRuntime instance with processing logic embedded.

**Parameters**:
- `name`: Instance name for the runtime (may differ from definition name if multiple instances exist)

**Returns**: A NodeRuntime subclass appropriate for the port configuration:
- 0 inputs, 1 output → SourceRuntime
- 0 inputs, 2 outputs → SourceOut2Runtime
- 1 input, 0 outputs → SinkRuntime
- 1 input, 1 output → TransformerRuntime
- 2 inputs, 1 output → In2Out1Runtime
- (etc. for all runtime type combinations)

**Preconditions**:
- Processing logic must be implemented (not the placeholder)

**Postconditions**:
- Returned runtime has no channels wired (caller must wire)
- Returned runtime is in IDLE state

#### toNodeTypeDefinition

```
toNodeTypeDefinition(): NodeTypeDefinition
```

Converts the node definition to a NodeTypeDefinition for palette display.

**Returns**: NodeTypeDefinition with correct name, category, port templates, and configuration.

## Contract: NodeDefinitionRegistry

### Methods

#### discoverAll

```
discoverAll()
```

Scans all three levels for node definitions. Populates internal maps.

**Side effects**:
- Classpath scan populates `compiledNodes`
- Filesystem scan populates `templateNodes`
- JSON load populates `legacyNodes`

#### getByName

```
getByName(name: String): CodeNodeDefinition?
```

**Returns**: The compiled node definition, or null if not found or not compiled.

**Used by**: Runtime flow resolution when wiring a pipeline.

#### getAllForPalette

```
getAllForPalette(): List<NodeTypeDefinition>
```

**Returns**: Merged list from all three sources (compiled, template, legacy), suitable for Node Palette display.

**Ordering**: Compiled nodes first, then templates (marked as non-executable), then legacy.

## Contract: Node Generator

### generateNode

```
generateNode(name: String, inputCount: Int, outputCount: Int, category: NodeCategory, level: PlacementLevel): File
```

**Parameters**:
- `name`: Node name (PascalCase)
- `inputCount`: Number of input ports (0-3)
- `outputCount`: Number of output ports (0-3)
- `category`: Source, Transformer, Processor, or Sink
- `level`: Module, Project, or Universal

**Returns**: The generated file path.

**Preconditions**:
- Name must not conflict with existing node in any level
- At least one port must be defined (inputCount + outputCount > 0)

**Postconditions**:
- File exists at the appropriate path for the selected level
- File compiles without errors
- File contains a processing logic placeholder (pass-through or default emitter)

**Error conditions**:
- Name conflict → error with existing node's location
- Invalid port configuration → validation error
