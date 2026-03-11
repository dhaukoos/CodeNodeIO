# Implementation Plan: Remove Repository Module

**Branch**: `048-remove-entity-module` | **Date**: 2026-03-11 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/048-remove-entity-module/spec.md`

## Summary

Add a "Remove Repository Module" button to the IP Type Properties Panel that replaces the disabled "Module exists" label when a module already exists. Clicking it shows a confirmation dialog, then removes all generated artifacts: 3 custom node definitions, the module directory, 3 persistence files, regenerates AppDatabase.kt, and removes Gradle entries.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP - Kotlin Multiplatform)
**Primary Dependencies**: Compose Desktop 1.7.3, kotlinx-coroutines 1.8.0, kotlinx-serialization 1.6.0, Room 2.8.4 (KMP), Koin 4.0.0
**Storage**: Room (KMP) with BundledSQLiteDriver — persistence module; FileCustomNodeRepository (JSON at `~/.codenode/custom-nodes.json`)
**Testing**: Manual verification (UI-driven feature, file system operations)
**Target Platform**: JVM Desktop (Compose Desktop)
**Project Type**: Multi-module KMP project
**Performance Goals**: Removal operation completes in < 2 seconds (file I/O bound)
**Constraints**: Must handle partial cleanup gracefully (missing artifacts should not cause failure)
**Scale/Scope**: 7 artifact categories to clean up per removal operation

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] **Licensing**: No new dependencies added. All existing dependencies (Compose, Room, Koin) are Apache 2.0 / MIT.
- [x] **Code Quality**: Single responsibility — removal logic in dedicated service class. Functions under 50 lines.
- [x] **TDD**: Removal is file-system and UI driven; manual testing with verify-after-remove workflow.
- [x] **UX Consistency**: Confirmation dialog follows Compose Material patterns. Status feedback via existing status bar.
- [x] **Performance**: File deletion and text file editing are sub-second operations.
- [x] **Observability**: Status message reports what was removed. Graceful error handling for missing artifacts.

No gate violations.

## Project Structure

### Documentation (this feature)

```text
specs/048-remove-entity-module/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
└── checklists/
    └── requirements.md  # Spec quality checklist
```

### Source Code (repository root)

```text
graphEditor/src/jvmMain/kotlin/
├── Main.kt                              # Add onRemoveRepositoryModule lambda, confirmation dialog
├── save/
│   └── ModuleSaveService.kt             # Add removeEntityModule() method (reverse of saveEntityModule)
└── ui/
    └── PropertiesPanel.kt               # Replace disabled "Module exists" with "Remove Repository Module" button
```

**Structure Decision**: No new files needed. The removal logic is the inverse of `saveEntityModule()` in `ModuleSaveService` and belongs in the same class. The UI changes are additions to existing `PropertiesPanel.kt` and `Main.kt`.

## Complexity Tracking

No constitution violations. No complexity justification needed.
