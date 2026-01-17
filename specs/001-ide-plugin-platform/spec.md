# Feature Specification: CodeNodeIO IDE Plugin Platform

**Feature Branch**: `001-ide-plugin-platform`
**Created**: 2026-01-13
**Status**: Draft
**Input**: User description: "CodeNodeIO is an IDE plugin (e.g., for Android Studio, IntelliJ, GoLand) CAD tool for creating full-stack applications based on Kotlin Multiplatform (KMP) for frontend apps (Android, iOS, and Web) and Go for their backend server-side services. The key common architecture pattern used is Flow-based programing (FBP) as described by J. Paul Morrison. A hybrid visual/textual graphEditor supports creation of graphNodes that function as virtual circuit boards of business logic. CodeNodeIO creates a paradigm-shifting approach to software development, using CAD tools to create a higher-level abstraction for greatly enhanced comprehensibility. The transition from modern 'hand-coded' software to the CodeNodeIO model is a direct echo of the shift from the Artisan Era to the Industrial Revolution."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Visual Flow Graph Creation (Priority: P1)

A developer opens their JetBrains IDE (Android Studio, IntelliJ IDEA, or GoLand) and needs to create a new full-stack application feature using Flow-based Programming principles. They access the CodeNodeIO plugin, create a new flow graph, and use the visual editor to drag and drop nodes onto a canvas, connecting them to define data flow and business logic. The nodes represent components, services, or operations, and connections represent how information packets flow between them.

**Why this priority**: This is the foundation of the entire platform. Without the ability to create and visualize flow graphs, no other features can function. It delivers immediate value by allowing developers to think in terms of data flow rather than imperative code.

**Independent Test**: Can be fully tested by creating a simple flow graph with 3-5 nodes and connections, saving it, reopening the IDE, and verifying the graph persists and displays correctly.

**Acceptance Scenarios**:

1. **Given** the CodeNodeIO plugin is installed in a JetBrains IDE, **When** the developer creates a new flow graph, **Then** a blank visual canvas appears with a palette of available node types
2. **Given** a flow graph canvas is open, **When** the developer drags a node from the palette onto the canvas, **Then** the node appears on the canvas with labeled input and output ports
3. **Given** two nodes exist on the canvas, **When** the developer draws a connection from one node's output port to another node's input port, **Then** a visual connection line appears showing the data flow direction
4. **Given** a flow graph has been created, **When** the developer saves the project, **Then** the graph structure is persisted and can be reopened without data loss
5. **Given** an invalid connection is attempted (incompatible port types), **When** the developer tries to connect them, **Then** the system prevents the connection and displays a clear error message

---

### User Story 2 - Textual Representation Viewing (Priority: P2)

A developer has created a flow graph visually and wants to understand the underlying structure or make precise edits. They switch to a textual view that shows the flow graph in a human-readable format (such as FBP notation or structured text). They can read the textual representation to understand the flow's structure, verify connections, and identify any issues.

**Why this priority**: The hybrid visual/textual approach is a core differentiator. Allowing developers to view and understand the textual representation builds trust in the tool and supports learning. This is essential before enabling full bidirectional editing.

**Independent Test**: Can be fully tested by creating a flow graph with 5+ nodes and connections, switching to textual view, and verifying that all nodes, ports, and connections are accurately represented in readable text format.

**Acceptance Scenarios**:

1. **Given** a flow graph is open in visual mode, **When** the developer switches to textual view, **Then** the complete graph structure is displayed in FBP notation or equivalent format
2. **Given** the textual representation is displayed, **When** the developer examines a node definition, **Then** the node's type, name, and all port configurations are clearly shown
3. **Given** the textual representation shows connections, **When** the developer reads a connection definition, **Then** the source node, output port, target node, and input port are unambiguously identified
4. **Given** the developer is in textual view, **When** they switch back to visual view, **Then** the visual canvas displays exactly the same graph structure

---

### User Story 3 - KMP Frontend Code Generation (Priority: P3)

