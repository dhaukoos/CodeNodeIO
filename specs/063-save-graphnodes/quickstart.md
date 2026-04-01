# Quickstart: Save GraphNodes

**Feature**: 063-save-graphnodes
**Date**: 2026-04-01

## Prerequisites

- CodeNodeIO graphEditor builds and runs
- DemoProject with at least one module containing CodeNodes (e.g., StopWatch, WeatherForecast)
- At least one GraphNode exists on the canvas (created via canvas context menu or programmatically)

---

## Scenario 1: Save a GraphNode to the Palette (US1 - P1)

### Steps

1. Launch graphEditor with DemoProject (`./gradlew runGraphEditor` from DemoProject root)
2. Open or create a flow containing a GraphNode with at least 2 child CodeNodes wired together
3. Click the GraphNode on the canvas to select it
4. In the Properties panel (right side), observe:
   - "GraphNode Properties" header with node name
   - A "Palette" section with:
     - Level selector dropdown (Project/Universal, or Module/Project/Universal if a module is loaded)
     - "Add to Palette" button
5. Select "Project" level
6. Click "Add to Palette"
7. Observe the GraphNode appears in the Node Palette (left side) under a "GraphNodes" collapsible section

### Expected Result

- The "GraphNodes" section in the Node Palette lists the saved GraphNode
- The card shows the GraphNode name, port counts, child node count, and a level indicator ("Project")
- The card has a visually-distinct blue-tinted design (different from CodeNode cards)
- A `.flow.kts` file exists at `{projectRoot}/graphnodes/{GraphNodeName}.flow.kts`

---

## Scenario 2: Instantiate a Saved GraphNode from Palette (US1 - P1)

### Steps

1. In the Node Palette, expand the "GraphNodes" section
2. Long-press on a saved GraphNode card to begin dragging
3. Drag it onto the canvas and release

### Expected Result

- A new GraphNode appears at the drop position
- The new GraphNode has the same child nodes, internal connections, and port mappings as the original
- The new instance has unique IDs (independent from the template and other instances)
- Selecting the new GraphNode shows correct properties in the Properties panel

---

## Scenario 3: Persistence Across Restart (US1 - P1)

### Steps

1. Save a GraphNode to the palette at Project level (Scenario 1)
2. Close the graphEditor
3. Re-launch the graphEditor

### Expected Result

- The saved GraphNode appears in the "GraphNodes" palette section on startup
- It can be dragged to the canvas and functions identically to when it was first saved

---

## Scenario 4: Remove a Saved GraphNode from Palette (US2 - P2)

### Steps

1. Select a GraphNode on the canvas that was previously saved to the palette
2. In the Properties panel, observe "Remove from Palette" button (instead of "Add to Palette")
3. Click "Remove from Palette"
4. Confirm in the dialog

### Expected Result

- The GraphNode disappears from the Node Palette's "GraphNodes" section
- The `.flow.kts` template file is deleted from disk
- Existing canvas instances of this GraphNode remain functional

---

## Scenario 5: Heterogeneous Level Promotion (US4 - P2)

### Steps

1. Create a GraphNode containing:
   - A Module-level CodeNode (only available in the current module)
   - A Project-level CodeNode (available project-wide)
2. Select the GraphNode and choose "Universal" level
3. Click "Add to Palette"
4. Observe the promotion dialog listing the Module-level and Project-level child nodes that will be promoted to Universal

### Expected Result

- Dialog explains: "The following child nodes will be promoted to Universal level:" with a list of affected nodes and their current levels
- Clicking "Continue":
  - The child node `.kt` files are copied to `~/.codenode/nodes/` (with updated package declarations)
  - If child nodes reference module-specific IP types, those are also copied to `~/.codenode/iptypes/`
  - The GraphNode template is saved to `~/.codenode/graphnodes/{Name}.flow.kts`
- Clicking "Cancel": No changes made

---

## Scenario 6: Search and Browse GraphNodes in Palette (US3 - P3)

### Steps

1. Save 2-3 GraphNodes to the palette at different levels
2. Expand the "GraphNodes" section in the Node Palette
3. Type part of a GraphNode name in the search box

### Expected Result

- All saved GraphNodes listed under "GraphNodes" section with distinct card design
- Each card shows: name, port count badges, child node count, level indicator
- Search filters both CodeNodes and GraphNodes simultaneously
- GraphNode cards are visually distinguishable from CodeNode cards (blue tint, composition indicator)

---

## Scenario 7: Duplicate Name Handling (Edge Case)

### Steps

1. Save a GraphNode named "MyProcessor" at Project level
2. Create a different GraphNode, also name it "MyProcessor"
3. Attempt to save it at Project level

### Expected Result

- System prompts: "A GraphNode named 'MyProcessor' already exists at Project level. Overwrite or rename?"
- "Overwrite" replaces the existing template
- "Rename" opens a name editor to choose a different name
- "Cancel" aborts the save
