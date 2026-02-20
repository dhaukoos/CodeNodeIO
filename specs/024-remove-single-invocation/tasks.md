# Tasks: Remove Single-Invocation Patterns

**Input**: Design documents from `/specs/024-remove-single-invocation/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/removal-verification.md

**Tests**: No new tests — this is a deletion feature. Backward-compatibility tests for removed code are deleted alongside the code they test.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story. US4 (Remove ProcessingLogic) from spec.md is **dropped** per research R1-R3 — ProcessingLogic is used by production code across multiple modules.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **fbpDsl module**: `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/`
- **Factory**: `fbpDsl/.../model/CodeNodeFactory.kt`
- **UseCases**: `fbpDsl/.../usecase/`
- **Tests**: `fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/runtime/ContinuousFactoryTest.kt`
- **Docs**: `fbpDsl/docs/`

---

## Phase 1: Setup

**Purpose**: No project initialization needed — all infrastructure exists. This is a deletion-only feature modifying existing files within the `fbpDsl` module.

No setup tasks required.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: No foundational tasks needed. Per research.md (R1-R9), all items to be removed have been verified to have zero production callers. The removal order follows the leaf-first strategy from plan.md D1.

**Checkpoint**: Foundation ready — all user stories can proceed.

---

## Phase 3: User Story 1 - Remove Deprecated Factory Methods (Priority: P1) MVP

**Goal**: Remove the `@Deprecated` `createGenerator` and `createSink` factory methods and their backward-compatibility tests, establishing that the continuous pattern is the sole supported approach.

**Independent Test**: After removal, `./gradlew :fbpDsl:compileKotlinJvm` succeeds and `./gradlew :fbpDsl:jvmTest` passes. Searching for `createGenerator` or `createSink` returns only the continuous versions.

### Implementation for User Story 1

- [ ] T001 [US1] Remove backward-compatibility test T041 (`existing createGenerator method still works`) from `fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/runtime/ContinuousFactoryTest.kt` — Delete the test function and its `@Suppress("DEPRECATION")` annotation. Keep all other tests intact.

- [ ] T002 [US1] Remove backward-compatibility test T042 (`existing createSink method still works`) from `fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/runtime/ContinuousFactoryTest.kt` — Delete the test function and its `@Suppress("DEPRECATION")` annotation. Keep all other tests intact.

- [ ] T003 [US1] Remove the deprecated `createGenerator<T>` factory method from `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNodeFactory.kt` — Delete the entire method including its `@Deprecated` annotation, KDoc, and function body. Do not modify any other methods.

- [ ] T004 [US1] Remove the deprecated `createSink<T>` factory method from `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNodeFactory.kt` — Delete the entire method including its `@Deprecated` annotation, KDoc, and function body. Do not modify any other methods.

- [ ] T005 [US1] Build verification for US1 — Run `./gradlew :fbpDsl:compileKotlinJvm` and `./gradlew :fbpDsl:jvmTest`. Verify compilation succeeds and all remaining tests pass. Search codebase for `createGenerator` and `createSink` to confirm only continuous versions remain.

**Checkpoint**: Deprecated factory methods removed. Only `createContinuousGenerator` and `createContinuousSink` remain for generator/sink creation.

---

## Phase 4: User Story 2 - Remove Single-Invocation UseCase Classes (Priority: P2)

**Goal**: Remove the typed UseCase base classes, lifecycle-aware variants, example implementations, and UseCase documentation — all unused abstractions that exist only for the single-invocation model.

**Independent Test**: After removal, `./gradlew :fbpDsl:compileKotlinJvm` succeeds, cross-module compilation succeeds, and all tests pass. The `usecase/` directory is empty or removed.

### Implementation for User Story 2

- [ ] T006 [P] [US2] Delete `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/usecase/examples/ExampleUseCases.kt` — Remove the entire file. This is the leaf dependency (depends on TypedUseCases, depended on by nothing).

- [ ] T007 [US2] Delete `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/usecase/TypedUseCases.kt` — Remove the entire file containing TransformerUseCase, FilterUseCase, ValidatorUseCase, SplitterUseCase, MergerUseCase, GeneratorUseCase, SinkUseCase. Depends on T006 (ExampleUseCases removed first).

- [ ] T008 [P] [US2] Delete `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/usecase/LifecycleAwareUseCases.kt` — Remove the entire file containing LifecycleAwareUseCase, LifecycleManager, LifecycleDecorator, DatabaseUseCase, CachedUseCase, BufferedUseCase. Can run in parallel with T006 (no cross-dependency).

- [ ] T009 [P] [US2] Delete `fbpDsl/docs/UseCase-Pattern-Guide.md` — Remove the UseCase pattern documentation file. Can run in parallel with T006-T008 (documentation, no code dependency).

- [ ] T010 [US2] Build verification for US2 — Run `./gradlew :fbpDsl:compileKotlinJvm :graphEditor:compileKotlinJvm :kotlinCompiler:compileKotlinJvm :StopWatch:compileKotlinJvm` and `./gradlew :fbpDsl:jvmTest`. Verify all modules compile and tests pass. Search for removed class names (TransformerUseCase, FilterUseCase, etc.) to confirm no remaining references in source code.

**Checkpoint**: All UseCase abstractions removed. The `usecase/` directory should be empty (or contain only the directory structure).

---

## Phase 5: User Story 3 - Remove Remaining Single-Invocation Factory Methods (Priority: P3)

**Goal**: Remove the non-deprecated single-invocation factory methods (`createTransformer`, `createFilter`, `createSplitter`, `createMerger`, `createValidator`) that return CodeNode with embedded ProcessingLogic.

**Independent Test**: After removal, `./gradlew :fbpDsl:compileKotlinJvm` succeeds and all tests pass. Only `create()` (generic) and the continuous factory methods remain in CodeNodeFactory.

### Implementation for User Story 3

- [ ] T011 [P] [US3] Remove `createTransformer<TIn, TOut>` factory method from `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNodeFactory.kt` — Delete the entire method including KDoc and function body.

- [ ] T012 [P] [US3] Remove `createFilter<T>` factory method from `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNodeFactory.kt` — Delete the entire method including KDoc and function body.

- [ ] T013 [P] [US3] Remove `createSplitter<T>` factory method from `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNodeFactory.kt` — Delete the entire method including KDoc and function body.

- [ ] T014 [P] [US3] Remove `createMerger<T>` factory method from `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNodeFactory.kt` — Delete the entire method including KDoc and function body.

- [ ] T015 [P] [US3] Remove `createValidator<T>` factory method from `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNodeFactory.kt` — Delete the entire method including KDoc and function body.

- [ ] T016 [US3] Build verification for US3 — Run `./gradlew :fbpDsl:compileKotlinJvm :graphEditor:compileKotlinJvm :kotlinCompiler:compileKotlinJvm :StopWatch:compileKotlinJvm` and `./gradlew :fbpDsl:jvmTest`. Verify all modules compile and tests pass.

**Checkpoint**: All single-invocation factory methods removed. CodeNodeFactory now contains only `create()` (generic) and the continuous factory methods.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final validation and cleanup across all user stories.

- [ ] T017 Remove backward-compatibility test T043 (`ProcessingLogic can be invoked directly`) from `fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/runtime/ContinuousFactoryTest.kt` — This test creates a node using `create()` with processingLogic and invokes it directly. Since `create()` is retained but the single-invocation invoke pattern is no longer the supported usage, remove this test if it only validates the deprecated pattern. If it tests `create()` functionality that remains valid, retain it.

- [ ] T018 Clean up empty `usecase/` directory structure in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/usecase/` — Remove the empty `usecase/` directory and its `examples/` subdirectory if they are empty after US2 file deletions.

