# Research: flowGraph-execute Module Extraction

## R1: Which Files Belong in flowGraph-execute?

**Decision**: Extract 6 files: 5 from circuitSimulator (entire module) + 1 from graphEditor (ModuleSessionFactory).

**Rationale**: The circuitSimulator module maps cleanly to the execute bucket per the feature 064 architecture audit. All 5 source files handle runtime execution concerns: lifecycle orchestration, animation choreography, debug snapshots, animation data model, and a placeholder stub. ModuleSessionFactory creates RuntimeSession instances and belongs logically with the execution pipeline it orchestrates.

**Files to extract (6)**:
1. `circuitSimulator/src/commonMain/.../RuntimeSession.kt` (239 lines, kotlinx-coroutines StateFlow)
2. `circuitSimulator/src/commonMain/.../DataFlowAnimationController.kt` (175 lines, kotlinx-coroutines only)
3. `circuitSimulator/src/commonMain/.../DataFlowDebugger.kt` (98 lines, kotlinx-coroutines only)
4. `circuitSimulator/src/commonMain/.../ConnectionAnimation.kt` (45 lines, pure data class)
5. `circuitSimulator/src/commonMain/.../CircuitSimulator.kt` (25 lines, placeholder)
6. `graphEditor/src/jvmMain/.../ui/ModuleSessionFactory.kt` (258 lines, java.lang.reflect + java.io.File)

**Internal cross-references**: RuntimeSession is imported by ModuleSessionFactory — both are in the extraction set.

**Alternatives considered**:
- Include RuntimePreviewPanel.kt: Rejected — it's a @Composable UI file, stays in graphEditor per the "Compose stays in graphEditor" principle
- Include FlowGraphCanvas.kt animation rendering: Rejected — it's a @Composable that consumes ConnectionAnimation data, not an execution concern

## R2: Runtime Type for CodeNode

**Decision**: Use `In2AnyOut3Runtime<String, String, String, String, String>` with `anyInput = true` and `ProcessResult3`.

**Rationale**: The execute CodeNode has 2 inputs (flowGraphModel, nodeDescriptors) and 3 outputs (executionState, animations, debugSnapshots). Using `anyInput` mode because:
- `flowGraphModel` arrives when the user changes the graph being executed
- `nodeDescriptors` arrives when node discovery completes (from flowGraph-inspect)
- Either input independently provides context for runtime pipeline configuration
- The 3 outputs use `ProcessResult3` for selective emission — not every input triggers all outputs

This matches the `FlowGraphPersistCodeNode` pattern (also 2-in/3-out, anyInput, ProcessResult3).

**Alternatives considered**:
- `In2Out3Runtime` (requires ALL inputs): Rejected — nodeDescriptors can arrive before flowGraphModel
- Separate CodeNodes for execution/animation/debugging: Rejected — over-decomposition, these concerns are tightly coupled in RuntimeSession

## R3: KMP Source Set Split

**Decision**: 5 circuitSimulator files in commonMain (no JVM-specific APIs), ModuleSessionFactory + CodeNode + tests in jvmMain/jvmTest.

**Rationale**:
- The 5 circuitSimulator files use only kotlinx-coroutines (StateFlow, Channel, CoroutineScope, delay) — no JVM-specific imports. Analysis confirms zero `import java.*`, zero `import androidx.compose.*` despite Compose being in circuitSimulator's build.gradle.kts.
- ModuleSessionFactory uses `java.lang.reflect.Proxy`, `java.lang.reflect.InvocationHandler`, and `java.io.File` — JVM-only APIs.
- The FlowGraphExecuteCodeNode will likely use JVM types from its internal implementation.

**Alternatives considered**:
- All files in jvmMain: Unnecessary — the 5 circuitSimulator files are genuinely multiplatform
- Split ModuleSessionFactory between common/jvm: Not feasible — reflection is JVM-only

## R4: circuitSimulator Compose Dependencies

**Decision**: Drop Compose dependencies from flowGraph-execute. They are unnecessary.

**Rationale**: The circuitSimulator build.gradle.kts includes `compose.runtime`, `compose.foundation`, `compose.material3`, and `compose.ui`, but analysis of all 5 source files shows zero Compose imports. These dependencies were likely added speculatively during initial module creation and are unused. flowGraph-execute should include only `kotlinx-coroutines` and project dependencies.

