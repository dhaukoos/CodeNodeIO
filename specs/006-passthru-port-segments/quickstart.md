# Quickstart: PassThruPort and ConnectionSegment

**Feature**: 006-passthru-port-segments
**Date**: 2026-02-04

## Developer Setup

### Prerequisites

1. CodeNodeIO repository with features 001-005 complete
2. Kotlin 2.1.21 and JDK 17+
3. IntelliJ IDEA or Android Studio with Kotlin plugin

### Running the graphEditor

```bash
# From repository root
./gradlew :graphEditor:run
```

## Feature Walkthrough

### Scenario 1: PassThruPort Creation via Grouping

**Steps**:
1. Open graphEditor and create a simple flow: Generator → Filter → Output
2. Connect Generator.output to Filter.input
3. Connect Filter.passed to Output.input
4. Shift-click to select Generator and Filter
5. Click "Group" button in toolbar

**Expected Result**:
- A new GraphNode appears containing Generator and Filter
- The GraphNode has an OUTPUT PassThruPort (square icon on right edge)
- The PassThruPort connects to Output.input
- Connection to Output shows as a single visible segment (exterior view)

**Verify**:
- Hover over the GraphNode's output port - it should be a square, not a circle
- The connection line still connects to Output correctly

---

### Scenario 2: Viewing Segments in Different Contexts

**Steps**:
1. From Scenario 1, double-click (or use expand button) on the GraphNode
2. Observe the interior view

**Expected Result**:
- Interior view shows Generator and Filter nodes
- PassThruPort visible as square on the right boundary
- Interior segment connects Filter.passed to the PassThruPort
- The segment from PassThruPort to Output is NOT visible (it's in exterior context)

**Verify**:
- Navigate back to root level
- Exterior segment (PassThruPort to Output) is now visible
- Interior segment (Filter to PassThruPort) is NOT visible

---

### Scenario 3: Input PassThruPort

**Steps**:
1. Create a flow: Source → Processor → Sink
2. Select only Processor and Sink
3. Click "Group" button

**Expected Result**:
- New GraphNode has an INPUT PassThruPort (square on left edge)
- Source.output connects to the GraphNode's input PassThruPort
- Inside the GraphNode, the PassThruPort connects to Processor.input

---

### Scenario 4: Multiple PassThruPorts

**Steps**:
1. Create a graph with multiple external connections to nodes that will be grouped
2. Example: A → X, B → X, X → Y, Y → C, Y → D (group X and Y)
3. Select X and Y, click Group

**Expected Result**:
- GraphNode has 2 INPUT PassThruPorts (from A and B)
- GraphNode has 2 OUTPUT PassThruPorts (to C and D)
- All boundary-crossing connections are properly segmented

---

### Scenario 5: Nested GraphNode Segments

**Steps**:
1. Create a flow with existing GraphNode: External → [GraphNode containing Internal] → External2
2. Navigate into the GraphNode
3. Select Internal node and some sibling
4. Group them into a nested GraphNode

**Expected Result**:
- Connection from outer boundary to nested boundary now has 3 segments
- Root view: External to outer GraphNode (1 segment)
- Outer GraphNode view: Outer boundary to inner GraphNode (1 segment)
- Inner GraphNode view: Inner boundary to Internal (1 segment)

---

### Scenario 6: Ungrouping Restores Single Segments

**Steps**:
1. From any scenario with a GraphNode
2. Select the GraphNode
3. Click "Ungroup" button

**Expected Result**:
- PassThruPorts are removed
- Multi-segment connections merge back to single segments
- Original direct connections restored

**Verify**:
- Check that connections show as single unbroken lines
- No orphaned PassThruPorts remain

---

### Scenario 7: Type Validation Prevents Invalid PassThruPorts

**Steps**:
1. Create a flow where you attempt to group nodes with incompatible port types
2. Example: String output → Integer input crossing boundary (if such mismatch exists)

**Expected Result**:
- Grouping fails with error message about type mismatch
- No GraphNode created
- Original structure preserved

---

## Key Code Locations

| Component | File Path |
|-----------|-----------|
| PassThruPort Model | `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/PassThruPort.kt` |
| ConnectionSegment Model | `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/ConnectionSegment.kt` |
| PassThruPortFactory | `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/factory/PassThruPortFactory.kt` |
| Port Rendering | `graphEditor/src/jvmMain/kotlin/rendering/PortRenderer.kt` |
| Segment Visibility | `graphEditor/src/jvmMain/kotlin/state/GraphState.kt` |
| Connection Rendering | `graphEditor/src/jvmMain/kotlin/rendering/ConnectionRenderer.kt` |

## Testing Commands

```bash
# Run PassThruPort model tests
./gradlew :fbpDsl:jvmTest --tests "io.codenode.fbpdsl.model.PassThruPortTest"

# Run ConnectionSegment model tests
./gradlew :fbpDsl:jvmTest --tests "io.codenode.fbpdsl.model.ConnectionSegmentTest"

# Run rendering tests
./gradlew :graphEditor:jvmTest --tests "io.codenode.grapheditor.rendering.PortRenderingTest"

# Run all graphEditor tests
./gradlew :graphEditor:jvmTest
```

## Common Issues

### Issue: PassThruPort not appearing after grouping
**Solution**: Ensure the connection actually crosses the group boundary. Internal-only connections don't create PassThruPorts.

### Issue: Segments not switching when navigating
**Solution**: Check that `getSegmentsInContext()` is being called with the correct `scopeNodeId` from NavigationContext.

### Issue: Square ports not rendering
**Solution**: Verify `getPortShape()` is returning `SQUARE` for PassThruPort instances. Check that the port is actually a PassThruPort, not a regular Port.

### Issue: Connection appears broken after grouping
**Solution**: Ensure segments form a continuous chain. Debug by checking `connection.validateSegmentChain()`.
