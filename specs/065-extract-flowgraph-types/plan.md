# Implementation Plan: Extract flowGraph-types Module

**Branch**: `065-extract-flowgraph-types` | **Date**: 2026-04-05 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/065-extract-flowgraph-types/spec.md`

## Summary

Extract the first vertical-slice module (flowGraph-types) from graphEditor as Phase B Step 1 of the migration plan defined in feature 064. Move 9 IP type lifecycle files (discovery, registry, repository, file generation, migration) into a new independently buildable KMP module. The module boundary is FBP-native: a coarse-grained CodeNode with 3 inputs (`filesystemPaths`, `classpathEntries`, `ipTypeCommands`) and 1 output (`ipTypeMetadata`). Consumers receive IP type data through the output port and query it locally; mutations flow in as commands through the input port — no service interfaces. Update 6 call sites to consume data from CodeNode ports, TDD-test the CodeNode contract, add a new `ipTypeCommands` port and connection to `architecture.flow.kt`, and populate the empty flowGraph-types GraphNode container with the live CodeNode. A prerequisite move of PlacementLevel.kt to fbpDsl resolves a shared-vocabulary dependency. Total: 10 file moves, 6 call site updates, 1 CodeNode (3-in/1-out, anyInput), 1 architecture FlowGraph wiring (19→20 connections).

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP - Kotlin Multiplatform)
**Primary Dependencies**: fbpDsl (core FBP domain model), Koin 4.0.0 (DI), kotlinx-coroutines 1.8.0, kotlinx-serialization 1.6.2
**Storage**: Filesystem (`~/.codenode/custom-ip-types.json` for legacy FileIPTypeRepository)
**Testing**: `./gradlew :graphEditor:jvmTest :kotlinCompiler:jvmTest :circuitSimulator:jvmTest` (kotlin.test + JUnit 5 + kotlinx-coroutines-test); new module: `./gradlew :flowGraph-types:jvmTest`
**Target Platform**: JVM Desktop (macOS/Linux/Windows); KMP module structure for future iOS parity
**Project Type**: KMP multi-module (adding flowGraph-types as the first vertical-slice module)
**Performance Goals**: N/A — structural refactor, no new runtime behavior
**Constraints**: All existing characterization tests must pass at every intermediate step (Strangler Fig invariant); module must depend only on fbpDsl (no cycles); KMP-first module structure required
**Scale/Scope**: 9 files extracted from graphEditor, 1 file moved to fbpDsl, 6 call sites migrated to data flow consumption, 1 CodeNode (3-in/1-out, anyInput) + TDD tests, 1 architecture.flow.kt wiring (19→20 connections)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Pre-Research Check

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality First | PASS | Extraction enforces single responsibility per module. Service interfaces make dependencies explicit. |
| II. Test-Driven Development | PASS | CodeNode tests written before implementation (TDD). Characterization tests provide regression safety net. All tests pass at every intermediate step. |
| III. User Experience Consistency | PASS | No user-facing changes. Pure structural refactor preserves all existing behavior. |
| IV. Performance Requirements | PASS | No runtime performance impact. No new code paths, only reorganization. |
| V. Observability & Debugging | PASS | Service interfaces make module boundaries explicit and debuggable. Architecture FlowGraph makes the dependency visible. |
| Licensing | PASS | No new dependencies. flowGraph-types uses only fbpDsl (Apache 2.0) and Kotlin stdlib. |
| Refactoring Specs | PASS | Constitution explicitly permits refactoring specs: "acceptance criteria center on behavior unchanged, tests green, architecture improved." |

### Post-Design Re-Check

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality First | PASS | FBP-native data flow boundary gives the module a single, well-defined contract (3 input ports, 1 output port). KMP source set split (commonMain/jvmMain) properly separates platform-dependent code. Internal classes remain cohesive within the module. |
| II. Test-Driven Development | PASS | CodeNode tests written first (TDD) cover port signatures, data flow through channels, command processing, and boundary conditions. Characterization tests validate regression safety at every Strangler Fig step. |
| III. User Experience Consistency | PASS | No UI changes. |
| IV. Performance Requirements | PASS | No runtime changes. CodeNode channel wiring adds negligible overhead. |
| V. Observability & Debugging | PASS | FBP-native boundary makes data flow visible and inspectable in the architecture FlowGraph. The CodeNode's ports and connections are visually verifiable in the graphEditor. |

No gate violations. No complexity tracking needed.

## Project Structure

### Documentation (this feature)

```text
specs/065-extract-flowgraph-types/
├── plan.md              # This file
├── spec.md              # Feature specification
├── research.md          # Research decisions (R1-R7)
├── quickstart.md        # Validation scenarios
├── checklists/
│   └── requirements.md  # Spec quality checklist
└── tasks.md             # Task list (created by /speckit.tasks)
```

### Source Code (repository root)

```text
# Prerequisite: PlacementLevel moves to fbpDsl shared vocabulary
fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/PlacementLevel.kt    # Moved from graphEditor

