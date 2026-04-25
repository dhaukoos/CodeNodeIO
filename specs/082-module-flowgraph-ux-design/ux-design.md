# UX Design: Module & FlowGraph Relationship

**Feature**: 082-module-flowgraph-ux-design
**Date**: 2026-04-25

---

## 1. Module Properties Dialog

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

**Layout mockup**:
```
┌──────────────────────────────────────────────────┐
│  [New] [Open] [Save]  │  [Undo] [Redo]  │ ...   │  ← Toolbar
├──────────────────────────────────────────────────┤
│  📁 StopWatch — /Users/dev/projects/StopWatch    │  ← Context Bar
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

**Layout mockup**:
```
┌────────────────────────────────────────────────────────────┐
│ [New▼] [Open] [Save] │ Module: [StopWatch ▼] │ [Undo] ... │
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

### 2.3 Variation C — Module as Workspace (Recommended)

**Concept**: The module IS the workspace. The application title bar shows the current workspace. New/Open/Save are scoped to the workspace module. Switching modules is an explicit workspace action.

**Layout mockup**:
```
╔══════════════════════════════════════════════════╗
║  CodeNodeIO — StopWatch                          ║  ← Window title
╠══════════════════════════════════════════════════╣
│  [New] [Open] [Save]  │  [Undo] [Redo]  │ ...   │  ← Toolbar
├──────────────────────────────────────────────────┤
│                                                  │
│               (Graph Canvas)                     │
```

**User flows**:

- **Startup**: Application opens with no workspace. Module Properties dialog appears automatically, prompting the user to create a new module or open an existing one (via a "Browse..." button that looks for directories containing `build.gradle.kts`).
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

**Recommendation**: **Variation C — Module as Workspace**

**Reasoning**:
1. **Zero screen space cost** — title bar is already there, just underutilized
2. **Simplest New dialog** — just a name, no module selection (it's the workspace)
3. **Deterministic Save** — never asks where to save, ever
4. **IDE-aligned pattern** — developers already understand "project = workspace" from IntelliJ, VS Code, Xcode
5. **Scoped Open is a feature** — seeing only the current module's flowGraphs reduces cognitive load
6. **Startup module setup** is a one-time cost that establishes the foundation for all subsequent work — and Module Properties makes it a 10-second operation (name + platforms + directory)
