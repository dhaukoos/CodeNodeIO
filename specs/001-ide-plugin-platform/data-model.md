# Data Model: CodeNodeIO IDE Plugin Platform

**Feature**: CodeNodeIO IDE Plugin Platform
**Branch**: `001-ide-plugin-platform`
**Date**: 2026-01-13

## Overview

This document defines the core data entities for the CodeNodeIO platform. The domain model follows Flow-based Programming (FBP) principles as described by J. Paul Morrison, with hierarchical graph nodes, ports for data flow, and Information Packets (IPs) as the data carriers.

## Core Entities

### InformationPacket (IP)

**Purpose**: Represents data flowing through the FBP graph. IPs are immutable data carriers that move between nodes through connections.

**Attributes**:
- `id`: Unique identifier (UUID)
- `type`: Data type descriptor (String, Number, Object, Custom)
- `payload`: The actual data (polymorphic - can be primitive or complex object)
- `metadata`: Optional metadata (source node, timestamp, trace ID)

**Validation Rules**:
- `id` must be unique within a flow execution
- `type` must match the expected type of the receiving port
- `payload` must be serializable for persistence and debugging

**State Transitions**:
- Created → In Transit → Consumed
- IPs are immutable once created
- Consumed IPs can be archived for debugging/replay

**Relationships**:
- Flows through `Connection` from source `Port` to target `Port`
- Created by source `CodeNode`, consumed by target `CodeNode`

---

### Port

**Purpose**: Entry or exit point on a Node for data flow. Ports define how nodes can be connected and what types of data they accept/emit.

**Attributes**:
- `id`: Unique identifier (UUID)
- `name`: Human-readable name (e.g., "input", "output", "error")
- `direction`: Enum (INPUT, OUTPUT)
- `dataType`: Expected InformationPacket type
- `required`: Boolean (must be connected for valid graph)
- `defaultValue`: Optional default IP if input port unconnected
- `validationRules`: Optional validation logic for incoming IPs
- `owningNodeId`: Reference to parent Node

**Validation Rules**:
- INPUT ports can have at most one incoming Connection
- OUTPUT ports can have multiple outgoing Connections
- `dataType` must be compatible with connected Port's dataType
- `required` INPUT ports must be connected for graph validation to pass

**Relationships**:
- Belongs to exactly one `Node` (CodeNode or GraphNode)
- Connected to other Ports via `Connection` entities
- For GraphNodes, ports are references to child CodeNode ports

---

### Node (Abstract)

**Purpose**: Base entity for components in the FBP graph. Represents a unit of processing or organizational container.

**Subtypes**:
- `CodeNode`: Terminal node with execution logic (no children)
- `GraphNode`: Hierarchical container node (has children, no own execution)

**Common Attributes**:
- `id`: Unique identifier (UUID)
- `name`: Human-readable name
- `nodeType`: String (e.g., "Transformer", "Validator", "APIEndpoint", "Container")
- `description`: Optional documentation
- `position`: Visual canvas position (x, y coordinates)
- `inputPorts`: List of INPUT Port entities
- `outputPorts`: List of OUTPUT Port entities
- `configuration`: Key-value property map
- `parentNodeId`: Optional reference to parent GraphNode (null for root)

**Validation Rules**:
- `name` must be unique within parent scope
- At least one port (input or output) must exist
- Position coordinates must be non-negative
- Configuration must conform to NodeTypeDefinition schema

---

### CodeNode

**Purpose**: Terminal node in the graph hierarchy that executes business logic. Controlled by long-running coroutines (Kotlin) or goroutines (Go).

**Attributes** (in addition to Node):
- `executionState`: Enum (IDLE, RUNNING, PAUSED, ERROR)
- `coroutineHandle`: Runtime reference to controlling coroutine
- `processingLogic`: Reference to code template or custom implementation
- `controlConfig`: Pause/resume/speed attenuation settings

**Execution Model**:
- Runs simple loop: listen on input ports → process → emit on output ports
- Supports control operations:
  - Pause: Halt processing, buffer incoming IPs
  - Resume: Continue processing from paused state
  - Speed attenuation: Introduce delays for simulation/debugging

**Validation Rules**:
- Cannot have child nodes (terminal in hierarchy)
- Must have at least one input port OR one output port
- `processingLogic` must be defined before code generation

