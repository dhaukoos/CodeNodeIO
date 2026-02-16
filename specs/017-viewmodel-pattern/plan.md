# Implementation Plan: ViewModel Pattern Migration

**Branch**: `017-viewmodel-pattern` | **Date**: 2026-02-16 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/017-viewmodel-pattern/spec.md`

## Summary

Migrate the graphEditor module from its current hybrid state management (centralized GraphState + scattered local mutableStateOf) to a consistent ViewModel pattern. Each major UI component will have a dedicated ViewModel that encapsulates its state and business logic, enabling better testability and modularity while preserving all existing functionality.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (Kotlin Multiplatform)
**Primary Dependencies**: Compose Desktop 1.7.3, kotlinx-coroutines 1.8.0, lifecycle-viewmodel-compose 2.10.0-alpha06
**Storage**: FileCustomNodeRepository (JSON persistence for custom nodes)
**Testing**: kotlin.test with JUnit runner
**Target Platform**: JVM Desktop (Compose Desktop)
**Project Type**: Single module within monorepo (graphEditor)
**Performance Goals**: Maintain current editor responsiveness (60fps interactions, <100ms for state updates)
**Constraints**: Must preserve all existing functionality; incremental migration; no breaking changes to public APIs
**Scale/Scope**: ~20 source files in graphEditor module; 5-7 ViewModels to extract

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality First | ✅ PASS | ViewModels improve separation of concerns and maintainability |
| II. Test-Driven Development | ✅ PASS | ViewModels enable unit testing without UI dependencies |
| III. User Experience Consistency | ✅ PASS | Refactoring is internal; no UX changes |
| IV. Performance Requirements | ✅ PASS | StateFlow pattern maintains reactivity performance |
| V. Observability & Debugging | ✅ PASS | Centralized ViewModels improve debugging |
| Licensing & IP | ✅ PASS | No new dependencies required |

**Quality Gates:**
- Automated Tests: ✅ Will write ViewModel unit tests
- Code Review: ✅ Standard process
- Performance Check: ✅ No performance-impacting changes
- Linting & Formatting: ✅ Standard Kotlin style
- Documentation: ✅ Will document ViewModel patterns

## Project Structure

### Documentation (this feature)

```text
specs/017-viewmodel-pattern/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
└── tasks.md             # Phase 2 output (via /speckit.tasks)
```

### Source Code (repository root)

```text
graphEditor/src/jvmMain/kotlin/
├── Main.kt                      # Composition root (reduce to layout only)
├── state/
│   ├── GraphState.kt            # Keep as shared state holder
│   ├── UndoRedoManager.kt       # Keep as-is
│   ├── PropertyChangeTracker.kt # Keep as-is
│   ├── ViewSynchronizer.kt      # Keep as-is
│   ├── IPTypeRegistry.kt        # Keep as-is
│   └── NodeGeneratorState.kt    # Keep as data class
├── viewmodel/                   # NEW: ViewModel layer
│   ├── GraphEditorViewModel.kt  # App-level orchestration
│   ├── NodeGeneratorViewModel.kt
│   ├── PropertiesPanelViewModel.kt
│   ├── NodePaletteViewModel.kt
│   ├── IPPaletteViewModel.kt
│   └── CanvasInteractionViewModel.kt
├── ui/                          # Simplified to rendering only
│   ├── FlowGraphCanvas.kt
│   ├── NodeGeneratorPanel.kt
│   ├── NodePalette.kt
│   ├── PropertiesPanel.kt
│   ├── IPPalette.kt
│   └── [other UI components]
└── repository/
    ├── CustomNodeRepository.kt
    ├── CustomNodeDefinition.kt
    └── FileCustomNodeRepository.kt

graphEditor/src/jvmTest/kotlin/
├── viewmodel/                   # NEW: ViewModel tests
│   ├── GraphEditorViewModelTest.kt
│   ├── NodeGeneratorViewModelTest.kt
│   ├── PropertiesPanelViewModelTest.kt
│   └── [other ViewModel tests]
└── [existing tests]
```

**Structure Decision**: Add `viewmodel/` package for ViewModel classes. Keep existing `state/` package for shared state (GraphState, UndoRedoManager). UI components remain in `ui/` but become pure rendering functions.

## Complexity Tracking

No violations requiring justification. The ViewModel pattern simplifies the architecture.
