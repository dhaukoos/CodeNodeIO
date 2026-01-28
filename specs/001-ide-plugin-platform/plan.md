# Implementation Plan: CodeNodeIO IDE Plugin Platform

**Branch**: `001-ide-plugin-platform` | **Date**: 2026-01-13 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/001-ide-plugin-platform/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

CodeNodeIO is a JetBrains IDE plugin that enables developers to create full-stack applications using Flow-based Programming (FBP) principles through a visual CAD-like interface. The platform consists of multiple Kotlin Multiplatform modules (fbpDsl, graphEditor, circuitSimulator, kotlinCompiler, goCompiler, idePlugin) that work together to provide visual graph creation, textual DSL generation, circuit simulation/debugging, and code generation for KMP (Android/iOS/Web) and Go (backend) targets. The technical approach leverages Compose Multiplatform for the visual editor, Kotlin coroutines/channels for FBP execution, and IntelliJ Platform SDK for IDE integration.

## Technical Context

**Language/Version**: Kotlin 1.9+, Go 1.21+

**Primary Dependencies**:
- IntelliJ Platform SDK 2023.1+ (IDE plugin framework)
- Compose Multiplatform Desktop (visual editor UI)
- Kotlin Coroutines & Channels (FBP execution model)
- NEEDS CLARIFICATION: Specific Compose Multiplatform version
- NEEDS CLARIFICATION: Graph rendering library (JGraphX, GraphStream, or custom)
- NEEDS CLARIFICATION: Kotlin compiler APIs for KMP code generation
- NEEDS CLARIFICATION: Go code generation approach (templates vs AST manipulation)

**Storage**:
- Flow graph persistence: DSL files (text format using Kotlin infix functions)
- Project metadata: JSON or YAML configuration files
- Generated code: Standard KMP project structure (build.gradle.kts) and Go modules (go.mod)

**Testing**:
- Kotlin: kotlin.test, JUnit 5 for unit/integration tests
- IDE Plugin: IntelliJ Platform Test Framework
- NEEDS CLARIFICATION: UI testing framework for Compose Multiplatform (Compose UI Test or custom)
- NEEDS CLARIFICATION: Contract testing approach for generated code validation

**Target Platform**:
- IDE Plugin: JetBrains IDEs (IntelliJ IDEA, Android Studio, GoLand) on Windows/macOS/Linux
- Generated KMP Code: Android, iOS, Desktop (JVM), Web (Kotlin/JS or Kotlin/Wasm)
- Generated Go Code: Server-side Linux/macOS/Windows

**Project Type**: Multi-module Kotlin Multiplatform project with discrete Gradle modules:
- fbpDsl: Core FBP domain model and DSL
- graphEditor: Visual editor (Compose Desktop)
- circuitSimulator: Debugging/visualization tool
- kotlinCompiler: KMP code generator
- goCompiler: Go code generator
- idePlugin: IntelliJ Platform plugin integration

**Performance Goals**:
- Visual editor: 60 fps rendering for graphs up to 50 nodes (per SC-002: <100ms interaction response)
- Code generation: <30 seconds for 10-15 node graphs (per SC-003)
- IDE responsiveness: UI must not block on graph operations
- DSL file size: <1MB for 30-node graphs (per SC-009)

**Constraints**:
- Licensing: NO GPL/LGPL/AGPL dependencies (per constitution)
- IDE compatibility: Must support IntelliJ Platform API 2023.1+
- Memory: Plugin must operate within typical IDE memory limits (<500MB heap for plugin alone)
- Offline-capable: Core editing and code generation work without network (library resolution requires network)

**Scale/Scope**:
- Initial release: Support graphs up to 50 nodes with acceptable performance
- Code generation: Handle common FBP patterns (data transformation, API endpoints, UI components)
- Module count: 6 discrete Gradle modules
- Target users: Individual developers and small teams (< 10 concurrent users per project)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Licensing & IP (CRITICAL - Breaking Build Constraint)