**Relationships**:
- Inherits from `Node`
- Can be child of `GraphNode`
- Executes with references to connected `Port` channels

---

### GraphNode

**Purpose**: Hierarchical container node functioning as a "virtual circuit board". Groups related logic flows without own execution.

**Attributes** (in addition to Node):
- `childNodes`: List of child Node entities (CodeNodes or nested GraphNodes)
- `internalConnections`: List of Connection entities between child nodes
- `portMappings`: Map of GraphNode ports to child node ports

**Control Inheritance**:
- GraphNodes do NOT have their own controlling coroutine
- Child nodes reactively inherit control state from parent
- Pause/resume operations propagate to all descendants
- All GraphNode ports are references to descendant CodeNode ports

**Validation Rules**:
- Must have at least one child node
- Cannot create circular parent-child relationships
- Port mappings must reference actual child ports
- Internal connections must only link child nodes

**Relationships**:
- Inherits from `Node`
- Contains child `Node` entities (composition)
- Can be nested within another GraphNode

---

### Connection (Edge)

**Purpose**: Represents data flow link between two ports. Implemented as channels in Kotlin or Go.

**Attributes**:
- `id`: Unique identifier (UUID)
- `sourceNodeId`: Reference to source Node
- `sourcePortId`: Reference to source OUTPUT Port
- `targetNodeId`: Reference to target Node
- `targetPortId`: Reference to target INPUT Port
- `transformationLogic`: Optional IP transformation function
- `channelCapacity`: Buffer size for channel (default: 0 = unbuffered)

**Validation Rules**:
- Source port must be OUTPUT direction
- Target port must be INPUT direction
- Source and target ports must have compatible dataTypes
- Cannot create cycles in non-hierarchical graphs (validation rule)
- Source and target nodes must be in same parent scope or properly mapped

**Relationships**:
- Links exactly one source `Port` to one target `Port`
- Belongs to parent `GraphNode` scope (or root FlowGraph)

---

### FlowGraph

**Purpose**: Top-level container representing a complete application feature or system.

**Attributes**:
- `id`: Unique identifier (UUID)
- `name`: Human-readable name
- `version`: Semantic version (MAJOR.MINOR.PATCH)
- `description`: Feature documentation
- `rootNodes`: List of top-level Node entities
- `metadata`: Project metadata (author, created date, tags)
- `targetPlatforms`: List of code generation targets (KMP-Android, KMP-iOS, Go-Server, etc.)

**Persistence**:
- Serialized to DSL file using Kotlin infix functions
- Example DSL syntax:
  ```kotlin
  val graph = flowGraph("UserValidation") {
      val inputNode = codeNode("UserInput") {
          output("userData", DataType.User)
      }
      val validatorNode = codeNode("EmailValidator") {
          input("user", DataType.User)
          output("valid", DataType.User)
          output("error", DataType.Error)
      }
      inputNode.output("userData") connect validatorNode.input("user")
  }
  ```

**Validation Rules**:
- `name` must be valid filename (for DSL persistence)
- `version` must follow semver format
- All referenced nodes must exist in graph
- No orphaned nodes (disconnected from any path to root)
- Graph must be acyclic at CodeNode level (GraphNodes can contain cycles internally)

**Relationships**:
- Contains root-level `Node` entities
- References `NodeTypeDefinition` catalog

---

### NodeTypeDefinition

**Purpose**: Template defining what a node can do. Catalog of available node types in the palette.

**Attributes**:
- `id`: Unique identifier (UUID)
- `name`: Display name (e.g., "HTTP GET Request", "JSON Transformer")
- `category`: Enum (UI_COMPONENT, SERVICE, TRANSFORMER, VALIDATOR, API_ENDPOINT, DATABASE)
- `description`: User-facing documentation
- `portTemplates`: List of port specifications (name, direction, dataType, required)
- `defaultConfiguration`: Default property values
- `configurationSchema`: JSON Schema for property validation
- `codeTemplates`: Map of platform → code generation template
  - KMP: Kotlin code template
  - Go: Go code template

**Validation Rules**:
- `name` must be unique within category
- Port templates must have unique names
- Configuration schema must be valid JSON Schema
- Code templates must be valid for target language

**Relationships**:
- Referenced by `Node` entities via `nodeType` attribute
- Stored in central catalog (part of IDE plugin resources)

