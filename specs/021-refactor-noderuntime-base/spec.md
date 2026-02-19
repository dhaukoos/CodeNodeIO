# Feature Specification: Refactor Base NodeRuntime Class

**Feature Branch**: `021-refactor-noderuntime-base`
**Created**: 2026-02-19
**Status**: Draft
**Input**: User description: "Refactor the base NodeRuntime class to remove the generics <T: Any> from its signature and remove its variables for inputChannel and outputChannel. Then update all of the In[a]Out[b]Runtime classes that inherit from it to add an explicit inputChannel1."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Remove Generic Type and Channels from Base Runtime (Priority: P1)

A framework developer maintaining the runtime class hierarchy needs the base NodeRuntime class to be type-agnostic. Currently, NodeRuntime carries a generic type parameter that serves as the type for its input and output channel properties. However, most specialized subclasses ignore these base-class channels entirely and define their own typed channels. Removing the generic parameter and the unused channel properties simplifies the base class to its true responsibility: lifecycle management (start, stop, pause, resume, execution state, job control, and registry).

**Why this priority**: This is the foundational change. All subsequent updates to subclasses depend on the base class no longer carrying a generic parameter or channel properties.

**Independent Test**: Can be verified by confirming that the base NodeRuntime class compiles without a type parameter and that lifecycle methods (start, stop, pause, resume) function correctly without channel references.

**Acceptance Scenarios**:

1. **Given** the base NodeRuntime class, **When** the type parameter is removed, **Then** the class signature has no generic parameter.
2. **Given** the base NodeRuntime class, **When** the input and output channel properties are removed, **Then** the start method no longer references output channel in its cleanup block.
3. **Given** any code referencing NodeRuntime with a wildcard generic, **When** the wildcard generic is removed, **Then** references update to plain NodeRuntime and all existing functionality is preserved.

---

### User Story 2 - Update Single-Type Runtime Subclasses (Priority: P2)

The four original single-type runtime subclasses (generator, sink, transformer, filter) currently inherit from the base class using its generic parameter and some use the base class's channel properties directly. After the base class loses these properties, each subclass must own its own channel properties with appropriate types and names. Single-input subclasses keep the name `inputChannel` (no number suffix, since there is only one input). Single-output subclasses that previously used prefixed names (e.g., `transformerOutputChannel`) rename to `outputChannel` for consistency.

**Why this priority**: These classes directly depend on base class channels and will break immediately when they are removed.

**Independent Test**: Each updated subclass can be tested by creating an instance, wiring channels, starting it, sending/receiving data, and verifying pause/resume/stop behavior.

**Acceptance Scenarios**:

1. **Given** the single-output generator runtime, **When** the base class no longer has output channel, **Then** the generator defines its own output channel property and manages its lifecycle.
2. **Given** the single-input sink runtime, **When** the base class no longer has input channel, **Then** the sink defines `inputChannel` as its own property (same name, single-input) and reads from it during processing.
3. **Given** the transformer runtime, **When** the base class no longer has input channel, **Then** the transformer defines `inputChannel` as its own property and renames `transformerOutputChannel` to `outputChannel`.
4. **Given** the filter runtime, **When** the base class no longer has either channel, **Then** the filter defines both `inputChannel` and `outputChannel` as its own properties.
5. **Given** any existing test for these single-type runtimes, **When** the refactoring is complete, **Then** the test passes after updating `transformerOutputChannel` references to `outputChannel` where applicable.

---

### User Story 3 - Update Multi-Port Runtime Subclasses (Priority: P3)

The twelve multi-port runtime subclasses currently inherit from the base class using the first input type as the generic parameter. Several of these use the base class's input channel for their first input. After the refactoring, multi-input classes that used the inherited input channel must add an explicit `inputChannel1` property (numbered for consistency with `inputChannel2`/`inputChannel3`). Single-input multi-output classes keep `inputChannel` (no number). Single-output processors rename prefixed output channels (e.g., `processorOutputChannel`) to `outputChannel`. All classes update their inheritance to the non-generic base class.

**Why this priority**: These classes are more numerous but the changes are mechanical and follow the same pattern established in the single-type subclass updates.

**Independent Test**: Each updated subclass can be tested by verifying channel wiring, data flow, and lifecycle behavior with the renamed inputChannel1 property.

**Acceptance Scenarios**:

1. **Given** a multi-input runtime (e.g., 2-input sink), **When** it previously relied on base class input channel, **Then** it defines `inputChannel1` as its own property.
2. **Given** a single-input multi-output runtime (e.g., In1Out2Runtime), **When** the base class generic is removed, **Then** it defines `inputChannel` as its own property (no number suffix for single-input).
3. **Given** a single-output processor runtime (e.g., In2Out1Runtime), **When** the base class generic is removed, **Then** it renames `processorOutputChannel` to `outputChannel`.
4. **Given** all 16 runtime subclasses updated, **When** the full test suite runs, **Then** all existing tests pass with updated channel property names.

