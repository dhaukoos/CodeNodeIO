# Research: Debuggable Data Runtime Preview

**Feature**: 044-debuggable-data-preview
**Date**: 2026-03-07

## R1: Channel Wrapping Strategy — DebuggableChannel

**Decision**: Create a `DebuggableChannel<T>` wrapper that delegates to a real `Channel<T>` and captures the most recent value in a `MutableStateFlow<T?>` when debug mode is enabled. The wrapper implements `SendChannel<T>` via delegation.

**Rationale**: Channels in the runtime are created in two places: (1) `init {}` blocks (e.g., `In2Out2Runtime` line 56) and (2) `start()` method recreation (e.g., `SourceOut2Runtime` line 72). The `DebuggableChannel` wraps the underlying channel at creation time, intercepting `send()` to snapshot the value. This is minimally invasive — runtime classes continue to use `Channel(capacity)` internally, and the wrapping happens at the wiring layer or channel creation.

**Key insight**: Output channels are typed as `Channel<U>?` (not `SendChannel`), but the downstream nodes only use `ReceiveChannel<A>?` for input. The `DebuggableChannel` needs to implement `SendChannel<T>` by delegating to the internal channel, while also exposing the internal channel as `ReceiveChannel<T>` for wiring.

**Alternatives considered**:
- Modify every runtime's `send()` call to also snapshot: Too invasive, touches 20+ runtime files. Rejected.
- Use a coroutine Flow tap/interceptor: Channels don't support tapping natively. Rejected.
- Store snapshots in the animation controller: Conflates animation (visual dots) with data capture (debugging). Rejected.

## R2: Where to Store and Access Snapshots

**Decision**: Store a `Map<String, StateFlow<Any?>>` in a new `DataFlowDebugger` class within the `circuitSimulator` module, keyed by connection ID. The `RuntimeSession` owns the debugger instance and exposes it to the UI.

**Rationale**: The `RuntimeSession` already manages the `animateDataFlow` toggle and the `DataFlowAnimationController`. Adding a parallel `DataFlowDebugger` follows the same ownership pattern. Connection IDs are already available from the `FlowGraph.connections` list and are used by the animation system to map emissions to connections.

**Alternatives considered**:
- Store snapshots directly on `Connection` model objects: Mixes runtime state with serializable model data. Rejected.
- Store in `DataFlowAnimationController`: Conflates concerns (animation vs debugging). Rejected.
- Store in individual `NodeRuntime` instances: Snapshots are per-connection, not per-node. A single node can have multiple output connections. Rejected.

## R3: Mapping Channels to Connection IDs

**Decision**: Reuse the existing `nodePortToConnections` mapping pattern from `DataFlowAnimationController.createEmissionObserver()`. When debug mode is enabled, the emission observer (which already receives `(nodeName, portIndex)`) can also update the snapshot for the corresponding connection(s).

**Rationale**: The animation controller already solves the "which connection does this emission belong to?" problem by pre-computing a `Map<String, List<String>>` from `"nodeName:portIndex"` to connection IDs. The same mapping can be used for debug snapshots, avoiding duplicate resolution logic.

**Key insight**: The `onEmit` callback in runtimes fires AFTER `send()` completes. To capture the actual value, we need to intercept at the `send()` level (DebuggableChannel), not at the `onEmit` level. However, the `onEmit` callback can be used to trigger snapshot retrieval from the DebuggableChannel if we associate channels with connections.

**Simplified approach**: Rather than intercepting `send()` directly, extend the `onEmit` callback signature or create a parallel callback that receives the value. The runtimes already have `result.out1?.let { out1.send(it); onEmit?.invoke(...) }` — adding value capture here is a 1-line change per emission site.

## R4: Properties Panel Integration

**Decision**: Extend the existing connection properties display in `PropertiesPanel.kt` to show the transit snapshot value when: (1) a connection is selected, (2) execution is paused, and (3) debug mode is enabled.

**Rationale**: `PropertiesPanel.kt` already handles displaying connection properties when a connection is selected. The snapshot display is an additive section below existing properties. The panel receives the selected connection via state, and can receive the snapshot map from `RuntimeSession` through the existing composable parameter chain.

**Display format**: Show the value's `toString()` representation in a read-only text area. For complex objects, this provides a reasonable default. Future enhancements could add structured display for known types.

## R5: Lifecycle — When to Capture and Clear

**Decision**:
- **Capture**: When `animateDataFlow` is true and execution state is RUNNING
- **Clear**: When `stop()` is called or when `animateDataFlow` is toggled off
- **Pause behavior**: Snapshots are retained when paused (that's the whole point — inspect while paused)

**Rationale**: Matches the existing animation lifecycle. The animation controller creates/clears animations on start/stop. The debugger follows the same pattern. Pausing freezes the snapshot state, allowing inspection.
