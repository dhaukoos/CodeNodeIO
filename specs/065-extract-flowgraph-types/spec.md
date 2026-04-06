# Feature Specification: Extract flowGraph-types Module

**Feature Branch**: `065-extract-flowgraph-types`
**Created**: 2026-04-05
**Status**: Draft
**Input**: User description: "flowGraph-types module extraction — Step 1 of Phase B vertical slice migration"
**Parent Feature**: 064-vertical-slice-refactor (Phase A planning)
**Phase**: B (Vertical Slice Extraction — first of seven)

## Context

This is the first Phase B extraction from the migration plan defined in feature 064. The flowGraph-types module consolidates IP type lifecycle concerns (discovery, registry, repository, file generation, migration) that are currently scattered across the graphEditor module's inspect, persist, and generate responsibility buckets. Extracting types first eliminates two cyclic dependencies in the module graph and provides stable interfaces for all subsequent extractions.

## User Scenarios & Testing

### User Story 1 - Extract IP Type Files into Dedicated Module (Priority: P1)

A developer working on the graphEditor codebase needs IP type lifecycle concerns (discovery, registry, persistence, file generation, migration) consolidated into a single, independently buildable module. Currently these 9 files are scattered across three different responsibility areas within graphEditor, creating implicit coupling. After extraction, the developer can find all IP-type-related code in one place, reason about it independently, and modify it without risk of unintended side effects in unrelated parts of graphEditor.

**Why this priority**: This is the structural keystone of the entire migration. All five subsequent module extractions depend on flowGraph-types being available as a stable dependency. Without it, the inspect-persist and inspect-generate cyclic dependencies block further decomposition. Extracting types first also validates the Strangler Fig extraction pattern for all future steps.

**Independent Test**: Build the new flowGraph-types module independently. Verify it compiles, its three service interfaces are accessible, and all existing tests across graphEditor, kotlinCompiler, and circuitSimulator continue to pass unchanged.

**Acceptance Scenarios**:

1. **Given** the 9 IP-type-related files currently reside in graphEditor, **When** the extraction is complete, **Then** all 9 files exist in the new flowGraph-types module and are removed from graphEditor.
2. **Given** the new module is created, **When** a developer builds it in isolation, **Then** it compiles successfully with only fbpDsl as a dependency.
3. **Given** the extraction is complete, **When** the full test suite runs, **Then** all existing characterization tests and unit tests pass without modification.

---

### User Story 2 - Define Data Flow Contracts for IP Type Access (Priority: P1)

A developer maintaining call sites in graphEditor needs to access IP type data through the FBP data flow model rather than calling directly into internal classes. The module boundary is the CodeNode's ports: `ipTypeMetadata` flows out as data, and consumers hold it locally for queries. Mutations (register, unregister, generate) flow in as commands through the `ipTypeCommands` input port. Read-only consumers (GraphState, PropertiesPanel, GraphNodeTemplateSerializer) query their locally-held copy of the IP type metadata. Mutating consumers (IPGeneratorViewModel, IPPaletteViewModel) send commands to the CodeNode and receive updated metadata from its output.

**Why this priority**: The module boundary must be data flow, not service calls. Without data flow contracts, the extraction would fall back to traditional DI service interfaces — contradicting the FBP-native architecture established in feature 064. The data contracts define what flows through the CodeNode's ports and are the foundation for all subsequent Phase B extractions.

**Independent Test**: Verify that read-only call sites query locally-held `ipTypeMetadata` (no direct reference to IPTypeRegistry). Verify that mutating call sites send commands to the CodeNode's `ipTypeCommands` port and receive updated state from its `ipTypeMetadata` output. Verify no call site imports concrete IP type classes from the types module.

**Acceptance Scenarios**:

1. **Given** GraphState.kt, GraphNodeTemplateSerializer.kt, and PropertiesPanel.kt currently call IPTypeRegistry methods directly, **When** the data flow contracts are in place, **Then** these files query a locally-held `ipTypeMetadata` data structure instead — no import of IPTypeRegistry.
2. **Given** IPGeneratorViewModel.kt currently calls IPTypeRegistry, IPTypeDiscovery, and FileIPTypeRepository directly, **When** the data flow contracts are in place, **Then** it sends registration/generation commands to the CodeNode's `ipTypeCommands` port and receives updated metadata from the output.
3. **Given** IPPaletteViewModel.kt currently calls IPTypeRegistry.unregister() and FileIPTypeRepository.remove() directly, **When** the data flow contracts are in place, **Then** it sends unregister/remove commands to the CodeNode's `ipTypeCommands` port and receives updated metadata from the output.
4. **Given** SharedStateProvider.kt currently holds an IPTypeRegistry instance, **When** the data flow contracts are in place, **Then** it holds the current `ipTypeMetadata` received from the CodeNode's output channel.

---

### User Story 3 - Wrap Module Boundary as a Coarse-Grained CodeNode (Priority: P1)

A developer needs the newly extracted flowGraph-types module to be accessible as a CodeNode — the fundamental building block of the FBP paradigm. The module boundary is wrapped as a single coarse-grained CodeNode with typed input/output ports: three inputs (`filesystemPaths`, `classpathEntries`, `ipTypeCommands`) and one output (`ipTypeMetadata`). The CodeNode uses `anyInput` mode — it re-emits updated metadata whenever any input changes, whether that's a filesystem scan trigger or a user-initiated mutation command. This CodeNode encapsulates the entire IP type lifecycle behind FBP-compatible ports, making it composable within a flow graph just like any other node.

**Why this priority**: Without the CodeNode wrapper, the extracted module is just a library — callable through direct references but invisible to the FBP runtime. Wrapping it as a CodeNode is what makes the vertical slice architecture self-describing: the module can participate in flow graphs, receive data through input ports, and emit data through output ports. This is the step that bridges "extracted code" to "executable flow node." The `ipTypeCommands` input port is essential — without it, there is no FBP-native path for user-initiated mutations (register, unregister, generate), and the architecture would fall back to service-oriented method calls.

**Independent Test**: Instantiate the CodeNode, verify it has the expected port signatures (3 inputs, 1 output with correct names), and verify that providing input data produces the expected ipTypeMetadata output through the FBP channel mechanism.

**Acceptance Scenarios**:

1. **Given** the flowGraph-types module is extracted, **When** the CodeNode is created, **Then** it exposes exactly 3 input ports (`filesystemPaths`, `classpathEntries`, `ipTypeCommands`) and 1 output port (`ipTypeMetadata`).
2. **Given** the CodeNode receives filesystem paths and classpath entries through its input ports, **When** the node executes, **Then** it discovers IP types from the filesystem and emits the complete registry state as `ipTypeMetadata`.
3. **Given** the CodeNode receives a registration command through `ipTypeCommands`, **When** the node processes it, **Then** the registry state is updated and a new `ipTypeMetadata` packet is emitted with the registered type included.
4. **Given** the CodeNode receives an unregistration command through `ipTypeCommands`, **When** the node processes it, **Then** the registry state is updated and a new `ipTypeMetadata` packet is emitted with the type removed.

---

### User Story 4 - Test CodeNode Port Contract (Priority: P1)

A developer building the coarse-grained CodeNode for flowGraph-types needs tests that verify the new node's FBP contract before wiring it into the architecture FlowGraph. Unlike the Phase A characterization tests — which pin pre-extraction behavior of existing code — these tests validate the *new* CodeNode abstraction: that it has the correct port signatures, that data flows through its channels correctly, and that it produces the expected output when given known inputs. These tests are written before the CodeNode implementation (TDD), defining the contract the CodeNode must satisfy.

**Why this priority**: The CodeNode is new code, not moved code. The characterization tests from feature 064 don't cover it — they test the underlying services, not the FBP wrapper. Without dedicated tests, there is no verification that the CodeNode correctly bridges between FBP port semantics and service interface calls. Following TDD, these tests also serve as the design specification for the CodeNode's behavior.

