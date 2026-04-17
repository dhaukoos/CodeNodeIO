# Research: Subscription Tier Feature Flags

**Feature**: 074-subscription-tier-flags
**Date**: 2026-04-17

## R1: Save vs Code Generation Separation

**Decision**: Split `ModuleSaveService.saveModule()` into two distinct operations: (1) a save-only path that writes the `.flow.kt` file, and (2) a generate path that produces the full module scaffolding.

**Rationale**: Currently `ModuleSaveService.saveModule()` is monolithic — it creates the module directory structure, writes `build.gradle.kts`, writes the `.flow.kt` file, generates 4 runtime controller files, generates the ViewModel, generates the UI stub, and conditionally generates the persistence layer. The `.flow.kt` write is embedded at step 3 in the middle of this chain. There is no way to save a graph without triggering full code generation.

The `.flow.kt` serialization is already handled by `FlowGraphSerializer.serialize()` from the `flowGraph-persist` module — this is a clean separation point. Save can call `FlowGraphSerializer` directly without going through `ModuleSaveService`.

**Current call chain**:
- User clicks Save → `GraphEditorApp.onSaveGraph(dir)` → `ModuleSaveService(dir, ...).saveModule(flowGraph, ...)` → generates everything

**Proposed call chain**:
- Save: User clicks Save → `FlowGraphSerializer.serialize()` → writes `.flow.kt` only
- Generate: User clicks "Generate Module" → `ModuleSaveService.saveModule()` → generates full module (including `.flow.kt` update)

**Key files**:
- `flowGraph-generate/src/jvmMain/.../save/ModuleSaveService.kt` — the monolithic save+generate method
- `flowGraph-persist/src/jvmMain/.../serialization/FlowGraphSerializer.kt` — already has standalone `.flow.kt` serialization
- `graphEditor/src/jvmMain/.../ui/GraphEditorApp.kt` (lines ~231-291) — triggers save via `onSaveGraph`

**Alternatives considered**:
- Add a flag to `saveModule(generateCode = false)`: Rejected — muddies the API. Two distinct operations should have distinct methods.
- Keep Save bundled but skip generation for Free tier: Rejected — breaks the mental model. Save should always mean "persist my work."

## R2: Feature Flag Architecture

**Decision**: Use a `SubscriptionTier` enum with a `FeatureGate` service that holds the current tier as a `StateFlow<SubscriptionTier>`. Feature checks are simple `tier >= requiredTier` comparisons using enum ordinal ordering (FREE < PRO < SIM).

**Rationale**: The tier model is cumulative (Sim includes Pro includes Free), which maps naturally to enum ordinal comparison. A `StateFlow` enables reactive UI updates when the tier changes at runtime (FR-009). The service lives in a new shared module (or `fbpDsl`) so both `flowGraph-generate` and `graphEditor` can depend on it.

**Tier → Feature mapping**:
- FREE: Graph editing, save `.flow.kt`, basic nodes (all standard arities), built-in IP types (5)
- PRO: Everything in Free + "Generate Module" action, entity/repository nodes, custom nodes, custom/architecture IP types
- SIM: Everything in Pro + Runtime Preview panel (start/stop, pause/resume, attenuation, animations, debug snapshots)

**Module placement**: The `SubscriptionTier` enum and `FeatureGate` interface belong in `fbpDsl` (the foundational domain model module) since both `flowGraph-generate` and `graphEditor` already depend on it. This avoids a new module for a small abstraction.

**Persistence**: The tier is read from a local properties file at `~/.codenode/config.properties` with key `subscription.tier=FREE|PRO|SIM`. Defaults to FREE if not set. This file can be updated at runtime (e.g., after license validation in a future feature).

**Alternatives considered**:
- Dedicated `subscription` module: Rejected — too heavy for an enum + service interface. `fbpDsl` is the right shared foundation.
- Koin DI for FeatureGate: Viable but deferred — current DI usage in the project is limited. A simple singleton or constructor injection is sufficient.
- Environment variable: Rejected — harder for non-technical users to change. Properties file is more accessible.

## R3: Runtime Preview Gating Strategy

**Decision**: Gate the Runtime Preview at the `CollapsiblePanel` level in `GraphEditorApp.kt`. When tier < SIM, replace the panel content with an upgrade prompt. The panel title and collapse state remain functional.

