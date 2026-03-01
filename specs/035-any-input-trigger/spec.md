# Feature Specification: Any-Input Trigger Mode for Node Generator

**Feature Branch**: `035-any-input-trigger`
**Created**: 2026-03-01
**Status**: Draft
**Input**: User description: "Node Generator update — add option to fire process block when data is received on any input (vs all inputs). Generates In{A}AnyOut{B}Runtime classes. Boolean switch in Node Generator UI."

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Create Any-Input Node via Node Generator (Priority: P1)

A graph editor user wants to create a custom node that processes data as soon as **any** of its inputs receives a value, rather than waiting for all inputs. This enables event-driven patterns where inputs arrive independently at different rates (e.g., a monitoring dashboard that updates whenever any sensor pushes new data).

The user opens the Node Generator panel, enters a node name, selects 2 or more inputs and 1 or more outputs, and toggles the new "Any Input" switch ON. The type preview updates to show the `any` variant (e.g., `in2anyout1`). On clicking Create, the custom node is saved and becomes available in the node palette for placement on the canvas.

**Why this priority**: This is the core feature — without the UI toggle and the corresponding new node type, no downstream functionality is possible.

**Independent Test**: Can be fully tested by opening the Node Generator, toggling the switch, creating a node, and verifying it appears in the palette with the correct type identifier.

**Acceptance Scenarios**:

1. **Given** the Node Generator panel is open with 2 inputs and 1 output selected, **When** the user toggles the "Any Input" switch ON, **Then** the type preview updates from `in2out1` to `in2anyout1`.
2. **Given** the "Any Input" switch is ON and the user has entered a valid name with 2 inputs and 1 output, **When** the user clicks Create, **Then** a custom node definition with the `in2anyout1` type is saved and appears in the node palette.
3. **Given** the Node Generator panel is open with only 1 input selected, **When** the user views the panel, **Then** the "Any Input" switch is not shown (since the toggle is only meaningful with 2+ inputs).
4. **Given** the Node Generator panel is open with 0 inputs selected (generator node), **When** the user views the panel, **Then** the "Any Input" switch is not shown.

---

### User Story 2 — Any-Input Runtime Behavior (Priority: P2)

When an any-input node is placed in a flow graph and the flow is started, its process block fires each time **any one** of its input channels delivers data. The process block receives the value from the input that triggered it along with the most recent values from all other inputs. If an input has not yet received any data since the last reset, a type-appropriate default value is used for that input.

This enables reactive, event-driven processing where each incoming data point is immediately processed without waiting for synchronization across all inputs.

**Why this priority**: The runtime behavior is what gives the feature its value. Without it, the toggle is cosmetic only. It depends on US1 having established the type identifier.

**Independent Test**: Can be tested by creating an any-input runtime instance, sending data on only one of its input channels, and verifying the process block fires with that value and defaults for the other inputs.

**Acceptance Scenarios**:

1. **Given** an any-input node with 2 inputs (Int, Int) and 1 output (Int), **When** data is sent to input 1 only, **Then** the process block fires with the received value for input 1 and the default value (0) for input 2.
2. **Given** an any-input node with 2 inputs where input 1 has previously received a value, **When** data is sent to input 2, **Then** the process block fires with the most recent value from input 1 and the new value from input 2.
3. **Given** an any-input node with 3 inputs, **When** data arrives on input 2, **Then** the process block fires immediately without waiting for inputs 1 or 3.
4. **Given** an any-input node that is paused, **When** data arrives on any input, **Then** the process block does not fire until the node is resumed.

---

### User Story 3 — Code Generation Support for Any-Input Nodes (Priority: P3)

When a flow graph containing any-input nodes is saved via the graph editor, the code generators produce the correct runtime class names, factory method calls, and tick type aliases for the any-input variant. The generated code uses `In{A}AnyOut{B}Runtime` classes, `createIn{A}AnyOut{B}Processor` factory methods, and `In{A}AnyOut{B}TickBlock` type aliases.

**Why this priority**: Code generation is the final integration step that allows any-input nodes to participate in the full save-compile-run lifecycle. It builds on US1 (type identifier) and US2 (runtime classes).

**Independent Test**: Can be tested by configuring a flow graph with an any-input node and verifying the generator output contains the correct class names, factory calls, and tick type aliases.

**Acceptance Scenarios**:

