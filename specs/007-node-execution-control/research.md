# Research: Node ExecutionState and ControlConfig

**Feature**: 007-node-execution-control
**Date**: 2026-02-07

## Research Tasks

### 1. State Propagation Pattern in Immutable Data Structures

**Question**: What is the optimal pattern for hierarchical state propagation in Kotlin's immutable data class model?

**Decision**: Explicit propagation via `copy()` chain with recursive child updates

**Rationale**:
- Kotlin data classes are immutable by convention; state changes create new instances
- The existing codebase uses `copy()` extensively for all mutations (see `GraphNode.withChildren()`, `CodeNode.withExecutionState()`)
- Recursive propagation maintains referential transparency and enables easy rollback/undo
- Performance is acceptable for expected graph sizes (100-500 nodes) since tree updates are O(n) at worst

**Alternatives Considered**:
1. **Mutable state with observers**: Rejected - violates immutability convention, complicates undo/redo, threading issues
2. **Event-based propagation**: Rejected - adds complexity, harder to reason about state consistency
3. **Lazy propagation on read**: Rejected - complicates caching, segment invalidation patterns already established in codebase

**Implementation Approach**:
```kotlin
// In GraphNode
fun withExecutionState(newState: ExecutionState, propagate: Boolean = true): GraphNode {
    val updatedChildren = if (propagate && !controlConfig.independentControl) {
        childNodes.map { child ->
            when (child) {
                is CodeNode -> child.copy(executionState = newState)
                is GraphNode -> child.withExecutionState(newState, propagate = true)
            }
        }
    } else {
        childNodes
    }
    return copy(executionState = newState, childNodes = updatedChildren)
}
```

---

### 2. Kotlin Sealed Class Extension with Abstract Properties

**Question**: Can we add abstract properties (`executionState`, `controlConfig`) to the existing Node sealed class without breaking binary compatibility?

**Decision**: Yes, add abstract properties to Node sealed class; concrete implementations in CodeNode and GraphNode

**Rationale**:
- Kotlin sealed classes support abstract members added after initial definition
- All subclasses (CodeNode, GraphNode) are defined in the same module (fbpDsl)
- CodeNode already has these properties; we're promoting them to the base class
- GraphNode needs to add implementations (with default values)

**Implementation Details**:
```kotlin
// In Node.kt - add abstract properties
sealed class Node {
    // Existing abstract properties
    abstract val id: String
    abstract val name: String
    // ... existing properties ...

    // NEW: Execution control properties (moved from CodeNode)
    abstract val executionState: ExecutionState
    abstract val controlConfig: ControlConfig
}

// In CodeNode.kt - already has these, just verify they satisfy the abstract contract
data class CodeNode(
    // ... existing properties ...
    override val executionState: ExecutionState = ExecutionState.IDLE,
    override val controlConfig: ControlConfig = ControlConfig()
) : Node()

// In GraphNode.kt - add new properties
data class GraphNode(
    // ... existing properties ...
    override val executionState: ExecutionState = ExecutionState.IDLE,  // NEW
    override val controlConfig: ControlConfig = ControlConfig()         // NEW
) : Node()
```

**Backward Compatibility**:
- Default values ensure existing CodeNode instantiation patterns work unchanged
- GraphNode defaults to IDLE state and default ControlConfig
- Serialization compatibility maintained through @Serializable annotations

---

### 3. KMP Module Generation Patterns

**Question**: What is the best approach for generating KMP modules from flowGraphs?

**Decision**: Template-based code generation with gradle build file synthesis

**Rationale**:
- CodeNodeIO already uses code generation patterns (see kotlinCompiler/ module placeholder)
- Gradle KMP project structure is well-documented and standardized
- Generated module should be self-contained with minimal external dependencies
- RootControlNode provides natural entry point for generated module

**Generated Module Structure**:
```text
<module-name>/
├── build.gradle.kts           # Generated KMP configuration
├── settings.gradle.kts        # Module settings
└── src/
    └── commonMain/
        └── kotlin/<package>/
            ├── <FlowName>Graph.kt        # Generated FlowGraph instantiation
            ├── <FlowName>Controller.kt   # RootControlNode wrapper
            └── nodes/
                └── *.kt                  # Generated CodeNode implementations
```

**Key Generation Steps**:
1. Prompt user for module name and target package
2. Generate build.gradle.kts with KMP multiplatform configuration
3. Generate FlowGraph class that constructs the flow structure
4. Generate RootControlNode wrapper for execution control
5. Optionally generate stub implementations for CodeNode processing logic

**Note**: Full implementation is P5 priority - foundational work in P1-P4 enables this.

---

### 4. Performance Optimization for Tree Traversal

**Question**: How do we ensure state propagation meets the < 100ms target for 5-level deep hierarchies?

**Decision**: Direct recursive traversal with early-exit optimization for independent control

**Rationale**:
- Typical flow graphs have 10-100 nodes; 500 is large but manageable
- Recursive copy() is O(n) where n = total nodes
- Early exit when `independentControl` flag is set avoids unnecessary subtree traversal
- Performance benchmarks added to test suite to catch regressions

**Optimizations Applied**:
1. **Early exit on independentControl**: Skip entire subtree if node has independent control enabled
2. **Lazy child evaluation**: Only create new child list if state actually changes
3. **Batch updates**: RootControlNode uses single traversal for all root nodes

**Benchmark Target**:
- 100 nodes, 5 levels deep: < 10ms for full propagation
- 500 nodes, 5 levels deep: < 50ms for full propagation
- 1000+ nodes: performance warning logged, but should still complete < 100ms

**Test Strategy**:
```kotlin
@Test
fun `state propagation completes within 100ms for large graphs`() {
    val largeGraph = createDeepHierarchy(depth = 5, nodesPerLevel = 20) // 100 nodes
    val rootController = RootControlNode(largeGraph)

    val duration = measureTime {
        rootController.startAll()
    }

    assertTrue(duration.inWholeMilliseconds < 100)
}
```

---

## Summary of Decisions

| Research Area | Decision | Impact |
|---------------|----------|--------|
| State Propagation | Explicit copy() chain, recursive | Maintains immutability, enables undo |
| Sealed Class Extension | Add abstract properties to Node | CodeNode unchanged, GraphNode gets new properties |
| Module Generation | Template-based gradle project synthesis | Deferred to P5, foundational APIs in P1-P4 |
| Performance | Direct traversal with early-exit | Meets < 100ms target for typical graphs |

## Dependencies Identified

- No new external dependencies required
- Existing kotlinx-serialization handles new properties automatically
- Existing kotlinx-coroutines sufficient for any async propagation needs

## Risks and Mitigations

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Serialization breaking change | Low | High | Add versioning to .flow.kts format, backward-compatible deserialization |
| Performance regression on large graphs | Low | Medium | Benchmark tests in CI, performance monitoring |
| KMP module generation complexity | Medium | Low | P5 priority allows incremental approach |
