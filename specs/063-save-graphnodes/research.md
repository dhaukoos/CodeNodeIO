# Research: Save GraphNodes

**Feature**: 063-save-graphnodes
**Date**: 2026-04-01

## R1: Serialization Format for Saved GraphNode Definitions

**Decision**: Use `.flow.kts` DSL format with a `graphNodeTemplate` wrapper and metadata comment header.

**Rationale**: The existing FlowGraphSerializer already serializes GraphNodes (including child nodes, internal connections, and port mappings) to `.flow.kts` DSL. Reusing this format:
- Leverages proven round-trip serialization (FlowGraphSerializer → FlowKtParser)
- Supports human-readable, version-controllable files
- Handles recursive GraphNode hierarchies natively
- Maintains consistency with existing `.flow.kts` conventions

The metadata comment header (similar to `@IPType` markers on IP type files) enables lightweight discovery without full parsing. The `graphNodeTemplate` wrapper distinguishes these files from full flow graphs.

**Alternatives considered**:
- **JSON via kotlinx-serialization**: GraphNode is `@Serializable` but `childNodes`, `internalConnections`, and ports are `@Transient` — would require a custom serializable wrapper to capture the full composition. More work for less human-readability.
- **Custom binary format**: Fast but not diffable or version-controllable. Contrary to project's existing text-based approach.

---

## R2: Three-Tier Filesystem Paths for GraphNode Templates

**Decision**: Follow the established tier pattern with a new `graphnodes/` directory at each level:

| Tier     | Path                                                                            |
|----------|---------------------------------------------------------------------------------|
| Module   | `{module}/src/commonMain/kotlin/io/codenode/{modname}/graphnodes/{Name}.flow.kts` |
| Project  | `{projectRoot}/graphnodes/{Name}.flow.kts`                                      |
| Universal| `~/.codenode/graphnodes/{Name}.flow.kts`                                        |

**Rationale**:
- Mirrors existing pattern: `nodes/` for CodeNodes, `iptypes/` for IP types, `graphnodes/` for GraphNodes
- Module tier requires KMP source set path for consistency, even though GraphNode templates are not compiled
- Project and Universal tiers use simpler paths (no deep `src/commonMain/kotlin/...` nesting) since these aren't compiled Kotlin sources — they're data files parsed at runtime
- Actually, Project tier for CodeNodes uses `{projectRoot}/nodes/src/commonMain/kotlin/io/codenode/nodes/` because those ARE compiled. GraphNode templates are NOT compiled, so simpler Project/Universal paths are appropriate.

**Alternatives considered**:
- **Reuse `nodes/` directory with a naming convention**: Would conflate CodeNode `.kt` sources with GraphNode `.flow.kts` templates. Discovery filtering by extension is fragile.
- **Store in a single `~/.codenode/graphnodes/` regardless of level**: Loses the tier precedence model that enables module-specific overrides.

---

## R3: GraphNode Discovery and Registry Pattern

**Decision**: Create a `GraphNodeTemplateRegistry` following the same pattern as `NodeDefinitionRegistry`, with filesystem scanning at all three tiers and in-memory caching.

**Rationale**: The existing registry pattern is proven:
1. Scan filesystem directories at each tier on startup
2. Parse metadata from comment headers for lightweight discovery (name, description, port counts, tier)
3. Full deserialization deferred to instantiation time (when dragged to canvas)
4. Deduplication by name with Module > Project > Universal precedence
5. Immediate in-memory registration after save (no restart required)

**Key difference from CodeNode discovery**: GraphNode templates are never "compiled" — they're always parsed from `.flow.kts` files. There is no ServiceLoader path. This simplifies the registry to filesystem-only discovery.

---

## R4: Child Node Promotion Strategy

**Decision**: When a GraphNode is saved at a level more general than one of its child nodes, the child node's source file is **copied** to the target level's corresponding directory. The original file at the more specific level is left intact.

**Rationale**:
- Copy (not move) preserves the original at its specific level — the module that owns it still works
- The promoted copy makes the child node available at the target level, satisfying the GraphNode's dependency
- This mirrors how a developer would manually make a module-specific node available project-wide
- Promotion applies to CodeNode `.kt` files (copy to target-level `nodes/` directory) and IP type `.kt` files (copy to target-level `iptypes/` directory)

**Complications addressed**:
- **Package declaration mismatch**: The promoted copy needs its `package` declaration updated to match the target level (e.g., `io.codenode.modulename.nodes` → `io.codenode.nodes` for Project level)
- **Import resolution**: If the promoted CodeNode references module-specific IP types, those IP types must also be promoted (transitive promotion)
- **Name collisions**: If a node with the same name already exists at the target level, the promotion dialog lists the conflict and offers to skip (reuse existing) or rename

**Alternatives considered**:
- **Move instead of copy**: Would break the source module. Rejected.
- **Symbolic links / references**: Not portable across OS or version control. Rejected.
- **Save warning badge only (original spec approach)**: Would create broken GraphNode templates. User clarified that promotion is the correct behavior.

---

## R5: Palette Card Design for GraphNodes

**Decision**: Use the existing GraphNodeRenderer color scheme (blue gradient) for palette cards, with a composition icon and child node count badge.

**Rationale**:
- GraphNodeRenderer already establishes a visual language: blue border (#1565C0), light blue fill (#E3F2FD), child node count badge
- Reusing this color scheme in the palette creates visual consistency — users recognize GraphNodes by color
- A composition icon (nested squares or circuit symbol) provides a secondary recognition signal
- Port badges remain the same format as CodeNode cards (green "N in", blue "M out") for consistency

**Alternatives considered**:
- **Different color entirely (e.g., purple)**: Would not connect to the existing canvas visual language for GraphNodes.
- **Same card as CodeNodes with a label**: Too subtle — users need to distinguish at a glance (SC-003).

---

## R6: Instantiation from Palette Template

**Decision**: When a saved GraphNode is dragged from the palette to the canvas, fully deserialize the `.flow.kts` template and create a deep copy with fresh IDs for all nodes, connections, and ports.

**Rationale**:
- Each canvas instance must be fully independent (FR-010)
- Fresh IDs prevent conflicts when multiple instances of the same template exist
- Child CodeNode types are resolved against the current registry (using latest definitions)
- Internal connections and port mappings reference child node IDs, so ID remapping must be applied consistently across the entire structure

**Process**:
1. Parse `.flow.kts` template via FlowKtParser
2. Generate new IDs for GraphNode and all child nodes
3. Remap all internal connection source/target references to new IDs
4. Remap all port `owningNodeId` references to new IDs
5. Remap all portMapping `childNodeId` references to new IDs
6. Set position from drop coordinates
7. Add to graph via `graphState.addNode()`

---

## R7: Properties Panel Integration

**Decision**: Extend the existing `GraphNodePropertiesPanel` section in `PropertiesPanel.kt` to include Add/Remove Palette controls. The panel already renders when a GraphNode is selected (lines 1367-1448).

**Rationale**:
- The Properties panel already handles GraphNode selection and shows name, ports, and child nodes
- Adding a "Palette" section below the existing content is consistent with how "Create Repository Module" works for IP types
- The level selector dropdown follows the same `PlacementLevel.availableLevels(moduleLoaded)` pattern used by NodeGeneratorPanel and IPGeneratorPanel
- Toggle between "Add to Palette" and "Remove from Palette" based on whether the GraphNode matches an existing palette entry (by name lookup in GraphNodeTemplateRegistry)