1. **Given** a flow graph with a 2-input any-input node and 1 output, **When** the flow generator runs, **Then** the generated flow code uses `CodeNodeFactory.createIn2AnyOut1Processor` and references the correct runtime class.
2. **Given** a flow graph with a 3-input any-input sink node, **When** the flow generator runs, **Then** the generated code uses `CodeNodeFactory.createIn3AnySink`.
3. **Given** a flow graph with an any-input node, **When** the processing logic stub generator runs, **Then** it produces a stub with the `In2AnyOut1TickBlock` (or equivalent) type alias.

---

### Edge Cases

- What happens when the user toggles "Any Input" ON and then reduces inputs to 1? The switch should automatically turn OFF and hide, since any-input mode is only meaningful with 2+ inputs.
- What happens when all inputs receive data simultaneously on an any-input node? The process block fires once per input event — it does not coalesce simultaneous arrivals into a single invocation.
- What happens when an any-input node is reset? All cached "most recent" values for inputs revert to their type-appropriate defaults.
- What happens when a custom any-input node definition is loaded from persistence? The `anyInput` flag is preserved and the node appears with the correct type identifier in the palette.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The Node Generator panel MUST display an "Any Input" toggle switch when the selected input count is 2 or more.
- **FR-002**: The "Any Input" toggle MUST be hidden when the input count is 0 or 1.
- **FR-003**: When the "Any Input" toggle is ON, the type preview MUST display the `any` variant (e.g., `in2anyout1` instead of `in2out1`).
- **FR-004**: Custom node definitions MUST persist the any-input flag so it survives application restarts.
- **FR-005**: The system MUST provide runtime classes following the `In{A}AnyOut{B}Runtime` naming pattern for all valid multi-input combinations (2-3 inputs, 0-3 outputs).
- **FR-006**: Any-input runtime classes MUST fire the process block each time any single input channel delivers data, without waiting for other inputs.
- **FR-007**: When an any-input process block fires, it MUST provide the triggering input's new value and the most recent values from all other inputs (or type-appropriate defaults if no value has been received yet).
- **FR-008**: Any-input runtime classes MUST support pause, resume, stop, and reset operations identically to existing all-input runtime classes.
- **FR-009**: The code generators (flow generator, processing logic stub generator) MUST recognize any-input nodes and generate the correct class names, factory methods, and tick type aliases.
- **FR-010**: Factory methods MUST exist for all any-input runtime variants, following the `createIn{A}AnyOut{B}Processor` naming pattern (and `createIn{A}AnySink` for zero-output variants).

### Key Entities

- **CustomNodeDefinition**: Extended with an `anyInput` boolean flag (default false). Determines whether the node uses all-input or any-input trigger semantics. Affects the `genericType` computation (e.g., `in2anyout1` vs `in2out1`).
- **In{A}AnyOut{B}Runtime**: New family of runtime classes that listen on multiple input channels concurrently and fire the process block whenever any single input delivers data. Holds cached "most recent" values per input.
- **In{A}AnyOut{B}TickBlock / In{A}AnySinkTickBlock**: New type aliases for the process/consume blocks used by any-input runtime classes.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can create an any-input custom node via the Node Generator in under 30 seconds (same workflow as existing node creation, plus one toggle).
- **SC-002**: An any-input node with 2 inputs fires its process block within 1 tick of receiving data on a single input, without requiring data on the other input.
- **SC-003**: All existing node creation workflows continue to function identically — the default behavior (all-input trigger) is unchanged when the toggle is OFF.
- **SC-004**: 100% of multi-input combinations (2-3 inputs x 0-3 outputs = 8 variants) have corresponding any-input runtime classes, factory methods, and tick type aliases.
- **SC-005**: Custom node definitions with the any-input flag persist correctly across application restarts with no data loss.

## Assumptions

- The "Any Input" toggle defaults to OFF, preserving backward compatibility with existing nodes.
- Type-appropriate default values follow existing conventions: `0` for Int, `0L` for Long, `0.0` for Double, `0.0f` for Float, `""` for String, `false` for Boolean.
- The any-input process block signature is the same as the all-input variant (all input values are always provided), but nullable or default-valued for inputs that haven't fired yet.
- Single-input nodes (1 input) do not need any-input mode since there's only one input — the distinction is meaningless.
- The any-input variant does not apply to generator nodes (0 inputs) since they have no input channels.
