# Implementation Plan: Generate CodeNodeDefinition-Based Repository Modules

**Branch**: `061-codenode-definition-codegen` | **Date**: 2026-03-30 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/061-codenode-definition-codegen/spec.md`

## Summary

Replace the legacy entity module code generators (factory functions + tick functions) with CodeNodeDefinition-based generators. Create three new generator classes that produce self-contained node definition objects for CUD source, Repository processor, and Display sink. Update EntityFlowGraphBuilder to tag all nodes with `_codeNodeClass`. Simplify RuntimeFlowGenerator by removing the legacy tick-function code path entirely — all nodes are now CodeNodeDefinition objects, eliminating the dual-path branching and ~50% of the generator's complexity.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP - Kotlin Multiplatform)
**Primary Dependencies**: kotlinCompiler module (code generation), fbpDsl (runtime types)
**Storage**: N/A (generates source code files)
**Testing**: kotlinCompiler/src/commonTest (existing generator tests)
**Target Platform**: JVM (code generation runs on JVM, generated code is KMP)
**Project Type**: Multi-module KMP project
**Performance Goals**: N/A (code generation, not runtime)
**Constraints**: Generated code must be KMP-compatible (commonMain source set). Must work with the separated repo architecture (feature 060).
**Scale/Scope**: ~6 files modified/created in kotlinCompiler, ~2 files modified in graphEditor

## Constitution Check

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality First | PASS | Generated code follows established CodeNodeDefinition pattern. Single responsibility per generator. |
| II. Test-Driven Development | PASS | Existing generator tests updated for new output patterns. |
| III. User Experience Consistency | PASS | Generated modules work identically to hand-written ones. |
| IV. Performance Requirements | N/A | Code generation, not runtime. |
| V. Observability & Debugging | PASS | Generated nodes include descriptive names and error messages. |
| Licensing | PASS | No new dependencies. |

## Project Structure

### Documentation (this feature)

```text
specs/061-codenode-definition-codegen/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   └── generated-node-contract.md
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/
├── EntityCUDCodeNodeGenerator.kt        # New: generates {Entity}CUDCodeNode.kt
├── EntityRepositoryCodeNodeGenerator.kt # New: generates {Entity}RepositoryCodeNode.kt
├── EntityDisplayCodeNodeGenerator.kt    # New: generates {PluralName}DisplayCodeNode.kt
├── EntityModuleGenerator.kt             # Modified: uses new generators, outputs to nodes/ dir
├── EntityFlowGraphBuilder.kt            # Modified: adds _codeNodeClass config to all nodes
├── RuntimeFlowGenerator.kt              # Simplified: remove legacy tick-function path, CodeNodeDefinition-only
├── EntityCUDGenerator.kt                # Deleted: legacy factory function generator (replaced by EntityCUDCodeNodeGenerator)
├── EntityDisplayGenerator.kt            # Deleted: legacy factory function generator (replaced by EntityDisplayCodeNodeGenerator)
└── ModuleGenerator.kt                   # Modified: generated build.gradle.kts includes preview-api

graphEditor/src/jvmMain/kotlin/save/
└── ModuleSaveService.kt                 # Modified: cleanup includes nodes/ directory

kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/
├── EntityCUDCodeNodeGeneratorTest.kt    # New: tests for CUD generator
├── EntityRepositoryCodeNodeGeneratorTest.kt # New: tests for Repository generator
├── EntityDisplayCodeNodeGeneratorTest.kt    # New: tests for Display generator
└── EntityModuleGeneratorTest.kt         # Modified: updated expectations
```

**Structure Decision**: All changes are in the kotlinCompiler module (code generation) with minor updates to graphEditor's ModuleSaveService. The generated output follows the established `{Module}/src/commonMain/kotlin/.../nodes/` convention.

## Complexity Tracking

No constitution violations. All changes follow existing patterns.