# New module: flowGraph-types
flowGraph-types/
├── build.gradle.kts                                                        # KMP module config, depends on :fbpDsl
├── src/
│   ├── commonMain/kotlin/io/codenode/flowgraphtypes/
│   │   ├── model/
│   │   │   ├── IPProperty.kt                                               # Moved from graphEditor
│   │   │   ├── IPPropertyMeta.kt                                           # Moved from graphEditor
│   │   │   ├── IPTypeFileMeta.kt                                           # Moved from graphEditor
│   │   │   └── SerializableIPType.kt                                       # Moved from graphEditor
│   │   └── registry/
│   │       └── IPTypeRegistry.kt                                           # Moved from graphEditor
│   ├── jvmMain/kotlin/io/codenode/flowgraphtypes/
│   │   ├── discovery/
│   │   │   └── IPTypeDiscovery.kt                                          # Moved from graphEditor (uses java.io.File)
│   │   ├── repository/
│   │   │   ├── FileIPTypeRepository.kt                                     # Moved from graphEditor (uses java.io.File)
│   │   │   └── IPTypeMigration.kt                                          # Moved from graphEditor (uses java.io.File transitively)
│   │   ├── generator/
│   │   │   └── IPTypeFileGenerator.kt                                      # Moved from graphEditor (uses java.io.File)
│   │   └── node/
│   │       └── FlowGraphTypesCodeNode.kt                                   # NEW: coarse-grained CodeNode (3-in/1-out, anyInput)
│   └── jvmTest/kotlin/io/codenode/flowgraphtypes/
│       └── node/
│           └── FlowGraphTypesCodeNodeTest.kt                               # NEW: TDD tests for CodeNode port contract

# Modified: graphEditor depends on new module
graphEditor/build.gradle.kts                                                # Add dependency on :flowGraph-types
graphEditor/architecture.flow.kt                                            # Populate flowGraph-types GraphNode container

# Modified: 6 call sites migrated to data flow consumption
graphEditor/src/jvmMain/kotlin/state/GraphState.kt                          # IPTypeRegistry → locally-held ipTypeMetadata
graphEditor/src/jvmMain/kotlin/serialization/GraphNodeTemplateSerializer.kt # IPTypeRegistry → ipTypeMetadata parameter
graphEditor/src/jvmMain/kotlin/viewmodel/IPGeneratorViewModel.kt            # IPTypeRegistry/Discovery/Repository → ipTypeCommands + ipTypeMetadata
graphEditor/src/jvmMain/kotlin/viewmodel/SharedStateProvider.kt             # IPTypeRegistry instance → ipTypeMetadata data
graphEditor/src/jvmMain/kotlin/ui/PropertiesPanel.kt                        # IPTypeRegistry → locally-held ipTypeMetadata
graphEditor/src/jvmMain/kotlin/viewmodel/IPPaletteViewModel.kt              # IPTypeRegistry/Repository → ipTypeCommands + ipTypeMetadata

