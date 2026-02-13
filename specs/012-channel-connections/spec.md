# Feature Specification: Channel-Based Connections

**Feature Branch**: `012-channel-connections`
**Created**: 2026-02-12
**Status**: Draft
**Input**: User description: "Use Channels for Connections - The original concept was to use Kotlin channels for the Connections, as opposed to MutableSharedFlows. Create a plan to refactor CodeNodeIO to use channels for implementing Connections."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Channel Semantics for Data Flow (Priority: P1)

As a flow designer, I want connections between nodes to use proper channel semantics so that data flows according to FBP principles with backpressure support and clear send/receive patterns.

**Why this priority**: This is the core architectural change that enables proper FBP behavior. Channels provide backpressure (sender blocks when buffer full), clear ownership semantics (single consumer), and align with the original FBP design where connections are bounded buffers between processes.

**Independent Test**: Can be tested by running the StopWatch flow and verifying that TimerEmitter sends data through a channel to DisplayReceiver, with the channel respecting its configured capacity.

**Acceptance Scenarios**:

1. **Given** a connection with channelCapacity=0 (rendezvous), **When** a sender emits data, **Then** the sender suspends until a receiver is ready to receive
2. **Given** a connection with channelCapacity=5 (buffered), **When** a sender emits 5 packets with no active receiver, **Then** the sender can emit all 5 without blocking, but blocks on the 6th
3. **Given** an active connection, **When** the flow is stopped, **Then** the channel is closed and both sender and receiver complete gracefully

---

### User Story 2 - Generated Code Uses Channels (Priority: P2)

As a developer using CodeNodeIO, I want the code generator to produce channel-based connection wiring so that generated modules follow the correct channel semantics.

**Why this priority**: The code generator must produce correct channel-based code for the runtime to work properly. This depends on US1 establishing the patterns.

**Independent Test**: Generate the StopWatch module and verify the generated FlowGenerator code creates Channels instead of MutableSharedFlows.

**Acceptance Scenarios**:

1. **Given** a FlowGraph with connections, **When** the module is generated, **Then** each connection is implemented as a Channel with the specified capacity
2. **Given** a generated module, **When** wireConnections() is called, **Then** channels are created and passed to components for send/receive operations
3. **Given** a multi-connection flow, **When** generated, **Then** each connection has its own independent channel instance

---

### User Story 3 - Component Channel Integration (Priority: P3)

As a component developer, I want a clear pattern for components to send to and receive from channels so that I can implement ProcessingLogic that works with the channel-based architecture.

**Why this priority**: Component authors need clear guidance on how to interact with channels. This builds on US1 and US2.

**Independent Test**: Update TimerEmitterComponent and DisplayReceiverComponent to use channel-based I/O and verify the StopWatch continues to function correctly.

**Acceptance Scenarios**:

1. **Given** a generator component (no inputs), **When** it produces output, **Then** it sends to a SendChannel provided by the flow orchestrator
2. **Given** a sink component (no outputs), **When** it receives data, **Then** it receives from a ReceiveChannel provided by the flow orchestrator
3. **Given** a transformer component (inputs and outputs), **When** processing, **Then** it receives from ReceiveChannel(s) and sends to SendChannel(s)

---

### Edge Cases

- What happens when a channel is closed while data is in the buffer? Data should remain consumable until the buffer is empty, then the channel reports closed
- How does the system handle a slow consumer? The sender blocks when the buffer is full, providing natural backpressure
- What happens on flow cancellation? Channels are cancelled, pending operations throw CancellationException which is handled gracefully
- How are channel leaks prevented? Channels are closed in stop() and through CoroutineScope cancellation

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Connections MUST be implemented using Kotlin Channels instead of MutableSharedFlow
- **FR-002**: Channel capacity MUST match the Connection.channelCapacity property (0 = rendezvous, >0 = buffered)
- **FR-003**: Generated flow code MUST create Channel instances for each Connection in the FlowGraph
- **FR-004**: Components MUST receive SendChannel/ReceiveChannel interfaces rather than MutableSharedFlow
- **FR-005**: Flow orchestrator MUST close all channels when stop() is called
- **FR-006**: Channel closure MUST propagate gracefully, allowing buffered data to be consumed before signaling closure
- **FR-007**: Backpressure MUST be applied when a buffered channel reaches capacity (sender suspends)
- **FR-008**: The system MUST support UNLIMITED capacity channels for specific use cases where backpressure is not desired

### Key Entities

- **Channel**: Communication primitive for sending data between coroutines; has a SendChannel side (producer) and ReceiveChannel side (consumer)
- **Connection**: Model representing the link between ports; the channelCapacity property maps directly to Channel buffer size
- **SendChannel**: Interface provided to producer components for emitting data packets
- **ReceiveChannel**: Interface provided to consumer components for receiving data packets

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: All existing flows (StopWatch) continue to function correctly after migration to channels
- **SC-002**: Generated code uses Channel instead of MutableSharedFlow for all connection implementations
- **SC-003**: Backpressure behavior is demonstrable: a slow consumer causes the producer to wait when the buffer is full
- **SC-004**: All existing tests pass after the refactoring
- **SC-005**: No data loss occurs during normal flow execution with channels
- **SC-006**: Flow shutdown completes gracefully with all channels properly closed

## Assumptions

- Kotlin Channels are available in kotlinx.coroutines (already a project dependency)
- Single-consumer pattern is acceptable (each channel has one receiver) - this aligns with FBP principles where each connection has exactly one source and one target
- The existing Connection.channelCapacity property correctly represents the desired buffer size
- Components can be updated to accept Channel interfaces without breaking the ProcessingLogic contract
