# Research: Reactive Feedback Loop for Source Nodes

**Feature Branch**: `038-reactive-source-loop`
**Date**: 2026-03-05

## R1: Feedback Loop Mechanism — StateFlow Observation vs Timed Tick Loops

### Decision
Source nodes reactively observe their corresponding `StateFlow` properties and re-emit when state changes, rather than using timed tick loops (which were removed in feature 037).

### Rationale
Timed tick loops were removed in 037-source-sink-refactor because source nodes became passive ViewModel-driven entry points. However, the stopwatch use case requires a self-perpetuating pipeline. Reactive StateFlow observation solves this: the processor updates state, the source observes the state change and re-emits, creating a feedback loop without reintroducing timed generators. This is event-driven (only runs when state changes) and leverages existing coroutines infrastructure (`combine`, `collect`, `drop`).

### Alternatives Considered
- **Reintroduce timed tick loops**: Rejected — contradicts the architectural direction of 037 and adds unnecessary complexity.
- **ViewModel-driven re-emission**: Rejected — couples the flow graph to the UI layer; feedback should be internal to the flow pipeline.
- **Channel-based feedback (direct wiring)**: Rejected — would require new channel types and break the source/processor/sink topology.

## R2: Attenuation Delay Placement — Processor vs Source

### Decision
The attenuation delay (`attenuationDelayMs`) is applied in processor runtimes (nodes with both inputs and outputs), not in source nodes.

### Rationale
The delay controls the cycle rate of the feedback loop. Placing it in the processor (between receive and process) means:
1. Source nodes remain simple reactive observers — they emit immediately when state changes.
2. The delay naturally throttles the entire loop since the processor is the bottleneck.
3. The same delay property works for all processor types (transformer, filter, InXOutY, InXAnyOutY).
4. When no delay is configured (null default), existing behavior is preserved with zero overhead.

### Alternatives Considered
- **Delay in source nodes**: Rejected — source nodes have no receive step; delay would need a timer, reintroducing timed behavior.
- **Delay in sink nodes**: Rejected — sinks are terminal; delaying them doesn't prevent the processor from spinning.
- **Delay in channel send/receive**: Rejected — would require modifying channel infrastructure and affect all channels globally.

## R3: Initial Emission Strategy — Controller Priming vs Auto-Emit

### Decision
The controller primes source node output channels on `start()` by sending the current state values. Source nodes use `drop(1)` to skip the initial `combine` emission.

### Rationale
When a source node starts observing StateFlows via `combine`, it immediately receives the current values as the first emission. But this creates a race condition: the source may emit before downstream processors are ready. Controller priming solves this by:
1. Starting all nodes first (sinks before processors before sources).
2. Explicitly sending initial state values into source output channels after start.
3. Using `drop(1)` in the source's reactive block to skip the redundant initial combine emission.

This ensures deterministic startup: the controller controls when the first values enter the pipeline.

### Alternatives Considered
- **Auto-emit on start (no drop)**: Rejected — creates a race condition where the source emits before processors are started.
- **Delayed start for sources**: Rejected — adds timing-dependent behavior; priming is explicit and deterministic.
- **No priming, rely on initial combine emission**: Rejected — the combine emission fires immediately, before the controller has finished wiring; priming after `start()` is safer.

## R4: `drop(1)` for Skipping Initial Combine Emission

### Decision
Use `drop(1)` on the `combine` flow to skip the initial emission from `StateFlow.combine()`.

### Rationale
`StateFlow` always has a current value, so `combine(flow1, flow2) { ... }` immediately emits once with the current values. Since the controller primes the source channels separately, this initial emission is redundant and would cause a double-emission on startup. `drop(1)` is the standard Kotlin Flow operator for skipping the first N emissions.

### Alternatives Considered
- **`distinctUntilChanged`**: Rejected — doesn't help if the first emission and the primed values are identical (they would be).
- **Boolean flag in generate block**: Rejected — more complex, stateful, and not idiomatic Kotlin Flow.
- **Use `SharedFlow` instead of `StateFlow`**: Rejected — `StateFlow` is already the established pattern; changing would affect the entire state architecture.

## R5: Files Requiring Changes

### Source Code Files (Active Code)

**fbpDsl module — Runtime classes (add attenuationDelayMs):**
1. `TransformerRuntime.kt` — Standard processor: insert delay between receive and transform
2. `FilterRuntime.kt` — Standard processor: insert delay between receive and predicate
3. `In1Out2Runtime.kt` — Standard processor: insert delay between receive and process
4. `In1Out3Runtime.kt` — Standard processor: insert delay between receive and process
5. `In2Out1Runtime.kt` — Standard processor: insert delay after both receives, before process
6. `In2Out2Runtime.kt` — Standard processor: insert delay after both receives, before process
7. `In2Out3Runtime.kt` — Standard processor: insert delay after both receives, before process
8. `In3Out1Runtime.kt` — Standard processor: insert delay after all receives, before process
9. `In3Out2Runtime.kt` — Standard processor: insert delay after all receives, before process
10. `In3Out3Runtime.kt` — Standard processor: insert delay after all receives, before process
11. `In2AnyOut1Runtime.kt` — Any-input: insert delay in each select branch, after lastValue update, before process
12. `In2AnyOut2Runtime.kt` — Any-input: insert delay in each select branch
13. `In2AnyOut3Runtime.kt` — Any-input: insert delay in each select branch
14. `In3AnyOut1Runtime.kt` — Any-input: insert delay in each select branch
15. `In3AnyOut2Runtime.kt` — Any-input: insert delay in each select branch
16. `In3AnyOut3Runtime.kt` — Any-input: insert delay in each select branch

**fbpDsl test files:**
17. `TypedNodeRuntimeTest.kt` — Add attenuationDelayMs tests

**kotlinCompiler module — Code generators:**
18. `RuntimeFlowGenerator.kt` — Reactive source generate blocks (replace awaitCancellation)
19. `RuntimeControllerGenerator.kt` — Priming logic + attenuation propagation to all nodes

**kotlinCompiler test files:**
20. `RuntimeFlowGeneratorTest.kt` — Update source block assertions
21. `RuntimeControllerGeneratorTest.kt` — Update attenuation + add priming tests

**graphEditor module:**
22. `ModuleSaveService.kt` — Pass viewModelPackage to controller generator

**StopWatch module:**
23. `TimeIncrementerProcessLogic.kt` — Use input values instead of reading state directly

### Documentation Files (specs/)
Specs are historical documentation. They should NOT be modified as part of this feature.
