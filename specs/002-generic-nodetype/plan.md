# Implementation Plan: Generic NodeType Definition

**Branch**: `002-generic-nodetype` | **Date**: 2026-01-28 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/002-generic-nodetype/spec.md`

## Summary

This feature adds a new "GENERIC" category to the NodeTypeDefinition system, enabling users to create flexible processing nodes with configurable numbers of inputs (0-5) and outputs (0-5). The implementation extends the existing fbpDsl module with a factory function `createGenericNodeType(numInputs, numOutputs)` and integrates generic nodes throughout the platform including NodePalette display, properties panel configuration, serialization/deserialization, and code generation support. Generic nodes support customizable properties including display name, icon/image resource, custom port names, and UseCase class references for processing logic.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (existing project configuration)

**Primary Dependencies** (all existing - no new dependencies required):
- IntelliJ Platform SDK 2024.1 (IDE plugin framework)
- Compose Multiplatform Desktop 1.10.0 (visual editor UI)
- KotlinPoet 2.2.0 (code generation)
- kotlinx-serialization-json 1.6.2 (DSL serialization)

**Storage**:
- Extended .flow.kts DSL format for generic node persistence
- Configuration stored as node properties/metadata

**Testing**:
- kotlin.test with JUnit 5 (unit tests)
- Existing graphEditor test infrastructure (UI tests)
- Contract tests for code generation

**Target Platform**:
- JetBrains IDEs via IntelliJ Platform plugin
- All existing KMP code generation targets

**Project Type**: Multi-module Kotlin Multiplatform project (extends existing structure)
- fbpDsl: Core changes (GENERIC category, factory function)
- graphEditor: UI integration (NodePalette, PropertiesPanel)
- kotlinCompiler: Code generation support
- idePlugin: IDE integration

**Performance Goals**:
- Factory function: <1ms to create any generic node type (36 combinations)
- NodePalette: Generic category loads within existing <100ms target
- Serialization: No performance impact on existing save/load operations

**Constraints**:
- Must maintain backward compatibility with existing NodeTypeDefinition usage
- No breaking changes to existing NodeCategory consumers
- All new code must follow existing Apache 2.0 licensing

**Scale/Scope**:
- 36 generic node type combinations (0-5 inputs × 0-5 outputs)
- Affects 4 modules: fbpDsl, graphEditor, kotlinCompiler, idePlugin

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Licensing & IP (CRITICAL - Breaking Build Constraint)

- [x] **Static Linking Rule**: NO GPL/LGPL dependencies
  - Status: PASS - No new dependencies required; uses existing Apache 2.0 licensed libraries

- [x] **KMP Dependency Protocol**: Apache 2.0 aligned libraries
  - Status: PASS - All existing dependencies already validated

- [x] **File Header Management**: All .kt files must have project header
  - Status: PASS - Will be enforced during implementation

### Code Quality First

- [x] **Readability**: Clear factory function API with descriptive naming
  - Status: PASS - `createGenericNodeType(numInputs, numOutputs)` is self-documenting

- [x] **Maintainability**: Single-purpose factory function, extends existing patterns
  - Status: PASS - Follows established NodeTypeDefinition patterns

- [x] **Type Safety**: Factory function validates input bounds (0-5)
  - Status: PASS - Compile-time and runtime validation

### Test-Driven Development

- [x] **Red-Green-Refactor**: TDD mandatory per constitution
  - Status: ACKNOWLEDGED - Tests first for factory function and all integrations

- [x] **Test Coverage**: >80% line coverage
  - Status: PLANNED - Unit tests for all 36 combinations, integration tests for UI

- [x] **Test Types**: Unit (factory), Integration (palette, serialization), Contract (code gen)
  - Status: PLANNED

### User Experience Consistency

- [x] **Design System**: Uses existing Compose Material components
  - Status: PASS - Extends NodePalette with same visual patterns

- [x] **Response Time**: <100ms for UI interactions
  - Status: PASS - Factory function <1ms, no UI blocking operations

### Performance Requirements

- [x] **Benchmarks**: Factory function performance
  - Status: PLANNED - Benchmark all 36 combinations

- [x] **Resource Limits**: No additional memory overhead
  - Status: PASS - Lazy initialization, no precomputed combinations

### Gate Evaluation

**OVERALL STATUS**: PASS - Proceed to Phase 0 Research

**Critical Blockers**: NONE

**Research Required**:
1. Review existing NodeTypeDefinition consumers to ensure GENERIC category compatibility
2. Determine optimal DSL syntax for generic node serialization
3. Review UseCase integration patterns in code generators

## Project Structure

### Documentation (this feature)

```text
specs/002-generic-nodetype/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (affected paths)

```text
CodeNodeIO/
├── fbpDsl/
│   └── src/
│       ├── commonMain/kotlin/io/codenode/fbpdsl/
│       │   ├── model/
│       │   │   └── NodeTypeDefinition.kt     # Add GENERIC category
│       │   └── factory/
│       │       └── GenericNodeTypeFactory.kt # NEW: Factory function
│       └── commonTest/kotlin/io/codenode/fbpdsl/
│           └── factory/
│               └── GenericNodeTypeFactoryTest.kt # NEW: Unit tests
│
├── graphEditor/
│   └── src/
│       ├── jvmMain/kotlin/io/codenode/grapheditor/
│       │   ├── ui/
│       │   │   ├── NodePalette.kt            # Update category display
│       │   │   └── PropertiesPanel.kt        # Update for generic node props
│       │   └── serialization/
│       │       ├── FlowGraphSerializer.kt    # Update for generic nodes
│       │       └── FlowGraphDeserializer.kt  # Update for generic nodes
│       └── jvmTest/kotlin/io/codenode/grapheditor/
│           ├── ui/
│           │   └── GenericNodePaletteTest.kt # NEW: UI tests
│           └── serialization/
│               └── GenericNodeSerializationTest.kt # NEW: Roundtrip tests
│
├── kotlinCompiler/
│   └── src/
│       ├── commonMain/kotlin/io/codenode/kotlincompiler/
│       │   └── generator/
│       │       └── GenericNodeGenerator.kt   # NEW: Code gen for generic nodes
│       └── commonTest/kotlin/io/codenode/kotlincompiler/
│           └── generator/
│               └── GenericNodeGeneratorTest.kt # NEW: Contract tests
│
└── idePlugin/
    └── src/main/resources/icons/
        └── generic-node.svg                  # NEW: Default generic node icon
```

**Structure Decision**: Extends existing multi-module structure without architectural changes. New code follows established patterns in each module. The GenericNodeTypeFactory is placed in a new `factory/` package within fbpDsl to keep model classes focused on data representation.

## Complexity Tracking

> No constitution violations requiring justification

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| N/A       | N/A        | N/A                                 |
