# Research: Module DataFlow Refinements

**Feature**: 043-dataflow-refinements
**Date**: 2026-03-07

## R1: UserProfiles — Replace combine() with Selective Channel Emission

**Decision**: Replace the `combine()` + Unit sentinel source pattern with individual `StateFlow` collectors that emit on only the triggered channel. Switch the downstream `UserProfileRepository` from `In3Out2` (all-input) to `In3AnyOut2` (any-input) processor.

**Rationale**: The current `combine()` pattern forces all three channels to emit whenever any single action is triggered, requiring Unit sentinel values as placeholders. The `In3AnyOut2Runtime` already exists and was specifically designed for this use case — it fires when ANY input receives data, caches previous values, and processes with the latest state.

**Current flow**:
1. User presses Add → `_save` StateFlow updates
2. `combine(_save, _update, _remove)` fires → emits `ProcessResult3(saveValue, Unit, Unit)` on all 3 channels
3. `In3Out2` processor waits for all 3 values, then processes
4. Processor checks which value is real vs Unit sentinel

**New flow**:
1. User presses Add → `_save` StateFlow updates
2. Individual collector detects `_save` change → emits only on channel 1
3. `In3AnyOut2` processor fires immediately on the single input, using cached defaults for other channels
4. Processor processes the real value directly

**Alternatives considered**:
- Keep `combine()` but suppress Unit sentinel sends: Still creates unnecessary coupling between StateFlows. Rejected.
- Use three separate single-output sources: Creates more nodes and connections, overcomplicates the flow graph. Rejected.
- Use `merge()` instead of `combine()`: Loses the 3-output structure needed for channel-specific routing. Rejected.

## R2: UserProfiles — SourceOut3 to Individual Collectors

**Decision**: Replace the single `SourceOut3` node with three individual `StateFlow.collect` calls that each emit to only one output channel of the source.

**Rationale**: The `SourceOut3Runtime.generate` lambda receives an `emit: (ProcessResult3) -> Unit` callback. Currently, `combine()` fires this with all three values. Instead, we can listen to each StateFlow independently and emit `ProcessResult3` with only the changed value (others null). The SourceOut3Runtime already supports selective output — it checks `result.out1?.let { outputChannel1.send(it) }` for each channel.

**Key insight**: `ProcessResult3` has nullable fields (`out1: U?`, `out2: V?`, `out3: W?`) and the `SourceOut3Runtime` only sends non-null values. So `ProcessResult3(saveValue, null, null)` correctly sends only on channel 1.

## R3: StopWatch — Selective ProcessResult2 Output

**Decision**: In `timeIncrementerTick`, use `ProcessResult2.first(newSeconds)` when minutes hasn't changed, and `ProcessResult2.both(newSeconds, newMinutes)` when minutes changes.

**Rationale**: `ProcessResult2` already has nullable fields and the `In2Out2Runtime` only sends non-null values (`result.out1?.let { out1.send(it) }`, `result.out2?.let { out2.send(it) }`). The helper methods `ProcessResult2.first()` and `ProcessResult2.both()` exist for exactly this purpose.

**Change**: 3 lines in `TimeIncrementerProcessLogic.kt`:
```
// Before:
ProcessResult2.both(newSeconds, newMinutes)

// After:
if (newMinutes != elapsedMinutes) {
    ProcessResult2.both(newSeconds, newMinutes)
} else {
    ProcessResult2.first(newSeconds)
}
```

**Alternatives considered**:
- Add `distinctUntilChanged()` on the minutes StateFlow: This would suppress downstream state updates but doesn't fix the emission issue at the source. Rejected.
- Track previous minutes in the runtime: Unnecessary — the current input value `elapsedMinutes` IS the previous value (before increment). Rejected — actually, the comparison `newMinutes != elapsedMinutes` is exactly this.

## R4: UserProfiles — In3AnyOut2 Initial Values

**Decision**: Use `Unit` as initial values for the `In3AnyOut2` processor since the processor needs defaults before first input. The processor's process block will treat Unit as "no action".

**Rationale**: `In3AnyOut2Runtime` requires `initialValue1`, `initialValue2`, `initialValue3` parameters. These are used as cached values before any input arrives. Using `Unit` as the initial value is consistent with the current sentinel approach and the processor already handles Unit values.

**Factory method**: `CodeNodeFactory.createIn3AnyOut2Processor(name, initialValue1, initialValue2, initialValue3, process = { ... })`
