# Implementation Plan: StopWatch Code Generation Refactor

**Branch**: `009-stopwatch-codegen-refactor` | **Date**: 2026-02-11 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/009-stopwatch-codegen-refactor/spec.md`

## Summary

This feature refactors the StopWatch virtual circuit so that the `createStopWatchFlowGraph()` function is generated rather than hand-coded. The implementation adds:

1. A file browser property editor in the PropertiesPanel for selecting ProcessingLogic implementation files
2. Pre-compilation validation ensuring all CodeNodes have required properties configured
3. A factory function generator that produces `createXXXFlowGraph()` in generated code
4. Migration of KMPMobileApp to use the generated code instead of manual FlowGraph construction

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP - Kotlin Multiplatform)
**Primary Dependencies**: Compose Desktop 1.7.3, kotlinx-coroutines 1.8.0, kotlinx-serialization 1.6.0, KotlinPoet (code generation)
**Storage**: .flow.kts files (text-based DSL serialization)
**Testing**: JUnit5 with kotlinx-coroutines-test
**Target Platform**: JVM (graphEditor), KMP (StopWatch module, KMPMobileApp)
**Project Type**: Multi-module KMP monorepo
**Performance Goals**: UI responsiveness (<100ms for file browser dialog), code generation (<5s for typical graphs)
**Constraints**: Relative file paths for portability, Apache 2.0 license compliance
**Scale/Scope**: StopWatch demo with 2 CodeNodes, extensible to larger graphs

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality First | ✅ PASS | Public APIs documented, single responsibility maintained |
| II. Test-Driven Development | ✅ PASS | Tests required for new editor, validation, and code generation |
| III. User Experience Consistency | ✅ PASS | File browser follows platform conventions, validation errors inline |
| IV. Performance Requirements | ✅ PASS | File dialog synchronous, generation <5s acceptable |
| V. Observability & Debugging | ✅ PASS | Validation errors enumerated with context |
| Licensing & IP | ✅ PASS | Using JFileChooser (JDK), KotlinPoet (Apache 2.0) |

## Project Structure

### Documentation (this feature)

```text
specs/009-stopwatch-codegen-refactor/
├── spec.md              # Feature specification
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   └── processing-logic-editor-contract.md
└── tasks.md             # Phase 2 output (via /speckit.tasks)
```

### Source Code (repository root)

```text
graphEditor/
├── src/jvmMain/kotlin/
│   ├── ui/
│   │   ├── PropertiesPanel.kt        # Add FILE_BROWSER editor type
│   │   └── PropertyEditors.kt        # Add FileBrowserEditor component
│   ├── state/
│   │   └── GraphState.kt             # Add processingLogicFile config key
│   ├── compilation/
│   │   ├── CompilationService.kt     # Add pre-compile validation
│   │   └── CompilationValidator.kt   # NEW: Required property validation
│   └── serialization/
│       └── FlowGraphSerializer.kt    # Already handles config serialization
└── src/jvmTest/kotlin/
    ├── ui/
    │   └── FileBrowserEditorTest.kt  # NEW: Editor tests
    └── compilation/
        └── CompilationValidatorTest.kt # NEW: Validation tests

kotlinCompiler/
├── src/commonMain/kotlin/
│   └── io/codenode/kotlincompiler/generator/
│       └── FlowGraphFactoryGenerator.kt # NEW: Factory function generation
└── src/commonTest/kotlin/
    └── io/codenode/kotlincompiler/generator/
        └── FlowGraphFactoryGeneratorTest.kt # NEW: Generator tests

StopWatch/
└── src/commonMain/kotlin/
    └── io/codenode/generated/stopwatch/
        └── StopWatchFlow.kt          # MODIFY: Add createStopWatchFlowGraph()

KMPMobileApp/
└── src/commonMain/kotlin/
    └── io/codenode/mobileapp/
        └── App.kt                    # MODIFY: Import generated factory function