**Independent Test**: Run the CodeNode test suite in isolation. All tests pass before the CodeNode is wired into architecture.flow.kt, confirming the node works correctly as a standalone component.

**Acceptance Scenarios**:

1. **Given** the CodeNode test suite exists, **When** tests are run, **Then** they verify the node exposes exactly 3 input ports (`filesystemPaths`, `classpathEntries`, `ipTypeCommands`) and 1 output port (`ipTypeMetadata`).
2. **Given** a test provides known filesystem paths and classpath entries to the CodeNode's input channels, **When** the node executes, **Then** it emits IP type metadata on its output channel that matches expected results.
3. **Given** a test provides empty or invalid inputs, **When** the node executes, **Then** it handles the boundary condition gracefully without crashing or producing corrupt output.
4. **Given** the CodeNode tests are written first (TDD), **When** the CodeNode implementation is completed, **Then** all tests pass — confirming the implementation satisfies the designed contract.

---

### User Story 5 - Wire CodeNode into Architecture FlowGraph (Priority: P1)

A developer opening `architecture.flow.kt` in the graphEditor needs to see flowGraph-types as a real, executable node — not an empty GraphNode container. The coarse-grained CodeNode created in US3 is wired into `architecture.flow.kt`, populating the empty flowGraph-types GraphNode container. The 4 existing outbound connections (ipTypeMetadata → compose, persist, generate, rootSink) remain unchanged. A new `ipTypeCommands` input port and connection from graphEditor-source are added to support mutation commands. After this wiring, the flowGraph-types portion of the architecture FlowGraph is live — it can actually discover, register, and serve IP type metadata when executed.

**Why this priority**: This is the deliverable that makes Phase B real. The architecture FlowGraph transitions from "blueprint with an empty placeholder" to "partially executable graph with one live module." Each subsequent Phase B feature repeats this pattern until the entire FlowGraph is executable. Completing this step for the first module validates that the architecture FlowGraph can progressively become the actual application wiring.

**Independent Test**: Open `architecture.flow.kt` in the graphEditor. Verify the flowGraph-types node is now a CodeNode (not an empty GraphNode container). Verify the 4 outbound connections and 1 new inbound connection render correctly. Run the ArchitectureFlowKtsTest suite to confirm structural invariants are preserved (updated for new port and connection).

**Acceptance Scenarios**:

1. **Given** the coarse-grained CodeNode exists, **When** it is wired into `architecture.flow.kt`, **Then** the flowGraph-types GraphNode container is populated with the CodeNode as its implementation.
2. **Given** the CodeNode is wired in, **When** the architecture FlowGraph is parsed, **Then** all 4 outbound connections (ipTypeMetadata → compose, persist, generate, rootSink) remain intact, and a new inbound connection (graphEditor-source → ipTypeCommands) exists.
3. **Given** the CodeNode is wired in, **When** ArchitectureFlowKtsTest runs, **Then** all structural invariant tests pass — 8 nodes, updated connection count (20), DAG property, hub source characteristics.
4. **Given** the architecture FlowGraph is opened in the graphEditor, **When** the developer inspects the flowGraph-types node, **Then** it displays as a populated CodeNode with its 3 input ports and 1 output port visible and connected.

---

### User Story 6 - Eliminate Cyclic Dependencies (Priority: P2)

A developer analyzing the module dependency graph needs confirmation that the two cyclic dependencies identified in Phase A (inspect-persist and inspect-generate) are eliminated by this extraction. After extraction, flowGraph-types is a pure source node in the dependency graph — it depends only on fbpDsl and has no dependencies on any other workflow module. All data flows from types outward.

**Why this priority**: Cycle elimination is a structural prerequisite for the DAG property that the architecture FlowGraph requires. If cycles remain after this extraction, the migration plan's extraction order is invalid.

**Independent Test**: Analyze the dependency graph after extraction. Verify that flowGraph-types has no dependencies on graphEditor workflow code (only fbpDsl). Verify that no module that depends on flowGraph-types is also depended upon by flowGraph-types.

**Acceptance Scenarios**:

1. **Given** the extraction is complete, **When** the module dependency graph is analyzed, **Then** flowGraph-types depends only on fbpDsl.
2. **Given** the extraction is complete, **When** the former inspect-persist cycle is examined, **Then** inspect and persist both depend on types (one-way), and neither depends on the other through IP type concerns.
3. **Given** the extraction is complete, **When** the former inspect-generate cycle is examined, **Then** inspect and generate both depend on types (one-way), and neither depends on the other through IP type concerns.

---

### User Story 7 - Validate Strangler Fig Pattern (Priority: P2)

A developer about to begin the next five module extractions needs confidence that the Strangler Fig extraction pattern works correctly on this codebase. The flowGraph-types extraction serves as the template: copy files to the new module, create interfaces, update call sites to delegate through interfaces, verify all tests pass, then remove the old copies. If this pattern succeeds here — on the simplest extraction target with no outbound dependencies — it validates the approach for all subsequent, more complex extractions.

**Why this priority**: This is a one-time validation cost that de-risks the remaining six extractions. A failed pattern here is cheap to fix; a failed pattern discovered during the fifth extraction would require significant rework.

**Independent Test**: Follow the Strangler Fig sequence step by step. At each intermediate state (files copied, interfaces created, call sites updated, old files removed), verify the full test suite passes. If any intermediate state fails, the pattern needs adjustment before applying it to more complex modules.

**Acceptance Scenarios**:

1. **Given** the 9 files are copied (not moved) to flowGraph-types, **When** graphEditor depends on the new module and tests run, **Then** all tests pass with both copies of the files present.
2. **Given** call sites are updated to use interfaces, **When** tests run, **Then** all tests pass with delegation in place.
3. **Given** the old copies are removed from graphEditor, **When** tests run, **Then** all tests pass with only the new module's copies remaining.
4. **Given** the full extraction sequence is complete, **When** the developer reviews the result, **Then** graphEditor contains no IP type lifecycle code — only interface references wired through dependency injection.

---

### Edge Cases

- What happens when a call site depends on both IP type data and non-IP-type classes staying in graphEditor? The call site continues using its graphEditor classes directly and receives `ipTypeMetadata` from the CodeNode's output channel — both data sources coexist naturally.
- What happens when an IP type file has internal dependencies on other IP type files? These become module-internal references — IPTypeMigration.kt calling FileIPTypeRepository.kt and IPTypeFileGenerator.kt becomes internal to flowGraph-types, which is correct and expected (these were the former cross-bucket back-edges that created cycles).
- What happens if the new module introduces a transitive dependency that graphEditor didn't previously have? flowGraph-types depends only on fbpDsl, which graphEditor already depends on — no new transitive dependencies are introduced.
- What happens if a characterization test directly instantiates an IP type class that has moved? The test's import path changes to the new module's package, but the test logic and assertions remain identical.
- What happens if the CodeNode's port types don't exactly match the placeholder types on the GraphNode container? The CodeNode's ports use data-oriented names matching the GraphNode's exposed ports. Port types may be refined from the placeholder String::class to more specific types as the data contracts become concrete during implementation — the structural invariant (port count and names) is what matters for architecture.flow.kt consistency.
- What happens to the 4 outbound connections when the GraphNode container is populated with a CodeNode? The connections remain unchanged — they connect to the container's exposed output ports, not directly to the internal implementation. Populating the container adds the CodeNode inside without modifying any external connections.

## Requirements

### Functional Requirements

