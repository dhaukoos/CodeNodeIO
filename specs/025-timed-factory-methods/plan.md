# Implementation Plan: Timed Factory Methods

**Branch**: `025-timed-factory-methods` | **Date**: 2026-02-20 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/025-timed-factory-methods/spec.md`

## Summary

Add timed wrapper factory methods for all continuous node types (generators, processors, sinks) following the existing `createTimedOut2Generator` pattern. Each timed method accepts a `tickIntervalMs` and a `tick` function, wraps the tick in a delay loop, and delegates to the corresponding continuous factory method. This adds 15 new factory methods and 15 new tick type aliases across 2 files.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP - Kotlin Multiplatform)
**Primary Dependencies**: kotlinx-coroutines 1.8.0, kotlinx-serialization 1.6.0
**Storage**: N/A (in-memory factory methods only)
**Testing**: kotlin.test + JUnit (fbpDsl commonTest)
**Target Platform**: All KMP targets (JVM, potentially native)
**Project Type**: Multi-module KMP project (fbpDsl library module)
**Performance Goals**: N/A (thin wrapper methods, no runtime overhead beyond the delay)
**Constraints**: Virtual time testing does not work with delay() compiled in commonMain (documented KMP limitation)
**Scale/Scope**: 15 new type aliases, 15 new factory methods, ~450 lines added across 2 files

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality | PASS | Each method follows the established pattern exactly. Public APIs include KDoc. Functions are short (< 15 lines). |
| II. Test-Driven Development | PASS | Each user story includes compilation and runtime verification tests. |
| III. UX Consistency | N/A | No user-facing UI changes. |
| IV. Performance | N/A | Thin wrapper methods with no performance impact beyond the intentional delay. |
| V. Observability | N/A | No runtime behavior changes beyond the delay. |
| Licensing | PASS | No new dependencies. All code is Apache 2.0. |

**Post-Design Re-check**: All gates still PASS. No unknowns remain after research.

## Project Structure

### Documentation (this feature)

```text
specs/025-timed-factory-methods/
├── plan.md              # This file
├── research.md          # Phase 0: pattern analysis
├── data-model.md        # Phase 1: type alias and method inventory
├── quickstart.md        # Phase 1: before/after examples
├── contracts/           # Phase 1: method signature contracts
│   └── method-signatures.md
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (files affected)

```text
fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/
├── runtime/
│   └── ContinuousTypes.kt          # MODIFY: add 15 tick type aliases
└── model/
    └── CodeNodeFactory.kt           # MODIFY: add 15 timed factory methods
```

**Structure Decision**: Both files already exist. No new files needed. All changes are additive — existing code is not modified.

## Design Decisions

### D1: Wrapper Pattern (not new runtime classes)

Each timed method is a thin wrapper that:
1. Creates a continuous block lambda containing `delay(tickIntervalMs); tick(...)`
2. Delegates to the corresponding continuous factory method

This avoids creating new runtime classes or modifying existing ones.

### D2: Dedicated Tick Type Aliases

Every timed method gets its own tick type alias even when the signature matches the continuous block. This provides:
- API clarity: `tick: TransformerTickBlock` vs `transform: ContinuousTransformBlock`
- Consistent naming: `{Arity}TickBlock` convention across all node types
- Future flexibility: tick blocks could diverge from continuous blocks if needed

### D3: Delay-Before-Tick Placement

- **Generators**: `while (isActive) { delay(tickIntervalMs); emit(tick()) }`
- **Processors**: `{ input -> delay(tickIntervalMs); tick(input) }` — wraps the process block
- **Sinks**: `{ value -> delay(tickIntervalMs); tick(value) }` — wraps the consume block

The delay happens before processing, ensuring consistent intervals.

### D4: Parameter Forwarding

- `channelCapacity` is forwarded only for methods that have it in their continuous counterpart (generators and multi-output processors)
- `position` and `description` are forwarded for all methods
- `tickIntervalMs` and `tick` are the only new parameters
