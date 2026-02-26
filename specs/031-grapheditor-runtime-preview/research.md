# Research: GraphEditor Runtime Preview

**Feature**: 031-grapheditor-runtime-preview
**Date**: 2026-02-25

## R1: Speed Attenuation Injection Strategy

**Decision**: Add a nullable `attenuationDelayMs: Long?` property to `NodeRuntime` base class (default `null`), and modify the timed generator factory methods to use `delay(attenuationDelayMs ?: tickIntervalMs)` in the delay loop.

**Rationale**: The `tickIntervalMs` is captured as a closure variable in the `timedGenerate` lambda inside `CodeNodeFactory.createTimedOut2Generator()`. It cannot be changed after creation. By adding a nullable `attenuationDelayMs` to `NodeRuntime`, the timed generator loop uses `delay(attenuationDelayMs ?: tickIntervalMs)` — when null (the default), normal `tickIntervalMs` behavior is preserved; when set by the runtime preview, the attenuation value **replaces** `tickIntervalMs` entirely as the sole delay. This means:
- `null` (default): uses original `tickIntervalMs` (normal app behavior, unchanged)
- `0ms`: no delay, runs as fast as the processing loop allows (maximum preview speed)
- `1000ms`: equivalent to the StopWatch's original 1-second tick interval
- `5000ms`: very slow, 5 seconds between ticks

Since `attenuationDelayMs` is a runtime property on the base class, the circuitSimulator can set it on any generator node after creation, and changes take effect on the next loop iteration.

**Alternatives considered**:
1. **Adding attenuation to tickIntervalMs** (`delay(tickIntervalMs + attenuationDelayMs)`): More complex — the user would need to understand the nominal tick rate to reason about the final speed. Using replacement instead of addition makes the slider value directly represent the actual delay.
2. **Recreating generators with modified intervals**: Would require tearing down and rebuilding the entire flow each time attenuation changes. Unacceptable latency and complexity.
3. **Using a global/contextual delay provider**: Too invasive and would affect all generators in the JVM, not just the preview session.

## R2: Module Dependency Architecture

**Decision**: The graphEditor should depend on circuitSimulator (not the reverse). CircuitSimulator depends on fbpDsl and StopWatch. The graphEditor calls circuitSimulator APIs to create runtime sessions and get composable content for the preview pane.

**Rationale**: Currently circuitSimulator depends on graphEditor, but this creates a circular dependency problem for the preview pane. The graphEditor needs to display UI from the circuitSimulator, so the dependency direction must be: graphEditor → circuitSimulator → fbpDsl + StopWatch. This means:
- Removing graphEditor from circuitSimulator's dependencies
- Adding circuitSimulator (and StopWatch) as dependencies of graphEditor
- circuitSimulator provides the orchestration layer and composable content functions

**Alternatives considered**:
1. **Keep circuitSimulator depending on graphEditor**: Creates circular dependency — graphEditor needs to render circuitSimulator composables but circuitSimulator depends on graphEditor.
2. **Introduce a shared interface module**: Overkill for the current scope. Can be refactored later if needed.

## R3: UI Composable Rendering Approach

**Decision**: Hard-code the StopWatch composable in the circuitSimulator module for the proof-of-concept. The circuitSimulator provides a `@Composable` function that the graphEditor invokes in the preview pane.

**Rationale**: Dynamic composable loading (reflection, class loading) is complex in KMP and unnecessary for the PoC. The StopWatch module is a compile-time dependency, so its `StopWatch` composable and `StopWatchViewModel` can be directly instantiated. Future modules can be added by extending the circuitSimulator with additional compile-time dependencies and a registry/factory pattern.

**Alternatives considered**:
1. **Dynamic class loading**: Loading composables by class name at runtime is fragile, platform-specific, and would require significant infrastructure.
2. **Plugin architecture**: A proper plugin system with service loaders. Valuable long-term but excessive scope for the PoC.

## R4: Layout Integration Strategy

**Decision**: Add a collapsible right-side panel to the graphEditor that contains the runtime controls and preview pane, positioned to the right of the properties panel.

**Rationale**: The graphEditor already has a right-side properties panel (280dp). Adding another panel alongside it follows the established pattern. The runtime preview panel should be collapsible (toggle button) so it doesn't consume screen space when not in use. When expanded, it shows the controls pane (buttons + attenuation slider) above the live preview pane.

**Alternatives considered**:
1. **Bottom panel**: Would reduce canvas vertical space, which is more valuable than horizontal space for flow graph editing.
2. **Floating/dialog window**: Disconnected from the main editor workflow. Users want to see the graph and preview simultaneously.
3. **Replace properties panel**: Properties panel is still needed while the preview runs.

## R5: StopWatch Flow Graph Instantiation

**Decision**: The circuitSimulator creates a StopWatch FlowGraph using the existing DSL (`stopWatchFlowGraph` val), then instantiates `StopWatchController` with it, and wraps it with `StopWatchViewModel` for UI binding.

**Rationale**: The StopWatch module already has a `stopWatchFlowGraph` val defined in `StopWatch.flow.kt` and all the generated infrastructure (Controller, ControllerAdapter, ViewModel, Flow). The circuitSimulator just needs to wire these together and expose the composable and control actions to the graphEditor.

**Alternatives considered**:
1. **Parse the flow graph from the editor's in-memory model**: Would require serialization/deserialization. The DSL val is simpler and already tested.
2. **Create a new FlowGraph from scratch**: Duplicates existing work and wouldn't prove that the generated code works.