- **FR-001**: The system MUST create a new independently buildable module containing exactly 9 files: IPTypeDiscovery, IPTypeRegistry, IPProperty, IPPropertyMeta, IPTypeFileMeta, IPTypeMigration, FileIPTypeRepository, SerializableIPType, and IPTypeFileGenerator.
- **FR-002**: The new module MUST depend only on the shared vocabulary module (fbpDsl) — no dependencies on graphEditor, kotlinCompiler, circuitSimulator, or any other workflow module.
- **FR-003**: The module boundary MUST be expressed as data flow through CodeNode ports — not as service interfaces. Consumers receive `ipTypeMetadata` as data and query it locally; mutations flow in as commands through `ipTypeCommands`.
- **FR-004**: The CodeNode's output port (`ipTypeMetadata`) MUST carry the complete IP type registry state as a data packet. When the state changes, a new packet MUST flow to all downstream consumers.
- **FR-005**: The CodeNode MUST accept mutation commands (register, unregister, generate, update) through an `ipTypeCommands` input port, enabling user-initiated actions to flow as data rather than method calls.
- **FR-006**: All 6 cross-boundary call sites in graphEditor MUST be updated to consume `ipTypeMetadata` data from the CodeNode's output — read-only consumers query locally, mutating consumers send commands to `ipTypeCommands`.
- **FR-007**: The composition root MUST orchestrate the CodeNode by wiring its input/output channels — sending filesystem context and mutation commands, distributing output metadata to ViewModels and UI state.
- **FR-008**: All existing tests across all modules MUST pass without modification after the extraction is complete.
- **FR-009**: The extraction MUST follow the Strangler Fig pattern: copy to new module, create interfaces, update call sites, verify tests, remove old copies.
- **FR-010**: The module dependency graph MUST form a DAG after extraction — no cyclic dependencies may exist between flowGraph-types and any other module.
- **FR-011**: The graphEditor module MUST declare a dependency on the new module in its build configuration.
- **FR-012**: The module boundary MUST be wrapped as a coarse-grained CodeNode with exactly 3 input ports (`filesystemPaths`, `classpathEntries`, `ipTypeCommands`) and 1 output port (`ipTypeMetadata`). The CodeNode MUST use `anyInput` mode — re-emitting updated metadata whenever any input changes.
- **FR-013**: The CodeNode MUST orchestrate the module's internal classes — receiving inputs through FBP ports, delegating to IPTypeDiscovery/IPTypeRegistry/IPTypeFileGenerator/FileIPTypeRepository internally, and emitting the updated registry state through the output port.
- **FR-014**: The CodeNode MUST be wired into `architecture.flow.kt` by populating the empty flowGraph-types GraphNode container, preserving all 4 existing outbound connections (ipTypeMetadata → compose, persist, generate, rootSink) and adding a new `ipTypeCommands` input port with a connection from graphEditor-source.
- **FR-015**: After wiring, the ArchitectureFlowKtsTest suite MUST be updated and MUST pass — 8 nodes, updated connection count (20), DAG property, and all other structural invariants MUST be preserved.
- **FR-016**: The CodeNode MUST have dedicated tests (written before implementation per TDD) that verify port signatures, channel-based data flow, and boundary condition handling.

### Key Entities

- **ipTypeMetadata**: The primary data product of the flowGraph-types module. A complete snapshot of the IP type registry state (all registered types, custom type definitions, properties, file paths, entity module associations). Flows as data from the CodeNode's output port to 4 downstream consumers (compose, persist, generate, rootSink). Consumers hold this data locally for queries.
- **ipTypeCommands**: Mutation commands that flow into the CodeNode's input port from the composition root. Includes operations like register, unregister, generate IP type file, and update color. Commands are data — not method calls.
- **flowGraph-types Module**: The new independently buildable unit containing all 9 IP type lifecycle files. Internal classes (IPTypeRegistry, IPTypeDiscovery, IPTypeFileGenerator, FileIPTypeRepository) are implementation details — the module's external boundary is the CodeNode's ports.
- **flowGraph-types CodeNode**: A coarse-grained CodeNode that wraps the module boundary for FBP composition. Receives filesystemPaths, classpathEntries, and ipTypeCommands as inputs; emits ipTypeMetadata as output. Uses `anyInput` mode. Wired into architecture.flow.kt as the implementation inside the flowGraph-types GraphNode container.

## Success Criteria

### Measurable Outcomes

