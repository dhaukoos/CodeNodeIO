# Tasks: Timed Factory Methods

**Input**: Design documents from `/specs/025-timed-factory-methods/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/method-signatures.md

**Tests**: No dedicated test tasks — each user story's build verification confirms compilation and existing tests pass. The timed wrappers follow the same pattern as `createTimedOut2Generator` which is already in production.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Type aliases**: `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/ContinuousTypes.kt`
- **Factory methods**: `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNodeFactory.kt`

---

## Phase 1: Setup

**Purpose**: No project initialization needed — all infrastructure exists. This is an additive feature modifying existing files within the `fbpDsl` module.

No setup tasks required.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Add all tick type aliases to ContinuousTypes.kt. These are needed by all user stories.

- [ ] T001 Add generator tick type aliases to `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/ContinuousTypes.kt` — Add `GeneratorTickBlock<T>` = `suspend () -> T` and `Out3TickBlock<U, V, W>` = `suspend () -> ProcessResult3<U, V, W>`. Place them in a new `// ========== Timed Tick Blocks ==========` section after the existing `Out2TickBlock`. Include KDoc matching the existing `Out2TickBlock` style.

- [ ] T002 [P] Add processor tick type aliases to `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/ContinuousTypes.kt` — Add `TransformerTickBlock<TIn, TOut>` = `suspend (TIn) -> TOut`, `FilterTickBlock<T>` = `suspend (T) -> Boolean`, `In2Out1TickBlock<A, B, R>` = `suspend (A, B) -> R`, `In3Out1TickBlock<A, B, C, R>` = `suspend (A, B, C) -> R`, `In1Out2TickBlock<A, U, V>` = `suspend (A) -> ProcessResult2<U, V>`, `In1Out3TickBlock<A, U, V, W>` = `suspend (A) -> ProcessResult3<U, V, W>`, `In2Out2TickBlock<A, B, U, V>` = `suspend (A, B) -> ProcessResult2<U, V>`, `In2Out3TickBlock<A, B, U, V, W>` = `suspend (A, B) -> ProcessResult3<U, V, W>`, `In3Out2TickBlock<A, B, C, U, V>` = `suspend (A, B, C) -> ProcessResult2<U, V>`, `In3Out3TickBlock<A, B, C, U, V, W>` = `suspend (A, B, C) -> ProcessResult3<U, V, W>`. Include KDoc for each.

- [ ] T003 [P] Add sink tick type aliases to `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/ContinuousTypes.kt` — Add `SinkTickBlock<T>` = `suspend (T) -> Unit`, `In2SinkTickBlock<A, B>` = `suspend (A, B) -> Unit`, `In3SinkTickBlock<A, B, C>` = `suspend (A, B, C) -> Unit`. Include KDoc for each.

- [ ] T004 Build verification for foundational phase — Run `./gradlew :fbpDsl:compileKotlinJvm`. Verify all tick type aliases compile.

**Checkpoint**: All 15 new tick type aliases defined. Factory method tasks can proceed.

---

## Phase 3: User Story 1 - Timed Generator Wrappers (Priority: P1) MVP

**Goal**: Add timed wrapper factory methods for all generator variants (single-output and 3-output; 2-output already exists).

**Independent Test**: After adding, `./gradlew :fbpDsl:compileKotlinJvm` succeeds and `./gradlew :fbpDsl:jvmTest` passes. Factory contains `createTimedGenerator` and `createTimedOut3Generator`.

### Implementation for User Story 1

- [ ] T005 [US1] Add `createTimedGenerator<T>` factory method to `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNodeFactory.kt` — Follow the exact `createTimedOut2Generator` pattern. Accept `tickIntervalMs: Long`, `channelCapacity: Int = Channel.BUFFERED`, and `tick: GeneratorTickBlock<T>`. Wrap tick in a generate block: `{ emit -> while (currentCoroutineContext().isActive) { delay(tickIntervalMs); emit(tick()) } }`. Delegate to `createContinuousGenerator`. Include KDoc. Place in the existing "Timed Generator Factory Methods" section, before `createTimedOut2Generator`.

- [ ] T006 [US1] Add `createTimedOut3Generator<U, V, W>` factory method to `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNodeFactory.kt` — Same pattern as `createTimedOut2Generator`. Accept `tick: Out3TickBlock<U, V, W>`. Wrap in generate block with `Out3GeneratorBlock`. Delegate to `createOut3Generator`. Include KDoc. Place after `createTimedOut2Generator`.

- [ ] T007 [US1] Build verification for US1 — Run `./gradlew :fbpDsl:compileKotlinJvm` and `./gradlew :fbpDsl:jvmTest`. Verify compilation succeeds and all tests pass.

**Checkpoint**: All 3 timed generator variants exist (1 new single-output + 1 existing 2-output + 1 new 3-output).