A developer has designed a flow graph representing frontend application logic for a mobile/web app. They trigger code generation for Kotlin Multiplatform, and the system produces scaffolded KMP code (for Android, iOS, and Web targets) that implements the flow's business logic. The generated code follows FBP principles, is properly structured, includes type-safe components, and can be compiled and run.

**Why this priority**: Code generation is the primary value proposition - transforming visual designs into working code. Frontend code generation comes before backend because it's typically where developers start when building applications. This proves the concept works.

**Independent Test**: Can be fully tested by creating a flow graph representing a simple feature (e.g., user input validation and display), generating KMP code, compiling it, and verifying it runs on at least one target platform (Android or Web).

**Acceptance Scenarios**:

1. **Given** a valid flow graph is defined, **When** the developer triggers KMP code generation, **Then** the system produces a complete KMP project structure with common, Android, iOS, and Web source sets
2. **Given** KMP code has been generated, **When** the developer examines the code, **Then** each node in the flow graph has a corresponding KMP component or function with proper type annotations
3. **Given** generated KMP code exists, **When** the developer compiles the project, **Then** the code compiles successfully without errors for all target platforms
4. **Given** connections exist in the flow graph, **When** code is generated, **Then** the generated code includes proper data flow mechanisms (e.g., flows, channels, or callbacks) connecting the components
5. **Given** the generated code follows the project constitution, **When** license validation runs, **Then** all generated dependencies use permissive licenses (MIT, Apache 2.0, BSD-3-Clause, or MPL 2.0)

---

### User Story 4 - Go Backend Code Generation (Priority: P4)

A developer has designed a flow graph representing backend services, APIs, and data processing logic. They trigger code generation for Go, and the system produces a Go project with properly structured packages, handlers, and service implementations that follow the flow graph's design. The generated Go code is idiomatic, follows best practices, and can be built and deployed.

**Why this priority**: Backend code generation completes the full-stack story. It comes after frontend generation because backend services typically support frontend applications, and proving the concept with frontend first reduces risk.

**Independent Test**: Can be fully tested by creating a flow graph representing a simple API endpoint (e.g., GET user data), generating Go code, building the binary, and verifying the endpoint responds correctly when called.

**Acceptance Scenarios**:

1. **Given** a flow graph representing backend logic is defined, **When** the developer triggers Go code generation, **Then** the system produces a Go module with proper package structure and go.mod file
2. **Given** Go code has been generated, **When** the developer examines the code, **Then** each node in the flow graph has a corresponding Go function, struct, or handler with proper type definitions
3. **Given** generated Go code exists, **When** the developer runs `go build`, **Then** the code compiles successfully without errors
4. **Given** API endpoints are defined in the flow graph, **When** code is generated, **Then** the generated code includes HTTP handlers with proper routing and middleware
5. **Given** the generated code follows the project constitution, **When** license validation runs, **Then** all Go module dependencies use permissive licenses (no GPL, LGPL, or AGPL)

---

### User Story 5 - Node Configuration and Properties (Priority: P5)

A developer has placed nodes on the flow graph canvas and needs to configure their behavior. They select a node and open a properties panel where they can set parameters, define business rules, specify data transformations, and configure validation logic. These configurations determine how the node processes information packets and are reflected in the generated code.

**Why this priority**: Basic node placement (P1) provides structure, but nodes need configuration to be useful. This priority comes after code generation (P3-P4) can be proven to work, but in practice, configuration and code generation will be developed together.

**Independent Test**: Can be fully tested by creating a flow graph with a validation node, configuring validation rules through the properties panel, generating code, and verifying the generated code enforces those validation rules.

**Acceptance Scenarios**:

1. **Given** a node is selected on the canvas, **When** the developer opens the properties panel, **Then** the panel displays all configurable properties for that node type with current values
2. **Given** the properties panel is open, **When** the developer modifies a property value and saves, **Then** the node's configuration is updated and reflected in both visual and textual representations
3. **Given** a node has configurable business logic, **When** the developer defines transformation rules, **Then** the generated code includes those transformations correctly implemented
4. **Given** property values have validation rules, **When** the developer enters an invalid value, **Then** the system prevents saving and displays a clear validation error message
5. **Given** different node types exist (components, services, validators, transformers), **When** each is configured, **Then** the properties panel adapts to show type-appropriate configuration options

