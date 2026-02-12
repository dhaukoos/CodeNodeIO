# Research: StopWatch Virtual Circuit Refactor

**Feature**: 011-stopwatch-refactor
**Date**: 2026-02-12

## Overview

This document captures research findings for implementing compile-time required property validation, properties panel enhancements, and redundant code removal.

---

## Decision 1: Validation Integration Point

**Decision**: Add property validation in CompilationService.compileToModule() after FlowGraph.validate() and before module generation.

**Rationale**:
- Follows existing validation pattern in the codebase
- Single point of validation before code generation
- Clear separation: structure validation (FlowGraph.validate) then property validation (new)
- User receives all validation errors at once, not incrementally

**Alternatives Considered**:
1. **Add to FlowGraph.validate()** - Rejected because it would require passing NodeTypeDefinition registry to model layer
2. **Validate in ModuleGenerator** - Rejected because validation should fail before any generation starts
3. **Lazy validation during UI** - Rejected because compile-time validation is the explicit requirement

---

## Decision 2: Required Property Detection for GENERIC Nodes

**Decision**: Create a hardcoded list of required properties for GENERIC node types: `_useCaseClass` is required, `_genericType` is required, `speedAttenuation` is optional.

**Rationale**:
- GENERIC nodes have well-defined required properties based on code generation needs
- `_useCaseClass` is mandatory because it links to the ProcessingLogic implementation class
- `_genericType` is mandatory because it defines the port configuration
- JSON Schema-based detection is already in place for UI; compile validation can use simpler approach
- Avoids complexity of runtime schema parsing for compilation

**Alternatives Considered**:
1. **Parse JSON Schema at compile time** - Rejected because GENERIC nodes don't have schema defined in NodeTypeDefinition
2. **Add required flags to CodeNode model** - Rejected because it would require model changes across modules
3. **Use reflection to detect missing implementations** - Rejected because too complex and slow

---

## Decision 3: Properties Panel Enhancement Approach

**Decision**: Add `_useCaseClass` as a visible editable field in PropertiesPanel for GENERIC nodes with required indicator.

**Rationale**:
- Currently `_useCaseClass` is filtered out (starts with `_`)
- Users need to see and edit this property to complete node configuration
- Follows existing pattern of showing required indicators with `*`

**Alternatives Considered**:
1. **Show all `_` prefixed properties** - Rejected because some are truly internal
2. **Create special section for "System Properties"** - Rejected as over-engineering
3. **Auto-generate _useCaseClass from node name** - Rejected because users need explicit control

---

## Decision 4: Redundant Code Removal Strategy

**Decision**: Remove `createStopWatchFlowGraph()` from App.kt and import `stopWatchFlowGraph` directly from StopWatch module.

**Rationale**:
- `stopWatchFlowGraph` is already a public `val` exported from StopWatch.flow.kt
- Both functions create identical FlowGraph structures
- Single source of truth in the StopWatch module
- KMPMobileApp already has StopWatch module dependency

**Alternatives Considered**:
1. **Keep both and sync manually** - Rejected because defeats purpose of module architecture
2. **Create factory function in StopWatch** - Rejected because `val` export is simpler
3. **Generate App.kt from StopWatch** - Rejected as over-engineering

---

## Technical Findings

### Existing Validation Infrastructure

**File**: `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/FlowGraph.kt`

```kotlin
data class ValidationResult(
    val success: Boolean,
    val errors: List<String> = emptyList()
)

fun validate(): ValidationResult {
    val errors = mutableListOf<String>()
    // Validates structure, nodes, connections
    return ValidationResult(success = errors.isEmpty(), errors = errors)
}
```

**Pattern**: Accumulate errors in list, return success only if list empty.

### PropertiesPanel Property Detection

**File**: `graphEditor/src/jvmMain/kotlin/ui/PropertiesPanel.kt`

```kotlin
// Line 553: Currently filters out internal properties
val configProperties = state.properties.filterKeys { !it.startsWith("_") }
```

**Pattern**: Properties starting with `_` are hidden from UI. Need exception for `_useCaseClass`.

### CompilationService Flow

**File**: `graphEditor/src/jvmMain/kotlin/compilation/CompilationService.kt`

```kotlin
fun compileToModule(...): CompilationResult {
    // 1. Validate flow graph (structure)
    val validation = flowGraph.validate()
    if (!validation.success) return CompilationResult(success = false, ...)

    // 2. Generate module <-- ADD PROPERTY VALIDATION HERE
    val generatedModule = moduleGenerator.generateModule(...)

    // 3. Write files
    generatedModule.writeTo(moduleDir)
}
```

**Integration Point**: Add property validation between step 1 and step 2.

### StopWatch Module Export

**File**: `StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/StopWatch.flow.kt`

```kotlin
val stopWatchFlowGraph = flowGraph("StopWatch", version = "1.0.0", ...) {
    val timerEmitter = codeNode("TimerEmitter", nodeType = "GENERIC") {
        config("_genericType", "in0out2")
        config("_useCaseClass", "io.codenode.stopwatch.usecases.TimerEmitterComponent")
        config("speedAttenuation", "1000")
    }
    ...
}
```

**Export Pattern**: Public `val` at package level, accessible via `import io.codenode.stopwatch.stopWatchFlowGraph`

---

## Risk Assessment

| Risk | Mitigation |
|------|------------|
| Breaking existing flow graphs | Only validate GENERIC nodes; non-GENERIC nodes unchanged |
| Compile errors in KMPMobileApp | Test import before removing function |
| UI confusion with new required fields | Add clear "Use Case Class" label and help text |

---

## Dependencies

No new dependencies required. All functionality uses existing:
- kotlinx-coroutines (existing)
- Compose Desktop (existing)
- fbpDsl model classes (existing)

---

## Next Steps

1. Create data-model.md with PropertyValidationResult structure
2. Create quickstart.md with implementation steps
3. Define validation interface in contracts/