---

### PropertyConfiguration

**Purpose**: Runtime configuration for a node instance, defining its behavior.

**Attributes**:
- `nodeId`: Reference to owning Node
- `properties`: Map<String, Any> of property key-value pairs
- `validationErrors`: List of current validation errors

**Property Types**:
- Primitives: String, Number, Boolean
- Complex: Object (JSON-serializable)
- Expressions: String references to other node outputs

**Validation**:
- Must conform to NodeTypeDefinition.configurationSchema
- Expressions must reference valid paths in graph

**Relationships**:
- Belongs to exactly one `Node`
- Validated against `NodeTypeDefinition` schema

---

### GeneratedProject

**Purpose**: Output artifact from code generation process.

**Attributes**:
- `id`: Unique identifier (UUID)
- `sourceGraphId`: Reference to source FlowGraph
- `platform`: Target platform (KMP-Android, KMP-iOS, KMP-Web, Go-Server)
- `projectStructure`: File tree representation
- `generatedFiles`: Map of file path → file content
- `buildConfiguration`: Platform-specific build files
  - KMP: build.gradle.kts, settings.gradle.kts
  - Go: go.mod, Makefile
- `dependencies`: List of external dependencies with licenses
- `licenseValidation`: Validation results (pass/fail per dependency)
- `generatedAt`: Timestamp

**Validation Rules**:
- All dependencies must pass license validation (no GPL/LGPL/AGPL)
- Generated code must compile (verified by contract tests)
- File paths must not conflict

**Relationships**:
- Generated from one `FlowGraph`
- References `NodeTypeDefinition` code templates

---

## Entity Relationship Diagram

```text
FlowGraph (1) ──→ (*) Node (abstract)
                     ├─→ CodeNode (terminal, has execution)
                     └─→ GraphNode (container, no execution)
                              └─→ (*) Node (children)

Node (1) ──→ (*) Port (INPUT/OUTPUT)

Port (1) ←──→ (1) Connection ←──→ (1) Port

Node (1) ──→ (1) PropertyConfiguration

Node (*) ──→ (1) NodeTypeDefinition (catalog)

FlowGraph (1) ──→ (*) GeneratedProject

Connection channels carry ──→ InformationPacket (runtime data)
```

## Key Invariants

1. **Hierarchy Constraint**: CodeNodes are terminal (no children), GraphNodes are containers (no execution)

2. **Port Ownership**: Every Port belongs to exactly one Node

3. **Connection Validity**: Connections only link OUTPUT → INPUT with compatible types

4. **Acyclic CodeNodes**: CodeNode-to-CodeNode connections must not form cycles (enables deterministic execution order)

5. **Control Inheritance**: GraphNode children inherit parent's control state (pause/resume/speed)

6. **License Compliance**: Generated code dependencies MUST pass license validation (no GPL/LGPL/AGPL)

7. **Type Safety**: InformationPacket type must match Port dataType constraints

## Data Flow Example

```text
User creates FlowGraph
  ↓
Add Nodes (from NodeTypeDefinition catalog)
  ↓
Configure Nodes (PropertyConfiguration)
  ↓
Connect Ports (create Connection entities)
  ↓
Validate Graph (check all invariants)
  ↓
Generate Code (create GeneratedProject)
  ↓
Compile & Deploy

Runtime:
CodeNode coroutine listens on input Port channels
  ↓
InformationPacket arrives
  ↓
Process using PropertyConfiguration logic
  ↓
Emit InformationPacket on output Port channel
  ↓
Connection channel carries IP to next CodeNode
```

## Persistence Format

**FlowGraph Persistence**: DSL files using Kotlin infix functions (text-based, version control friendly)

**NodeTypeDefinition Catalog**: JSON or YAML files bundled with IDE plugin

**PropertyConfiguration**: Embedded in DSL files as nested configuration blocks

**GeneratedProject**: Standard project structures (KMP Gradle, Go modules) written to filesystem

## Future Considerations

- **Versioning**: Track FlowGraph version history for migration
- **Diff/Merge**: Support for concurrent editing and conflict resolution
- **Templates**: Pre-built FlowGraph templates for common patterns
- **Custom NodeTypes**: User-defined node types with custom code templates
- **Bidirectional Sync**: Update FlowGraph from manually edited generated code (round-trip engineering)