---

### User Story 6 - Flow Graph Validation and Error Detection (Priority: P6)

A developer is building a flow graph and makes mistakes such as leaving ports unconnected, creating circular dependencies, or defining incompatible data flows. The system continuously validates the graph and highlights errors or warnings. The developer can view error messages, navigate to problem areas, and fix issues before attempting code generation.

**Why this priority**: Validation prevents wasted time from generating invalid code. While important, it's not required for the initial MVP - early adopters can tolerate manual validation. This feature significantly improves the developer experience once the core platform is proven.

**Independent Test**: Can be fully tested by intentionally creating invalid flow graphs (disconnected ports, cycles, type mismatches) and verifying the system detects and clearly reports each error type.

**Acceptance Scenarios**:

1. **Given** a flow graph has unconnected input ports, **When** the system performs validation, **Then** those ports are visually highlighted and an error message lists the unconnected ports
2. **Given** a flow graph contains a circular dependency, **When** the system performs validation, **Then** the cycle is detected, the involved nodes are highlighted, and an error explains the circular dependency
3. **Given** two connected ports have incompatible data types, **When** the system performs validation, **Then** the connection is marked as invalid and an error message explains the type mismatch
4. **Given** validation errors exist, **When** the developer attempts code generation, **Then** the system prevents generation and displays all validation errors that must be fixed
5. **Given** a validation error is displayed, **When** the developer clicks the error message, **Then** the IDE navigates to and highlights the problematic node or connection

---

### Edge Cases

- What happens when a flow graph becomes very large (100+ nodes)? (Performance expectations for rendering, validation, and code generation)
- How does the system handle version updates to the plugin when existing flow graphs use older node types?
- What happens when generated code is manually modified by developers - can the graph be regenerated without losing changes?
- How does the system handle platform-specific code requirements (iOS-specific UI, Android-specific APIs)?
- What happens when multiple developers work on the same flow graph simultaneously (concurrent editing)?
- How are third-party libraries and dependencies managed when generating code?
- What happens when FBP flow patterns conflict with platform best practices or limitations?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST integrate as a plugin into JetBrains IDEs (IntelliJ IDEA, Android Studio, GoLand) using the IntelliJ Platform SDK
- **FR-002**: System MUST provide a visual canvas where developers can create, view, and edit flow graphs
- **FR-003**: System MUST support Flow-based Programming (FBP) concepts including nodes (components), ports (input/output), connections (data flows), and information packets
- **FR-004**: System MUST provide a palette of node types representing common patterns (UI components, services, data transformers, validators, API endpoints, database operations)
- **FR-005**: System MUST allow developers to create connections between compatible ports using drag-and-drop or click-based interaction
- **FR-006**: System MUST prevent invalid connections (incompatible port types, creating cycles where prohibited) with clear error messages
- **FR-007**: System MUST persist flow graphs to disk in a readable format (JSON, YAML, or custom DSL) that integrates with version control systems
- **FR-008**: System MUST provide a textual representation of flow graphs viewable alongside or instead of the visual representation
- **FR-009**: System MUST generate Kotlin Multiplatform code from flow graphs targeting Android, iOS, and Web platforms
- **FR-010**: System MUST generate Go code from flow graphs for backend services and APIs
- **FR-011**: Generated KMP code MUST follow Kotlin coding conventions, use proper type safety, and be structured in common/platform-specific source sets
- **FR-012**: Generated Go code MUST follow Go idioms, use proper package structure, and include a valid go.mod file
- **FR-013**: System MUST enforce licensing constraints: no GPL, LGPL, or AGPL dependencies in generated KMP native code or Go modules (per constitution)
- **FR-014**: System MUST validate generated code dependencies against the project constitution's licensing rules before code generation completes
- **FR-015**: System MUST provide a properties panel for configuring node behavior, parameters, and business logic
- **FR-016**: System MUST validate flow graphs and report errors (unconnected ports, type mismatches, invalid configurations) before allowing code generation
- **FR-017**: System MUST support undo/redo operations for flow graph editing
- **FR-018**: System MUST allow developers to zoom and pan the visual canvas for working with large flow graphs
- **FR-019**: System MUST provide search/filter capabilities for finding nodes in large flow graphs
- **FR-020**: Generated code MUST include appropriate comments and documentation derived from node configurations and flow graph metadata