- **SC-001**: All existing tests pass after extraction — zero test regressions across graphEditor, kotlinCompiler, and circuitSimulator test suites.
- **SC-002**: The new module compiles independently with only the shared vocabulary module as a dependency.
- **SC-003**: All 6 cross-boundary call sites consume `ipTypeMetadata` as data from the CodeNode's output — no direct references to IPTypeRegistry, IPTypeDiscovery, or FileIPTypeRepository.
- **SC-004**: The module dependency graph contains no cycles involving the new module.
- **SC-005**: graphEditor contains zero IP type lifecycle source files after extraction — all 9 have moved.
- **SC-006**: The extraction follows the Strangler Fig pattern with tests passing at every intermediate step (copy, interface, delegate, remove).
- **SC-007**: Application startup and all IP-type-related workflows (type creation, discovery, registration, persistence, migration, file generation) function identically to pre-extraction behavior.
- **SC-008**: The coarse-grained CodeNode has exactly 3 input ports (`filesystemPaths`, `classpathEntries`, `ipTypeCommands`) and 1 output port (`ipTypeMetadata`), using `anyInput` mode.
- **SC-009**: The CodeNode is wired into architecture.flow.kt, populating the empty GraphNode container while preserving all 4 outbound connections and adding 1 new inbound connection (graphEditor-source → ipTypeCommands).
- **SC-010**: ArchitectureFlowKtsTest passes after wiring — 8 nodes, 20 connections (updated from 19), DAG property maintained.
- **SC-011**: Dedicated CodeNode tests exist and pass — covering port signatures, data flow through channels, and boundary conditions — written before the CodeNode implementation.

## Scope

### In Scope

- Moving PlacementLevel.kt to fbpDsl (shared vocabulary prerequisite)
- Creating the new flowGraph-types module with KMP build configuration
- Moving 9 files from graphEditor to flowGraph-types (commonMain/jvmMain split)
- Defining data flow contracts: `ipTypeMetadata` output, `ipTypeCommands` input
- Updating 6 call sites to consume data from CodeNode ports instead of calling classes directly
- Wrapping the module boundary as a coarse-grained CodeNode (3 inputs, 1 output, anyInput mode)
- TDD testing the CodeNode's port contract before implementation
- Adding `ipTypeCommands` input port and graphEditor-source connection to architecture.flow.kt
- Populating the empty flowGraph-types GraphNode container in architecture.flow.kt with the CodeNode
- Updating ArchitectureFlowKtsTest for new connection count (20)
- Verifying all existing tests pass at every intermediate Strangler Fig step

### Out of Scope

- Extracting any other module (persist, inspect, execute, generate, compose) — those are separate Phase B features
- Deepening the CodeNode's internal granularity into a sub-flow graph — that is Phase C work
- Changing any IP type behavior — this is a pure structural refactor with no functional changes
- Replacing placeholder IP types (String::class) on architecture.flow.kt ports with real types — proper types emerge as the data contracts are finalized across Phase B

## Assumptions

- The 9 files identified in the Phase A audit (MIGRATION.md) are the correct and complete set of IP type lifecycle files.
- The existing characterization test suite from feature 064 provides sufficient coverage to detect any regression caused by the extraction.
- The project's build system supports adding a new module without disrupting existing module builds.
- PlacementLevel.kt can be moved to fbpDsl without breaking any module that currently imports it (it has no dependencies beyond standard library).
- The `ipTypeMetadata` data packet can carry sufficient information for all downstream consumers to query locally — no consumer needs capabilities that can only be expressed as method calls on a live registry.
- The `ipTypeCommands` input port can model all user-initiated mutations (register, unregister, generate, update color) as serialized command data.
- The architecture.flow.kt connection count can be updated from 19 to 20 without invalidating the structural invariants that other tests depend on — ArchitectureFlowKtsTest will be updated accordingly.

## Dependencies

- **Feature 064 (Phase A)**: Must be complete — provides the audit, characterization tests, and migration map that this feature executes against.
- **fbpDsl module**: The shared vocabulary module that flowGraph-types will depend on. Must be stable and independently buildable.
- **Existing test suites**: The characterization tests from feature 064 and all pre-existing unit tests serve as the regression safety net.
