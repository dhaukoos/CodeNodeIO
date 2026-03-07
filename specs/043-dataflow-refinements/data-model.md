# Data Model: Module DataFlow Refinements

**Feature**: 043-dataflow-refinements
**Date**: 2026-03-07

## No New Entities

This feature modifies the data flow behavior of existing modules without introducing new entities. The changes affect how existing data flows through channels, not what data is stored or represented.

## Behavioral Changes

### UserProfiles Channel Emission

| Scenario | Before (All Channels) | After (Selective) |
|----------|----------------------|-------------------|
| Add pressed | emit(save, Unit, Unit) on 3 channels | emit(save, null, null) on 1 channel |
| Update pressed | emit(Unit, update, Unit) on 3 channels | emit(null, update, null) on 1 channel |
| Remove pressed | emit(Unit, Unit, remove) on 3 channels | emit(null, null, remove) on 1 channel |

### UserProfiles Processor Trigger

| Aspect | Before (In3Out2) | After (In3AnyOut2) |
|--------|------------------|---------------------|
| Trigger | Waits for all 3 inputs | Fires on any 1 input |
| Caching | No caching | Caches previous values |
| Initial state | N/A | Unit for all 3 channels |

### StopWatch Minutes Emission

| Tick | Seconds Change | Minutes Change | Before | After |
|------|---------------|----------------|--------|-------|
| 0→1 | Yes | No | Both emit | Seconds only |
| 1→2 | Yes | No | Both emit | Seconds only |
| ... | ... | ... | ... | ... |
| 58→59 | Yes | No | Both emit | Seconds only |
| 59→0 | Yes | Yes (N→N+1) | Both emit | Both emit |
