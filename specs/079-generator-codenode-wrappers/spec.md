# Feature Specification: Generator CodeNode Wrappers

**Feature Branch**: `079-generator-codenode-wrappers`
**Created**: 2026-04-23
**Status**: Draft
**Input**: User description: "Wrap each individual generator as a CodeNode with typed input and output ports, enabling them to participate in flow graphs."

## Context

This is Step 3 of the Code Generation Migration Plan from feature 076. The dependency analysis (076-codegen-decomposition/dependency-analysis.md, Section 6) identified 15 generators that can be wrapped as CodeNodes. With module scaffolding extracted (feature 078), generators no longer assume directory creation — they can operate independently as composable units.

**15 Generators to wrap** (from dependency analysis Section 6):

| Generator | Input | Output |
|-----------|-------|--------|
| FlowKtGenerator | FlowGraph | .flow.kt content |
| RuntimeFlowGenerator | FlowGraph | Flow.kt content |
| RuntimeControllerGenerator | FlowGraph | Controller.kt content |
| RuntimeControllerInterfaceGenerator | FlowGraph | ControllerInterface.kt content |
| RuntimeControllerAdapterGenerator | FlowGraph | ControllerAdapter.kt content |
| RuntimeViewModelGenerator | FlowGraph | ViewModel.kt content |
| UserInterfaceStubGenerator | FlowGraph | UI stub content |
| EntityCUDCodeNodeGenerator | EntityModuleSpec | CUD CodeNode content |
| EntityRepositoryCodeNodeGenerator | EntityModuleSpec | Repository CodeNode content |
| EntityDisplayCodeNodeGenerator | EntityModuleSpec | Display CodeNode content |
| EntityPersistenceGenerator | EntityModuleSpec | Persistence module content |
| UIFBPStateGenerator | UIFBPSpec | State.kt content |
| UIFBPViewModelGenerator | UIFBPSpec | ViewModel.kt content |
| UIFBPSourceCodeNodeGenerator | UIFBPSpec | Source CodeNode content |
| UIFBPSinkCodeNodeGenerator | UIFBPSpec | Sink CodeNode content |

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Define GeneratorCodeNode Interface (Priority: P1)

A developer defines a common interface for generator CodeNodes that establishes the pattern: each generator CodeNode has a typed input port (spec/config data) and a typed output port (generated file content as a String). This interface extends CodeNodeDefinition and adds generation-specific semantics. All 15 wrapper CodeNodes will implement this interface.

**Why this priority**: The interface defines the contract that all wrappers implement. Without it, each wrapper would define its own ad-hoc structure.

**Independent Test**: The interface compiles and can be implemented by a test class. It extends CodeNodeDefinition and defines input/output port conventions.

**Acceptance Scenarios**:

1. **Given** the GeneratorCodeNode interface, **When** a developer implements it for a specific generator, **Then** the implementation has a typed input port for its spec data and a typed output port for generated file content
2. **Given** the interface, **When** it is inspected, **Then** it extends CodeNodeDefinition and is compatible with the existing node palette and flow graph systems

---

### User Story 2 - Wrap Module-Level Generators as CodeNodes (Priority: P2)

The 7 module-level generators (FlowKt, RuntimeFlow, RuntimeController, RuntimeControllerInterface, RuntimeControllerAdapter, RuntimeViewModel, UserInterfaceStub) are each wrapped as a CodeNode. Each wrapper is a thin delegate — it instantiates the existing generator, accepts input via its port, and produces the generated file content as output. The wrappers are discoverable by the node palette.

**Why this priority**: These 7 generators form the core "Generate Module" pipeline. Wrapping them first enables the most common code generation path to be expressed as a flow graph.

**Independent Test**: Each wrapped CodeNode can be instantiated, given a FlowGraph input, and produces non-empty String output matching the underlying generator's output. All 7 appear in the node palette when discovered.

**Acceptance Scenarios**:

1. **Given** each of the 7 module-level generator wrappers, **When** instantiated with a FlowGraph, **Then** it produces the same output as calling the underlying generator directly
2. **Given** the 7 wrappers, **When** the graph editor scans for CodeNode definitions, **Then** all 7 appear in the node palette under a "Generator" category
3. **Given** a wrapper, **When** its ports are inspected, **Then** the input port type matches the generator's expected input and the output port type is String

