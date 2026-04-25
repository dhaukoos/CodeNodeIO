# UX Design: Module & FlowGraph Relationship

**Feature**: 082-module-flowgraph-ux-design
**Date**: 2026-04-25

---

## 1. Module Properties Dialog

### 1.0 Current State & Integration with Toolbar

Currently, the toolbar's left side shows a gear icon + the flow graph name label (e.g., "⚙ StopWatch"). Clicking it opens the "FlowGraph Properties" dialog. This feature renames it to "Module Properties" and redefines its role.

**How Module Properties integrates with each UI variation**:

- **Variation A (Context Bar)**: The gear icon + label moves into the context bar. Clicking the module name in the context bar opens Module Properties. The toolbar no longer hosts this control.
- **Variation B (Toolbar Dropdown)**: The gear icon is replaced by the module dropdown selector. Module Properties is accessible via a "Module Settings..." item at the bottom of the dropdown.
- **Variation C (Workspace)**: The gear icon + label remain in the toolbar but display the module name. Clicking it opens Module Properties. The title bar shows the flowGraph name.
- **Variation D (Workspace via Dropdown — Recommended)**: The gear icon + label are replaced by a module dropdown (`[StopWatch ▼]`). The dropdown includes "Module Settings..." to open Module Properties in edit mode, "Create New Module..." for create mode, and an MRU list for quick module switching. The title bar shows the active flowGraph name ("CodeNodeIO — MainFlow"). At startup with no module, the dropdown shows "[No Module ▼]" with create/open options.

In all variations, Module Properties serves dual purposes:
1. **Create mode** (no module loaded): Entry point for creating a new module — the first step before any work can begin
2. **Edit mode** (module loaded): View/edit module settings (platforms), view module path

### 1.1 Dialog Specification

**Title**: "Module Properties" (renamed from "FlowGraph Properties")

**Fields**:

| Field | Type | Initial Value | Validation | Notes |
|-------|------|---------------|------------|-------|
| Module Name | Text input | Empty (no default) | Minimum 3 characters, no spaces, alphanumeric + hyphens | Live validation — error indicator appears when < 3 chars |
| Target Platforms | Checkbox group | None selected | At least 1 must be selected | Options: Android, iOS, Desktop, Web, WASM |
| Module Path | Read-only label | Hidden until module created | N/A | Shows absolute path after creation. Clicking opens file explorer to that location. |

**Buttons**:

| Button | Visibility | Enabled When | Action |
|--------|-----------|--------------|--------|
| Create Module | No module loaded | Name ≥ 3 chars AND ≥ 1 platform selected | Opens directory chooser → creates module scaffolding via ModuleScaffoldingGenerator |
| Close | Always | Always | Dismisses dialog, applies any platform changes to existing module |

**States**:

- **Create mode** (no module loaded): Name field empty, platforms unchecked, "Create Module" button visible (disabled until validation passes). Module Path hidden.
- **Edit mode** (module loaded): Name pre-filled (read-only — module name cannot change after creation), platforms pre-filled and editable, "Create Module" replaced by Module Path display. Platform changes apply on Close.

### 1.2 Dialog Layout Mockup

```
┌─────────────────────────────────────┐
│  Module Properties                  │
├─────────────────────────────────────┤
│                                     │
│  Module Name                        │
│  ┌─────────────────────────────┐    │
│  │                             │    │
│  └─────────────────────────────┘    │
│  ⚠ Name must be at least 3 chars   │
│                                     │
│  Target Platforms                   │
│  ☐ Android                         │
│  ☐ iOS                             │
│  ☐ Desktop                         │
│  ☐ Web                             │
│  ☐ WASM                            │
│  ⚠ Select at least 1 platform      │
│                                     │
│  ┌─────────────┐  ┌────────┐       │
│  │Create Module│  │ Close  │       │
│  └─────────────┘  └────────┘       │
│                                     │
└─────────────────────────────────────┘
```

**Edit mode** (module loaded):

```
┌─────────────────────────────────────┐
│  Module Properties                  │
├─────────────────────────────────────┤
│                                     │
│  Module Name                        │
│  StopWatch                (locked)  │
│                                     │
│  Target Platforms                   │
│  ☑ Android                         │
│  ☑ iOS                             │
│  ☑ Desktop                         │
│  ☐ Web                             │
│  ☐ WASM                            │
│                                     │
│  Module Path                        │
│  /Users/dev/projects/StopWatch      │
│                                     │
│              ┌────────┐             │
│              │ Close  │             │
│              └────────┘             │
│                                     │
└─────────────────────────────────────┘
```

