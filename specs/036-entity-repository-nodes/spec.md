# Feature Specification: Entity Repository Nodes

**Feature Branch**: `036-entity-repository-nodes`
**Created**: 2026-03-01
**Status**: Draft
**Input**: User description: "Add functionality to create EntityRepository nodes with KMP Room database integration, repository pattern wrapping DAOs, and UI to generate RepositoryNodes from custom IP Types."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Display IP Type Properties in Properties Panel (Priority: P1)

When a user selects a custom IP Type in the IP Types palette, the Properties Panel on the right displays the selected IP type's properties (name, type, required/optional status), its color, and its description. This provides the foundation for viewing entity details and later acting on them (e.g., creating a RepositoryNode).

**Why this priority**: Without being able to see a custom IP type's properties in the Properties Panel, the user has no context to decide whether to create a RepositoryNode for that entity. This is the visual foundation all subsequent stories depend on.

**Independent Test**: Select a custom IP Type (e.g., "UserProfile" with properties: name/String, age/Int, email/String) in the IP Types palette. Verify the Properties Panel shows the type name, color swatch, description, and a "Properties" section listing each property with its name, resolved type, and required/optional badge. Select a different IP type and verify the panel updates. Select a built-in type (e.g., Int) and verify only name, color, and description are shown (no properties section).

**Acceptance Scenarios**:

1. **Given** a custom IP Type "Order" with properties (orderId/Int/required, total/Double/required, notes/String/optional) exists, **When** the user clicks "Order" in the IP Types palette, **Then** the Properties Panel displays: type name "Order", color swatch, and a Properties section listing all three properties with their types and required/optional badges.
2. **Given** a built-in IP Type "String" is selected, **When** the user clicks "String" in the IP Types palette, **Then** the Properties Panel displays: type name "String", color swatch, description, and no Properties section.
3. **Given** a custom IP Type is selected in the Properties Panel, **When** the user clicks a node on the canvas, **Then** the Properties Panel switches to show the selected node's properties (IP type selection is cleared).

---

### User Story 2 - Create Repository Node from Custom IP Type (Priority: P2)

When a custom IP Type is displayed in the Properties Panel, the user sees a "Create Repository Node" button. Clicking it generates a new custom node definition representing a data repository for that entity type. The repository node has standardized ports: a "save" input port, an "update" input port, and a "remove" input port (all typed to the custom IP type), a "result" output port (a reactive stream that continuously emits the latest full entity list whenever any mutation occurs, enabling composable reactive UI), and an "error" output port (typed as String for error messages). The created node appears in the Node Palette and can be placed on a flow graph.

**Why this priority**: This is the core value proposition — allowing users to generate repository nodes directly from their entity definitions without manually configuring ports. It bridges the IP type system and the node system.

**Independent Test**: Create a custom IP Type "User" with properties (id/Int, name/String, email/String). Select it in the IP Types palette. Click "Create Repository Node" in the Properties Panel. Verify a new node "UserRepository" appears in the Node Palette with the correct ports (save, update, remove inputs; result, error outputs). Place the node on the canvas and verify its ports display correctly.

**Acceptance Scenarios**:

1. **Given** a custom IP Type "Product" is selected and displayed in the Properties Panel, **When** the user clicks "Create Repository Node", **Then** a new node definition "ProductRepository" is created with input ports (save/Product, update/Product, remove/Product) and output ports (result/Product, error/String), and it appears in the Node Palette.
2. **Given** a repository node "UserRepository" already exists for IP Type "User", **When** the user selects "User" in the IP Types palette, **Then** the "Create Repository Node" button is disabled or shows "Repository exists" to prevent duplicates.
3. **Given** the user has created a "ProductRepository" node, **When** the user places it on the canvas, **Then** the node displays with 3 input ports (save, update, remove) and 2 output ports (result, error), each showing the correct port name and type color.

---

### User Story 3 - Repository Node Code Generation (Priority: P3)

When the user saves/compiles a flow graph containing a repository node, the code generators produce the appropriate runtime code. This includes generating: a Repository interface for the entity type, a DAO interface with standard CRUD operations, an Entity class annotated for Room persistence, and the runtime node wiring that connects input ports to repository operations and output ports to query results and error streams.

**Why this priority**: Code generation transforms the visual repository node into executable data persistence code. Without this, the repository node is only a visual placeholder. This story depends on US2 for the node definition.

**Independent Test**: Place a "UserRepository" node on a flow graph, connect its ports to other nodes, and trigger code generation. Verify the generated code includes a UserEntity class, UserDao interface, UserRepository class implementing the Repository interface, and correct runtime wiring connecting input channels to save/update/remove operations, a reactive observe-all stream on the result output, and error routing.

**Acceptance Scenarios**:

1. **Given** a flow graph with a "ProductRepository" node connected to other nodes, **When** the user triggers code generation, **Then** the generated module includes: ProductEntity data class, ProductDao interface extending BaseDao, ProductRepository class implementing Repository<ProductEntity>, and runtime wiring code.
2. **Given** a flow graph with multiple repository nodes ("UserRepository", "OrderRepository"), **When** code is generated, **Then** each entity gets its own Entity/DAO/Repository classes, and a shared database configuration is generated that includes all entities.
3. **Given** a repository node's "error" output port is connected to an error handler node, **When** a runtime operation fails, **Then** the error message is sent through the error output channel to the connected error handler.

---

### User Story 4 - Database Module Integration (Priority: P4)

When a flow graph contains one or more repository nodes, the generated code includes a self-contained database module with Room database configuration. This module provides the database instance, manages entity registration, and exposes the repository instances. Other flow graphs can connect to this database module's repository nodes through shared channels, enabling data access across flow graph boundaries.

