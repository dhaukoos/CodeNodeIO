# Research: GraphEditor Save Functionality

**Feature**: 029-grapheditor-save
**Date**: 2026-02-25

## R1: Merge Strategy for Save + Compile

**Decision**: Create a new unified `saveModule()` that performs both module creation and code generation in a single call.

**Rationale**: The current architecture has `saveModule()` (structure + .flow.kt) and `compileModule()` (runtime files + stubs) as independent methods. The user wants a single Save action. Rather than having Save call both methods sequentially, merge the logic into one method to avoid redundant directory creation and package computation.

**Alternatives Considered**:
- **Option A: Save calls saveModule() then compileModule()** — Simple but wasteful (directory structure created twice, packages computed twice). Also requires both methods to stay in sync.
- **Option B: Unified saveModule() that does everything** (CHOSEN) — Single method, single transaction-like operation. Cleaner error handling. ModuleSaveResult captures all outcomes.
- **Option C: Keep separate methods, add a new saveAndCompile()** — Adds a third method that's just a wrapper. Increases surface area without benefit.

## R2: Save Location Registry Design

**Decision**: Use a `MutableMap<String, File>` in Main.kt's composable state, keyed by FlowGraph name.

**Rationale**: The current `lastSaveOutputDir: File?` tracks only the most recent save directory. The spec requires tracking per FlowGraph name so that renaming triggers a new directory prompt. A map keyed by name is the simplest structure.

**Alternatives Considered**:
- **Option A: Single `lastSaveOutputDir: File?`** (current) — Cannot distinguish between FlowGraph names. Fails US4 (name change detection).
- **Option B: `Map<String, File>` in composable state** (CHOSEN) — Session-scoped, cleared on app restart. Simple, no persistence needed. Keyed by FlowGraph name.
- **Option C: Persistent storage (preferences/config file)** — Over-engineering for current needs. Spec explicitly says session-scoped is sufficient.

## R3: Orphan Deletion Strategy

**Decision**: Change existing `detectOrphanedComponents()` and `detectOrphanedStateProperties()` from warn-only to delete-and-report.

**Rationale**: The current methods already identify orphaned files by scanning the directory and comparing to expected files from the FlowGraph. The change is minimal: instead of adding a warning string, delete the file and add a deletion report string.

**Implementation Approach**:
- Rename to `deleteOrphanedComponents()` and `deleteOrphanedStateProperties()`
- Return `List<String>` of deleted file paths instead of warning messages
- Add `filesDeleted: List<String>` to `ModuleSaveResult`
- The file scanning logic stays the same

**Risk**: Deleting user-edited files is irreversible. Mitigation: the save result reports all deletions, and the behavior matches the spec requirement. Users who want to preserve orphaned code should copy it before removing nodes.

## R4: ModuleSaveResult Enhancement

**Decision**: Add `filesDeleted` and `filesOverwritten` fields to `ModuleSaveResult`.

**Rationale**: The spec requires the save result to report which files were created, overwritten, preserved, and deleted (FR-011). The current result has `filesCreated` and `warnings`. Need to add fields for deletions and overwrites.

```
ModuleSaveResult(
    success: Boolean,
    moduleDir: File?,
    errorMessage: String?,
    filesCreated: List<String>,      // New stub files
    filesOverwritten: List<String>,  // .flow.kt + 5 runtime files
    filesDeleted: List<String>,      // Orphaned stubs removed
    warnings: List<String>           // Any non-fatal issues
)
```

## R5: Compile Button Disposition

**Decision**: Remove the separate Compile button from the UI since Save now includes compilation.

**Rationale**: With the unified Save operation, a separate Compile button is redundant. Keeping it would confuse users about which action generates files. The spec's assumption section states "The current separate Compile button becomes redundant."

**Alternative**: Keep Compile as an alias for Save (both do the same thing). Rejected because it adds UI clutter with no benefit.

## R6: Directory Existence Check

**Decision**: Before re-saving, verify the remembered directory still exists. If not, re-prompt.

**Rationale**: FR-010 requires this. Users may delete or move directories between saves. The check is a simple `File.exists()` call on the remembered path before proceeding.

**Implementation**: In the Save handler in Main.kt, after looking up the registry:
```
val savedDir = saveLocationRegistry[flowGraphName]
if (savedDir != null && savedDir.exists()) {
    // Re-save directly
} else {
    // Prompt for directory (first save or directory gone)
}
```
