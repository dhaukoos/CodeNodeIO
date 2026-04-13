# Quickstart: Fix Node and Graph Positioning Errors

## Verification Scenarios

### Scenario 1: Negative positions accepted
```
1. Open the graph editor with any flow graph
2. Drag a node to the extreme top-left corner of the canvas
3. Expected: Node moves freely; no crash occurs
4. Verify the node position can be negative (no invisible wall at 0,0)
```

### Scenario 2: Hierarchy navigation view preservation
```
1. Open a flow graph with graphNodes
2. Note the current pan position and zoom level
3. Double-click (or expand) a graphNode to navigate into it
4. Navigate back to the parent level
5. Expected: Pan and zoom are restored exactly
6. Verify: Nodes appear in the same screen positions as step 2
```

### Scenario 3: Multiple navigation cycles
```
1. Navigate into a graphNode, then back — repeat 10 times
2. Expected: Zero drift in node positions after all cycles
```

### Scenario 4: Undo after drag to negative position
```
1. Drag a node to a negative position
2. Undo the move
3. Expected: Node returns to its original position
```

### Scenario 5: GraphNode move undo
```
1. Drag a GraphNode to a new position
2. Undo the move
3. Expected: GraphNode returns to its original position (not 0, 0)
```

### Scenario 6: Zoom preservation across hierarchy
```
1. Set zoom to 150%
2. Navigate into a graphNode
3. Navigate back
4. Expected: Zoom is restored to 150%
```