**Why this priority**: This story addresses the architectural question of how repository nodes integrate with a shared Room database and how multiple flow graphs share data. It depends on US3 for basic code generation.

**Independent Test**: Create two flow graphs — one with a "UserRepository" node that saves user data, and another that queries user data. Generate code for both. Verify the generated database module includes the Room database configuration with all registered entities, and that both flow graphs reference the same database instance through dependency injection or a shared module pattern.

**Acceptance Scenarios**:

1. **Given** a flow graph with "UserRepository" and "OrderRepository" nodes, **When** code is generated, **Then** a single database module is produced containing a Room database class configured with both UserEntity and OrderEntity tables.
2. **Given** two separate flow graphs each using a "UserRepository" node, **When** both are running, **Then** they share the same underlying database instance, and a save operation in one flow graph is visible to queries in the other.
3. **Given** a database module has been generated, **When** the user adds a new repository node to any flow graph and regenerates, **Then** the database module is updated to include the new entity without losing existing entity configurations.

---

### Edge Cases

- What happens when a user deletes a custom IP Type that has a repository node created from it? The repository node should remain in the palette but display a warning that its source type no longer exists.
- What happens when a user modifies a custom IP Type's properties after a repository node has been created? The repository node definition is static at creation time; changes to the IP type do not automatically propagate. The user must delete and recreate the repository node to pick up changes.
- What happens when the user tries to create a repository node from a built-in type (Int, String, etc.)? The "Create Repository Node" button should only appear for custom IP types that have properties defined.
- How does the system handle repository node names that conflict with existing custom nodes? The system appends "Repository" to the IP type name and checks for conflicts. If "UserRepository" already exists, the create button is disabled.
- What happens when a repository node is on the canvas but not connected to any other nodes? Code generation should still produce the Entity/DAO/Repository classes but skip runtime wiring for unconnected ports.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST display a custom IP type's properties (name, type, required/optional) in the Properties Panel when that IP type is selected in the IP Types palette.
- **FR-002**: System MUST show a "Create Repository Node" button in the Properties Panel only when a custom IP type with at least one property is selected.
- **FR-003**: System MUST generate a repository node definition with standardized ports: input ports for save, update, and remove operations; output ports for result (reactive observe-all stream) and error.
- **FR-004**: System MUST type the save, update, and remove input ports to the source custom IP type, the result output port to the source custom IP type, and the error output port to String.
- **FR-005**: System MUST persist created repository node definitions so they survive application restarts.
- **FR-006**: System MUST prevent duplicate repository nodes for the same IP type by disabling creation when a repository node already exists for the selected type.
- **FR-007**: System MUST generate Entity data classes, DAO interfaces, and Repository classes from repository node definitions during code generation.
- **FR-008**: System MUST generate a shared database configuration module when one or more repository nodes exist in a flow graph.
- **FR-009**: System MUST wire repository node input ports to the corresponding repository operations (save, update, remove) and start a reactive observe-all stream on the result output port that emits the latest entity list whenever any mutation occurs.
- **FR-010**: System MUST route operation errors to the error output port channel in generated runtime code.
- **FR-011**: System MUST implement the result mechanism as a reactive observe-all stream — the repository continuously emits the latest full entity list via an observable flow whenever any mutation occurs. No query input port is needed. Downstream nodes receive updates automatically, enabling composable reactive UI.
- **FR-012**: System MUST use a singleton database module shared by all flow graphs. Repository nodes in any flow graph access the same database instance. A single generated database configuration manages all registered entities.

### Key Entities

- **Repository Node Definition**: A custom node definition specialized for data persistence, derived from a custom IP type. Contains references to the source IP type, standardized port configuration, and metadata for code generation.
- **Entity**: A data class representing a database table row, derived from the custom IP type's properties. Each property maps to a column.
- **DAO (Data Access Object)**: An interface defining database operations (insert, update, delete, query) for a specific entity type.
- **Repository**: A class wrapping a DAO to provide a clean interface for the runtime node, implementing standard CRUD operations and observable queries.
- **Database Module**: A configuration unit that registers all entities, provides the Room database instance, and exposes repository instances to the runtime.

## Clarifications

### Session 2026-03-01

- Q: Should the query input port accept a simple string, structured query, or observe all entities reactively? → A: Observe all (reactive stream). Repository continuously emits the latest entity list via observeAll() Flow whenever any mutation occurs. No query input port needed — the result output is always active, enabling composable reactive UI.
- Q: How should the database module be shared across flow graphs? → A: Singleton database module. One generated database instance shared by all flow graphs automatically.

## Assumptions

- Repository nodes follow the established `CustomNodeDefinition` pattern for persistence and palette display.
- The Repository pattern (interface with save/update/remove/observeAll) is the standard abstraction, hiding the Room/DAO implementation details.
- Each custom IP type maps to exactly one database entity (one-to-one relationship between IP types and tables).
- IP type properties map directly to entity columns — no nested objects or complex relationships between entities in the initial implementation.
- The BaseDao pattern (with @Insert, @Update, @Delete annotations) is the foundation for all generated DAOs.
- Code generation for repository nodes extends the existing RuntimeFlowGenerator and ProcessingLogicStubGenerator patterns.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can view all properties of a custom IP type in the Properties Panel within 1 click (select the type in the palette).
- **SC-002**: Users can create a repository node from a custom IP type in under 3 clicks (select type, click create, confirm).
- **SC-003**: Created repository nodes appear in the Node Palette and can be placed on the canvas like any other node.
- **SC-004**: Generated code for a repository node compiles without errors and includes all required Entity/DAO/Repository classes.
- **SC-005**: A flow graph with repository nodes produces a complete, self-contained database module upon code generation.
- **SC-006**: 100% of repository mutation operations (save, update, remove) are accessible through the node's input ports, and the result output port automatically reflects the current database state.
