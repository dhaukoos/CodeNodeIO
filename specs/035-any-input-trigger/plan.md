# Implementation Plan: Any-Input Trigger Mode for Node Generator

**Branch**: `035-any-input-trigger` | **Date**: 2026-03-01 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/035-any-input-trigger/spec.md`

## Summary

Add an "Any Input" trigger mode to the Node Generator that creates runtime classes firing their process block when data arrives on ANY input (vs waiting for ALL inputs). This involves:
1. A boolean toggle in the Node Generator UI (visible only with 2+ inputs)
2. 8 new `In{A}AnyOut{B}Runtime` classes using Kotlin's `select` expression for concurrent channel listening
3. Corresponding factory methods, type aliases, and code generator updates

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP - Kotlin Multiplatform)
**Primary Dependencies**: Compose Desktop 1.7.3, kotlinx-coroutines 1.8.0 (`select` expression), kotlinx-serialization 1.6.0, lifecycle-viewmodel-compose 2.8.0
**Storage**: JSON file via kotlinx-serialization (CustomNodeRepository persistence)
**Testing**: kotlin.test + kotlinx-coroutines-test (`runTest`, `advanceTimeBy`, `advanceUntilIdle`)
**Target Platform**: JVM (Graph Editor), Kotlin Multiplatform (fbpDsl runtime)
**Project Type**: Multi-module Kotlin Multiplatform (fbpDsl, kotlinCompiler, graphEditor)
**Constraints**: Must maintain backward compatibility with existing serialized CustomNodeDefinitions (default `anyInput = false`)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Status | Notes |
|------|--------|-------|
| Licensing (Apache 2.0) | PASS | All dependencies (kotlinx-coroutines, Compose) are Apache 2.0/compatible |
| KMP Dependency Protocol | PASS | No new dependencies — `kotlinx.coroutines.selects.select` is part of existing kotlinx-coroutines |
| Code Quality First | PASS | New runtime classes follow established patterns (In2Out1Runtime as template) |
| Test-Driven Development | PASS | Tests planned for all new runtime classes, factory methods, and UI behavior |
| Type Safety | PASS | All new classes are fully generic with explicit type parameters |

## Project Structure

### Documentation (this feature)

```text
specs/035-any-input-trigger/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── checklists/
│   └── requirements.md  # Spec quality checklist
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/
├── runtime/
│   ├── ContinuousTypes.kt           # Modified: add 16 new type aliases
│   ├── In2AnyOut1Runtime.kt          # NEW: 2-input any-trigger, 1-output
│   ├── In2AnyOut2Runtime.kt          # NEW: 2-input any-trigger, 2-output
│   ├── In2AnyOut3Runtime.kt          # NEW: 2-input any-trigger, 3-output
│   ├── In2AnySinkRuntime.kt          # NEW: 2-input any-trigger sink
│   ├── In3AnyOut1Runtime.kt          # NEW: 3-input any-trigger, 1-output
│   ├── In3AnyOut2Runtime.kt          # NEW: 3-input any-trigger, 2-output
│   ├── In3AnyOut3Runtime.kt          # NEW: 3-input any-trigger, 3-output
│   └── In3AnySinkRuntime.kt          # NEW: 3-input any-trigger sink
├── model/
│   └── CodeNodeFactory.kt            # Modified: add 8 factory methods
└── factory/
    └── GenericNodeTypeFactory.kt      # Modified: support "anyout" in _genericType

kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/
├── RuntimeTypeResolver.kt            # Modified: anyInput flag in method dispatch
├── RuntimeFlowGenerator.kt           # Modified: pass anyInput to resolver
└── ProcessingLogicStubGenerator.kt   # Modified: any-input tick type aliases

graphEditor/src/jvmMain/kotlin/
├── repository/
│   └── CustomNodeDefinition.kt       # Modified: add anyInput field
├── viewmodel/
│   └── NodeGeneratorViewModel.kt     # Modified: anyInput state + toggle logic
└── ui/
    └── NodeGeneratorPanel.kt          # Modified: add Switch composable

fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/runtime/
└── AnyInputRuntimeTest.kt            # NEW: tests for all 8 any-input variants

kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/
├── RuntimeTypeResolverTest.kt         # Modified: test anyInput variants
├── RuntimeFlowGeneratorTest.kt        # Modified: test any-input code gen
└── ProcessingLogicStubGeneratorTest.kt # Modified: test any-input tick types
```

**Structure Decision**: Follows existing module structure. New runtime classes in `fbpDsl/runtime/`, code generator changes in `kotlinCompiler/generator/`, UI changes in `graphEditor/`. No new modules needed.
