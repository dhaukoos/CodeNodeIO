# Implementation Plan: Modify Source and Sink Nodes

**Branch**: `037-source-sink-refactor` | **Date**: 2026-03-04 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/037-source-sink-refactor/spec.md`

## Summary

Rename "generator" to "source" throughout the codebase, change source nodes from autonomous tick-based producers to passive ViewModel-driven data entry points, remove processing logic stubs for both source and sink nodes, and apply consistent `{Role}{Direction}{Count}` naming to multi-input/output runtime classes. Approximately 60 source files across 3 modules require changes.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (Kotlin Multiplatform)
**Primary Dependencies**: kotlinx-coroutines 1.8.0, kotlinx-serialization 1.6.0, Compose Multiplatform 1.7.3, lifecycle-viewmodel-compose 2.8.0
**Storage**: N/A (in-memory models, generated source code files)
**Testing**: Kotlin Test (commonTest), JVM test runner via Gradle
**Target Platform**: JVM (Compose Desktop), with KMP common source sets
**Project Type**: Multi-module KMP project (fbpDsl, kotlinCompiler, graphEditor)
**Performance Goals**: N/A (refactoring — no performance-sensitive changes)
**Constraints**: Must maintain backward compatibility for existing `.flow.kt` files (deserialize `"GENERATOR"` as `SOURCE`)
**Scale/Scope**: ~60 source files across 3 modules + 5 generated modules

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality First | PASS | Rename improves readability; consistent naming convention |
| II. Test-Driven Development | PASS | All existing tests updated; new tests for behavioral changes |
| III. User Experience Consistency | PASS | UI label "Generator" → "Source" — consistent terminology |
| IV. Performance Requirements | N/A | No performance-sensitive changes |
| V. Observability & Debugging | N/A | Internal refactoring only |
| Licensing & IP | PASS | No new dependencies |

**Post-Phase 1 Re-check**: All gates still pass. No new dependencies, no architectural pattern violations.

## Project Structure

### Documentation (this feature)

```text
specs/037-source-sink-refactor/
├── spec.md
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
└── tasks.md             # Phase 2 output (created by /speckit.tasks)
```

### Source Code (repository root)

```text
fbpDsl/src/
├── commonMain/kotlin/io/codenode/fbpdsl/
│   ├── model/
│   │   ├── CodeNode.kt              # GENERATOR → SOURCE enum
│   │   └── CodeNodeFactory.kt       # Rename factory methods, remove timed variants
│   └── runtime/
│       ├── SourceRuntime.kt          # Renamed from GeneratorRuntime.kt
│       ├── SourceOut2Runtime.kt      # Renamed from Out2GeneratorRuntime.kt
│       ├── SourceOut3Runtime.kt      # Renamed from Out3GeneratorRuntime.kt
│       ├── SinkRuntime.kt            # Unchanged
│       ├── SinkIn2Runtime.kt         # Renamed from In2SinkRuntime.kt
│       ├── SinkIn3Runtime.kt         # Renamed from In3SinkRuntime.kt
│       ├── SinkIn2AnyRuntime.kt      # Renamed from In2AnySinkRuntime.kt
│       ├── SinkIn3AnyRuntime.kt      # Renamed from In3AnySinkRuntime.kt
│       └── ContinuousTypes.kt        # Rename type aliases, remove tick types
└── commonTest/kotlin/io/codenode/fbpdsl/
    └── runtime/                       # ~10 test files updated

kotlinCompiler/src/
├── commonMain/kotlin/io/codenode/kotlincompiler/
│   ├── generator/
│   │   ├── RuntimeTypeResolver.kt         # Update type/method mappings
│   │   ├── ProcessingLogicStubGenerator.kt # Skip source/sink nodes
│   │   ├── RuntimeFlowGenerator.kt         # Remove tick pattern for sources
│   │   ├── FlowKtGenerator.kt              # SOURCE in serialized output
│   │   ├── ComponentGenerator.kt            # Update references
│   │   ├── FlowGraphFactoryGenerator.kt     # Update references
│   │   └── SourceTemplate.kt               # Renamed from GeneratorTemplate.kt
│   └── ...
├── commonTest/kotlin/io/codenode/kotlincompiler/
│   └── generator/                     # ~15 test files updated
└── jvmMain/kotlin/.../tools/
    └── RegenerateStopWatch.kt          # Update references

graphEditor/src/
├── jvmMain/kotlin/
│   ├── rendering/NodeRenderer.kt       # GENERATOR → SOURCE label/color
│   ├── ui/DragAndDropHandler.kt         # GENERATOR → SOURCE mapping
│   ├── serialization/FlowGraphSerializer.kt # Handle GENERATOR→SOURCE compat
│   ├── save/ModuleSaveService.kt        # Update orphan detection
│   └── Main.kt                          # Update node type definitions
└── jvmTest/kotlin/
    └── save/ModuleSaveServiceTest.kt    # Update test references