# Modified: settings.gradle.kts
settings.gradle.kts                                                         # Add include("flowGraph-types")
```

**Structure Decision**: The new flowGraph-types module follows the established KMP pattern from fbpDsl. Files are split between `commonMain` (5 platform-independent model/registry files) and `jvmMain` (4 filesystem-dependent files + CodeNode). No service interface layer — the module's external boundary is the CodeNode's ports. The CodeNode and its tests are in `jvmMain` since they depend on JVM-specific internal implementations (IPTypeDiscovery, FileIPTypeRepository, IPTypeFileGenerator).

## Key Technical Decisions

### 1. PlacementLevel Moves to fbpDsl (Prerequisite)

PlacementLevel is a simple enum (MODULE, PROJECT, UNIVERSAL) used by both IP type files and node discovery files. It must move to fbpDsl before the extraction begins, since flowGraph-types cannot depend on graphEditor. This is a shared vocabulary term analogous to IPColor and InformationPacketType which already live in fbpDsl. See research R1.

### 2. KMP Source Set Split (commonMain / jvmMain)

Six of the 9 files are pure Kotlin with no platform dependencies (models + registry) and go in `commonMain`. Three files use `java.io.File` for filesystem operations and go in `jvmMain`. This satisfies the KMP-first mandate while avoiding premature `expect`/`actual` abstractions. See research R2 and R7.

### 3. FBP-Native Data Flow Boundary (No Service Interfaces)

The module boundary is the CodeNode's ports — data flows in and out. No service interfaces (IPTypeRegistryService, etc.). Consumers receive `ipTypeMetadata` as data and query it locally. Mutations flow in as `ipTypeCommands`. This follows the data-oriented naming convention established in 064 R6: `ipTypeMetadata` (not `ipTypeRegistry`), reflecting that the interface is the data shape, not a service contract. See research R3.

### 4. CodeNode as In3AnyOut1Runtime Wrapper

The flowGraph-types CodeNode implements `CodeNodeDefinition` and creates an `In3AnyOut1Runtime<String, String, String, String>`. The 3 inputs (filesystemPaths, classpathEntries, ipTypeCommands) and 1 output (ipTypeMetadata) support both passive discovery and active mutation commands. Uses `anyInput` mode — re-emits updated metadata whenever any input changes. See research R4.

### 5. Composition Root Channel Wiring (No Koin Service Interfaces)

The composition root orchestrates the CodeNode by wiring its input/output channels directly — no Koin-wired service interfaces. The composition root sends filesystem context and mutation commands to input ports, and distributes the `ipTypeMetadata` output to ViewModels and UI state. This matches the 064 vision that architecture.flow.kt becomes the actual application wiring. See research R5.

### 6. Six Call Sites Migrated to Data Flow (Corrected from Eight)

Detailed usage analysis revealed that ConnectionContextMenu.kt and GraphNodePaletteSection.kt don't access IP type internals directly — they already consume downstream data. The 6 actual call sites are migrated: read-only consumers hold `ipTypeMetadata` locally, mutating consumers send `ipTypeCommands`. See research R6.

### 7. Strangler Fig Execution Sequence

The extraction follows a strict sequence with tests at every step:
1. Move PlacementLevel to fbpDsl → tests pass
2. Create flowGraph-types module with build config → compiles
3. Copy 9 files to new module (keep originals) → tests pass with both copies
4. Write CodeNode tests (TDD) → tests fail (no implementation)
5. Implement CodeNode (3-in/1-out, anyInput) → CodeNode tests pass
6. Update 6 call sites to consume data from CodeNode ports → tests pass
7. Remove original files from graphEditor → tests pass
8. Add `ipTypeCommands` port and connection to architecture.flow.kt → update ArchitectureFlowKtsTest
9. Populate flowGraph-types GraphNode container with CodeNode → ArchitectureFlowKtsTest passes
