# Implementation Plan: Remove Single-Invocation Patterns

**Branch**: `024-remove-single-invocation` | **Date**: 2026-02-20 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/024-remove-single-invocation/spec.md`

## Summary

Remove factory methods, abstract UseCase base classes, lifecycle-aware variants, example implementations, and documentation that exist solely for the single-invocation execution pattern. The continuous channel-based runtime is the sole supported execution model. Per research (R1-R3), `ProcessingLogic`, `processingLogic` property, `create()` generic factory, and `InformationPacket` are retained because they serve the code generation pipeline and are used by StopWatch, kotlinCompiler, and graphEditor modules.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP - Kotlin Multiplatform)
**Primary Dependencies**: kotlinx-coroutines 1.8.0, kotlinx-serialization 1.6.0
**Storage**: N/A (code deletion only)
**Testing**: kotlin.test + JUnit (fbpDsl commonTest)
**Target Platform**: All KMP targets (JVM, potentially native)
**Project Type**: Multi-module KMP project (fbpDsl library module)
**Performance Goals**: N/A (deletion reduces code, no performance impact)
**Constraints**: Must not break compilation in any module (fbpDsl, graphEditor, kotlinCompiler, StopWatch)
**Scale/Scope**: ~7 factory methods removed, ~3 files deleted, ~1 doc deleted, ~3 tests removed. Estimated ~600 lines of code removed.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality | PASS | Deletion improves maintainability by removing unused code. Single responsibility: each removal is independent and verifiable. |
| II. Test-Driven Development | PASS | Backward-compatibility tests (T041-T043) are removed along with the code they test. No new code = no new tests needed. All remaining tests must pass after each step. |
| III. UX Consistency | N/A | No user-facing changes. |
| IV. Performance | N/A | Deletion only — no performance impact. |
| V. Observability | N/A | No runtime behavior changes. |
| Licensing | PASS | No new dependencies. Removing Apache 2.0 code from Apache 2.0 project. |

**Post-Design Re-check**: All gates still PASS. Research (R1-R3) confirmed ProcessingLogic must be retained, narrowing scope to safe deletions only.

## Project Structure

### Documentation (this feature)

```text
specs/024-remove-single-invocation/
├── plan.md              # This file
├── research.md          # Phase 0: dependency analysis
├── data-model.md        # Phase 1: removal inventory
├── quickstart.md        # Phase 1: before/after
├── contracts/           # Phase 1: verification protocol
│   └── removal-verification.md
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (files affected)

```text
fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/
├── model/
│   └── CodeNodeFactory.kt          # MODIFY: remove 7 factory methods
└── usecase/
    ├── TypedUseCases.kt             # DELETE
    ├── LifecycleAwareUseCases.kt    # DELETE
    └── examples/
        └── ExampleUseCases.kt       # DELETE

fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/
└── runtime/
    └── ContinuousFactoryTest.kt     # MODIFY: remove tests T041-T043

fbpDsl/docs/
└── UseCase-Pattern-Guide.md         # DELETE
```

**Structure Decision**: Multi-module KMP project. All changes are within the `fbpDsl` module. No other modules are modified — only verified to still compile after changes.

## Design Decisions

### D1: Removal Order — Leaf-First

Remove code starting from leaf dependencies (no dependents) working inward:

1. **ExampleUseCases.kt** → depends on TypedUseCases, depended on by nothing
2. **TypedUseCases.kt** → depends on ProcessingLogic, depended on by ExampleUseCases (removed in step 1)
3. **LifecycleAwareUseCases.kt** → depends on ProcessingLogic, depended on by nothing
4. **UseCase-Pattern-Guide.md** → documentation, no code dependencies
5. **Factory methods** → depend on ProcessingLogic/CodeNode, depended on by nothing (after test removal)
6. **Backward-compat tests** → depend on factory methods, removed alongside or before them

### D2: Spec Scope Reduction

Per research R1-R3, spec US4 (Remove ProcessingLogic Interface) is **dropped**. ProcessingLogic is used by:
- StopWatch production components
- kotlinCompiler code generation pipeline
- graphEditor module save and compilation validation

The spec's scope is reduced to US1 (deprecated factory methods), US2 (UseCase classes), and US3 (remaining factory methods).

### D3: Cross-Module Verification

After each removal step, compilation must be verified across all 4 modules:
```
./gradlew :fbpDsl:compileKotlinJvm :graphEditor:compileKotlinJvm :kotlinCompiler:compileKotlinJvm :StopWatch:compileKotlinJvm
```

This catches any transitive dependency that the grep-based research may have missed.