```

**Structure Decision**: Existing multi-module KMP structure maintained. Changes span `fbpDsl` (runtime), `kotlinCompiler` (code generation), and `graphEditor` (UI/save). File renames in `fbpDsl/runtime/` are the most significant structural change.

## Implementation Strategy

### Phase 1: Core Rename (fbpDsl module)

The foundation — rename the enum, runtime classes, type aliases, and factory methods in the runtime layer. All downstream modules depend on these.

**Step 1.1: Rename `CodeNodeType.GENERATOR` → `SOURCE`**
- File: `fbpDsl/.../model/CodeNode.kt`
- Change enum value and KDoc description
- This will cause compile errors everywhere `GENERATOR` is referenced (good — forces us to find all references)

**Step 1.2: Rename runtime class files**
- Create new files with new names and updated class/content, delete old files
- 7 files renamed (see Project Structure above)
- Update class names, constructor references, and internal documentation

**Step 1.3: Rename type aliases in `ContinuousTypes.kt`**
- Rename continuous block types (Generator → Source, InXSink → SinkInX)
- Remove tick block types (GeneratorTickBlock, SinkTickBlock, OutXTickBlock, InXSinkTickBlock)

**Step 1.4: Rename factory methods in `CodeNodeFactory.kt`**
- Rename continuous factory methods
- Remove timed factory methods (createTimedGenerator, createTimedSink, etc.)
- Update internal `CodeNodeType.GENERATOR` → `CodeNodeType.SOURCE`

**Step 1.5: Update fbpDsl tests**
- Update all test files to use new names
- ~10 test files in `fbpDsl/src/commonTest/`
- Compile and run: `./gradlew :fbpDsl:jvmTest`

### Phase 2: Code Generator Updates (kotlinCompiler module)

Update the code generation layer to use new names and implement behavioral changes (no source stubs, no sink stubs, no tick loops for sources).

**Step 2.1: Update `RuntimeTypeResolver.kt`**
- Update `getFactoryMethodName()` to return new method names
- Update `getRuntimeTypeName()` to return new class names
- Remove timed factory method mappings

**Step 2.2: Update `ProcessingLogicStubGenerator.kt`**
- Add filtering in `shouldGenerateStub()` to return `false` for SOURCE and SINK nodes
- Update type alias references for remaining node types

**Step 2.3: Update `RuntimeFlowGenerator.kt`**
- Remove timed tick pattern for source nodes (no `tickIntervalMs`, no `tick = ...Tick`)
- Remove tick call from sink consume blocks (only state update remains)
- Update factory method names in generated code

**Step 2.4: Update remaining generators**
- `FlowKtGenerator.kt` — emit `"SOURCE"` instead of `"GENERATOR"`
- `ComponentGenerator.kt` — update GENERATOR references
- `FlowGraphFactoryGenerator.kt` — update factory method references
- `GeneratorTemplate.kt` → rename to `SourceTemplate.kt`

**Step 2.5: Update kotlinCompiler tests**
- ~15 test files
- Compile and run: `./gradlew :kotlinCompiler:jvmTest`

### Phase 3: Graph Editor Updates

Update the UI, serialization, and save service.

**Step 3.1: Update UI components**
- `NodeRenderer.kt` — `GENERATOR` → `SOURCE` in color mapping and labels
- `DragAndDropHandler.kt` — `GENERATOR` → `SOURCE` in type mapping
- `Main.kt` — Update node type definitions and category mappings

**Step 3.2: Update `FlowGraphSerializer.kt`**
- Deserialize both `"GENERATOR"` (legacy) and `"SOURCE"` (new) to `CodeNodeType.SOURCE`
- Serialize as `"SOURCE"`

**Step 3.3: Update `ModuleSaveService.kt`**
- Update orphan detection to also delete source/sink stub files
- The `shouldGenerateStub()` change in Phase 2 prevents new stubs; orphan detection cleans up old ones

**Step 3.4: Update graphEditor tests**
- `ModuleSaveServiceTest.kt`, `RequiredPropertyValidatorTest.kt`, `CompilationValidatorTest.kt`
- Compile and run: `./gradlew :graphEditor:jvmTest`

### Phase 4: Existing Module Updates

Update the generated modules that reference old names.

**Step 4.1: StopWatch and StopWatchV2 modules**
- Update processing logic stubs that reference `GeneratorTickBlock`, `SinkTickBlock`
- These stubs will become orphaned when the modules are re-saved

**Step 4.2: UserProfiles and RepositoryPattern modules**
- Same pattern as Step 4.1

**Step 4.3: StopWatchOriginal and KMPMobileApp**
- `StopWatchOriginal` has hand-written components referencing old names
- `KMPMobileApp` has integration test referencing old names

**Step 4.4: RegenerateStopWatch tool**
- Update `kotlinCompiler/.../tools/RegenerateStopWatch.kt`

### Phase 5: Final Verification

**Step 5.1: Full build**
```bash
./gradlew build
```

**Step 5.2: Verify zero "Generator" references in active code**
- Search for `GeneratorRuntime`, `GENERATOR`, `createTimedGenerator`, etc.
- Only historical spec docs and comments should remain

## Risk Assessment

| Risk | Impact | Mitigation |
|------|--------|------------|
| Missing a GENERATOR reference causes compile error | Low | Compile errors are self-revealing; systematic search before committing |
| Existing .flow.kt files break on deserialization | Medium | Add backward-compatible parsing of "GENERATOR" → SOURCE in FlowGraphSerializer |
| StopWatchOriginal hand-written code breaks | Low | Update manually; this module is a reference, not actively developed |
| Generated modules' processing logic stubs become orphaned | Low | Orphan deletion in ModuleSaveService handles cleanup on next save |
