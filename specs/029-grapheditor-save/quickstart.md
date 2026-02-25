# Quickstart: GraphEditor Save Functionality

**Feature**: 029-grapheditor-save
**Date**: 2026-02-25

## Scenario: StopWatch3-like FlowGraph Save Lifecycle

### Input: FlowGraph with 2-output generator + 2-input sink

```
TimerEmitter (GENERATOR)          DisplayReceiver (SINK)
  outputs:                          inputs:
    - elapsedSeconds: Int             - seconds: Int
    - elapsedMinutes: Int             - minutes: Int
```

### Step 1: First Save (New FlowGraph)

1. User creates the StopWatch3 FlowGraph in graphEditor
2. User clicks Save
3. System shows directory chooser → user selects `/output/`
4. System creates full module:

```
/output/StopWatch3/
├── build.gradle.kts
├── settings.gradle.kts
├── StopWatch3.flow.kt
└── src/commonMain/kotlin/io/codenode/stopwatch3/
    ├── generated/
    │   ├── StopWatch3Flow.kt
    │   ├── StopWatch3Controller.kt
    │   ├── StopWatch3ControllerInterface.kt
    │   ├── StopWatch3ControllerAdapter.kt
    │   └── StopWatch3ViewModel.kt
    ├── processingLogic/
    │   ├── TimerEmitterProcessLogic.kt
    │   └── DisplayReceiverProcessLogic.kt
    └── stateProperties/
        ├── TimerEmitterStateProperties.kt
        └── DisplayReceiverStateProperties.kt
```

5. Save result reports: 12 files created, 0 overwritten, 0 deleted

### Step 2: Re-Save After Modification (Same Name)

1. User adds a third node: `Logger` (SINK, 1 input: `message: String`)
2. User connects TimerEmitter output to Logger input
3. User clicks Save
4. System saves directly to `/output/` (no directory prompt)
5. System:
   - Overwrites `StopWatch3.flow.kt` (now includes Logger node)
   - Overwrites all 5 runtime files in `generated/` (now reference Logger)
   - Preserves existing `TimerEmitterProcessLogic.kt` and `DisplayReceiverProcessLogic.kt`
   - Preserves existing `TimerEmitterStateProperties.kt` and `DisplayReceiverStateProperties.kt`
   - Creates new `LoggerProcessLogic.kt` and `LoggerStateProperties.kt`
6. Save result reports: 2 files created, 6 files overwritten, 0 deleted

### Step 3: Re-Save After Node Removal

1. User removes the `Logger` node from the FlowGraph
2. User clicks Save
3. System saves directly to `/output/` (no directory prompt)
4. System:
   - Overwrites `StopWatch3.flow.kt` (Logger removed)
   - Overwrites all 5 runtime files in `generated/` (Logger references removed)
   - Preserves existing stubs for TimerEmitter and DisplayReceiver
   - Deletes `LoggerProcessLogic.kt` from `processingLogic/`
   - Deletes `LoggerStateProperties.kt` from `stateProperties/`
5. Save result reports: 0 files created, 6 files overwritten, 2 files deleted

### Step 4: Save Under New Name

1. User renames FlowGraph from "StopWatch3" to "StopWatch4"
2. User clicks Save
3. System shows directory chooser (new name = new save)
4. User selects `/output/`
5. System creates new `/output/StopWatch4/` module
6. Original `/output/StopWatch3/` is untouched

## Verification Checklist

1. First save prompts for directory and creates full module structure
2. Re-save (same name) skips directory prompt
3. .flow.kt is overwritten on every save
4. All 5 generated runtime files are overwritten on every save
5. Existing processing logic stubs are preserved (not overwritten)
6. Existing state properties stubs are preserved (not overwritten)
7. New stubs are created for added nodes
8. Orphaned stubs are deleted for removed nodes
9. Save result reports created, overwritten, and deleted files
10. Renaming FlowGraph triggers directory prompt (treated as new save)
11. If saved directory no longer exists, system re-prompts for directory
