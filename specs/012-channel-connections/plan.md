# Implementation Plan: Channel-Based Connections

**Branch**: `012-channel-connections` | **Date**: 2026-02-12 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/012-channel-connections/spec.md`

## Summary

Refactor CodeNodeIO to use raw Kotlin `Channel<T>` from kotlinx.coroutines.channels for implementing Connections between nodes. This replaces the current hardcoded `MutableSharedFlow<Any>(replay = 1)` with proper FBP point-to-point channel semantics. The infrastructure largely exists - the Connection model already has a `channelCapacity` property, DSL supports `connectBuffered()`, and tests specify capacity values. The primary work is updating code generation to create Channels with proper capacity and updating components to use `SendChannel`/`ReceiveChannel` interfaces.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP - Kotlin Multiplatform)
**Primary Dependencies**: kotlinx-coroutines 1.8.0, kotlinx-serialization 1.6.0, KotlinPoet (code generation)
**Storage**: N/A (in-memory FlowGraph models, generated code)
**Testing**: JUnit 5, Kotlin Test (via `./gradlew :kotlinCompiler:jvmTest :StopWatch:jvmTest`)
**Target Platform**: JVM Desktop (graphEditor/kotlinCompiler), Android/iOS (KMPMobileApp)
**Project Type**: Multi-module KMP project
**Performance Goals**: Channel backpressure should not introduce >10ms latency overhead
**Constraints**: Must maintain backward compatibility with existing flows; generated code must compile without modification
**Scale/Scope**: ~5 affected files in kotlinCompiler module, 2 component files in StopWatch module

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality First | ✅ PASS | Improves code by using infrastructure that already exists |
| II. Test-Driven Development | ✅ PASS | Tests already exist for channelCapacity; will add channel-specific tests |
| III. User Experience Consistency | ✅ PASS | No user-facing changes; internal refactoring |
| IV. Performance Requirements | ✅ PASS | Channel backpressure is standard coroutine behavior |
| V. Observability & Debugging | ✅ PASS | Channel state can be observed via coroutine debugging |
| Licensing & IP | ✅ PASS | kotlinx-coroutines is Apache 2.0 licensed |

**Quality Gates**:
- All existing tests must pass after refactoring
- Generated code must compile without errors
- StopWatch flow must function correctly with channel-based connections

## Project Structure

### Documentation (this feature)

```text
specs/012-channel-connections/
├── plan.md              # This file
├── research.md          # Phase 0 output - architecture analysis
├── data-model.md        # Phase 1 output - channel mapping
├── quickstart.md        # Phase 1 output - implementation guide
├── contracts/           # Phase 1 output - channel interface contracts
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
kotlinCompiler/
├── src/commonMain/kotlin/io/codenode/kotlincompiler/generator/
│   ├── ModuleGenerator.kt     # MODIFY: Channel initialization with capacity
│   ├── FlowGenerator.kt       # MODIFY: Connection wiring with channels
│   └── ComponentGenerator.kt  # REVIEW: Component port generation
└── src/commonTest/kotlin/
    └── generator/
        └── ChannelConnectionTest.kt  # NEW: Channel capacity tests

StopWatch/
├── src/commonMain/kotlin/io/codenode/stopwatch/
│   ├── usecases/
│   │   ├── TimerEmitterComponent.kt    # MODIFY: Use SendChannel pattern
│   │   └── DisplayReceiverComponent.kt # MODIFY: Use ReceiveChannel pattern
│   └── generated/
│       └── StopWatchFlow.kt            # REGENERATE: After generator changes
└── src/commonTest/kotlin/
    └── ChannelBackpressureTest.kt      # NEW: Backpressure verification

fbpDsl/
└── src/commonMain/kotlin/io/codenode/fbpdsl/model/
    └── Connection.kt                   # Reference only (no changes needed)
```

**Structure Decision**: Existing multi-module structure maintained. Changes primarily in kotlinCompiler generator code, with component updates in StopWatch module.

## Complexity Tracking

No constitutional violations. The infrastructure already exists:
- Connection.channelCapacity property (defined, unused)
- ConnectionFactory.createBuffered() method (implemented)
- FlowGraphDsl.connectBuffered() syntax (working)
- Tests already specify capacity values

This is a "wire up what's already there" refactoring.

## Phase 0 Findings

Based on codebase exploration:

### Current Implementation

1. **Channel Declaration** (ModuleGenerator.kt:406): Hardcoded `MutableSharedFlow<Any>(replay = 1)` - ignores connection.channelCapacity
2. **Connection Wiring** (ModuleGenerator.kt:446-459): Direct collect/emit between component flows instead of using intermediate channels
3. **Component Ports** (TimerEmitterComponent.kt:52, DisplayReceiverComponent.kt:30): Hardcoded `MutableSharedFlow<TimerOutput>(replay = 1)`

### Capacity Mapping Strategy

| Connection.channelCapacity | Channel Configuration | Behavior |
|---------------------------|----------------------|----------|
| 0 (rendezvous) | `Channel(Channel.RENDEZVOUS)` | Sender suspends until receiver ready |
| 1-N (buffered) | `Channel(N)` | N packets buffered, then sender suspends |
| -1 (unlimited) | `Channel(Channel.UNLIMITED)` | No backpressure (use with caution) |

**Key Insight**: Raw `Channel<T>` provides true FBP point-to-point semantics with single-consumer guarantee, explicit `close()` for graceful shutdown, and built-in backpressure via suspending `send()`.

### Files Requiring Changes

| File | Lines | Change |
|------|-------|--------|
| ModuleGenerator.kt | 405-407 | Generate `Channel<Any>(capacity)` instead of `MutableSharedFlow` |
| ModuleGenerator.kt | 446-459 | Wire channels directly to components via `SendChannel`/`ReceiveChannel` |
| FlowGenerator.kt | 183-209 | Add Channel imports, update wiring code generation |
| TimerEmitterComponent.kt | 52 | Replace `output: MutableSharedFlow` with `outputChannel: SendChannel` |
| DisplayReceiverComponent.kt | 30, 68-71 | Replace `input: MutableSharedFlow` with `inputChannel: ReceiveChannel` |
| StopWatchFlow.kt | generated | Regenerate after generator changes |

## Post-Design Constitution Check

*Re-evaluated after Phase 1 design completion.*

| Principle | Status | Design Validation |
|-----------|--------|-------------------|
| I. Code Quality First | ✅ PASS | Single responsibility: channels handle buffering, components handle logic |
| II. Test-Driven Development | ✅ PASS | Backpressure tests defined in contracts |
| III. User Experience Consistency | ✅ PASS | No UI changes |
| IV. Performance Requirements | ✅ PASS | Channel overhead is standard coroutine behavior |
| V. Observability & Debugging | ✅ PASS | ClosedSendChannelException provides clear failure indication |
| Licensing & IP | ✅ PASS | No new dependencies |

**Design Artifacts Generated**:
- `research.md` - Architecture analysis and capacity mapping strategy
- `data-model.md` - Channel capacity mapping rules
- `contracts/channel-interface.md` - Component channel interface contract
- `quickstart.md` - Step-by-step implementation guide

## Next Steps

Run `/speckit.tasks` to generate implementation tasks from this plan.