- [x] **Static Linking Rule**: NO GPL/LGPL dependencies in KMP native or Go modules
  - Status: PASS (pending research to validate all dependencies)
  - Action: Phase 0 must audit all proposed libraries (Compose Multiplatform, graph rendering, compiler APIs)

- [x] **KMP Dependency Protocol**: Favor Apache 2.0 aligned libraries (JetBrains/Google)
  - Status: PASS (IntelliJ Platform SDK = Apache 2.0, Compose Multiplatform = Apache 2.0, Kotlin = Apache 2.0)
  - Action: Verify transitive dependencies during research phase

- [x] **File Header Management**: All .kt and .kts files must have project header above package declaration
  - Status: PASS (will be enforced during implementation)

### Code Quality First

- [x] **Readability**: Multi-module structure with clear separation of concerns (fbpDsl, graphEditor, etc.)
  - Status: PASS

- [x] **Maintainability**: Each module has single responsibility (DSL, editor, simulator, compilers, plugin)
  - Status: PASS

- [x] **Type Safety**: Kotlin's type system ensures safety; DSL uses generic types (InformationPacket<T>, Port<T>) and ProcessingLogic fun interface for compile-time type checking
  - Status: PASS
  - Architecture: UseCase pattern with 7 typed base classes (TransformerUseCase, FilterUseCase, ValidatorUseCase, SplitterUseCase, MergerUseCase, GeneratorUseCase, SinkUseCase)


### Test-Driven Development

- [x] **Red-Green-Refactor**: TDD mandatory per constitution
  - Status: ACKNOWLEDGED (will be enforced during task execution)

- [x] **Test Coverage**: >80% line coverage goal
  - Status: PENDING (testing framework decisions needed in Phase 0)
  - Action: Research Compose UI testing, contract testing for generated code

- [x] **Test Types**: Unit (DSL logic), Integration (IDE plugin), Contract (generated code compilation)
  - Status: PLANNED

- [x] **Test Independence**: Using kotlin.test/JUnit 5 with proper fixtures
  - Status: PASS

### User Experience Consistency

- [x] **Design System**: Compose Multiplatform provides consistent UI components
  - Status: PASS

- [x] **Response Time**: <100ms for UI interactions (per SC-002)
  - Status: PASS (performance goal defined)

### Performance Requirements

- [x] **Benchmarks**: Required for graph rendering, code generation
  - Status: PENDING (benchmarking approach to be defined in Phase 0)

- [x] **Response Time SLAs**: <100ms UI interaction, <30s code generation
  - Status: PASS (aligned with SC-002, SC-003)

- [x] **Resource Limits**: <500MB plugin heap, <1MB DSL files
  - Status: PASS (constraints defined)

- [x] **Scalability**: Support up to 50 nodes initially
  - Status: PASS (scope limited appropriately)

- [x] **Monitoring**: IDE plugin metrics via IntelliJ Platform diagnostic tools
  - Status: PENDING (monitoring strategy to be defined)


### Gate Evaluation

**OVERALL STATUS**: CONDITIONAL PASS - Proceed to Phase 0 Research

**Critical Blockers**: NONE

**Research Required**:
1. Validate ALL dependency licenses (Compose Multiplatform version, graph rendering library, compiler APIs)
2. Determine Compose Desktop accessibility support for WCAG 2.1 AA compliance
3. Select UI testing framework for Compose Multiplatform
4. Define contract testing strategy for generated code validation
5. Choose Go code generation approach (templates vs AST) and validate library licenses

## Project Structure

### Documentation (this feature)