---

## Phase 4: User Story 2 - Timed Processor Wrappers (Priority: P2)

**Goal**: Add timed wrapper factory methods for all processor variants (transformer, filter, and all multi-input/multi-output processors).

**Independent Test**: After adding, `./gradlew :fbpDsl:compileKotlinJvm` succeeds and all tests pass. Factory contains all 10 timed processor methods.

### Implementation for User Story 2

- [ ] T008 [US2] Add `createTimedTransformer<TIn, TOut>` factory method to `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNodeFactory.kt` — Accept `tickIntervalMs: Long` and `tick: TransformerTickBlock<TIn, TOut>`. Wrap tick: `{ input -> delay(tickIntervalMs); tick(input) }`. Delegate to `createContinuousTransformer`. Include KDoc. Create a new `// ========== Timed Processor Factory Methods ==========` section after the existing timed generator section.

- [ ] T009 [US2] Add `createTimedFilter<T>` factory method to `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNodeFactory.kt` — Accept `tick: FilterTickBlock<T>`. Wrap tick: `{ value -> delay(tickIntervalMs); tick(value) }`. Delegate to `createContinuousFilter`. Include KDoc.

- [ ] T010 [US2] Add `createTimedIn2Out1Processor<A, B, R>` factory method to `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNodeFactory.kt` — Accept `tick: In2Out1TickBlock<A, B, R>`. Wrap tick: `{ a, b -> delay(tickIntervalMs); tick(a, b) }`. Delegate to `createIn2Out1Processor`. Include KDoc.

- [ ] T011 [US2] Add `createTimedIn3Out1Processor<A, B, C, R>` factory method to `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNodeFactory.kt` — Accept `tick: In3Out1TickBlock<A, B, C, R>`. Wrap tick: `{ a, b, c -> delay(tickIntervalMs); tick(a, b, c) }`. Delegate to `createIn3Out1Processor`. Include KDoc.

- [ ] T012 [US2] Add `createTimedIn1Out2Processor<A, U, V>` factory method to `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNodeFactory.kt` — Accept `channelCapacity: Int = Channel.BUFFERED` and `tick: In1Out2TickBlock<A, U, V>`. Wrap tick: `{ a -> delay(tickIntervalMs); tick(a) }`. Delegate to `createIn1Out2Processor`. Include KDoc.

- [ ] T013 [US2] Add `createTimedIn1Out3Processor<A, U, V, W>` factory method to `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNodeFactory.kt` — Accept `channelCapacity` and `tick: In1Out3TickBlock<A, U, V, W>`. Wrap tick: `{ a -> delay(tickIntervalMs); tick(a) }`. Delegate to `createIn1Out3Processor`. Include KDoc.

- [ ] T014 [US2] Add `createTimedIn2Out2Processor<A, B, U, V>` factory method to `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNodeFactory.kt` — Accept `channelCapacity` and `tick: In2Out2TickBlock<A, B, U, V>`. Wrap tick: `{ a, b -> delay(tickIntervalMs); tick(a, b) }`. Delegate to `createIn2Out2Processor`. Include KDoc.

- [ ] T015 [US2] Add `createTimedIn2Out3Processor<A, B, U, V, W>` factory method to `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNodeFactory.kt` — Accept `channelCapacity` and `tick: In2Out3TickBlock<A, B, U, V, W>`. Wrap tick: `{ a, b -> delay(tickIntervalMs); tick(a, b) }`. Delegate to `createIn2Out3Processor`. Include KDoc.

- [ ] T016 [US2] Add `createTimedIn3Out2Processor<A, B, C, U, V>` factory method to `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNodeFactory.kt` — Accept `channelCapacity` and `tick: In3Out2TickBlock<A, B, C, U, V>`. Wrap tick: `{ a, b, c -> delay(tickIntervalMs); tick(a, b, c) }`. Delegate to `createIn3Out2Processor`. Include KDoc.

- [ ] T017 [US2] Add `createTimedIn3Out3Processor<A, B, C, U, V, W>` factory method to `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNodeFactory.kt` — Accept `channelCapacity` and `tick: In3Out3TickBlock<A, B, C, U, V, W>`. Wrap tick: `{ a, b, c -> delay(tickIntervalMs); tick(a, b, c) }`. Delegate to `createIn3Out3Processor`. Include KDoc.

- [ ] T018 [US2] Build verification for US2 — Run `./gradlew :fbpDsl:compileKotlinJvm` and `./gradlew :fbpDsl:jvmTest`. Verify compilation succeeds and all tests pass.

**Checkpoint**: All 10 timed processor variants exist.

---

## Phase 5: User Story 3 - Timed Sink Wrappers (Priority: P3)

**Goal**: Add timed wrapper factory methods for all sink variants (single-input, 2-input, 3-input).

