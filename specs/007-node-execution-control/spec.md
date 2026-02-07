# Feature Specification: Node ExecutionState and ControlConfig

**Feature Branch**: `007-node-execution-control`
**Created**: 2026-02-07
**Status**: Draft
**Input**: User description: "Node ExecutionState and ControlConfig - Add to the GraphNode class the functionality to recursively control the execution state of its children nodes (both GraphNodes and CodeNodes). To support this, move the executionState and controlConfig properties from the CodeNode class to the Node class (so that GraphNode inherits them also.) By default, whenever the executionState or controlConfig of a node is changed, that state should be propagated to all of its child nodes. For the purpose of future debugging capability, the control of a subGraph can be selectively altered and updated from that of its parent. Create a specific class for the fbpDsl called a RootControlNode. Instances of this class will serve as a master controller for all of the nodes of a flowGraph. When CodeNodeIO generates the code for a flowGraph, it will create a new KMP module to contain it. The name of the module will be prompted for when beginning the compilation. This KMP module will embody the virtual circuitBoard concept in the CodeNodeIO toolset."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Hierarchical Execution Control (Priority: P1) 🎯 MVP

A flow developer wants to control the execution state (run, pause, stop) of an entire subgraph by controlling its parent GraphNode, rather than individually controlling each child node.

**Why this priority**: This is the core functionality that enables hierarchical flow control. Without it, developers would need to manually control each node individually, which is impractical for complex flows.

**Independent Test**: Create a GraphNode containing multiple CodeNodes, change the parent's execution state, and verify all children reflect the new state.

**Acceptance Scenarios**:

1. **Given** a GraphNode containing 3 CodeNodes all in IDLE state, **When** the GraphNode's execution state is set to RUNNING, **Then** all 3 child CodeNodes transition to RUNNING state.
2. **Given** a GraphNode containing nested GraphNodes and CodeNodes all in RUNNING state, **When** the parent GraphNode is PAUSED, **Then** all descendants (nested GraphNodes and their children) transition to PAUSED state.
3. **Given** a GraphNode in RUNNING state with 5 child nodes, **When** the GraphNode encounters an ERROR, **Then** all children are transitioned to ERROR state and processing stops.

---

### User Story 2 - Selective Subgraph Control Override (Priority: P2)

A developer debugging a flow wants to selectively control a specific subgraph independently from its parent, allowing focused debugging of a subsection of the flow.

**Why this priority**: Essential for debugging complex flows where developers need to isolate and control specific subgraphs without affecting the entire flow hierarchy.

**Independent Test**: Set a subgraph to "independent control mode", change parent state, verify subgraph retains its own state while siblings update.

**Acceptance Scenarios**:

1. **Given** a GraphNode with independent control enabled, **When** its parent GraphNode's state changes to PAUSED, **Then** this GraphNode retains its current RUNNING state.
2. **Given** a GraphNode with independent control enabled in PAUSED state, **When** the developer manually sets it to RUNNING, **Then** only this subgraph and its children start running while siblings remain paused.
3. **Given** an independently controlled subgraph, **When** independent control is disabled, **Then** the subgraph synchronizes to its parent's current state.

---

### User Story 3 - RootControlNode Master Control (Priority: P3)

A developer wants to control the entire flowGraph execution from a single master controller, providing unified start/stop/pause control for the entire system.

**Why this priority**: Provides the top-level entry point for flow execution control, essential for production deployment and testing scenarios.

**Independent Test**: Create a RootControlNode for a flowGraph, use it to start/stop the entire flow, verify all nodes respond correctly.

**Acceptance Scenarios**:

1. **Given** a RootControlNode attached to a flowGraph with multiple top-level nodes, **When** the RootControlNode executes a "start all" command, **Then** all root-level nodes and their descendants transition to RUNNING state.
2. **Given** a running flowGraph with RootControlNode, **When** the RootControlNode executes "pause all", **Then** all nodes in the graph transition to PAUSED state.
3. **Given** a RootControlNode, **When** it queries the flow's execution status, **Then** it returns an aggregated status summarizing the states of all nodes in the graph.

---

### User Story 4 - Speed Attenuation Propagation (Priority: P4)

A developer wants to slow down execution of an entire subgraph for debugging purposes, with the speed attenuation setting propagating to all child nodes.

**Why this priority**: Important debugging capability but not essential for basic execution control functionality.

**Independent Test**: Set speed attenuation on a GraphNode, verify all child nodes inherit the attenuation value and execute at the slower pace.

**Acceptance Scenarios**:

1. **Given** a GraphNode with speedAttenuation set to 500ms, **When** the setting is applied, **Then** all child CodeNodes inherit the 500ms delay between processing cycles.
2. **Given** nested GraphNodes where parent has 200ms attenuation and child has 100ms, **When** both are running, **Then** each level respects its own attenuation value (child overrides parent for its subtree).
3. **Given** a subgraph with independent control and its own speedAttenuation, **When** parent's attenuation changes, **Then** the independent subgraph retains its own attenuation setting.

