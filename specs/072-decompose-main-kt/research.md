# Research: Decompose graphEditor Main.kt

**Feature**: 072-decompose-main-kt
**Date**: 2026-04-13

## R1: Compose Desktop Refactoring Patterns

**Decision**: Extract composables to separate files with explicit parameter passing. Use a state holder pattern for complex initialization.

**Rationale**: Compose Desktop composables are regular functions — they can be called across files within the same module without restriction. The key constraint is that `remember` blocks must stay within composable scope, so extracted initialization functions must themselves be `@Composable`. State created in one composable can be passed to sub-composables as parameters.

**Alternatives considered**:
- CompositionLocal for shared state: Rejected — adds implicit coupling and makes dependencies harder to trace. Only appropriate for truly cross-cutting concerns (theme, navigation).
- ViewModel pattern: Already partially used (GraphEditorViewModel, etc.). The existing ViewModels handle specific concerns. The orchestration state in GraphEditorApp is Compose-specific and belongs in composable scope, not a ViewModel.

## R2: State Holder Pattern for Complex Composables

**Decision**: Create a `GraphEditorState` class that bundles the 30+ state variables initialized in `GraphEditorApp`, returned by a `@Composable fun rememberGraphEditorState()` factory.

**Rationale**: Google's Compose guidance recommends "state hoisting" and "state holder" patterns for complex composables. A state holder class:
- Makes the parameter list of sub-composables manageable
- Groups related state (registries, ViewModels, UI toggles)
- Enables a clean separation between "what state exists" and "how it's laid out"

**Alternatives considered**:
- Passing 30+ individual parameters: Rejected — unwieldy and error-prone
- Single god-object ViewModel: Rejected — mixes Compose-lifecycle state (remember) with ViewModel-lifecycle state

## R3: File Organization for Extracted Composables

**Decision**: Place all extracted UI composables in `io.codenode.grapheditor.ui` package. Place utility functions in `io.codenode.grapheditor.util` package.

**Rationale**: Follows existing project convention where UI composables live in the `ui/` directory. The `util/` package is new but appropriate for pure functions (file resolution, IP type resolution) that are not composables.

**Alternatives considered**:
- Keep utilities as top-level functions in Main.kt: Rejected — goal is to empty Main.kt
- Put utilities in existing files: Rejected — none of the existing files are a natural fit

## R4: TDD Strategy for Refactoring

**Decision**: Use compilation + existing test suite as the behavioral contract. Each extraction step must:
1. Compile successfully (`./gradlew :graphEditor:compileKotlinJvm`)
2. Pass all existing tests (`./gradlew :graphEditor:jvmTest`)

**Rationale**: This is a pure structural refactoring — no logic changes. The existing test suite verifies behavior hasn't changed. Writing new unit tests for each extracted composable would be low-value since the composables are identical to the original code. The real regression risk is broken imports, lost state, or recomposition order changes — all caught by compilation and existing tests.

**Alternatives considered**:
- Write snapshot tests for each composable: Rejected — Compose Desktop snapshot testing has limited tooling and the existing tests already cover behavior
- Write new integration tests: Rejected — would test the same thing the existing tests already cover

## R5: Extraction Order Risk Assessment

**Decision**: Extract in risk-ascending order: self-contained functions first (TopToolbar, StatusBar, utilities), then GraphEditorApp decomposition.

**Rationale**: Self-contained functions have zero coupling risk — they already accept all inputs as parameters. The GraphEditorApp decomposition is higher risk because it requires state to be passed across new function boundaries. Doing low-risk extractions first establishes green baseline and provides safe rollback points.

## R6: Connection Color Mapper Complexity

**Decision**: Extract the connection color computation (lines 713–847, ~135 lines) to its own file as a `@Composable fun rememberConnectionColors(...)` function.

**Rationale**: This is the densest computation block in GraphEditorApp. It computes color maps for connections based on IP types and boundary ports. It's a pure computation with clear inputs (graph, registries, navigation context) and outputs (color maps). Extracting it significantly improves readability of the layout composable.

**Alternatives considered**:
- Leave inline: Rejected — it's 135 lines of non-layout logic embedded in layout code
- Move to a ViewModel: Rejected — it uses `remember` and `derivedStateOf` which are Compose-specific