**Rationale**: The Runtime Preview panel is rendered inside a `CollapsiblePanel` in `GraphEditorApp.kt`. All execution controls (Start/Stop, Pause/Resume, attenuation, animation, debug) are inside `RuntimePreviewPanel.kt`. The cleanest gate point is at the panel content level — show an upgrade message instead of the preview controls.

**graphEditor imports from flowGraph-execute**:
- `ModuleSessionFactory`, `RuntimeSession`, `ExecutionState`, `ConnectionAnimation`
- `DataFlowAnimationController`, `DataFlowDebugger`, `PreviewRegistry`

These imports remain — the code stays compiled. The gate is a runtime check, not a compile-time removal. This keeps the codebase unified and avoids conditional compilation complexity.

**Alternatives considered**:
- Remove flowGraph-execute dependency for Free/Pro builds: Rejected — requires separate build configurations, complicates CI/CD, and doesn't match the desktop app distribution model (single binary).
- Disable individual controls inside RuntimePreviewPanel: Rejected — more invasive, harder to maintain, and the upgrade prompt is a better UX than a panel full of disabled buttons.

## R4: Node Palette Gating Strategy (Revised)

**Decision**: Filter the node palette at the ViewModel/data level, hiding only entity/repository nodes for Free-tier users. All other node types — including custom (user-defined) nodes — are available at all tiers.

**Rationale**: Nodes are presented via `NodeDefinitionRegistry` which discovers `NodeTypeDefinition` objects. Each definition has a `category` field (e.g., "Generator", "Transformer", "Filter", "Sink", "Repository"). The only category gated behind Pro is "Repository" (entity/repository nodes). Custom nodes from `FileCustomNodeRepository` or filesystem discovery are available to all tiers.

**Free tier includes**: All standard built-in node configurations at any arity (generators, transformers, processors, filters, sinks), plus all custom (user-defined) nodes, plus all IP types (built-in, custom, architecture, module-level).

**Pro tier adds**: Entity/repository nodes (category "Repository") and any future Pro-only node categories.

**Filtering approach**: The palette ViewModel filters based on:
- Category == "Repository" (or future Pro-only categories) → hidden when tier < PRO
- Everything else → visible at all tiers

**Alternatives considered**:
- Gate custom nodes behind Pro: Rejected — user clarification explicitly includes custom nodes in Free tier.
- Add `tier` field to `NodeTypeDefinition`: More explicit but unnecessary given only one category is gated. Category-based filtering is sufficient.
- Show but disable Pro nodes in Free tier: Could be a good upsell UX, but adds complexity. Start with hiding and iterate.

## R5: IP Type Palette Gating Strategy (Removed)

**Decision**: No IP type gating. All IP types are available at all tiers.

**Rationale**: Per user clarification (2026-04-17), Free tier includes all IP types — built-in, custom, architecture, and module-level. IP types do not need tier-based filtering. This simplifies the implementation by removing the need for an `isBuiltIn()` check or palette filtering for IP types.

## R6: "Generate Module" UI Placement

**Decision**: Add a "Generate Module" toolbar button next to the existing Save button. The button is disabled (with tooltip) when the user is on Free tier.

**Rationale**: The toolbar is the most discoverable location. Placing it adjacent to Save reinforces the mental model: Save persists your work, Generate creates the runnable module. A disabled state with tooltip ("Upgrade to Pro to generate modules") provides clear upgrade guidance without being disruptive.

**Alternatives considered**:
- Menu item only: Less discoverable. The toolbar is where Save lives, and Generate is a peer action.
- Combined Save + Generate with confirmation dialog: Rejected — contradicts US1's goal of clean separation.
- Right-click context menu: Too hidden for a primary action.

## R7: Module Placement for FeatureGate

**Decision**: Place `SubscriptionTier` enum and `FeatureGate` interface in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/`. Place `LocalFeatureGate` (properties-file implementation) in `fbpDsl/src/jvmMain/kotlin/io/codenode/fbpdsl/subscription/`.

**Rationale**: `fbpDsl` is the foundational module that all other modules depend on. Placing the tier model here allows `flowGraph-generate`, `flowGraph-execute`, and `graphEditor` to all reference it without circular dependencies. The enum goes in `commonMain` (KMP-compatible), and the file-based implementation goes in `jvmMain` (uses `java.io.File`).

**Alternatives considered**:
- New `subscription` module: Rejected — overkill for an enum + interface + one implementation. Adds build complexity.
- In `graphEditor` only: Rejected — `flowGraph-generate` also needs tier awareness for future API-level gating.