---

### User Story 5 - KMP Module Generation (Priority: P5)

A developer wants to compile a flowGraph into a deployable KMP module that can be used as a standalone "virtual circuit board" component.

**Why this priority**: This is the code generation aspect which builds upon the execution control infrastructure but represents a separate delivery concern.

**Independent Test**: Compile a simple flowGraph to a named KMP module, verify the module structure is created with proper gradle configuration.

**Acceptance Scenarios**:

1. **Given** a complete flowGraph with nodes and connections, **When** the developer initiates compilation and provides a module name "MyFlowModule", **Then** a new KMP module directory is created with the specified name.
2. **Given** a flowGraph being compiled, **When** the KMP module is generated, **Then** it includes all necessary gradle configuration for multiplatform deployment.
3. **Given** a generated KMP module, **When** it is imported into another project, **Then** the flow can be instantiated and controlled via its RootControlNode.

---

### Edge Cases

- What happens when a child node is in ERROR state and parent attempts to set RUNNING?
  - The ERROR state should be cleared and node transitions to RUNNING (error recovery scenario).
- How does the system handle circular parent references?
  - Validation should prevent circular references during graph construction.
- What happens when a RootControlNode is attached to an already-running flow?
  - The RootControlNode should synchronize to the current flow state without disruption.
- What happens when an independently controlled subgraph's parent is deleted?
  - The subgraph becomes a root-level node and retains its independent state.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST move `executionState` property from CodeNode to the base Node class so all node types inherit it.
- **FR-002**: System MUST move `controlConfig` property from CodeNode to the base Node class so all node types inherit it.
- **FR-003**: GraphNode MUST propagate execution state changes to all direct children by default.
- **FR-004**: GraphNode MUST recursively propagate state changes through nested GraphNode hierarchies.
- **FR-005**: Nodes MUST support an "independent control" flag that exempts them from parent state propagation.
- **FR-006**: When independent control is enabled, a node MUST retain its own execution state regardless of parent changes.
- **FR-007**: When independent control is disabled, the node MUST synchronize to its parent's current state.
- **FR-008**: System MUST provide a RootControlNode class that serves as master controller for a flowGraph.
- **FR-009**: RootControlNode MUST be able to start, pause, and stop all nodes in its attached flowGraph.
- **FR-010**: RootControlNode MUST provide an aggregated status query that summarizes node states across the flow.
- **FR-011**: ControlConfig changes (including speedAttenuation) MUST propagate to children following the same rules as executionState.
- **FR-012**: Child nodes with their own ControlConfig settings MUST override inherited values for their subtree.
- **FR-013**: System MUST generate KMP modules from flowGraphs with a developer-specified module name.
- **FR-014**: Generated KMP modules MUST include proper gradle configuration for multiplatform deployment.
- **FR-015**: Generated KMP modules MUST expose a RootControlNode for flow execution control.
- **FR-016**: System MUST validate that no circular parent-child references exist in the node hierarchy.
- **FR-017**: Backward compatibility MUST be maintained - existing CodeNode usage should not break.

### Key Entities

- **Node**: Base entity extended with `executionState` and `controlConfig` properties (moved from CodeNode).
- **GraphNode**: Container node now capable of hierarchical execution control over children, with optional independent control flag.
- **CodeNode**: Terminal processing node that continues to support execution state and control config (now inherited from Node).
- **RootControlNode**: New entity serving as master controller for an entire flowGraph, providing unified execution control.
- **ControlConfig**: Configuration for execution control (pauseBufferSize, speedAttenuation, autoResumeOnError) plus new `independentControl` flag.
- **ExecutionState**: Enum (IDLE, RUNNING, PAUSED, ERROR) now available on all Node types.
- **KMP Module**: Generated output representing a compiled flowGraph as a deployable "virtual circuit board" component.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: State changes propagate through a 5-level deep hierarchy within 100 milliseconds.
- **SC-002**: Developers can control execution of an entire subgraph with a single operation instead of N operations (where N = number of child nodes).
- **SC-003**: 100% of existing tests continue to pass after moving executionState and controlConfig to Node base class.
- **SC-004**: Independent control mode correctly isolates a subgraph from parent state changes in 100% of test scenarios.
- **SC-005**: RootControlNode can manage flowGraphs with 100+ nodes without performance degradation.
- **SC-006**: Generated KMP modules compile successfully and pass gradle build verification.
- **SC-007**: Developers can specify a custom module name and see it reflected in the generated project structure.

## Assumptions

- The existing Node sealed class structure can accommodate new abstract properties without breaking binary compatibility.
- ExecutionState enum values (IDLE, RUNNING, PAUSED, ERROR) are sufficient for all control scenarios.
- KMP module generation will use standard gradle conventions consistent with existing CodeNodeIO project structure.
- The virtual circuit board concept maps to a self-contained KMP module with its own RootControlNode entry point.
- Speed attenuation propagation follows the same inheritance pattern as execution state (parent to children, overridable at any level).
