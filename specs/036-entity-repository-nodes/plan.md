# Implementation Plan: Entity Repository Nodes

**Branch**: `036-entity-repository-nodes` | **Date**: 2026-03-01 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/036-entity-repository-nodes/spec.md`

## Summary

Add entity repository nodes to CodeNodeIO: (1) display custom IP type properties in the Properties Panel, (2) generate repository node definitions from custom IP types with standardized CRUD ports, (3) produce Room persistence code (Entity/DAO/Repository/Database) during code generation, and (4) integrate a singleton database module shared across flow graphs. Uses KMP Room 2.8.4 with the Repository pattern wrapping DAOs and reactive observe-all streams for composable UI.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (Kotlin Multiplatform)
**Primary Dependencies**: Compose Desktop 1.7.3, kotlinx-coroutines 1.8.0, kotlinx-serialization 1.6.0, Room 2.8.4, KSP 2.1.21-2.0.1, SQLite Bundled 2.6.2
**Storage**: Room (KMP) with BundledSQLiteDriver — persisted to `~/.codenode/data/app.db` (JVM)
**Testing**: Kotlin Test (JVM), `./gradlew :graphEditor:jvmTest :kotlinCompiler:jvmTest :fbpDsl:jvmTest`
**Target Platform**: JVM Desktop (primary), Android/iOS (generated modules)
**Project Type**: Multi-module KMP (existing: fbpDsl, graphEditor, kotlinCompiler)
**Performance Goals**: UI interactions < 100ms, code generation < 5s for typical flow graphs
**Constraints**: Apache 2.0 compatible dependencies only (Room is Apache 2.0)
**Scale/Scope**: Supports 1-10 repository nodes per flow graph, 1-20 properties per entity

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Pre-Research Check

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality | PASS | Follows existing patterns (CustomNodeDefinition, generators). Single responsibility per class. |
| II. Test-Driven Development | PASS | Tests planned for each user story. Code generation outputs verified in tests. |
| III. User Experience Consistency | PASS | Extends existing Properties Panel and palette patterns. Follows established Compose Material style. |
| IV. Performance Requirements | PASS | Code generation is batch operation, not latency-critical. UI operations are lightweight. |
| V. Observability & Debugging | PASS | Repository nodes visible in palette with clear naming. Generated code includes error channel for runtime debugging. |
| Licensing | PASS | Room 2.8.4 (Apache 2.0), KSP (Apache 2.0), SQLite Bundled (Apache 2.0). All permitted. |

### Post-Design Re-Check

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality | PASS | RepositoryCodeGenerator is single-purpose. CustomNodeDefinition extension is minimal. |
| II. TDD | PASS | Contract tests for generator outputs. Integration tests for UI creation flow. |
| III. UX Consistency | PASS | "Create Repository Node" button follows existing panel button patterns. |
| IV. Performance | PASS | No O(n²) algorithms. Database singleton avoids repeated initialization. |
| V. Observability | PASS | Error output port provides runtime error visibility. |
| Licensing | PASS | All new dependencies are Apache 2.0. |

## Project Structure

### Documentation (this feature)

```text
specs/036-entity-repository-nodes/
├── plan.md              # This file
├── spec.md              # Feature specification
├── research.md          # Technology decisions
├── data-model.md        # Entity definitions
├── quickstart.md        # Integration scenarios
├── contracts/           # Interface contracts
│   └── internal-interfaces.md
├── checklists/
│   └── requirements.md  # Quality checklist
└── tasks.md             # Task breakdown (via /speckit.tasks)
```

### Source Code (repository root)

```text
graphEditor/src/jvmMain/kotlin/
├── repository/
│   └── CustomNodeDefinition.kt        # MODIFY: Add isRepository, sourceIPTypeId fields
├── ui/
│   └── PropertiesPanel.kt             # MODIFY: Add "Create Repository Node" button
├── viewmodel/
│   └── PropertiesPanelViewModel.kt    # MODIFY: Add repository creation logic
└── Main.kt                            # MODIFY: Wire repository creation callback

kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/
├── RepositoryCodeGenerator.kt          # NEW: Generates Entity/DAO/Repository/Database code
├── ModuleGenerator.kt                  # MODIFY: Integrate repository code generation
└── RuntimeFlowGenerator.kt            # MODIFY: Handle repository node runtime wiring

kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/
└── RepositoryCodeGeneratorTest.kt      # NEW: Tests for generated code

graphEditor/src/jvmTest/kotlin/
├── repository/
│   └── CustomNodeDefinitionTest.kt     # MODIFY: Test repository node creation
└── viewmodel/
    └── PropertiesPanelViewModelTest.kt # NEW or MODIFY: Test repository creation flow
```

**Structure Decision**: Follows the existing multi-module KMP pattern. No new Gradle modules needed for US1-US2. US3-US4 add generated persistence code within existing generated module output structure (adds `persistence/` package).

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| Repository pattern (per constitution "direct DB access") | User explicitly requested Repository wrapping DAOs. Standard KMP Room pattern. | Direct DAO access in runtime nodes would couple node code to Room annotations, violating clean architecture. Repository provides testable abstraction. |