---

### User Story 4 - Update External References (Priority: P4)

Components outside the runtime module that reference NodeRuntime with generic parameters, inputChannel, or outputChannel must be updated. This includes the runtime registry, factory methods, application-level components, and test files.

**Why this priority**: These are downstream consumers that must be updated to match the new API, but the changes are straightforward.

**Independent Test**: The full test suite across all modules passes with no compilation errors.

**Acceptance Scenarios**:

1. **Given** the registry references NodeRuntime with a wildcard generic, **When** the generic is removed, **Then** references update to plain NodeRuntime.
2. **Given** factory methods create runtime instances, **When** subclass signatures change, **Then** factory methods compile and return correctly typed instances.
3. **Given** the full project, **When** all modules are built and tested, **Then** zero compilation errors and all tests pass.

---

### Edge Cases

- What happens when the base start method no longer closes output channel in its cleanup block? Each subclass that has output channels must handle their own channel closure in their overridden start method. Most already do this today.
- What happens to FilterRuntime which uses the same type for input and output? It still needs both an `inputChannel` and an `outputChannel` property, just defined on the class itself instead of inherited.
- What happens to TransformerRuntime's specially-named output channel? It is renamed from `transformerOutputChannel` to `outputChannel`. The prefix was only needed when the base class owned `outputChannel`; now that subclasses own their channels, the plain name is used.
- What happens to Processor runtimes with specially-named output channels (e.g., `processorOutputChannel`)? They are renamed to `outputChannel` for the same reason as TransformerRuntime.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The base NodeRuntime class MUST NOT have a generic type parameter in its signature.
- **FR-002**: The base NodeRuntime class MUST NOT define input channel or output channel properties.
- **FR-003**: The base NodeRuntime start method MUST NOT reference output channel in its cleanup block.
- **FR-004**: Single-input runtime subclasses MUST define their own `inputChannel` property (same name as previously inherited). Multi-input runtime subclasses MUST define their own `inputChannel1` property (numbered for consistency with `inputChannel2`/`inputChannel3`).
- **FR-005**: Each runtime subclass that needs output channels MUST define and manage its own output channel properties using the name `outputChannel` for single-output (renaming `transformerOutputChannel` and `processorOutputChannel`), and numbered names (`outputChannel1`, `outputChannel2`, `outputChannel3`) for multi-output (with appropriate close-on-stop behavior).
- **FR-006**: All existing tests MUST pass after the refactoring, with channel property name updates where applicable (`transformerOutputChannel` → `outputChannel`, `processorOutputChannel` → `outputChannel`, `inputChannel` → `inputChannel1` for multi-input runtimes only).
- **FR-007**: The runtime registry MUST work with the non-generic NodeRuntime (replacing wildcard generic references with plain NodeRuntime).
- **FR-008**: All 16 runtime subclasses MUST compile and function correctly after the refactoring.

### Key Entities

- **NodeRuntime**: Base class providing lifecycle management (execution state, job control, registry) without data type coupling.
- **Runtime Subclasses**: 16 specialized classes that own their typed channel properties and processing logic.
- **RuntimeRegistry**: Central registry that tracks NodeRuntime instances by ID for coordinated lifecycle control.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: The base NodeRuntime class has zero generic type parameters (down from 1).
- **SC-002**: The base NodeRuntime class has zero channel properties (down from 2).
- **SC-003**: All 16 runtime subclasses compile and pass their existing tests.
- **SC-004**: The full test suite across all modules passes with zero failures.
- **SC-005**: Channel naming follows the convention: single-input uses `inputChannel`, multi-input uses `inputChannel1`/`inputChannel2`/`inputChannel3`; single-output uses `outputChannel`, multi-output uses `outputChannel1`/`outputChannel2`/`outputChannel3`.

## Assumptions

- Single-input runtimes keep the name `inputChannel` (no number suffix). Only multi-input runtimes use the numbered `inputChannel1`, `inputChannel2`, `inputChannel3` convention.
- Single-output runtimes use the name `outputChannel` (no number suffix). This includes renaming `transformerOutputChannel` and `processorOutputChannel` to `outputChannel`. Multi-output runtimes keep their numbered `outputChannel1`, `outputChannel2`, `outputChannel3` convention.
- Subclasses that already override start (most of them) already handle their own channel lifecycle. The base class cleanup change only affects subclasses that rely on the base implementation.
- External consumers of single-input runtimes (e.g., DisplayReceiverComponent, StopWatchFlow) do NOT need property name changes since `inputChannel` is preserved. Only consumers of multi-input runtimes or prefixed output channels need updates.