### Key Entities

- **Flow Graph**: The top-level container representing a complete application feature or system. Contains metadata (name, version, description), a collection of nodes, and a collection of connections. Persisted to disk and managed by version control.

- **Node**: A component in the flow graph representing a unit of processing or behavior. Has a type (component, service, transformer, validator, API endpoint, etc.), a unique identifier, a human-readable name, configuration properties, input ports, and output ports. Functions as a "virtual circuit board" in the FBP model.

- **Port**: An entry or exit point on a node for data flow. Has a direction (input or output), a data type (string, number, object, custom types), a name, and optionally a default value or validation rules. Ports define how nodes can be connected.

- **Connection**: A link between two ports representing data flow. Has a source node and output port, a target node and input port, and optional transformation or mapping logic. Represents an information packet pathway in FBP terminology.

- **Node Type Definition**: A template defining what a node can do. Includes the node's category, available ports (types and directions), configurable properties (with types and validation), default configurations, and code generation templates for KMP and Go.

- **Generated Project**: The output of code generation. For KMP: a multiplatform project with common, Android, iOS, and Web source sets, build configuration (build.gradle.kts), and dependency declarations. For Go: a Go module with package structure, go.mod file, and implementation files.

- **Property Configuration**: Settings that define a node's behavior. Includes key-value pairs where values can be primitives, objects, or expressions, validation rules ensuring valid configurations, and metadata used in code generation.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Developers can create a working full-stack application (KMP frontend + Go backend) with basic CRUD functionality in under 2 hours using CodeNodeIO, compared to 8+ hours with hand-coding
- **SC-002**: Flow graphs with up to 50 nodes render and respond to interactions (pan, zoom, selection) in under 100ms
- **SC-003**: Code generation for a typical feature (10-15 nodes) completes in under 30 seconds for both KMP and Go targets
- **SC-004**: Generated code compiles successfully without errors on first generation for valid flow graphs 95% of the time
- **SC-005**: 80% of developers new to CodeNodeIO can create their first working flow graph and generate runnable code within 30 minutes of installing the plugin
- **SC-006**: Flow graphs created with CodeNodeIO are comprehensible to new team members 40% faster than equivalent hand-written code (measured by time to understand and explain the flow)
- **SC-007**: 90% of licensing validation checks pass automatically, preventing GPL/LGPL dependencies from being included in generated projects
- **SC-008**: The plugin integrates with all target JetBrains IDEs (IntelliJ IDEA, Android Studio, GoLand) without installation errors or compatibility issues
- **SC-009**: Flow graph files remain under 1MB for typical features (up to 30 nodes) and can be diff'd meaningfully in version control systems
- **SC-010**: Developers report 60% reduction in time spent debugging business logic errors compared to hand-coding (measured via surveys after 3 months of use)

### Assumptions

- Developers using CodeNodeIO have basic familiarity with Flow-based Programming concepts or are willing to learn through provided documentation
- Target developers are already using JetBrains IDEs for Kotlin and/or Go development
- Generated code will be checked into version control alongside flow graph definitions
- Initial version focuses on code generation for new features; bidirectional synchronization (round-trip engineering) is deferred to future iterations
- Developers will manually integrate generated code into existing projects initially; automated project integration is a future enhancement
- The plugin will require JetBrains IDE version 2023.1 or later for compatibility
- KMP projects target Kotlin 1.9+ and use standard KMP project structures
- Go projects target Go 1.21+ and follow standard Go module conventions
- Flow graphs represent logical application flows; UI layout and styling are configured through node properties but not visually designed in the graph editor
- Performance optimizations for very large graphs (100+ nodes) are deferred until user feedback validates the need
