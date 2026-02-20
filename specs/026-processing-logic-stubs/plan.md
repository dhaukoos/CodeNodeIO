# Implementation Plan: Redesign Processing Logic Stub Generator

**Branch**: `026-processing-logic-stubs` | **Date**: 2026-02-20 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/026-processing-logic-stubs/spec.md`

## Summary

Redesign the ProcessingLogicStubGenerator to generate typed tick function stubs instead of untyped ProcessingLogic class stubs. Remove the ProcessingLogic fun interface, processingLogic property, and all related references from the codebase. Update the code generators (FlowGraphFactoryGenerator, FlowKtGenerator) to reference tick stubs and create NodeRuntime instances via the timed/continuous factory methods. This replaces the single-invocation ProcessingLogic pattern with the channel-based tick function pattern already proven in the StopWatch module.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP - Kotlin Multiplatform)
**Primary Dependencies**: kotlinx-coroutines 1.8.0, kotlinx-serialization 1.6.0, KotlinPoet (code generation)
**Storage**: N/A (in-memory models, generated source files)
**Testing**: kotlin.test + JUnit (kotlinCompiler commonTest, fbpDsl commonTest)
**Target Platform**: All KMP targets (JVM, potentially native)
**Project Type**: Multi-module KMP project (fbpDsl library, kotlinCompiler code generator, graphEditor UI)
**Performance Goals**: N/A (code generation tool, not runtime-critical)
**Constraints**: Generated stubs must compile independently. Existing stubs must never be overwritten.
**Scale/Scope**: ~5 files modified in kotlinCompiler, ~2 files modified in fbpDsl, ~1 file modified in graphEditor. ~200 lines removed, ~300 lines added/modified.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality | PASS | Generated stubs include KDoc. Generator methods are under 50 lines. Public APIs have explicit type annotations. |
| II. Test-Driven Development | PASS | Existing ProcessingLogicStubGeneratorTest will be updated to test new tick stub generation. All generator tests updated. |
| III. UX Consistency | N/A | No user-facing UI changes (PropertiesPanel _useCaseClass field removed, but this is cleanup of obsolete functionality). |
| IV. Performance | N/A | Code generation tool, not runtime-critical. |
| V. Observability | N/A | No runtime behavior changes. |
| Licensing | PASS | No new dependencies. All code is Apache 2.0. |

**Post-Design Re-check**: All gates still PASS. No unknowns remain after research.

## Project Structure

### Documentation (this feature)

```text
specs/026-processing-logic-stubs/
├── plan.md              # This file
├── research.md          # Phase 0: pattern analysis and design decisions
├── data-model.md        # Phase 1: entity mapping and naming conventions
├── quickstart.md        # Phase 1: before/after examples
├── contracts/           # Phase 1: generator contracts
│   └── stub-generator-contract.md
├── checklists/
│   └── requirements.md  # Spec quality checklist
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (files affected)

```text
kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/
├── ProcessingLogicStubGenerator.kt    # MODIFY: rewrite to generate tick stubs
├── FlowGraphFactoryGenerator.kt       # MODIFY: replace ProcessingLogic refs with tick stub refs
├── FlowKtGenerator.kt                 # MODIFY: remove processingLogic<T>() generation
└── KotlinCodeGenerator.kt             # MODIFY: update orchestration if needed

kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/
├── ProcessingLogicStubGeneratorTest.kt    # MODIFY: rewrite tests for tick stubs
├── FlowGraphFactoryGeneratorTest.kt       # MODIFY: update tests for tick pattern
└── FlowKtGeneratorTest.kt                # MODIFY: remove processingLogic tests

fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/
├── CodeNode.kt                        # MODIFY: remove ProcessingLogic interface + property + helpers
└── CodeNodeFactory.kt                 # MODIFY: remove processingLogic parameter from create()

graphEditor/src/jvmMain/kotlin/ui/
└── PropertiesPanel.kt                 # MODIFY: remove _useCaseClass property editor
```

**Structure Decision**: All changes modify existing files. No new files created in source. The `logicmethods/` folder is a convention for generated output, not a source folder in this project.

## Design Decisions

### D1: Tick Val Stubs (not class stubs)

Generated stubs are top-level `val` properties with tick type alias types, not classes implementing an interface. This matches the lightweight lambda pattern already used in the StopWatch module and is the minimum viable stub for the developer to fill in.

### D2: Convention-Based Naming (not configuration)

The tick function name is derived from the node name (`{nodeName}Tick`), placed in the `logicmethods` package. This eliminates the `_useCaseClass` configuration key. The code generator knows where to find the tick function without any configuration.

### D3: LogicMethods Folder (not generated/)

Tick stubs go in `logicmethods/` as a peer to `generated/`. Files in `generated/` are overwritten on regeneration; files in `logicmethods/` are preserved. This makes the developer's intent clear through directory structure.

### D4: Never Overwrite Existing Stubs

The generator checks if a stub file exists before writing. Existing files are preserved regardless of content. This protects user edits.

### D5: Factory Method Selection from Port Configuration

The generator selects the correct `createTimed{X}` factory method based on the node's (inputCount, outputCount) pair. The `_genericType` config value or direct port counting provides this information. Timed vs. continuous is determined by whether a tick interval config exists.

### D6: Transformer vs Filter Distinction

When inputs=1 and outputs=1: if input type equals output type, generate a FilterTickBlock; if types differ, generate a TransformerTickBlock. This heuristic matches the existing CodeNodeType categorization.
