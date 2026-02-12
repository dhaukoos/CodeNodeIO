# Implementation Plan: StopWatch Virtual Circuit Refactor

**Branch**: `011-stopwatch-refactor` | **Date**: 2026-02-12 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/011-stopwatch-refactor/spec.md`

## Summary

Refactor the StopWatch virtual circuit architecture to eliminate code duplication by adding compile-time required property validation to the graphEditor. This enables the KMPMobileApp to reference the StopWatch module's `stopWatchFlowGraph` directly instead of maintaining a redundant `createStopWatchFlowGraph()` function. The implementation adds pre-compilation property validation, enhances the properties panel to display required property indicators, and removes the duplicate FlowGraph definition.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP - Kotlin Multiplatform)
**Primary Dependencies**: Compose Desktop 1.7.3, kotlinx-coroutines 1.8.0, kotlinx-serialization 1.6.0
**Storage**: N/A (in-memory FlowGraph models)
**Testing**: JUnit 5, Kotlin Test (via `./gradlew :graphEditor:jvmTest`)
**Target Platform**: JVM Desktop (graphEditor), Android/iOS (KMPMobileApp)
**Project Type**: Multi-module KMP project
**Performance Goals**: Validation feedback within 2 seconds (SC-002)
**Constraints**: Must maintain backward compatibility with existing flow graphs
**Scale/Scope**: ~50 node types, 2 affected modules (graphEditor, KMPMobileApp)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality First | ✅ PASS | Removes code duplication, improves maintainability |
| II. Test-Driven Development | ✅ PASS | Tests will be written for validation logic first |
| III. User Experience Consistency | ✅ PASS | Clear error messages, required field indicators |
| IV. Performance Requirements | ✅ PASS | Validation completes within 2 seconds |
| V. Observability & Debugging | ✅ PASS | Clear error messages with node names and missing properties |
| Licensing & IP | ✅ PASS | No new dependencies required |

**Quality Gates**:
- All tests pass before merge
- Code review required
- Documentation updated for compile validation

## Project Structure

### Documentation (this feature)

```text
specs/011-stopwatch-refactor/
├── plan.md              # This file
├── research.md          # Phase 0 output - architecture analysis
├── data-model.md        # Phase 1 output - validation data structures
├── quickstart.md        # Phase 1 output - implementation guide
├── contracts/           # Phase 1 output - validation interface contracts
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
graphEditor/
├── src/jvmMain/kotlin/
│   ├── compilation/
│   │   ├── CompilationService.kt      # MODIFY: Add property validation
│   │   └── RequiredPropertyValidator.kt # NEW: Property validation logic
│   └── ui/
│       └── PropertiesPanel.kt         # MODIFY: Add required indicators for _useCaseClass
└── src/jvmTest/kotlin/
    └── compilation/
        └── RequiredPropertyValidatorTest.kt # NEW: Validation tests

KMPMobileApp/
└── src/commonMain/kotlin/io/codenode/mobileapp/
    └── App.kt                         # MODIFY: Remove createStopWatchFlowGraph()

fbpDsl/
└── src/commonMain/kotlin/io/codenode/fbpdsl/model/
    └── CodeNode.kt                    # Reference only (no changes)
```

**Structure Decision**: Existing multi-module structure maintained. New validation logic added to graphEditor/compilation, redundant code removed from KMPMobileApp.

## Complexity Tracking

No constitutional violations. Complexity remains minimal:
- 1 new file (RequiredPropertyValidator.kt)
- 3 modified files (CompilationService.kt, PropertiesPanel.kt, App.kt)
- No new architectural patterns introduced

## Phase 0 Findings

Based on codebase exploration:

### Validation Architecture

1. **Existing ValidationResult Pattern**: Used throughout codebase (FlowGraph.validate(), CodeNode.validate())
2. **Property Definition System**: PropertiesPanelState already parses JSON Schema for property metadata
3. **Compilation Flow**: CompilationService calls flowGraph.validate() before module generation

### Required Properties for GENERIC Nodes

| Property | Purpose | Required For |
|----------|---------|--------------|
| `_useCaseClass` | Links to ProcessingLogic implementation | Code generation |
| `_genericType` | Specifies port configuration (in0out2, etc.) | Node structure |
| `speedAttenuation` | Tick interval in milliseconds | Runtime execution |

### Key Integration Points

1. **CompilationService.compileToModule()** (line 48-88): Add validation before line 65
2. **PropertiesPanelState.derivePropertyDefinitions()**: Already parses required from JSON Schema
3. **NodeTypeDefinition.configurationSchema**: JSON Schema with "required" array

### StopWatch Module Export

The `stopWatchFlowGraph` is already properly exported as a public `val` in `StopWatch.flow.kt`. KMPMobileApp can directly import it via:
```kotlin
import io.codenode.stopwatch.stopWatchFlowGraph
```

## Post-Design Constitution Check

*Re-evaluated after Phase 1 design completion.*

| Principle | Status | Design Validation |
|-----------|--------|-------------------|
| I. Code Quality First | ✅ PASS | Single responsibility: RequiredPropertyValidator handles only property validation |
| II. Test-Driven Development | ✅ PASS | Test contract defined in contracts/validation-interface.md |
| III. User Experience Consistency | ✅ PASS | Error format matches existing ValidationResult pattern |
| IV. Performance Requirements | ✅ PASS | O(n) validation where n = number of nodes |
| V. Observability & Debugging | ✅ PASS | toErrorMessage() provides structured, actionable output |
| Licensing & IP | ✅ PASS | No external dependencies added |

**Design Artifacts Generated**:
- `research.md` - Architecture analysis and decision rationale
- `data-model.md` - PropertyValidationError, PropertyValidationResult structures
- `contracts/validation-interface.md` - RequiredPropertyValidator interface and test contract
- `quickstart.md` - Step-by-step implementation guide

## Next Steps

Run `/speckit.tasks` to generate implementation tasks from this plan.
