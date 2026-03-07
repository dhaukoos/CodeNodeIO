# Implementation Plan: Module DataFlow Refinements

**Branch**: `043-dataflow-refinements` | **Date**: 2026-03-07 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/043-dataflow-refinements/spec.md`

## Summary

Refine data flow in two modules: (1) UserProfiles — replace the `combine()` + Unit sentinel source pattern with individual StateFlow collectors and switch the downstream processor to the existing `In3AnyOut2` "any-input" pattern so only the triggered channel emits; (2) StopWatch — use `ProcessResult2.first()` for selective output so minutes only emits when the value actually changes.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (Kotlin Multiplatform) + kotlinx-coroutines 1.8.0
**Primary Dependencies**: kotlinx-coroutines (channels, StateFlow, select), kotlinx-serialization 1.6.0
**Storage**: N/A (in-memory runtime state)
**Testing**: kotlinx-coroutines-test (runTest, virtual time)
**Target Platform**: JVM Desktop (Compose Desktop)
**Project Type**: KMP multiplatform with StopWatch and UserProfiles modules
**Performance Goals**: Reduce unnecessary channel emissions (UserProfiles: 3→1 per action, StopWatch: 60→1 minutes emissions per minute)
**Constraints**: Must not change external ViewModel API or UI behavior
**Scale/Scope**: 2 modules, ~4 files modified

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality First | PASS | Simplifies code by removing Unit sentinel workaround, uses purpose-built runtime pattern. |
| II. Test-Driven Development | PASS | Existing test patterns for any-input runtimes (AnyInputRuntimeTest.kt). Module behavior testable via manual runtime preview. |
| III. User Experience Consistency | PASS | No UI changes. Data flow animation becomes more accurate. |
| IV. Performance Requirements | PASS | Reduces unnecessary channel emissions. |
| V. Observability & Debugging | N/A | No logging changes needed. |
| Licensing & IP | PASS | No new dependencies. |

## Project Structure

### Documentation (this feature)

```text
specs/043-dataflow-refinements/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (repository root)

```text
StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/
├── processingLogic/
│   └── TimeIncrementerProcessLogic.kt    # MODIFY: Use ProcessResult2.first() when minutes unchanged
└── generated/
    └── StopWatchFlow.kt                   # READ-ONLY (no changes needed)

UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/
├── generated/
│   ├── UserProfilesFlow.kt               # MODIFY: Replace combine() source with individual collectors,
│   │                                      #   change processor to In3AnyOut2
│   └── UserProfilesController.kt         # MODIFY: Update start() to match new flow pattern
├── processingLogic/
│   └── UserProfileRepositoryProcessLogic.kt  # MODIFY: Adapt process block for any-input semantics
└── UserProfilesState.kt                  # READ-ONLY (state structure unchanged)

fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/
├── In3AnyOut2Runtime.kt                  # EXISTING: Any-input processor (already built)
├── ProcessResult.kt                      # EXISTING: ProcessResult2 with nullable selective output
└── SourceOut3Runtime.kt                  # EXISTING: Current 3-output source pattern
```

**Structure Decision**: Changes are within the existing StopWatch and UserProfiles modules. No new files created. The `In3AnyOut2Runtime` and `ProcessResult2` selective output features already exist in fbpDsl — this feature simply uses them.

## Constitution Check (Post-Design)

*Re-evaluated after Phase 1 design completion.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality First | PASS | Removes anti-pattern (Unit sentinels). Uses existing purpose-built infrastructure. |
| II. Test-Driven Development | PASS | Both changes are verifiable via runtime preview with data flow animation. |
| III. User Experience Consistency | PASS | More accurate data flow animation. No UI behavior changes. |
| IV. Performance Requirements | PASS | Strictly reduces unnecessary emissions. |
| V. Observability & Debugging | N/A | No change. |
| Licensing & IP | PASS | No new dependencies. |

## Complexity Tracking

No violations to justify.