---

### User Story 3 - Wrap Entity and UI-FBP Generators as CodeNodes (Priority: P3)

The remaining 8 generators (4 entity generators + 4 UI-FBP generators) are wrapped as CodeNodes following the same pattern. Entity generators accept EntityModuleSpec input. UI-FBP generators accept UIFBPSpec input. All are discoverable.

**Why this priority**: These extend the wrapping to cover all generation paths, completing the full set of 15 generator CodeNodes.

**Independent Test**: Each of the 8 wrappers produces correct output for its input type. All 15 total generator CodeNodes appear in the node palette.

**Acceptance Scenarios**:

1. **Given** each of the 4 entity generator wrappers, **When** instantiated with an EntityModuleSpec, **Then** it produces the same output as calling the underlying generator directly
2. **Given** each of the 4 UI-FBP generator wrappers, **When** instantiated with a UIFBPSpec, **Then** it produces the same output as calling the underlying generator directly
3. **Given** all 15 generator CodeNodes, **When** the node palette is displayed, **Then** all 15 are listed and categorized appropriately

---

### Edge Cases

- What happens when a generator wrapper receives invalid input (e.g., empty FlowGraph)? The wrapper delegates to the underlying generator which handles its own error cases — the wrapper does not add validation logic.
- What happens when two generator CodeNodes have the same name? Each wrapper has a unique name derived from the generator class name (e.g., "FlowKtGenerator", "RuntimeControllerGenerator").
- What happens when a generator wrapper is used in a flow graph but the output is not consumed? The generated content is produced but not written to disk — file writing is a separate concern (future Step 4).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST define a GeneratorCodeNode interface that extends CodeNodeDefinition and establishes the pattern for generator wrappers (input port for spec data, output port for generated content)
- **FR-002**: Each of the 15 generators identified in the dependency analysis MUST be wrapped as a CodeNode implementing the GeneratorCodeNode interface
- **FR-003**: Each wrapper MUST be a thin delegate — it instantiates the existing generator class and delegates the generation call, adding no additional logic
- **FR-004**: Each wrapper MUST have a descriptive name unique among all CodeNode definitions (e.g., "FlowKtGenerator", "RuntimeControllerGenerator")
- **FR-005**: Each wrapper MUST define input and output ports with appropriate types matching the underlying generator's signature
- **FR-006**: All 15 generator CodeNodes MUST be discoverable by the graph editor's node palette
- **FR-007**: Each wrapper MUST be independently testable — given valid input, it produces the same output as calling the underlying generator directly
- **FR-008**: The existing generator classes MUST remain unchanged — wrappers add CodeNodeDefinition semantics without modifying the generators

### Key Entities

- **GeneratorCodeNode**: An interface extending CodeNodeDefinition that defines the contract for generator wrappers — input port for configuration/spec data, output port for generated file content.
- **Generator Wrapper**: A thin CodeNode implementation that delegates to an existing generator class, adding port typing and CodeNodeDefinition compatibility.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: All 15 generators from the dependency analysis have corresponding CodeNode wrappers — zero generators are missing
- **SC-002**: Each wrapper produces identical output to the underlying generator for the same input — verified by unit tests
- **SC-003**: All 15 generator CodeNodes appear in the graph editor node palette when discovered
- **SC-004**: The existing generator classes have zero modifications — wrappers are purely additive
- **SC-005**: Each wrapper can be instantiated and tested in isolation without requiring the full generation pipeline

## Assumptions

- The wrappers live in the `flowGraph-generate` module alongside the existing generators
- The wrappers are placed in a `nodes/` subdirectory or `codegen/` subdirectory within the generator package to distinguish them from the generators themselves
- The wrappers use the existing CodeNodeDefinition interface and PortSpec from fbpDsl — no new infrastructure is needed
- Generator wrappers do not write files to disk — they only produce content. File writing is a separate concern for Step 4 (Code Generation FlowGraph)
- The node palette category for generator CodeNodes is a new category (e.g., "GENERATOR" or reuse of existing CodeNodeType) — the exact category is a design decision for the planning phase
- Input port types use `Any::class` as the KClass since FlowGraph, EntityModuleSpec, and UIFBPSpec are not standard IP types — the runtime uses type casting internally
