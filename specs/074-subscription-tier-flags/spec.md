# Feature Specification: Subscription Tier Feature Flags

**Feature Branch**: `074-subscription-tier-flags`
**Created**: 2026-04-15
**Status**: Draft
**Input**: User description: "Plan for feature flagged subscription tiers — Free, Pro, Sim — gating code generation and runtime preview behind feature flags, separating Save from code generation."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Separate Save from Code Generation (Priority: P1)

A graph designer clicks Save and only the `.flow.kt` file is written, preserving the current flowGraph model configuration. Code generation (module scaffolding, runtime controllers, ViewModels, UI stubs, persistence layers) no longer happens as part of Save. Instead, code generation is triggered by a separate explicit action (e.g., a "Generate Module" button or menu item). This separation is the prerequisite for gating code generation behind the Pro tier.

**Why this priority**: Save currently bundles file persistence with full module code generation. Until these are separated, feature flags cannot meaningfully gate code generation — a Free-tier user saving their graph would either trigger blocked code generation or break the save flow entirely.

**Independent Test**: A user clicks Save, and only the `.flow.kt` file is written to disk. No runtime controller files, ViewModel stubs, UI composables, or persistence files are generated. The user can then separately trigger "Generate Module" and see the full module scaffolding produced. Both actions succeed independently.

**Acceptance Scenarios**:

1. **Given** a user has a flow graph open with unsaved changes, **When** they click Save, **Then** only the `.flow.kt` file is written to the output directory — no other files are created or modified
2. **Given** a user has saved a `.flow.kt` file, **When** they trigger "Generate Module", **Then** the full module scaffolding is produced (runtime controllers, ViewModel, UI stubs, persistence if applicable)
3. **Given** a user has previously generated a module, **When** they modify the graph and click Save, **Then** only the `.flow.kt` is updated — previously generated files are not regenerated or deleted
4. **Given** a user opens a previously saved `.flow.kt`, **When** they trigger "Generate Module" without any other changes, **Then** the module is regenerated from the current graph state

---

### User Story 2 - Feature Flag Infrastructure (Priority: P2)

The system introduces a subscription tier model with three levels: Free, Pro, and Sim. Each tier unlocks a cumulative set of capabilities. A feature flag service determines which tier the current user holds and gates access to tier-restricted features. Pro-tier features include code generation and entity/repository node types. Sim-tier features include everything in Pro plus the circuit simulator (Runtime Preview). Free tier includes all IP types and custom nodes.

**Why this priority**: The feature flag infrastructure is the mechanism by which tier restrictions are enforced. Without it, the separated code generation (US1) and runtime preview cannot be gated.

**Independent Test**: The feature flag service can be queried for the current tier. When set to Free, code generation and runtime preview are disabled. When set to Pro, code generation is enabled but runtime preview is disabled. When set to Sim, all features are enabled.

**Acceptance Scenarios**:

1. **Given** a user is on the Free tier, **When** they use the graph editor, **Then** they can create, edit, save, and open unlimited flow graphs using all standard node types, custom nodes, and all IP types
2. **Given** a user is on the Free tier, **When** they attempt to trigger code generation, **Then** the action is disabled and a message indicates this is a Pro-tier feature
3. **Given** a user is on the Pro tier, **When** they trigger code generation, **Then** the full KMP module is generated successfully
4. **Given** a user is on the Pro tier, **When** they attempt to use the Runtime Preview, **Then** the panel indicates this is a Sim-tier feature
5. **Given** a user is on the Sim tier, **When** they use any feature, **Then** all capabilities are available without restriction

---

### User Story 3 - Gate Runtime Preview Behind Sim Tier (Priority: P3)

A Free or Pro tier user sees the Runtime Preview panel in a disabled or locked state with a clear indication that circuit simulation requires the Sim tier. A Sim-tier user sees the Runtime Preview panel fully functional — they can start, stop, pause, and resume execution, adjust attenuation, toggle animations, and view data flow debug snapshots.

**Why this priority**: The runtime preview/simulator is the highest-value differentiator and the premium feature. Gating it separately from code generation creates a clear upgrade path: Free -> Pro (code gen) -> Sim (simulation).

**Independent Test**: Set the feature flag to Pro tier. The Runtime Preview panel is visible but shows a locked/upgrade prompt. Set the flag to Sim tier. The panel becomes fully interactive with all execution controls.

**Acceptance Scenarios**:

1. **Given** a user is on Free or Pro tier, **When** they view the Runtime Preview area, **Then** they see a clear indication that simulation requires the Sim tier, and all execution controls are disabled
2. **Given** a user is on the Sim tier, **When** they open a flow graph with a generated module, **Then** the Runtime Preview panel is fully functional with Start/Stop, Pause/Resume, attenuation, and animation controls
3. **Given** a user upgrades from Pro to Sim tier during a session, **When** the tier change takes effect, **Then** the Runtime Preview panel becomes functional without restarting the application

---

### User Story 4 - Gate Entity/Repository Nodes Behind Pro Tier (Priority: P4)

A Free-tier user has access to all standard node types (generators, transformers, filters, sinks at all arities), all custom (user-defined) nodes, and all IP types. The only node types gated behind Pro are entity/repository nodes (NodeTypeDefinition category = Repository) and any future Pro-only categories. This keeps the Free tier highly capable for graph design while reserving database-backed persistence nodes for paying users.

**Why this priority**: Entity/repository node gating is the narrowest tier boundary. It is lower priority because the primary differentiators are code generation (Pro) and simulation (Sim). Most users can build complete flow graphs without entity nodes.