demos/stopwatch/
├── StopWatch.flow.kts               # MODIFY: Add processingLogicFile configs
├── TimerEmitterComponent.kt          # Existing ProcessingLogic implementation
└── DisplayReceiverComponent.kt       # Existing ProcessingLogic implementation
```

**Structure Decision**: Using existing multi-module KMP structure. New code goes in graphEditor (UI/validation) and kotlinCompiler (code generation). Integration modifies StopWatch module and KMPMobileApp.

## Complexity Tracking

> No violations. Standard pattern extension.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| N/A | N/A | N/A |

## Phase 0: Research Summary

### R1: File Browser Integration in Compose Desktop

**Decision**: Use JFileChooser from Swing (JDK included)
**Rationale**: Already used in graphEditor for Open/Save dialogs (Main.kt lines 1026-1058). Consistent UX, no additional dependencies.
**Alternatives Rejected**:
- Native file dialogs (JNA/JNI): Adds dependency complexity, license concerns
- Custom file picker composable: Significant development effort, worse UX

### R2: Property Configuration Storage

**Decision**: Store processingLogicFile in CodeNode.configuration map with key "processingLogicFile"
**Rationale**:
- Configuration map already serialized by FlowGraphSerializer
- No model changes required
- Follows existing pattern for speedAttenuation, outputFormat, etc.
**Alternatives Rejected**:
- Add processingLogicFile as CodeNode property: Requires model change, serialization update
- Separate metadata file: Complicates project structure

### R3: Relative vs Absolute Paths

**Decision**: Store relative paths from project root
**Rationale**:
- Enables portability across developer machines
- Flow graph files can be version controlled without machine-specific paths
- Pattern used by IDEs (project-relative includes)
**Alternatives Rejected**:
- Absolute paths: Breaks when cloning/sharing project
- Package-qualified class names: Requires parsing Kotlin files to resolve

### R4: Validation Architecture

**Decision**: Create CompilationValidator class separate from CompilationService
**Rationale**:
- Single responsibility: validation vs generation
- Testable in isolation
- Can be extended for other required properties
**Alternatives Rejected**:
- Inline validation in CompilationService: Harder to test, violates SRP
- Validation in PropertiesPanel: Wrong layer, compile-time vs edit-time

### R5: Factory Function Placement

**Decision**: Generate createXXXFlowGraph() as top-level function in same file as Flow class
**Rationale**:
- Follows Kotlin conventions (factory functions as top-level)
- Single import for controller + factory
- Consistent with existing StopWatchFlow.kt location
**Alternatives Rejected**:
- Static method on Flow class: Less idiomatic Kotlin
- Separate file: Increases import complexity

## Phase 1: Design Artifacts

See:
- [data-model.md](./data-model.md) - Entity definitions
- [contracts/processing-logic-editor-contract.md](./contracts/processing-logic-editor-contract.md) - UI contract
- [quickstart.md](./quickstart.md) - Developer setup guide

## Implementation Phases

### Phase 1: Properties Panel File Browser (User Story 1)

**Goal**: Add FILE_BROWSER editor type to PropertiesPanel

**Scope**:
1. Add `PropertyType.FILE_PATH` enum value
2. Add `EditorType.FILE_BROWSER` enum value
3. Create `FileBrowserEditor` composable with text field + browse button
4. Wire editor to show JFileChooser filtered to *.kt files
5. Store selected path in node configuration

**Tests**:
- FileBrowserEditor renders with text field and button
- Click browse button triggers file chooser
- Selected file path appears in text field
- Path stored in node configuration on selection

### Phase 2: Compile-Time Validation (User Story 2)

**Goal**: Validate required properties before code generation

**Scope**:
1. Create `CompilationValidator` class
2. Define `RequiredPropertySpec` for processingLogicFile
3. Implement `validateForCompilation(FlowGraph): CompilationValidationResult`
4. Integrate validation into CompilationService before generation
5. Display validation errors in UI

**Tests**:
- Graph with all processingLogicFile configured passes validation
- Graph missing processingLogicFile fails with specific node listed
- Multiple missing properties reported for each node
- File existence validation (file path must exist)

### Phase 3: Factory Function Generator (User Story 3)

**Goal**: Generate createXXXFlowGraph() function in Flow file

**Scope**:
1. Create `FlowGraphFactoryGenerator` using KotlinPoet
2. Generate function that creates FlowGraph with all CodeNodes
3. Include ProcessingLogic instantiation from file references
4. Generate Port and Connection creation
5. Integrate into ModuleGenerator pipeline

**Tests**:
- Generated function returns FlowGraph instance
- Generated FlowGraph has correct nodes with ProcessingLogic
- Generated FlowGraph has correct connections
- Function compiles and produces runnable code

### Phase 4: Integration & Migration (User Story 4)

**Goal**: Update StopWatch demo to use generated code

**Scope**:
1. Update StopWatch.flow.kts with processingLogicFile config
2. Regenerate StopWatchFlow.kt with factory function
3. Remove manual createStopWatchFlowGraph() from App.kt
4. Update App.kt imports to use generated function
5. Verify app compiles and runs

**Tests**:
- KMPMobileApp compiles with generated imports
- StopWatch demo functions identically to before
- No manual FlowGraph construction in App.kt

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| JFileChooser modal blocks UI thread | Low | Medium | Run in coroutine with Dispatchers.IO |
| File paths break cross-platform | Medium | High | Normalize to forward slashes, test on multiple OS |
| Generated code doesn't compile | Low | High | Integration tests compile generated code |
| ProcessingLogic class not found at runtime | Medium | Medium | Validate class exists during compile validation |

## Dependencies

- Phase 2 depends on Phase 1 (need editor to set values before validating)
- Phase 3 depends on Phase 2 (validation must pass before generation)
- Phase 4 depends on Phase 3 (need generated code before migration)

## Next Steps

Run `/speckit.tasks` to generate actionable task list from this plan.
