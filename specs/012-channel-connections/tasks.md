# Tasks: Channel-Based Connections

**Input**: Design documents from `/specs/012-channel-connections/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/channel-interface.md, quickstart.md

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **kotlinCompiler/**: Code generation for flows
- **StopWatch/**: Example flow module with components
- **fbpDsl/**: Connection model (reference only, no changes needed)

---

## Phase 1: Setup

**Purpose**: Verify existing infrastructure and prepare for Channel migration

- [x] T001 Verify Connection.channelCapacity property exists in fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/Connection.kt
- [x] T002 Review current ModuleGenerator channel creation at kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/ModuleGenerator.kt:405-407
- [x] T003 Review current wireConnections generation at kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/ModuleGenerator.kt:446-459

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core Channel infrastructure that MUST be complete before user story implementation

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T004 Add Channel import generation to kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/ModuleGenerator.kt (add kotlinx.coroutines.channels.Channel, SendChannel, ReceiveChannel imports to generated code)
- [x] T005 Create channel capacity mapping helper function in ModuleGenerator.kt that maps Connection.channelCapacity to Channel constructor argument (0→Channel.RENDEZVOUS, -1→Channel.UNLIMITED, N→N)

**Checkpoint**: Foundation ready - channel infrastructure in place

---

## Phase 3: User Story 1 - Channel Semantics for Data Flow (Priority: P1) 🎯 MVP

**Goal**: Connections between nodes use proper Kotlin Channel semantics with backpressure support

**Independent Test**: Run StopWatch flow and verify TimerEmitter sends data through a Channel to DisplayReceiver, with backpressure behavior when buffer is full

### Implementation for User Story 1

- [x] T006 [US1] Update ModuleGenerator.kt channel declaration to generate `Channel<Any>(capacity)` instead of `MutableSharedFlow<Any>(replay = 1)` at kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/ModuleGenerator.kt:405-407
- [x] T007 [US1] Update ModuleGenerator.kt wireConnections to assign channels directly to component SendChannel/ReceiveChannel properties instead of using collect/emit pattern at kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/ModuleGenerator.kt:446-459
- [x] T008 [US1] Add channel close() calls to generated stop() method in ModuleGenerator.kt for graceful shutdown
- [x] T009 [US1] Write unit test for channel capacity mapping in kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/ChannelCapacityTest.kt
- [x] T010 [US1] Write backpressure verification test in kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/ChannelBackpressureTest.kt

**Checkpoint**: Channel semantics implemented in generator - core FBP behavior verified

---

## Phase 4: User Story 2 - Generated Code Uses Channels (Priority: P2)

**Goal**: Code generator produces channel-based connection wiring that compiles and runs correctly

**Independent Test**: Generate StopWatch module and verify generated FlowGenerator code creates Channels, compiles without errors

### Implementation for User Story 2

- [x] T011 [US2] Update FlowGenerator.kt to include Channel imports in generated flow class at kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/FlowGenerator.kt
- [x] T012 [US2] Update ComponentGenerator.kt to generate SendChannel/ReceiveChannel properties instead of MutableSharedFlow at kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/ComponentGenerator.kt
- [x] T013 [US2] Regenerate StopWatchFlow.kt to verify generated code compiles with channel-based connections
- [x] T014 [US2] Verify generated code handles ClosedSendChannelException and ClosedReceiveChannelException gracefully
- [x] T015 [US2] Run existing ModuleGeneratorTest to ensure backward compatibility at kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/ModuleGeneratorTest.kt

**Checkpoint**: Generated code compiles and uses proper channel patterns

---

## Phase 5: User Story 3 - Component Channel Integration (Priority: P3)

**Goal**: Components use SendChannel/ReceiveChannel interfaces for data flow

**Independent Test**: Update TimerEmitterComponent and DisplayReceiverComponent to use channels and verify StopWatch functions correctly

### Implementation for User Story 3

- [x] T016 [P] [US3] Update TimerEmitterComponent to use SendChannel<TimerOutput> output pattern at StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/usecases/TimerEmitterComponent.kt
- [x] T017 [P] [US3] Update DisplayReceiverComponent to use ReceiveChannel<TimerOutput> input pattern with for-loop iteration at StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/usecases/DisplayReceiverComponent.kt
- [x] T018 [US3] Update component stop() methods to properly close output channels and handle channel closure
- [x] T019 [US3] Run StopWatch integration tests to verify timer emitter and display receiver work with channels at StopWatch/src/commonTest/kotlin/io/codenode/stopwatch/
- [x] T020 [US3] Verify graceful shutdown - buffered data consumed before channel reports closed

**Checkpoint**: Components fully integrated with channel-based architecture

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final validation and cleanup

- [ ] T021 [P] Run all kotlinCompiler tests: `./gradlew :kotlinCompiler:jvmTest`
- [ ] T022 [P] Run all StopWatch tests: `./gradlew :StopWatch:jvmTest`
- [ ] T023 Run full StopWatch flow end-to-end to verify complete data flow
- [ ] T024 Review and update quickstart.md verification checklist at specs/012-channel-connections/quickstart.md
- [ ] T025 Clean up any remaining MutableSharedFlow references in StopWatch module

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-5)**: All depend on Foundational phase completion
  - US1 (Channel Semantics) must complete before US2 (Generated Code)
  - US2 (Generated Code) must complete before US3 (Component Integration)
- **Polish (Phase 6)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - Core channel infrastructure
- **User Story 2 (P2)**: Depends on US1 - Code generation uses channel patterns from US1
- **User Story 3 (P3)**: Depends on US2 - Components integrate with generated channel code

### Within Each User Story

- Core implementation tasks before tests
- Generator changes before component changes
- Channel creation before channel wiring
- Channel wiring before graceful shutdown

### Parallel Opportunities

- T001, T002, T003 can run in parallel (review only)
- T004, T005 must run sequentially (same file)
- T016, T017 can run in parallel (different files)
- T021, T022 can run in parallel (different modules)

---

## Parallel Example: User Story 3

```bash
# Launch component updates together:
Task: "Update TimerEmitterComponent in StopWatch/.../TimerEmitterComponent.kt"
Task: "Update DisplayReceiverComponent in StopWatch/.../DisplayReceiverComponent.kt"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (verification)
2. Complete Phase 2: Foundational (channel infrastructure)
3. Complete Phase 3: User Story 1 (channel semantics)
4. **STOP and VALIDATE**: Test channel creation and backpressure
5. Verify with unit tests before proceeding

### Incremental Delivery

1. Complete Setup + Foundational → Channel infrastructure ready
2. Add User Story 1 → Channel semantics verified → Core FBP behavior works
3. Add User Story 2 → Generated code compiles → Test generation
4. Add User Story 3 → Components integrated → Full StopWatch flow works
5. Each story adds value and builds on previous stories

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- User stories in this feature have sequential dependencies (US1→US2→US3)
- Verify tests pass after each user story before proceeding
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