**Independent Test**: After adding, `./gradlew :fbpDsl:compileKotlinJvm` succeeds and all tests pass. Factory contains all 3 timed sink methods.

### Implementation for User Story 3

- [ ] T019 [US3] Add `createTimedSink<T>` factory method to `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNodeFactory.kt` — Accept `tickIntervalMs: Long` and `tick: SinkTickBlock<T>`. Wrap tick: `{ value -> delay(tickIntervalMs); tick(value) }`. Delegate to `createContinuousSink`. Include KDoc. Create a new `// ========== Timed Sink Factory Methods ==========` section.

- [ ] T020 [US3] Add `createTimedIn2Sink<A, B>` factory method to `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNodeFactory.kt` — Accept `tick: In2SinkTickBlock<A, B>`. Wrap tick: `{ a, b -> delay(tickIntervalMs); tick(a, b) }`. Delegate to `createIn2Sink`. Include KDoc.

- [ ] T021 [US3] Add `createTimedIn3Sink<A, B, C>` factory method to `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNodeFactory.kt` — Accept `tick: In3SinkTickBlock<A, B, C>`. Wrap tick: `{ a, b, c -> delay(tickIntervalMs); tick(a, b, c) }`. Delegate to `createIn3Sink`. Include KDoc.

- [ ] T022 [US3] Build verification for US3 — Run `./gradlew :fbpDsl:compileKotlinJvm` and `./gradlew :fbpDsl:jvmTest`. Verify compilation succeeds and all tests pass.

**Checkpoint**: All 3 timed sink variants exist.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final validation across all user stories.

- [ ] T023 Full cross-module build verification — Run `./gradlew :fbpDsl:compileKotlinJvm :graphEditor:compileKotlinJvm :kotlinCompiler:compileKotlinJvm :StopWatch:compileKotlinJvm` and `./gradlew :fbpDsl:jvmTest`. Verify all modules compile and all tests pass.

- [ ] T024 Verify method count and completeness — Search CodeNodeFactory.kt for all `createTimed` methods and confirm exactly 16 exist (1 existing + 15 new): `createTimedGenerator`, `createTimedOut2Generator`, `createTimedOut3Generator`, `createTimedTransformer`, `createTimedFilter`, `createTimedIn2Out1Processor`, `createTimedIn3Out1Processor`, `createTimedIn1Out2Processor`, `createTimedIn1Out3Processor`, `createTimedIn2Out2Processor`, `createTimedIn2Out3Processor`, `createTimedIn3Out2Processor`, `createTimedIn3Out3Processor`, `createTimedSink`, `createTimedIn2Sink`, `createTimedIn3Sink`. Search ContinuousTypes.kt for all `TickBlock` aliases and confirm exactly 16 exist (1 existing + 15 new).

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No tasks needed
- **Foundational (Phase 2)**: No dependencies — tick type aliases can start immediately
- **US1 (Phase 3)**: Depends on Phase 2 (T001 for generator tick aliases)
- **US2 (Phase 4)**: Depends on Phase 2 (T002 for processor tick aliases)
- **US3 (Phase 5)**: Depends on Phase 2 (T003 for sink tick aliases)
- **Polish (Phase 6)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Depends on T001 (generator tick aliases)
- **User Story 2 (P2)**: Depends on T002 (processor tick aliases); independent of US1
- **User Story 3 (P3)**: Depends on T003 (sink tick aliases); independent of US1 and US2

### Within Each User Story

- All factory method tasks within a story modify the same file (CodeNodeFactory.kt) and must run sequentially
- Build verification last

### Parallel Opportunities

```bash
# Foundational phase: type alias tasks can run in parallel (same file, different sections)
T001: Generator tick aliases
T002: Processor tick aliases  # [P] with T001
T003: Sink tick aliases       # [P] with T001

# User stories are independent and could run in parallel,
# but all modify CodeNodeFactory.kt so sequential is safer:
US1 → US2 → US3
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 2: Foundational (T001-T004)
2. Complete Phase 3: User Story 1 (T005-T007)
3. **STOP and VALIDATE**: Timed generators work, project compiles, tests pass
4. This alone delivers value — completes the timed generator family

### Incremental Delivery

1. Phase 2 (T001-T004) → Tick type aliases defined → Foundation ready
2. US1 (T005-T007) → Timed generators → MVP
3. US2 (T008-T018) → Timed processors → Most coverage (10 methods)
4. US3 (T019-T022) → Timed sinks → Complete family
5. Polish (T023-T024) → Final verification

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Total: 24 tasks across 2 files (both modified, no new files)
- All changes are additive — no existing code is modified
- The existing `createTimedOut2Generator` and `Out2TickBlock` are not touched
- All user stories are independent and can be executed in any order after foundational phase
