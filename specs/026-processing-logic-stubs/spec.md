# Feature Specification: Redesign Processing Logic Stub Generator

**Feature Branch**: `026-processing-logic-stubs`
**Created**: 2026-02-20
**Status**: Draft
**Input**: User description: "Redesign the ProcessingLogicStubGenerator. Remove the existing ProcessingLogic interface and processingLogic property from the CodeNode class. Instead, the ProcessingLogicStubGenerator will generate tick function stubs with the correct type alias for the node's input/output configuration. Stub files go in a LogicMethods folder (user-editable, not in generated folder). The generated NodeRuntime objects should replicate/replace the functionality of the manually-created StopWatch components."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Generate Tick Function Stubs (Priority: P1)

When the code generator processes a flow graph, it generates a tick function stub file for each node. The stub contains a function with the correct signature matching the node's input and output port count and types. The stub file is placed in a `LogicMethods/` folder within the flow graph's module, separate from auto-generated code, because the developer is expected to edit it with actual business logic.

For example, a node named "TimerEmitter" with 0 inputs and 2 outputs would produce a stub file `TimerEmitterProcessLogic.kt` containing a function with the 2-output generator tick signature. A node named "DisplayReceiver" with 2 inputs and 0 outputs would produce a stub with the 2-input sink tick signature.

**Why this priority**: This is the core new capability — without tick function stubs, there is no replacement for the removed ProcessingLogic pattern. Developers need these generated templates as starting points for writing node behavior.

**Independent Test**: Can be fully tested by running the stub generator against a flow graph with various node configurations (0-3 inputs, 0-3 outputs) and verifying the generated files contain the correct tick type alias signatures, are placed in the `LogicMethods/` folder, and compile successfully.

**Acceptance Scenarios**:

1. **Given** a flow graph with a generator node (0 inputs, 1 output of type T), **When** the stub generator runs, **Then** a file `{NodeName}ProcessLogic.kt` is created in `LogicMethods/` containing a function with the single-output generator tick signature.
2. **Given** a flow graph with a sink node (2 inputs, 0 outputs), **When** the stub generator runs, **Then** a file `{NodeName}ProcessLogic.kt` is created containing a function with the 2-input sink tick signature.
3. **Given** a flow graph with a processor node (1 input, 2 outputs), **When** the stub generator runs, **Then** a file `{NodeName}ProcessLogic.kt` is created containing a function with the 1-input, 2-output processor tick signature.
4. **Given** a stub file already exists for a node, **When** the stub generator runs again, **Then** the existing stub file is NOT overwritten (preserving user edits).

---

### User Story 2 - Update Code Generator to Reference Tick Stubs (Priority: P2)

When the code generator produces the flow graph factory code, it references the tick functions from the stub files instead of the old ProcessingLogic class references. The generated factory code creates the appropriate timed or continuous NodeRuntime by passing the tick function from the stub. This wiring replaces the old pattern where a ProcessingLogic class was instantiated and assigned to the CodeNode.

The generated code should follow the same pattern used in the manually-created StopWatch module, where `TimerEmitterComponent` defines a tick block and passes it to `createTimedOut2Generator`, and `DisplayReceiverComponent` defines a consume block and passes it to `createIn2Sink`.

**Why this priority**: Depends on US1 (stubs must exist to be referenced). This connects the stubs to the runtime, making generated flow graphs actually executable with user-defined behavior.

**Independent Test**: Can be tested by generating a flow graph factory, verifying the generated code references the tick stub functions, compiles, and creates the correct NodeRuntime types.

**Acceptance Scenarios**:

1. **Given** a flow graph with a generator node and a corresponding tick stub exists, **When** the factory code is generated, **Then** the generated code creates a timed generator runtime using the tick function from the stub.
2. **Given** a flow graph with a sink node and a corresponding tick stub exists, **When** the factory code is generated, **Then** the generated code creates a sink runtime using the consume function from the stub.
3. **Given** a flow graph with a processor node and a corresponding tick stub exists, **When** the factory code is generated, **Then** the generated code creates the appropriate processor runtime using the tick function from the stub.

---

### User Story 3 - Remove ProcessingLogic Interface and References (Priority: P3)

The existing `ProcessingLogic` functional interface and the `processingLogic` property on CodeNode are removed, along with all helper methods and references throughout the codebase. This completes the migration from the single-invocation ProcessingLogic pattern to the channel-based tick function pattern.

**Why this priority**: This is cleanup that depends on US1 and US2 being complete. The old pattern must be fully replaced before it can be safely removed. Removing it reduces API surface, eliminates confusion between the two patterns, and simplifies the CodeNode model.

