# Research: Animate Data Flow

**Feature**: 041-animate-data-flow
**Date**: 2026-03-06

## R1: Connection Curve Rendering

**Decision**: Connections are rendered as cubic Bezier curves with horizontal control point distribution.

**Rationale**: The existing `ConnectionRenderer.kt` uses `createBezierPath()` with control points offset horizontally at 50% of the horizontal distance between ports. This provides the parametric curve data needed for dot animation interpolation.

**Key Details**:
- `control1 = Offset(start.x + dx * 0.5f, start.y)`
- `control2 = Offset(end.x - dx * 0.5f, end.y)`
- Stroke widths: 2f (normal), 2.5f (hover), 3f (selected) — all multiplied by `scale`
- Port positions calculated via `calculatePortPosition()` in `ConnectionRenderer.kt` (lines 296-331)

**Files**: `graphEditor/src/jvmMain/kotlin/rendering/ConnectionRenderer.kt`, `graphEditor/src/jvmMain/kotlin/ui/FlowGraphCanvas.kt`

## R2: IP Emission Interception Strategy

**Decision**: Add `onEmit` callback to `NodeRuntime` base class and `setEmissionObserver` to `ModuleController` interface.

**Rationale**: Each runtime class has explicit `send()` calls on output channels. Adding a single callback property to the base class is minimally invasive. Generated controllers set the callback on each runtime when requested. This keeps the hook in fbpDsl minimal while letting circuitSimulator own the animation logic.

**Alternatives Considered**:
1. **Channel wrapping**: Create `MonitoredChannel<T>` wrappers — rejected because channel assignment order (wireConnections) makes wrapping fragile and requires unsafe type casts
2. **Extend RuntimeRegistry**: Add emission tracking — rejected because registry manages lifecycle, mixing concerns; also requires same per-runtime modifications
3. **StateFlow observation**: Monitor module state flows — rejected because not all emissions produce observable state changes, and it's module-specific

**Emission Points to Instrument** (one-line addition per send point):
- `SourceRuntime` (1 point), `TransformerRuntime` (1), `FilterRuntime` (1)
- `SourceOut2Runtime` (2), `SourceOut3Runtime` (3)
- `In1Out2Runtime` (2), `In1Out3Runtime` (3)
- `In2Out1Runtime` (1), `In2Out2Runtime` (2), `In2Out3Runtime` (3)
- `In3Out1Runtime` (1), `In3Out2Runtime` (2), `In3Out3Runtime` (3)
- Plus `In2AnyOut*` and `In3AnyOut*` variants

## R3: Animation State Architecture

**Decision**: Create `DataFlowAnimationController` in circuitSimulator that manages animation events and exposes state via StateFlow.

**Rationale**: Per the spec constraint, animation logic should reside in circuitSimulator. The controller:
- Receives emission callbacks from runtimes (via ModuleController)
- Maps (nodeId, portIndex) → connectionId(s) using FlowGraph
- Creates timed animation events (duration = 80% of attenuationMs)
- Exposes `StateFlow<List<ConnectionAnimation>>` for graphEditor to render

The graphEditor only needs to: observe the animation state, interpolate Bezier positions, and draw dots.

## R4: Bezier Interpolation for Dot Position

**Decision**: Use standard cubic Bezier interpolation formula with the existing control point strategy.

**Rationale**: The formula `B(t) = (1-t)³P0 + 3(1-t)²tP1 + 3(1-t)t²P2 + t³P3` gives exact position at progress `t ∈ [0,1]`. The control points are already computed in `createBezierPath()`. We extract the same logic for the interpolation utility.

## R5: Canvas Integration

**Decision**: Pass animation state to FlowGraphCanvas as a new parameter; render dots in a dedicated drawing pass after connections.

**Rationale**: FlowGraphCanvas already receives `displayConnections` and computes port positions. Adding an `activeAnimations` parameter keeps the existing rendering pipeline intact. Drawing dots after connections ensures they render on top of the curves.

**Key Insight**: Port positions are computed per-frame in the rendering pipeline, not cached. The animation rendering must use the same `calculatePortPosition()` logic to stay in sync with connection curves.

## R6: Toggle UI Placement

**Decision**: Place toggle in upper-right of the Speed Attenuation section within RuntimePreviewPanel.

**Rationale**: The toggle is directly related to attenuation (gated by threshold), so co-locating them provides clear visual relationship. Using a compact toggle button rather than a full-width checkbox saves space in the narrow panel.
