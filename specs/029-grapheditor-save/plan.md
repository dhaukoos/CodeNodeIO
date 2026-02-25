# Implementation Plan: GraphEditor Save Functionality

**Branch**: `029-grapheditor-save` | **Date**: 2026-02-25 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/029-grapheditor-save/spec.md`

## Summary

Merge the separate Save and Compile operations into a single Save action that creates/updates the full module on disk. First-time save prompts for a directory; subsequent saves (same FlowGraph name) auto-save to the remembered location. On every save: overwrite .flow.kt and generated runtime files, preserve existing stub files, create new stubs for added nodes, and delete orphaned stubs for removed nodes. Track save locations per FlowGraph name in a session-scoped registry.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + Compose Desktop 1.7.3
**Primary Dependencies**: kotlinx-coroutines 1.8.0, kotlinx-serialization 1.6.0, lifecycle-viewmodel-compose 2.8.0
**Storage**: Filesystem (generated KMP module directories)
**Testing**: kotlin.test (JVM target), `./gradlew :graphEditor:jvmTest`
**Target Platform**: Desktop (JVM) — Compose Desktop application
**Project Type**: Multi-module KMP project (graphEditor, kotlinCompiler, fbpDsl modules)
**Performance Goals**: Save completes in <2s for typical FlowGraphs (<20 nodes)
**Constraints**: No external runtime dependencies beyond existing stack
**Scale/Scope**: Single-user desktop application, FlowGraphs with 1-50 nodes

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality | PASS | Refactoring existing methods, maintaining single responsibility |
| II. TDD | PASS | Tests exist for save/compile; will update and extend |
| III. UX Consistency | PASS | Simplifies UX by merging two actions into one |
| IV. Performance | PASS | File I/O only, no new computation bottlenecks |
| V. Observability | PASS | Save result already reports files created/warnings |
| Licensing | PASS | No new dependencies |

No violations. No complexity tracking needed.

## Project Structure

### Documentation (this feature)

```text
specs/029-grapheditor-save/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── quickstart.md        # Phase 1 output
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
graphEditor/
├── src/jvmMain/kotlin/
│   ├── Main.kt                          # UI: Save button handler, directory chooser, save location registry
│   └── save/
│       └── ModuleSaveService.kt         # Service: unified saveModule() with compile + orphan deletion
└── src/jvmTest/kotlin/save/
    └── ModuleSaveServiceTest.kt         # Tests: unified save behavior, orphan deletion
```

**Structure Decision**: All changes are within the existing graphEditor module. No new modules or files needed — this is a refactoring of existing save/compile methods into a unified save flow.

### Key Files Modified

| File | Change |
|------|--------|
| `graphEditor/src/jvmMain/kotlin/save/ModuleSaveService.kt` | Merge saveModule + compileModule into unified saveModule(); change orphan detection from warn to delete; add `filesDeleted` to result |
| `graphEditor/src/jvmMain/kotlin/Main.kt` | Replace separate Save/Compile handlers with single Save handler; add save location registry (Map<String, File>); detect name changes |
| `graphEditor/src/jvmTest/kotlin/save/ModuleSaveServiceTest.kt` | Update tests for unified save; add orphan deletion tests; remove compile-only tests |