**Independent Test**: Set the feature flag to Free tier. The node palette shows all standard and custom node types but not entity/repository nodes. All IP types are visible. Set the flag to Pro tier. Entity/repository nodes become available.

**Acceptance Scenarios**:

1. **Given** a user is on the Free tier, **When** they open the node palette, **Then** they see all standard built-in node configurations (all arities), all custom nodes, but not entity/repository nodes
2. **Given** a user is on the Free tier, **When** they open the IP type palette, **Then** they see all registered IP types (built-in, custom, architecture, module-level)
3. **Given** a user is on the Pro tier, **When** they open the node palette, **Then** they see all available node types including entity/repository nodes
4. **Given** a user opens a flow graph containing entity/repository nodes while on the Free tier, **When** the graph loads, **Then** all nodes are displayed but entity/repository nodes are visually marked as restricted and cannot be edited or connected

---

### Edge Cases

- What happens when a Pro-tier user saves a graph with entity/repository nodes, then downgrades to Free? The graph opens and displays all nodes, but entity/repository nodes are read-only and marked as restricted. The user can still save the graph (preserving existing configuration) but cannot add new entity/repository nodes or modify restricted ones.
- What happens when a Free-tier user opens a `.flow.kt` file that contains entity/repository nodes? The graph loads fully — all nodes and connections are visible. Entity/repository nodes are marked as restricted. The user can view and understand the architecture but cannot modify restricted elements.
- How does the system determine the current tier at startup? The feature flag service reads the subscription tier from a local configuration. For the initial implementation, this is a locally-set value that defaults to Free. Remote subscription validation is a future concern.
- What happens if a user triggers "Generate Module" while the graph contains validation errors? Code generation is blocked with an appropriate error message, regardless of tier — the same behavior as today's save-with-generation.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The Save action MUST write only the `.flow.kt` file to disk, without triggering any code generation
- **FR-002**: A separate "Generate Module" action MUST be available to trigger full module code generation (runtime controllers, ViewModel, UI stubs, persistence layer)
- **FR-003**: The system MUST support three subscription tiers: Free, Pro, and Sim, where each higher tier includes all capabilities of lower tiers
- **FR-004**: Code generation ("Generate Module") MUST be gated behind the Pro tier — Free-tier users cannot trigger it
- **FR-005**: The Runtime Preview panel and all execution controls (start, stop, pause, resume, attenuation, animation, debug snapshots) MUST be gated behind the Sim tier — Free and Pro-tier users cannot use them
- **FR-006**: Free-tier users MUST have access to unlimited flow graphs with all standard built-in node configurations (all arities), all custom (user-defined) nodes, and all IP types (built-in, custom, architecture, module-level)
- **FR-007**: Pro-tier users MUST additionally have access to entity/repository nodes (NodeTypeDefinition category = Repository) and any future Pro-gated node categories
- **FR-008**: When a tier-restricted feature is accessed by a user without the required tier, the system MUST display a clear, non-disruptive message indicating which tier is required
- **FR-009**: The feature flag service MUST allow the subscription tier to be changed without restarting the application
- **FR-010**: Flow graphs created at any tier MUST be openable at any other tier — tier restrictions apply to editing and generation capabilities, not to viewing
- **FR-011**: The current subscription tier MUST be determined from a local configuration source at startup, defaulting to Free if not set

### Key Entities

- **SubscriptionTier**: Represents the three levels of access — Free, Pro, Sim. Each tier defines which features are enabled. Tiers are cumulative (Sim includes Pro, Pro includes Free).
- **FeatureFlag**: A named capability gate (e.g., "codeGeneration", "runtimePreview", "repositoryNodes") associated with a minimum required tier.
- **TierConfiguration**: The locally-persisted subscription state that the feature flag service reads at startup and can update at runtime.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Saving a flow graph writes only the `.flow.kt` file — zero additional files are created or modified by the Save action
- **SC-002**: Free-tier users can create, edit, save, and re-open flow graphs without encountering any blocked functionality in the core graph editing workflow
- **SC-003**: 100% of code generation entry points are gated — no path exists for a Free-tier user to trigger module generation
- **SC-004**: 100% of runtime preview entry points are gated — no path exists for a Free or Pro-tier user to start execution or view runtime data
- **SC-005**: Tier changes take effect immediately without application restart
- **SC-006**: Flow graphs created at any tier can be opened and viewed at any other tier without errors

## Clarifications

### Session 2026-04-17

- Q: What is the Free tier boundary for nodes and IP types? → A: Free tier includes all IP types (built-in, custom, architecture, module-level) and all custom nodes. Only entity/repository nodes (NodeTypeDefinition category = Repository) and future Pro-only categories are gated behind Pro. This eliminates the previously planned IP type palette filtering (research R5 removed).

## Assumptions

- Remote subscription validation (license server, payment integration) is out of scope for this feature. The tier is set locally.
- The "Generate Module" action will be exposed as a distinct UI element (button, menu item, or toolbar action) separate from Save. The exact UI placement is a design decision for the implementation phase.
- Free tier includes all standard built-in node configurations at any arity, all custom (user-defined) nodes, and all IP types. Only entity/repository nodes (and future Pro-only categories) are gated behind Pro.
- Go code generation mentioned for Pro tier is post-MVP and out of scope for this feature.
- The existing flowGraph-generate module aligns with Pro-tier gating and flowGraph-execute aligns with Sim-tier gating. Where current coupling doesn't match, refactoring will bring them into alignment.
- No changes to the subscription tier model or pricing are in scope — this feature implements only the technical gating mechanism.