```text
specs/[###-feature]/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
CodeNodeIO/                        # Repository root
├── fbpDsl/                        # Module: FBP Domain Specific Language
│   ├── src/
│   │   ├── commonMain/kotlin/
│   │   │   ├── model/            # Generic data classes: InformationPacket<T>, Node, Port<T>, Edge
│   │   │   │                     # CodeNode (extends Node), CodeNodeType enum, CodeNodeFactory
│   │   │   ├── usecase/          # UseCase pattern support
│   │   │   │   ├── TypedUseCases.kt           # 7 typed base classes for common FBP patterns
│   │   │   │   ├── LifecycleAwareUseCases.kt  # Lifecycle management (initialize/cleanup)
│   │   │   │   └── examples/                  # Example implementations (DI, decorators, composition)
│   │   │   ├── dsl/              # Infix functions for DSL (connect, to, etc.)
│   │   │   └── execution/        # Coroutine-based FBP execution engine
│   │   ├── commonTest/kotlin/    # Unit tests for DSL and execution
│   │   └── jvmMain/kotlin/       # JVM-specific implementations if needed
│   ├── build.gradle.kts
│   └── README.md
│
├── graphEditor/                   # Module: Visual graph editor (Compose Desktop)
│   ├── src/
│   │   ├── jvmMain/kotlin/
│   │   │   ├── ui/               # Compose UI components (Canvas, NodePalette, PropertiesPanel)
│   │   │   ├── state/            # UI state management
│   │   │   ├── rendering/        # Graph rendering logic
│   │   │   └── serialization/    # Graph -> DSL file conversion
│   │   └── jvmTest/kotlin/       # UI and integration tests
│   ├── build.gradle.kts
│   └── README.md
│
├── circuitSimulator/              # Module: Circuit simulation & debugging
│   ├── src/
│   │   ├── jvmMain/kotlin/
│   │   │   ├── ui/               # Simulation UI (pause/resume/speed controls)
│   │   │   ├── engine/           # Simulation execution with debugging hooks
│   │   │   └── visualization/    # Data flow visualization
│   │   └── jvmTest/kotlin/
│   ├── build.gradle.kts
│   └── README.md
│
├── kotlinCompiler/                # Module: KMP code generator
│   ├── src/
│   │   ├── commonMain/kotlin/
│   │   │   ├── generator/        # Code generation logic
│   │   │   ├── templates/        # Code templates for nodes/components
│   │   │   └── validator/        # License validation, dependency checking
│   │   └── commonTest/kotlin/    # Contract tests (generated code compiles)
│   ├── build.gradle.kts
│   └── README.md
│
├── goCompiler/                    # Module: Go code generator
│   ├── src/
│   │   ├── commonMain/kotlin/
│   │   │   ├── generator/        # Go code generation logic
│   │   │   ├── templates/        # Go code templates
│   │   │   └── validator/        # License validation for Go modules
│   │   └── commonTest/kotlin/    # Contract tests (generated Go compiles)
│   ├── build.gradle.kts
│   └── README.md
│
├── idePlugin/                     # Module: IntelliJ Platform plugin
│   ├── src/
│   │   ├── main/kotlin/
│   │   │   ├── actions/          # IDE actions (New Graph, Generate Code)
│   │   │   ├── toolwindows/      # Graph editor tool window integration
│   │   │   ├── services/         # IDE services (project, file management)
│   │   │   └── integration/      # Integration with graphEditor/compilers
│   │   ├── main/resources/
│   │   │   ├── META-INF/
│   │   │   │   └── plugin.xml    # Plugin descriptor
│   │   │   └── icons/
│   │   └── test/kotlin/          # IDE plugin integration tests
│   ├── build.gradle.kts
│   └── README.md
│
├── build.gradle.kts               # Root build configuration
├── settings.gradle.kts            # Module declarations
├── gradle.properties              # Kotlin/Gradle versions
├── LICENSE                        # Project license
└── README.md                      # Project overview
```

**Structure Decision**: Multi-module Kotlin Multiplatform project with clear separation of concerns:

1. **fbpDsl**: Foundation module providing FBP domain model with generic types (InformationPacket<T>, Node, Port<T>, Edge) and DSL syntax using Kotlin infix functions. Includes ProcessingLogic fun interface for type-safe business logic, CodeNodeType enum for node classification, and UseCase pattern support with 7 typed base classes (Transformer, Filter, Validator, Splitter, Merger, Generator, Sink). Provides lifecycle management (initialize/cleanup hooks) and supports decorator/composition patterns for cross-cutting concerns. Multiplatform (common) to enable reuse across modules.

2. **graphEditor**: Compose Desktop application for visual graph creation. JVM-only as Compose Desktop targets JVM. Depends on fbpDsl.

3. **circuitSimulator**: Debugging tool built on graphEditor. Adds execution visualization and control (pause/resume/speed). JVM-only, depends on fbpDsl and graphEditor.

4. **kotlinCompiler**: Code generator for KMP targets. Multiplatform (common) for potential future use in web-based editor. Depends on fbpDsl for graph model understanding.

5. **goCompiler**: Code generator for Go backends. Multiplatform (common) for consistency. Depends on fbpDsl.

6. **idePlugin**: IntelliJ Platform plugin that integrates all modules into JetBrains IDEs. JVM-only (IntelliJ Platform requirement). Depends on all other modules.

**Dependencies Flow**:
```
idePlugin → graphEditor → fbpDsl
         → circuitSimulator → fbpDsl
         → kotlinCompiler → fbpDsl
         → goCompiler → fbpDsl
```

**Module Boundaries**:
- Each module has its own build.gradle.kts with explicit dependencies
- No circular dependencies
- Shared domain model lives in fbpDsl
- UI logic isolated in graphEditor/circuitSimulator
- Code generation isolated in *Compiler modules
- IDE integration isolated in idePlugin

**CodeNode Processing Architecture**:

The fbpDsl module uses the UseCase pattern for business logic encapsulation:

- **ProcessingLogic**: Fun interface enabling both lambda syntax (`ProcessingLogic { ... }`) and class implementations
- **Typed Base Classes**: 7 abstract classes for common FBP patterns:
  - `TransformerUseCase<TIn, TOut>`: Type-safe transformation from input to output
  - `FilterUseCase<T>`: Conditional pass-through with predicate
  - `ValidatorUseCase<T>`: Routes to valid/invalid output ports
  - `SplitterUseCase<T>`: Dynamic output port selection
  - `MergerUseCase<T>`: Combines multiple inputs into single output
  - `GeneratorUseCase<T>`: Generates output without input
  - `SinkUseCase<T>`: Consumes input for side effects (logging, persistence)
- **Lifecycle Management**: Initialize/cleanup hooks for resource management (database connections, file handles, caches)
- **Composition Patterns**: Support for decorator pattern (logging, retry, metrics) and pipeline composition

Benefits: Dependency injection, testability, reusability, state management, observability

Documentation: See `fbpDsl/docs/UseCase-Pattern-Guide.md` for detailed implementation guide

**Connection Architecture**:

Following the Single Responsibility Principle, the Connection class is designed with a focused purpose:

- **Single Responsibility**: Connections only link ports and manage data flow - no transformation logic
- **Design Rationale**: Data transformation should be handled by dedicated transformer nodes, not by connections
  - Keeps Connection class simple and focused
  - Enables better testability and debuggability
  - Allows transformations to be visible in the graph as explicit nodes
  - Supports composition of multiple transformations in sequence
- **Channel Semantics**: Supports buffered and unbuffered channels via `channelCapacity` property
- **Type Safety**: Validates port direction (OUTPUT → INPUT) and type compatibility at connection creation
- **Scope Awareness**: Optional `parentScopeId` links connections to containing GraphNode or FlowGraph

Benefits: Simplicity, maintainability, explicit data flow, separation of concerns

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| [e.g., 4th project] | [current need] | [why 3 projects insufficient] |
| [e.g., Repository pattern] | [specific problem] | [why direct DB access insufficient] |
