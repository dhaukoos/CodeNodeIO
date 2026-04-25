# Research: Module & FlowGraph UX Design

**Feature**: 082-module-flowgraph-ux-design
**Date**: 2026-04-25

## R1: Module/FlowGraph Relationship Model

**Decision**: Establish a clear many-to-one model:

- **Module** = a KMP directory with scaffolding (build.gradle.kts, settings.gradle.kts, source directories including flow/, controller/, viewmodel/, userInterface/, nodes/, iptypes/). Created explicitly via Module Properties. Has a name, target platforms, and a root directory.
- **FlowGraph** = a `.flow.kt` file inside a module's `flow/` directory. Has a name (derived from filename), nodes, connections, and port types. Multiple flowGraphs can coexist in one module.
- **Relationship**: Module contains 0..N flowGraphs. A flowGraph belongs to exactly 1 module. A flowGraph cannot be saved without a module context.

**Operations by level**:
- Module: Create (Module Properties), Configure (name, platforms), Delete
- FlowGraph: New (blank canvas + name), Open (browse .flow.kt), Save (write to module's flow/), Generate (Code Generator panel)

## R2: UI Variation A — Module Context Bar

**Description**: A persistent "module context bar" below the toolbar shows the current module name and path. New/Open/Save operate on flowGraphs within that module context.

**User flows**:
- **New**: Prompts for flowGraph name (e.g., "MainFlow"). Creates blank canvas. Saves to `{module}/flow/{name}.flow.kt` when Save is clicked.
- **Open**: Shows file chooser filtered to `.flow.kt` files. Infers module from the file's parent directory. Updates the module context bar.
- **Save**: Writes the current flowGraph to `{module}/flow/{flowGraphName}.flow.kt`. If no module is loaded, prompts user to create one first.
- **Module context**: Displayed as "{ModuleName} — {path}" in the context bar. Clicking the module name opens Module Properties.

**Pros**:
- Module context is always visible — user always knows where they are
- New/Open/Save are simple and predictable — they always operate within the visible module
- Natural workflow: set module context once, then create/edit multiple flowGraphs within it

**Cons**:
- Requires a new UI element (context bar) taking vertical space
- Switching modules requires explicit action (Open from a different module, or Module Properties → Create/Switch)
- First-time users must create a module before doing anything — potentially confusing onboarding

## R3: UI Variation B — Module Dropdown in Toolbar

**Description**: The toolbar includes a module dropdown selector next to the graph name. New/Open/Save include the module selection inline.

**User flows**:
- **New**: A dialog with two fields: FlowGraph name + Module selector (dropdown of known modules or "Create New Module..."). Creates blank canvas within the selected module.
- **Open**: Standard file chooser for `.flow.kt` files. Module is inferred from the file path.
- **Save**: Writes to the current module's flow/ directory. If no module is set, the Save dialog includes a module selector.
- **Module selector**: Dropdown in toolbar showing current module name. Lists recently used modules. "Create New Module..." option opens Module Properties.

**Pros**:
- Module selection is inline with existing toolbar — no new UI element
- Easy switching between modules via dropdown
- New flowGraph dialog makes module association explicit at creation time

**Cons**:
- Toolbar gets crowded with the module dropdown
- Module management mixed with flowGraph operations — less clear separation of concerns
- Dropdown requires maintaining a "known modules" list (MRU or project scanning)

## R4: UI Variation C — Module as Workspace (Recommended)

**Description**: The module IS the workspace. Opening a module (via Module Properties or Open) sets the workspace context. The graph editor title bar shows "CodeNodeIO — {ModuleName}". New/Open/Save are scoped to the workspace. A "Switch Module" action changes the workspace.

**User flows**:
- **Startup**: Graph editor opens with no workspace. Module Properties prompts to create or open a module. Once a module is set, the workspace is active.
- **New**: Prompts only for flowGraph name. The module is implicitly the current workspace. Creates `{module}/flow/{name}.flow.kt`.
- **Open**: Shows only `.flow.kt` files within the current module's `flow/` directory (not a system-wide file chooser). Optionally, a secondary "Open from..." opens any `.flow.kt` and switches workspace.
- **Save**: Writes to the current workspace module's `flow/` directory. No directory prompt needed — the destination is deterministic.
- **Switch Module**: Explicit action (menu or Module Properties) to change the workspace to a different module.

**Pros**:
- Cleanest conceptual model — module = workspace, flowGraph = document within workspace
- Save never needs a directory prompt after initial module setup
- Open is scoped to the module — less browsing, more focused
- Title bar provides persistent, unobtrusive module context
- Matches IDE patterns (project = workspace, files = documents)

**Cons**:
- Working across multiple modules requires explicit switching
- Startup requires module setup — no "just start drawing" instant experience
- Scoped Open may feel restrictive — users may want to browse freely

**Recommendation**: Variation C (Module as Workspace) provides the clearest mental model and the most streamlined day-to-day workflow. The title bar shows context without consuming layout space. Save becomes deterministic. The trade-off of requiring module setup at startup is acceptable because module creation is a lightweight operation (name + platforms + directory), and it establishes the foundation for all subsequent work.

## R5: Module Properties Dialog Design

**Decision**: Redesigned dialog with the following layout:

**Fields**:
- **Module Name**: Text field, initially empty, minimum 3 characters. Validated live with error indicator.
- **Target Platforms**: Checkbox list — Android, iOS, Desktop, Web, WASM. At least 1 must be selected.
- **Module Path**: Read-only display of the module's root directory (shown after creation or when editing existing module).

**Buttons**:
- **Create Module** (when no module loaded): Enabled when name ≥ 3 chars AND ≥ 1 platform selected. Opens directory chooser, then creates scaffolding.
- **Close** / **Cancel**: Dismisses dialog.

**When editing existing module**: Name and platforms are pre-filled. "Create Module" is replaced by the module path display. Platforms can be modified (future: update build.gradle.kts).