- [ ] T019 Full cross-module build verification and dangling reference scan — Run `./gradlew build` across all modules. Search entire codebase for all removed symbol names: `createGenerator`, `createSink`, `createTransformer`, `createFilter`, `createSplitter`, `createMerger`, `createValidator`, `TransformerUseCase`, `FilterUseCase`, `ValidatorUseCase`, `SplitterUseCase`, `MergerUseCase`, `GeneratorUseCase`, `SinkUseCase`, `LifecycleAwareUseCase`, `DatabaseUseCase`, `CachedUseCase`, `BufferedUseCase`. Confirm zero results in source code (references in historical specs and this tasks.md are acceptable).

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No tasks needed
- **Foundational (Phase 2)**: No tasks needed — all infrastructure exists
- **US1 (Phase 3)**: Can start immediately
- **US2 (Phase 4)**: Can start immediately (independent of US1)
- **US3 (Phase 5)**: Can start immediately (independent of US1 and US2)
- **Polish (Phase 6)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: No dependencies — can start immediately
  - T001 and T002 must complete before T003 and T004 (tests removed before methods)
  - T003 and T004 can be done in either order (different methods in same file)
  - T005 depends on T001-T004
- **User Story 2 (P2)**: No dependencies on other stories
  - T006 must complete before T007 (ExampleUseCases before TypedUseCases)
  - T008 and T009 can run in parallel with T006 (different files)
  - T010 depends on T006-T009
- **User Story 3 (P3)**: No dependencies on other stories
  - T011-T015 are all [P] — can run in parallel (all modify same file but remove independent methods)
  - T016 depends on T011-T015

### Within Each User Story

- Remove tests before removing the code they test (prevents compile errors from missing test subjects)
- Factory method removals within the same file can be done in any order
- Build verification last

### Parallel Opportunities

```bash
# All three user stories can run in parallel (independent files/methods):
# US1: ContinuousFactoryTest.kt + CodeNodeFactory.kt (deprecated methods)
# US2: usecase/ directory files + docs
# US3: CodeNodeFactory.kt (non-deprecated methods)

# Within US2, parallel file deletions:
T006: ExampleUseCases.kt
T008: LifecycleAwareUseCases.kt  # [P] with T006
T009: UseCase-Pattern-Guide.md   # [P] with T006

# Within US3, all method removals are parallel:
T011: createTransformer
T012: createFilter
T013: createSplitter
T014: createMerger
T015: createValidator
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 3: User Story 1 (T001-T005)
2. **STOP and VALIDATE**: Deprecated methods removed, project compiles, tests pass
3. This alone delivers value — eliminates the deprecated API surface

### Incremental Delivery

1. US1 (T001-T005) → Deprecated factory methods removed → MVP
2. US2 (T006-T010) → UseCase abstractions and examples removed → Cleaner codebase
3. US3 (T011-T016) → All single-invocation factory methods removed → Full cleanup
4. Polish (T017-T019) → Final verification and empty directory cleanup

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Total: 19 tasks across 6 files (3 deleted, 2 modified, 1 doc deleted)
- No new files created — all changes delete or reduce existing code
- ProcessingLogic, processingLogic property, create() generic, and InformationPacket are explicitly RETAINED per research R1-R3
- US4 from spec.md (Remove ProcessingLogic) is DROPPED — not feasible per dependency analysis
- All US1-US3 stories are fully independent and can be executed in any order