**Alternatives considered**:
- Keep Compose dependencies "just in case": Rejected — unnecessary dependencies violate Code Quality First principle

## R5: Dependencies

**Decision**: flowGraph-execute depends on `:fbpDsl` and `:flowGraph-inspect`.

**Rationale**:
- `:fbpDsl` — FlowGraph, DynamicPipelineController, DynamicPipelineBuilder, ModuleController, ExecutionState, CodeNodeFactory, CodeNodeDefinition, runtime types
- `:flowGraph-inspect` — NodeDefinitionRegistry (used by ModuleSessionFactory.registry)
- No dependency on `:graphEditor` — consumers in graphEditor depend on flowGraph-execute, not the reverse
- No dependency on `:persistence` — ModuleSessionFactory accesses PersistenceBootstrap entirely via reflection (zero compile-time coupling)
- No Koin dependency — ModuleSessionFactory does not import any Koin APIs

**Dependency direction**: graphEditor → flowGraph-execute → flowGraph-inspect → {flowGraph-types, flowGraph-persist} → fbpDsl

## R6: Data-Oriented Port Naming

**Decision**: Use data-oriented names matching architecture.flow.kt: flowGraphModel, nodeDescriptors, executionState, animations, debugSnapshots.

**Rationale**: Feature 064 R6 established data-oriented naming over service-oriented naming. These names describe the data shape flowing through ports:
- `flowGraphModel` — the flow graph structure to execute (JSON command data)
- `nodeDescriptors` — discovered node definitions needed for pipeline building
- `executionState` — lifecycle state (IDLE, RUNNING, PAUSED, ERROR)
- `animations` — active connection animation dot data for canvas rendering
- `debugSnapshots` — per-connection most-recent-value captures

Already defined in architecture.flow.kt.

## R7: Call Site Migration Scope

**Decision**: 4 graphEditor files need import updates, plus idePlugin build dependency update and 1 test migration.

**Call sites that import from circuitSimulator** (graphEditor files):
1. `Main.kt` — imports ConnectionAnimation, RuntimeSession
2. `ui/FlowGraphCanvas.kt` — imports ConnectionAnimation
3. `ui/RuntimePreviewPanel.kt` — imports RuntimeSession
4. `ui/ModuleSessionFactory.kt` — imports RuntimeSession (but this file moves, so its import becomes internal)

**Build dependency updates**:
- `graphEditor/build.gradle.kts` — replace `:circuitSimulator` with `:flowGraph-execute`
- `idePlugin/build.gradle.kts` — replace `:circuitSimulator` with `:flowGraph-execute` (has build dependency but zero source imports from circuitSimulator)

**Test files to migrate**:
- `circuitSimulator/src/commonTest/kotlin/characterization/RuntimeSessionCharacterizationTest.kt` → move to flowGraph-execute

**Alternatives considered**:
- Leave idePlugin dependency on circuitSimulator: Not possible — circuitSimulator source files are being removed

## R8: Package Structure

**Decision**: Use `io.codenode.flowgraphexecute` as root package with `node/` and `characterization/` sub-packages for tests.

**Rationale**: Follows the pattern from flowGraph-inspect (`io.codenode.flowgraphinspect`). The 5 circuitSimulator files are all in the same flat package (`io.codenode.circuitsimulator`) and stay flat in the new module. ModuleSessionFactory joins the same package. Sub-packages only for the new CodeNode and migrated tests.

Structure:
- `io.codenode.flowgraphexecute` — RuntimeSession, DataFlowAnimationController, DataFlowDebugger, ConnectionAnimation, CircuitSimulator, ModuleSessionFactory
- `io.codenode.flowgraphexecute.node` — FlowGraphExecuteCodeNode (+ test)
- `io.codenode.flowgraphexecute.characterization` — RuntimeSessionCharacterizationTest (migrated)

**Note on duplicate code**: DataFlowAnimationController and DataFlowDebugger both implement identical `buildPortConnectionMap()` logic. This duplication is preserved as-is during extraction — refactoring is out of scope.