**Independent Test**: Can be tested by removing the interface and property, then verifying the entire project compiles and all tests pass with no references to the removed symbols.

**Acceptance Scenarios**:

1. **Given** the codebase contains the ProcessingLogic interface in CodeNode, **When** the interface and all references are removed, **Then** the project compiles and all tests pass.
2. **Given** the CodeNode class has a `processingLogic` property, **When** the property and its helper methods (`hasProcessingLogic`, `withProcessingLogic`, `process`) are removed, **Then** no code references these symbols and the project compiles.
3. **Given** code generators previously produced ProcessingLogic references, **When** those code paths are removed, **Then** the generators produce only tick-function-based code and all generator tests pass.

---

### Edge Cases

- What happens when a node has 0 inputs and 0 outputs? The generator should skip stub generation for such nodes (no meaningful tick function exists).
- What happens when a node has more than 3 inputs or 3 outputs? The generator should report an error or warning, since the type system supports up to 3 inputs and 3 outputs.
- What happens when a stub file already exists from a previous generation? The existing file must be preserved (not overwritten) since the developer may have edited it.
- What happens when a node name contains special characters or spaces? The generator should normalize the name to a valid identifier (PascalCase) for the file and function names.
- What happens when port types are not yet specified on a node? The generator should use a sensible default type (e.g., `Any`) or skip generation with a warning.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The stub generator MUST produce a file named `{NodeName}ProcessLogic.kt` for each node in the flow graph.
- **FR-002**: The generated stub file MUST be placed in a `LogicMethods/` folder within the flow graph's module, separate from auto-generated code.
- **FR-003**: The generated stub function MUST use the correct tick type alias signature matching the node's input count (0-3) and output count (0-3).
- **FR-004**: The stub generator MUST NOT overwrite existing stub files (to preserve user edits).
- **FR-005**: The code generator MUST reference tick functions from stub files when creating NodeRuntime objects in the generated factory code.
- **FR-006**: The `ProcessingLogic` functional interface MUST be removed from the CodeNode class.
- **FR-007**: The `processingLogic` property and all helper methods (`hasProcessingLogic`, `withProcessingLogic`, `process`) MUST be removed from CodeNode.
- **FR-008**: All references to ProcessingLogic in code generators (stub generator, factory generator, DSL generator) MUST be updated or removed.
- **FR-009**: The generated NodeRuntime objects MUST replicate the functionality pattern of the existing StopWatch module's TimerEmitterComponent and DisplayReceiverComponent.
- **FR-010**: The stub generator MUST handle all valid input/output combinations: generators (0 inputs, 1-3 outputs), sinks (1-3 inputs, 0 outputs), and processors (1-3 inputs, 1-3 outputs).
- **FR-011**: The stub generator MUST produce compilable code that includes the necessary imports for the tick type alias and any referenced types.

### Key Entities

- **Tick Function Stub**: A generated template file containing a function whose signature matches the node's tick type alias. Intended to be edited by the developer with actual business logic. Named `{NodeName}ProcessLogic.kt`.
- **LogicMethods Folder**: A designated directory within the flow graph's module for user-editable processing logic files. Not part of the auto-generated code folder, signaling that these files are meant to be modified.
- **Node Configuration**: The input/output port count and port types of a CodeNode, which determine which tick type alias signature the stub function should use.
- **Tick Type Alias**: One of 16 predefined function signatures (e.g., `GeneratorTickBlock<T>`, `In2SinkBlock<A, B>`, `TransformerTickBlock<TIn, TOut>`) that define the contract for node processing behavior.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of nodes in a flow graph receive a correctly-typed tick function stub file upon code generation.
- **SC-002**: Generated stub files compile without errors when placed in the target module.
- **SC-003**: The generated flow graph factory code compiles and creates the correct NodeRuntime types using tick functions from the stubs.
- **SC-004**: Zero references to the removed ProcessingLogic interface remain in the codebase after cleanup.
- **SC-005**: Existing stub files are never overwritten during regeneration (0% data loss of user edits).
- **SC-006**: All existing tests pass after the full migration from ProcessingLogic to tick function stubs.

## Assumptions

- The tick type alias system (16 aliases for all input/output combinations) is already complete and stable (delivered in Feature 025).
- The timed factory methods (16 methods) are already available in CodeNodeFactory (delivered in Feature 025).
- Node names can be reliably converted to valid PascalCase identifiers for file and function naming.
- The `LogicMethods/` folder follows the same package naming convention as the rest of the generated module.
- Port types on CodeNode instances are available at generation time (from the flow graph model).
- The StopWatch module's TimerEmitterComponent and DisplayReceiverComponent serve as the reference implementation for the target pattern.
