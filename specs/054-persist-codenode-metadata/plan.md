# Implementation Plan: Persist CodeNode Metadata Through Save Pipeline

**Branch**: `054-persist-codenode-metadata` | **Date**: 2026-03-19 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/054-persist-codenode-metadata/spec.md`

## Summary

Close three gaps in the save pipeline so that CodeNodeDefinition metadata (`_codeNodeClass`) flows from node placement through serialization to code generation. This ensures regenerated Flow files use `CodeNodeDefinition.createRuntime()` instead of falling back to legacy `CodeNodeFactory` patterns. Additionally, remove the legacy `CustomNodeDefinition` infrastructure (JSON repository, `FileCustomNodeRepository`, legacy palette path) since all production nodes are now backed by `CodeNodeDefinitions`.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP - Kotlin Multiplatform)
**Primary Dependencies**: Compose Desktop 1.7.3, kotlinx-coroutines 1.8.0, kotlinx-serialization 1.6.0
**Storage**: N/A (removes `~/.codenode/custom-nodes.json` dependency)
**Testing**: kotlin.test, kotlinx-coroutines-test (commonTest)
**Target Platform**: JVM (graphEditor), Android, iOS (KMPMobileApp)
**Project Type**: KMP multi-module
**Constraints**: CodeNodeDefinition objects must be in commonMain for cross-platform access

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Status | Notes |
|------|--------|-------|
| Licensing | PASS | No new dependencies. All existing deps are Apache 2.0 / MIT |
| Code Quality | PASS | Eliminates legacy parallel code path, simplifies node lifecycle |
| TDD | N/A | Spec does not request new tests. Existing tests must pass (SC-003) |
| Type Safety | PASS | `_codeNodeClass` carries fully-qualified class name for type-safe generation |
| Security | N/A | No user input, no network, no secrets |

## Project Structure

### Documentation (this feature)

```text
specs/054-persist-codenode-metadata/
├── plan.md              # This file
├── research.md          # Phase 0 output (6 decisions)
├── quickstart.md        # Phase 1 output (6 steps)
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (repository root)

```text
# Files MODIFIED (metadata injection):
fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/CodeNodeDefinition.kt

# Files MODIFIED (serialization fix):
kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/FlowKtGenerator.kt

# Files MODIFIED (drag-and-drop fix):
graphEditor/src/jvmMain/kotlin/ui/DragAndDropHandler.kt

# Files MODIFIED (legacy cleanup):
graphEditor/src/jvmMain/kotlin/Main.kt
graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/state/NodeDefinitionRegistry.kt
graphEditor/src/jvmMain/kotlin/viewmodel/NodeGeneratorViewModel.kt
graphEditor/src/jvmMain/kotlin/ui/NodeGeneratorPanel.kt
graphEditor/src/jvmMain/kotlin/save/ModuleSaveService.kt

# Files DELETED (legacy infrastructure):
graphEditor/src/jvmMain/kotlin/repository/FileCustomNodeRepository.kt
graphEditor/src/jvmMain/kotlin/repository/CustomNodeDefinition.kt
graphEditor/src/jvmMain/kotlin/repository/CustomNodeRepository.kt
```

**Structure Decision**: This is a refactoring of existing files across 3 modules (fbpDsl, kotlinCompiler, graphEditor). No new directories or modules are created. Three legacy repository files are deleted.
