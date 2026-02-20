# Research: Timed Factory Methods

**Feature**: 025-timed-factory-methods
**Date**: 2026-02-20

## R1: Timed Wrapper Pattern Analysis

**Decision**: Follow the exact `createTimedOut2Generator` pattern — each timed method wraps a tick function and tick interval into the corresponding continuous method's block parameter.

**Rationale**: The existing `createTimedOut2Generator` (CodeNodeFactory.kt lines 507-533) establishes a proven pattern:
1. Accept `tickIntervalMs: Long` and `tick: TickBlock` parameters
2. Wrap the tick in a loop: `while (isActive) { delay(tickIntervalMs); emit/process/consume(tick(...)) }`
3. Delegate to the corresponding continuous factory method
4. Return the same runtime type

**Alternatives considered**:
- Custom timed runtime classes: Rejected — adds unnecessary class hierarchy when wrapping achieves the same result
- Decorator pattern on existing runtimes: Rejected — more complex, same outcome

## R2: Tick Block Signature Design

**Decision**: Create dedicated tick type aliases for each node arity, even when the signature matches the continuous block.

**Rationale**:
- For generators, tick blocks are genuinely different from continuous blocks (tick returns a value; continuous block receives an emit function and manages its own loop)
- For processors and sinks, tick block signatures happen to match continuous block signatures (`suspend (A, B) -> R`), but semantic distinction matters: a tick block is called once per interval, a continuous block is called as fast as possible
- Consistent naming convention: `{Arity}TickBlock` matches `{Arity}ProcessBlock`/`{Arity}GeneratorBlock`

**Signature mapping**:

| Node Type | Continuous Block | Tick Block |
|-----------|-----------------|------------|
| Generator<T> | `suspend (emit: suspend (T) -> Unit) -> Unit` | `suspend () -> T` |
| Out2Generator | `suspend (emit: suspend (PR2) -> Unit) -> Unit` | `suspend () -> PR2` |
| Out3Generator | `suspend (emit: suspend (PR3) -> Unit) -> Unit` | `suspend () -> PR3` |
| Transformer | `suspend (TIn) -> TOut` | `suspend (TIn) -> TOut` |
| Filter | `suspend (T) -> Boolean` | `suspend (T) -> Boolean` |
| In2Out1 | `suspend (A, B) -> R` | `suspend (A, B) -> R` |
| In3Out1 | `suspend (A, B, C) -> R` | `suspend (A, B, C) -> R` |
| In1Out2 | `suspend (A) -> PR2` | `suspend (A) -> PR2` |
| In1Out3 | `suspend (A) -> PR3` | `suspend (A) -> PR3` |
| In2Out2 | `suspend (A, B) -> PR2` | `suspend (A, B) -> PR2` |
| In2Out3 | `suspend (A, B) -> PR3` | `suspend (A, B) -> PR3` |
| In3Out2 | `suspend (A, B, C) -> PR2` | `suspend (A, B, C) -> PR2` |
| In3Out3 | `suspend (A, B, C) -> PR3` | `suspend (A, B, C) -> PR3` |
| Sink<T> | `suspend (T) -> Unit` | `suspend (T) -> Unit` |
| In2Sink | `suspend (A, B) -> Unit` | `suspend (A, B) -> Unit` |
| In3Sink | `suspend (A, B, C) -> Unit` | `suspend (A, B, C) -> Unit` |

(PR2 = ProcessResult2, PR3 = ProcessResult3)

## R3: Delay Placement in Timed Wrappers

**Decision**: Use delay-before-tick for generators, delay-before-tick for processors and sinks.

**Rationale**:
- Generators: `while (isActive) { delay(tickIntervalMs); emit(tick()) }` — delay first ensures consistent interval, tick returns the value
- Processors: Wrap the process block to delay before calling tick: `{ input -> delay(tickIntervalMs); tick(input) }` — the runtime still receives from the channel first, then delays, then processes
- Sinks: Same pattern as processors: `{ value -> delay(tickIntervalMs); tick(value) }`

**Note**: This means for processors/sinks, the sequence is: receive input → delay → process/consume. The delay is between receiving and acting, not between actions. This is the most natural fit since the continuous runtime manages the receive loop.

## R4: Virtual Time Testing Limitation

**Decision**: Document the same limitation as `createTimedOut2Generator` — timed wrappers compile their `delay()` call in the library module, so virtual time advancement in tests does not work. Tests requiring precise timing control should use the continuous factory methods with test-defined loops.

**Rationale**: This is a KMP limitation documented in MEMORY.md. The `delay()` call inside a lambda compiled in `commonMain` does not respond to `StandardTestDispatcher` virtual time advancement. Only lambdas written at the test call site work with virtual time.

## R5: Method Count and File Impact

**Decision**: All changes are confined to exactly 2 files.

**Analysis**:
- **ContinuousTypes.kt**: Add 15 new tick type aliases (1 already exists: `Out2TickBlock`)
- **CodeNodeFactory.kt**: Add 15 new timed factory methods (1 already exists: `createTimedOut2Generator`)

No new files needed. No other files affected. No cross-module impact.