---

## 2. New/Open/Save UI Variations

### 2.1 Variation A — Module Context Bar

**Concept**: A persistent horizontal bar below the toolbar displays the current module context. New/Open/Save operate on flowGraphs within that module.

**Module Properties access**: Clicking the module name in the context bar opens Module Properties.

**Layout mockup**:
```
┌──────────────────────────────────────────────────┐
│  [New] [Open] [Save]  │  [Undo] [Redo]  │ ...   │  ← Toolbar
├──────────────────────────────────────────────────┤
│  📁 StopWatch — /Users/dev/projects/StopWatch    │  ← Context Bar (clickable → Module Properties)
├──────────────────────────────────────────────────┤
│                                                  │
│               (Graph Canvas)                     │
```

**User flows**:

- **New FlowGraph**: Click "New" → dialog appears with "FlowGraph Name" text field → enter name (e.g., "MainFlow") → blank canvas. The module context remains unchanged. The new flowGraph will be saved to `{module}/flow/{name}.flow.kt`.
- **Open FlowGraph**: Click "Open" → file chooser opens, filtered to `.flow.kt` files → select a file → graph loads. If the file is in a different module, the context bar updates to show the new module.
- **Save FlowGraph**: Click "Save" → writes to `{module}/flow/{flowGraphName}.flow.kt`. No directory prompt (deterministic path). If no module is loaded, shows "No module loaded — create or open a module first" in the status bar.

| Pros | Cons |
|------|------|
| Module context always visible — no ambiguity | Takes vertical screen space (~24dp) |
| Natural multi-flowGraph workflow within one module | Switching modules requires Open from different location |
| Save is fully deterministic — no directory prompts | New bar is an additional UI element to learn |
| Module path is clickable for file explorer access | First-time users see an empty context bar — may confuse |

---

### 2.2 Variation B — Module Dropdown in Toolbar

**Concept**: The toolbar includes a dropdown showing the current module name. Module selection is inline with New/Open/Save.

**Module Properties access**: "Module Settings..." option at the bottom of the module dropdown.

**Layout mockup**:
```
┌────────────────────────────────────────────────────────────┐
│ [New▼] [Open] [Save] │ Module: [StopWatch ▼] │ [Undo] ... │  ← dropdown includes "Module Settings..."
├────────────────────────────────────────────────────────────┤
│                                                            │
│                     (Graph Canvas)                          │
```

**User flows**:

- **New FlowGraph**: Click "New" → dropdown with two fields: "FlowGraph Name" + "Module" selector (lists known modules + "Create New Module..." option). Select module, enter name, click Create.
- **Open FlowGraph**: Click "Open" → standard file chooser for `.flow.kt` files → module inferred from directory. Module dropdown updates.
- **Save FlowGraph**: Click "Save" → writes to the module shown in the dropdown. If no module selected, Save is disabled.
- **Switch Module**: Click module dropdown → select from recently-used list or browse.

| Pros | Cons |
|------|------|
| No new UI element — reuses toolbar space | Toolbar becomes crowded |
| Easy module switching via dropdown | New dialog has 2 fields — slightly more complex |
| Module association explicit at flowGraph creation | Requires maintaining "known modules" list (MRU) |
| Compact — no vertical space consumed | Module context less prominent than Context Bar |

---

### 2.3 Variation C — Module as Workspace

**Concept**: The module IS the workspace. The title bar shows the active flowGraph name. New/Open/Save are scoped to the workspace module. Switching modules is an explicit workspace action.

**Module Properties access**: Gear icon + module name in toolbar (e.g., "⚙ StopWatch"). Clicking opens Module Properties. At startup with no module, shows "⚙ No Module" and clicking opens create mode.

**Layout mockup**:
```
╔══════════════════════════════════════════════════╗
║  CodeNodeIO — MainFlow                           ║  ← Window title (active flowGraph name)
╠══════════════════════════════════════════════════╣
│ ⚙ StopWatch │ [New] [Open] [Save] │ [Undo] ... │  ← Toolbar (⚙ clickable → Module Properties)
├──────────────────────────────────────────────────┤
│                                                  │
│               (Graph Canvas)                     │
```

**User flows**:

- **Startup**: Application opens with no workspace. Toolbar shows "⚙ No Module". Module Properties dialog appears automatically on first launch, prompting the user to create a new module or open an existing one (via a "Browse..." button that looks for directories containing `build.gradle.kts`). Subsequent launches remember the last workspace.
- **New FlowGraph**: Click "New" → simple dialog with only "FlowGraph Name" field (module is implicit — it's the workspace). Enter name → blank canvas. Saved to `{workspace}/flow/{name}.flow.kt`.
- **Open FlowGraph**: Click "Open" → file chooser opens **scoped to the current workspace module's `flow/` directory** (not the entire filesystem). Shows only `.flow.kt` files in that directory. An "Open from..." secondary option allows browsing any location (and switches workspace if the file is in a different module).
- **Save FlowGraph**: Click "Save" → writes to `{workspace}/flow/{flowGraphName}.flow.kt`. Completely deterministic — no directory prompt ever needed after initial module setup.
- **Switch Workspace**: Via Module Properties → "Open Module..." or by opening a `.flow.kt` from a different module location.

| Pros | Cons |
|------|------|
| Cleanest mental model — module = workspace | Working across modules requires explicit switching |
| Save is 100% deterministic — zero prompts | Startup requires module setup (no instant blank canvas) |
| Open is focused — shows only relevant flowGraphs | Scoped Open may feel restrictive initially |
| Title bar provides context without consuming layout | "Open from..." is a secondary action |
| Matches IDE patterns (project = workspace) | Cannot view two modules side by side |
| New dialog is simplest — just one field | |

---

### 2.4 Comparison and Recommendation

| Criterion | A — Context Bar | B — Toolbar Dropdown | C — Workspace |
|-----------|----------------|---------------------|---------------|
| Context visibility | High (persistent bar) | Medium (dropdown) | High (title bar) |
| Screen space cost | 24dp vertical bar | Toolbar crowding | Zero (title bar) |
| Save determinism | Yes (within module) | Yes (dropdown module) | Yes (workspace) |
| New dialog complexity | 1 field (name) | 2 fields (name + module) | 1 field (name) |
| Module switching | Open from elsewhere | Dropdown selection | Explicit action |
| Learning curve | Low — bar is obvious | Low — dropdown familiar | Medium — workspace concept |
| Multi-module workflow | Fair | Good (quick dropdown) | Fair (explicit switch) |

### 2.4 Variation D — Module as Workspace via Dropdown (Recommended)

**Concept**: Combines B and C — the module IS the workspace (deterministic Save, scoped Open, 1-field New), the title bar shows the active flowGraph name, and the toolbar gear icon + label becomes a module dropdown for quick workspace switching. The dropdown lists recently used modules plus "Create New Module..." and "Open Module..." options. No additional screen space — the dropdown replaces the existing gear icon + label in the same toolbar position.

**Module Properties access**: "Module Settings..." option at the bottom of the dropdown. Also accessible by selecting the current module name (re-selecting it opens settings).

**Layout mockup**:
```
╔══════════════════════════════════════════════════════╗
║  CodeNodeIO — MainFlow                               ║  ← Window title (active flowGraph name)
╠══════════════════════════════════════════════════════╣
│ [StopWatch ▼] │ [New] [Open] [Save] │ [Undo] [Redo] │  ← Toolbar (dropdown = workspace selector)
├──────────────────────────────────────────────────────┤
│                                                      │
│                   (Graph Canvas)                      │
```

**Dropdown contents**:
```
┌──────────────────────────┐
│ ✓ StopWatch              │  ← current workspace (checkmark)
│   WeatherForecast        │  ← recently used
│   UserProfiles           │  ← recently used
│ ─────────────────────── │
│   Open Module...         │  ← browse for existing module
│   Create New Module...   │  ← opens Module Properties in create mode
│ ─────────────────────── │
│   Module Settings...     │  ← opens Module Properties in edit mode
└──────────────────────────┘
```

**User flows**:

- **Startup**: Application opens with last workspace remembered. If no previous workspace, toolbar shows "[No Module ▼]" and the dropdown offers "Create New Module..." and "Open Module...". Module Properties appears automatically on first launch.
- **New FlowGraph**: Click "New" → simple dialog with only "FlowGraph Name" field (module is implicit — it's the workspace). Enter name → blank canvas. Saved to `{workspace}/flow/{name}.flow.kt`.
- **Open FlowGraph**: Click "Open" → file chooser scoped to current workspace module's `flow/` directory. Shows only `.flow.kt` files. An "Open from..." secondary option allows browsing any location (and switches workspace if the file is in a different module).
- **Save FlowGraph**: Click "Save" → writes to `{workspace}/flow/{flowGraphName}.flow.kt`. Completely deterministic — no directory prompt.
- **Switch Workspace**: Click the module dropdown → select a different module from the MRU list. Prompts to save unsaved changes if needed. Or use "Open Module..." to browse.

| Pros | Cons |
|------|------|
| Zero additional screen space — replaces existing gear icon | Requires maintaining MRU module list |
| Quick module switching via dropdown (best of B) | Dropdown is slightly more complex than static label |
| Deterministic Save — no prompts (best of C) | |
| 1-field New dialog — simplest (best of C) | |
| Scoped Open — focused on current module (best of C) | |
| IDE-aligned workspace pattern (best of C) | |
| Title bar provides passive context (best of C) | |
| Startup remembers last workspace — no repeated setup | |

---

### 2.5 Comparison and Recommendation

| Criterion | A — Context Bar | B — Toolbar Dropdown | C — Workspace | D — Workspace via Dropdown |
|-----------|----------------|---------------------|---------------|---------------------------|
| Context visibility | High (persistent bar) | Medium (dropdown) | High (title bar) | High (title bar + dropdown) |
| Screen space cost | 24dp vertical bar | Same as current | Same as current | Same as current |
| Save determinism | Yes | Yes | Yes | Yes |
| New dialog complexity | 1 field | 2 fields | 1 field | 1 field |
| Module switching | Open from elsewhere | Dropdown (quick) | Explicit action | Dropdown (quick) |
| Learning curve | Low | Low | Medium | Low |
| Multi-module workflow | Fair | Good | Fair | Good |

**Recommendation**: **Variation D — Module as Workspace via Dropdown**

**Reasoning**:
1. **Best of B and C combined** — workspace semantics (deterministic Save, scoped Open, 1-field New) with quick module switching via dropdown
2. **Zero screen space cost** — the dropdown replaces the existing gear icon + label in the same toolbar position
3. **Quick module switching** — dropdown MRU list eliminates the friction of C's explicit switching
4. **Simplest New dialog** — just a name, module is implicit
5. **Deterministic Save** — never asks where to save
6. **IDE-aligned pattern** — developers understand workspace + project switcher (IntelliJ's recent projects, VS Code's workspace selector)
7. **Startup remembers last workspace** — returning users go straight to work

---

## 3. Module/FlowGraph Relationship Model

### 3.1 Entity Definitions

**Module**

A Module is a KMP project directory containing:
- **Scaffolding**: `build.gradle.kts`, `settings.gradle.kts`
- **Source directories**: `src/commonMain/kotlin/{package}/` with subdirectories:
  - `flow/` — FlowGraph definitions (.flow.kt files) and runtime Flow class
  - `controller/` — Controller, ControllerInterface, ControllerAdapter
  - `viewmodel/` — ViewModel, State object
  - `userInterface/` — Compose UI composables
  - `nodes/` — CodeNode definitions
  - `iptypes/` — Module-level IP type definitions
- **Identity**: Module name (derived from directory name, immutable after creation)
- **Configuration**: Target platforms (Android, iOS, Desktop, Web, WASM)
- **Created by**: Module Properties dialog → "Create Module" button

**FlowGraph**

A FlowGraph is a `.flow.kt` file within a module's `flow/` directory containing:
- **Graph definition**: Nodes (CodeNodes, GraphNodes), connections, port types
- **Identity**: FlowGraph name (derived from filename, e.g., `MainFlow.flow.kt` → "MainFlow")
- **Content**: DSL code defining the node graph using `flowGraph("Name") { ... }` syntax
- **Created by**: "New" toolbar button
- **Persisted by**: "Save" toolbar button

### 3.2 Relationship Model

```
┌─────────────┐        contains        ┌─────────────┐
│   Module    │ ◄──────────────────── │  FlowGraph  │
│             │     0..N               │             │
│  name       │                        │  name       │
│  platforms  │                        │  nodes      │
│  rootDir    │                        │  connections │
└─────────────┘                        └─────────────┘
       │
       │ contains
       ▼
  flow/, controller/,
  viewmodel/, nodes/,
  userInterface/, iptypes/
```

**Cardinality**: One Module contains zero or more FlowGraphs. Each FlowGraph belongs to exactly one Module.

**Constraints**:
- A FlowGraph cannot exist (be saved) without a Module context
- FlowGraph names must be unique within a Module
- A Module can exist with zero FlowGraphs (empty module — valid state)
- The Module name is immutable after creation (directory name is the source of truth)

### 3.3 Operations by Level

| Level | Operation | Description | Trigger |
|-------|-----------|-------------|---------|
| **Module** | Create | Creates directory structure + Gradle files via ModuleScaffoldingGenerator | Module Properties → "Create Module" |
| **Module** | Configure | Edit target platforms | Module Properties (edit mode) |
| **Module** | Open/Switch | Set as current workspace | Toolbar dropdown → select module, or "Open Module...", or open a .flow.kt from a different module |
| **Module** | Delete | Remove module directory (manual — not a tool operation) | File system |
| **FlowGraph** | New | Create blank canvas with a name, within the current workspace module | Toolbar → "New" |
| **FlowGraph** | Open | Load a .flow.kt from the current module's flow/ directory | Toolbar → "Open" |
| **FlowGraph** | Save | Write the current graph to `{module}/flow/{name}.flow.kt` | Toolbar → "Save" |
| **FlowGraph** | Generate | Produce runtime files (controller, viewmodel, etc.) from the graph | Code Generator panel → "Generate" |

### 3.4 Edge Case Behavior

| Scenario | Behavior |
|----------|----------|
| **No module loaded** | Toolbar dropdown shows "[No Module ▼]". Title bar shows "CodeNodeIO". New and Save are disabled. Open is available but only via "Open from..." (full file browser) which also sets the workspace. Dropdown offers "Create New Module..." and "Open Module...". |
| **Empty module (0 flowGraphs)** | Valid state. Canvas is blank. New is enabled. Open shows empty list. Save is disabled (nothing to save). Generate is disabled (no graph). |
| **Orphan .flow.kt (outside any module)** | Can be opened via "Open from..." — loads the graph but with no module context. Save and Generate are disabled. Status bar shows "No module context — create or open a module to save." |
| **Duplicate flowGraph name** | New dialog validates the name against existing .flow.kt files in the module's flow/ directory. If duplicate, shows error "A flowGraph named '{name}' already exists in this module." |
| **Switching modules with unsaved changes** | Prompt: "You have unsaved changes to {flowGraphName}. Save before switching?" with Save / Don't Save / Cancel options. |
| **Module directory deleted externally** | On next Save or Open attempt, shows error "Module directory not found: {path}. The module may have been moved or deleted." Module Properties opens for resolution. |

---

## 4. Migration Notes

### 4.1 What Changes from Current Behavior

| Current | New |
|---------|-----|
| "FlowGraph Properties" dialog | "Module Properties" dialog |
| Default graph name "New Graph" | Empty name field, must enter ≥ 3 chars |
| Implied 1:1 flowGraph/module | Explicit many:1 — module contains N flowGraphs |
| Save prompts for directory on first save | Save is deterministic — always writes to workspace module's flow/ |
| Open browses entire filesystem | Open scoped to current module's flow/ directory (with "Open from..." fallback) |
| No module creation step | Module must be created first via toolbar dropdown → "Create New Module..." or Module Properties |
| Title bar shows "CodeNodeIO" | Title bar shows "CodeNodeIO — {FlowGraphName}" (module context in toolbar dropdown) |

### 4.2 Implementation Feature Scope (Subsequent Feature)

The next feature should implement:

1. **Rename "FlowGraph Properties" to "Module Properties"** — update dialog title, menu item, keyboard shortcut tooltip
2. **Redesign dialog fields** — empty name, platform checkboxes, Create Module button with validation, edit mode
3. **Replace gear icon + label with module dropdown** — toolbar dropdown showing current module name with MRU list, "Create New Module...", "Open Module...", "Module Settings..."
4. **Implement workspace model** — track current module as workspace state; title bar shows flowGraph name ("CodeNodeIO — {FlowGraphName}"), toolbar dropdown shows module name
5. **Redesign "New"** — simple name-only dialog, creates .flow.kt in workspace module
6. **Redesign "Open"** — scoped to workspace flow/ directory with "Open from..." fallback
7. **Redesign "Save"** — deterministic write to workspace flow/ directory, disabled without module
8. **Add unsaved-changes prompt** when switching modules via dropdown
9. **Update status bar** for no-module-context scenarios
10. **Persist last workspace** — remember and restore workspace module on next launch
11. **Backward compatibility** — existing .flow.kt files in old locations should still be openable via "Open from..."

### 4.3 Backward Compatibility Considerations

- Existing modules with a single .flow.kt in the base package (pre-feature-077 layout) should still be openable — the system infers the module from `build.gradle.kts` presence
- The `saveFlowKtOnly()` method in ModuleSaveService should be updated to write to the workspace module's `flow/` directory instead of the base package
- The save location registry (mapping flowGraph name → directory) should be replaced by the workspace model (module root directory is the single source of truth)
