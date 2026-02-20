# Feature Specification: Timed Factory Methods

**Feature Branch**: `025-timed-factory-methods`
**Created**: 2026-02-20
**Status**: Draft
**Input**: Use the Timed Generator Factory Method as the general pattern to create timed wrapper methods for all continuous factory methods (generators, processors, sinks).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Timed Generator Wrappers (Priority: P1)

A library consumer wants to create generator nodes that emit values at a fixed interval without writing the delay loop themselves. They call a timed generator factory method, pass a tick interval and a tick function, and receive a runtime that automatically delays between invocations.

**Why this priority**: Generators are the entry points of any flow graph. The existing `createTimedOut2Generator` already proves the pattern works for 2-output generators. Extending it to single-output and 3-output generators completes the generator family and provides the template for all other timed wrappers.

**Independent Test**: Create a timed single-output generator and a timed 3-output generator, start them, and verify that values are emitted at the expected interval.

**Acceptance Scenarios**:

1. **Given** a consumer needs a single-output generator that ticks every N milliseconds, **When** they call the timed single-output generator factory method with a tick interval and a tick function, **Then** the returned runtime emits the tick function's return value once per interval until stopped.
2. **Given** a consumer needs a 3-output generator that ticks every N milliseconds, **When** they call the timed 3-output generator factory method with a tick interval and a tick function, **Then** the returned runtime distributes the tick function's result across three output channels once per interval until stopped.

---

### User Story 2 - Timed Processor Wrappers (Priority: P2)

A library consumer wants to create processor nodes (transformer, filter, or multi-input/multi-output) that process their inputs at a fixed interval rather than as fast as possible. They call a timed processor factory method, pass a tick interval and a tick function that receives the latest input(s) and returns output(s), and the runtime manages the delay loop.

**Why this priority**: Processors are the most numerous node type (9 variants: In1Out1 transformer, In1Out1 filter, In2Out1, In3Out1, In1Out2, In1Out3, In2Out2, In2Out3, In3Out2, In3Out3). Adding timed wrappers for all of them provides the most coverage.

**Independent Test**: Create a timed single-input/single-output processor and a timed multi-input/multi-output processor, wire input channels, and verify outputs arrive at the expected interval.

**Acceptance Scenarios**:

1. **Given** a consumer needs a transformer that processes input at a fixed rate, **When** they call the timed transformer factory method with a tick interval and a transform-tick function, **Then** the returned runtime reads from the input channel, applies the tick function, and sends results to the output channel once per interval.
2. **Given** a consumer needs a filter that evaluates input at a fixed rate, **When** they call the timed filter factory method with a tick interval and a predicate-tick function, **Then** the returned runtime reads from the input channel, applies the predicate, and forwards matching values once per interval.
3. **Given** a consumer needs a multi-input/multi-output processor that processes at a fixed rate, **When** they call the timed processor factory method with a tick interval and a tick function matching the input/output arity, **Then** the returned runtime reads all inputs, applies the tick function, and distributes outputs once per interval.

---

### User Story 3 - Timed Sink Wrappers (Priority: P3)

A library consumer wants to create sink nodes that consume values at a fixed interval. They call a timed sink factory method, pass a tick interval and a consume-tick function, and the runtime manages the delay loop.

**Why this priority**: Sinks are the terminal nodes — fewer variants (3: single-input, 2-input, 3-input) and simpler signatures (no output channels). They complete the timed wrapper family.

**Independent Test**: Create a timed single-input sink and a timed multi-input sink, wire input channels, and verify values are consumed at the expected interval.

**Acceptance Scenarios**:

1. **Given** a consumer needs a sink that consumes values at a fixed rate, **When** they call the timed single-input sink factory method with a tick interval and a consume-tick function, **Then** the returned runtime reads from the input channel and calls the tick function once per interval.
2. **Given** a consumer needs a multi-input sink that consumes at a fixed rate, **When** they call the timed multi-input sink factory method with a tick interval and a tick function matching the input arity, **Then** the returned runtime reads from all input channels and calls the tick function once per interval.

---

### Edge Cases

- What happens when the tick function takes longer than the tick interval? The delay between ticks should still apply after the tick completes (delay-then-tick, not fixed-rate scheduling).
- What happens when an input channel is empty at tick time? The runtime should block waiting for input (same behavior as the continuous variant), then delay after processing.
- What happens when the tick interval is zero or negative? The behavior should match the continuous variant (no delay, process as fast as possible). This is an edge case that can be documented but does not need special handling — passing zero to `delay()` yields immediately.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST provide timed wrapper factory methods for all continuous generator variants: single-output, 2-output (already exists), and 3-output.
- **FR-002**: The system MUST provide timed wrapper factory methods for all continuous processor variants: transformer (In1Out1), filter (In1Out1), In2Out1, In3Out1, In1Out2, In1Out3, In2Out2, In2Out3, In3Out2, and In3Out3.
- **FR-003**: The system MUST provide timed wrapper factory methods for all continuous sink variants: single-input, 2-input, and 3-input.
- **FR-004**: Each timed factory method MUST accept a `tickIntervalMs` parameter specifying the delay in milliseconds between ticks.
- **FR-005**: Each timed factory method MUST accept a `tick` parameter that is a dedicated type alias for a suspend lambda function whose signature matches the input/output arity of the node.
- **FR-006**: Each timed factory method MUST delegate to its corresponding continuous factory method, wrapping the tick function in a loop that delays, then invokes the tick.
- **FR-007**: Each timed factory method MUST return the same runtime type as its corresponding continuous factory method.
- **FR-008**: The tick function type aliases MUST be defined alongside the existing continuous type aliases.
- **FR-009**: Each timed factory method MUST use the same loop structure as the existing timed 2-output generator: `while (isActive) { delay(tickIntervalMs); emit/process/consume(tick(...)) }`.

### Key Entities

- **Tick Function Type Alias**: A suspend lambda type specific to each node arity. For generators, it takes no inputs and returns the output type(s). For processors, it takes the input type(s) and returns the output type(s). For sinks, it takes the input type(s) and returns Unit.
- **Timed Factory Method**: An inline function that wraps a tick function and tick interval into the corresponding continuous factory method's process/generate/consume block.

## Assumptions

- The existing `createTimedOut2Generator` is the canonical reference implementation. All new timed methods follow its exact structure.
- The delay-then-tick pattern (delay first, then invoke tick) is used consistently, matching the existing implementation.
- No new runtime classes are needed — timed wrappers return existing runtime types.
- Virtual time testing limitations apply: timed wrappers compile their delay loop in the library module, so `delay()` does not advance virtual time in tests. Tests requiring virtual time should use the continuous factory methods with test-defined loops instead.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of continuous factory methods (generators, processors, sinks) have corresponding timed wrapper variants.
- **SC-002**: All timed factory methods compile and pass type-checking with their respective tick function type aliases.
- **SC-003**: All existing tests continue to pass after the addition (no regressions).
- **SC-004**: The total number of new timed factory methods is 15 (1 generator + 10 processors + 3 sinks + 1 already exists = 16 total).
