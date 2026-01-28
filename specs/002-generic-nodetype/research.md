# Research: Generic NodeType Definition

**Feature Branch**: `002-generic-nodetype`
**Date**: 2026-01-28
**Status**: Complete

## Summary

Research confirms that adding a GENERIC NodeCategory is a minimal change with well-established patterns throughout the codebase. The existing infrastructure supports dynamic category handling, requiring only targeted modifications to a few key files.

## Research Findings

### 1. NodeCategory Integration Points

**Decision**: Add GENERIC enum value to existing NodeCategory enum

**Rationale**: The codebase uses NodeCategory in a polymorphic pattern where adding a new enum value requires changes to exactly 2 code locations:
1. `NodeTypeDefinition.kt` - Enum definition
2. `DragAndDropHandler.kt` - Category-to-CodeNodeType mapping

**Alternatives Considered**:
- Creating a separate GenericNodeCategory class: REJECTED - breaks existing groupBy patterns and adds unnecessary complexity
- Using a string-based category system: REJECTED - loses type safety and compile-time validation

### 2. Category-to-CodeNodeType Mapping

**Decision**: Map GENERIC category to `CodeNodeType.TRANSFORMER` as default

**Rationale**:
- Transformers are the most flexible CodeNodeType, supporting arbitrary input/output configurations
- Existing patterns in DragAndDropHandler.kt show this is the standard approach
- Users can change the underlying behavior via UseCase class reference

**Code Pattern** (from DragAndDropHandler.kt lines 219-225):
```kotlin
val codeNodeType = when (nodeType.category) {
    NodeTypeDefinition.NodeCategory.UI_COMPONENT -> CodeNodeType.GENERATOR
    NodeTypeDefinition.NodeCategory.SERVICE -> CodeNodeType.TRANSFORMER
    NodeTypeDefinition.NodeCategory.TRANSFORMER -> CodeNodeType.TRANSFORMER
    NodeTypeDefinition.NodeCategory.VALIDATOR -> CodeNodeType.VALIDATOR
    NodeTypeDefinition.NodeCategory.API_ENDPOINT -> CodeNodeType.API_ENDPOINT
    NodeTypeDefinition.NodeCategory.DATABASE -> CodeNodeType.DATABASE
    NodeTypeDefinition.NodeCategory.GENERIC -> CodeNodeType.TRANSFORMER  // NEW
}
```

### 3. NodePalette Category Display

**Decision**: No changes needed to NodePalette.kt

**Rationale**: The palette already handles categories dynamically:
```kotlin
// Category name formatting (line 139):
category.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
// "GENERIC" → "Generic"

// Grouping (lines 41-49):
nodeTypes.groupBy { it.category }.toSortedMap()
```

The existing implementation will automatically:
- Display "Generic" as category header
- Group generic nodes together
- Support expand/collapse functionality
- Include in search filtering

### 4. Serialization Approach

**Decision**: Serialize generic node configuration as node properties/metadata

**Rationale**:
- NodeTypeDefinition itself is NOT serialized (it's runtime palette metadata)
- CodeNodes created from NodeTypeDefinitions ARE serialized
- Custom properties (port names, UseCase reference, icon) should be stored in node's configuration map

**DSL Syntax for Generic Nodes**:
```kotlin
codeNode("myGenericNode") {
    type = CodeNodeType.TRANSFORMER
    metadata("genericType", "in2out1")
    metadata("useCaseClass", "com.example.MyUseCase")
    metadata("iconResource", "custom-icon.svg")

    inputPort("emailAddress")  // Custom port name
    inputPort("validationRules")
    outputPort("result")
}
```

### 5. Code Generation Support

**Decision**: Extend existing code generators to handle UseCase references

**Rationale**:
- Current generators work with CodeNode, not NodeTypeDefinition
- Generic nodes become CodeNodes at drag-and-drop time
- UseCase reference should be used to generate delegation code

**Generated Code Pattern** (when UseCase provided):
```kotlin
class MyGenericNodeComponent(
    private val useCase: MyUseCase
) {
    suspend fun process(emailAddress: String, validationRules: String): Result {
        return useCase.execute(emailAddress, validationRules)
    }
}
```

**Generated Code Pattern** (when UseCase NOT provided):
```kotlin
class MyGenericNodeComponent {
    suspend fun process(input1: Any, input2: Any): Any {
        // TODO: Implement processing logic or provide UseCase reference
        throw NotImplementedError("Processing logic not implemented")
    }
}
```

### 6. Factory Function Design

**Decision**: Create `createGenericNodeType(numInputs: Int, numOutputs: Int)` factory function

**Rationale**:
- Single function covers all 36 combinations
- Input validation (0-5 range) happens at creation time
- Returns standard NodeTypeDefinition with pre-configured ports

**API Design**:
```kotlin
fun createGenericNodeType(
    numInputs: Int,
    numOutputs: Int,
    customName: String? = null,
    customDescription: String? = null,
    iconResource: String? = null,
    useCaseClass: String? = null
): NodeTypeDefinition {
    require(numInputs in 0..5) { "numInputs must be between 0 and 5" }
    require(numOutputs in 0..5) { "numOutputs must be between 0 and 5" }

    val defaultName = "in${numInputs}out${numOutputs}"
    // ... create NodeTypeDefinition
}
```

### 7. Port Naming Convention

**Decision**: Use "input1", "input2", etc. for inputs and "output1", "output2", etc. for outputs

**Rationale**:
- Consistent with FBP conventions
- Numbered ports allow easy reference in connections
- Custom names can override defaults via configuration

**Default Port Templates**:
```kotlin
// For in2out1:
portTemplates = listOf(
    PortTemplate("input1", Port.Direction.INPUT, Any::class, required = false),
    PortTemplate("input2", Port.Direction.INPUT, Any::class, required = false),
    PortTemplate("output1", Port.Direction.OUTPUT, Any::class, required = false)
)
```

### 8. Existing Test Infrastructure

**Finding**: Comprehensive test patterns exist for NodeTypeDefinition

**Files with relevant test patterns**:
- `graphEditor/src/jvmTest/kotlin/ui/NodePaletteTest.kt` - Category grouping tests
- `graphEditor/src/jvmTest/kotlin/serialization/GraphSerializationTest.kt` - Roundtrip tests
- `fbpDsl/src/commonTest/kotlin/model/PropertyConfigurationTest.kt` - Configuration validation

**Test Strategy**:
1. Unit tests for factory function (all 36 combinations + edge cases)
2. Integration tests for palette display
3. Roundtrip serialization tests
4. Contract tests for code generation

## Files Requiring Changes

### Must Modify

| File | Change |
|------|--------|
| `fbpDsl/.../model/NodeTypeDefinition.kt` | Add GENERIC to NodeCategory enum |
| `graphEditor/.../ui/DragAndDropHandler.kt` | Add GENERIC case to when expression |
| `graphEditor/.../Main.kt` | Add generic nodes to sample node types |

### New Files to Create

| File | Purpose |
|------|---------|
| `fbpDsl/.../factory/GenericNodeTypeFactory.kt` | Factory function implementation |
| `fbpDsl/.../factory/GenericNodeTypeFactoryTest.kt` | Unit tests |
| `graphEditor/.../ui/GenericNodePaletteTest.kt` | UI integration tests |
| `graphEditor/.../serialization/GenericNodeSerializationTest.kt` | Roundtrip tests |
| `kotlinCompiler/.../generator/GenericNodeGenerator.kt` | Code generation for UseCase refs |
| `idePlugin/.../resources/icons/generic-node.svg` | Default icon |

### No Changes Needed

- `graphEditor/.../ui/NodePalette.kt` - Already handles dynamic categories
- `graphEditor/.../serialization/FlowGraphSerializer.kt` - Serializes CodeNodes, not NodeTypeDefinitions
- `graphEditor/.../serialization/FlowGraphDeserializer.kt` - Deserializes CodeNodes
- `kotlinCompiler/.../generator/KotlinCodeGenerator.kt` - Works with CodeNodes
- `fbpDsl/.../validation/PropertyValidator.kt` - Category-agnostic

## Dependencies

No new external dependencies required. All functionality can be implemented using:
- Existing Kotlin stdlib
- Existing KotlinPoet (for code generation)
- Existing test frameworks (kotlin.test, JUnit 5)

## Risks and Mitigations

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Breaking existing category consumers | Low | High | Exhaustive `when` expressions will fail to compile if case missing |
| Performance degradation with 36 combinations | Low | Medium | Lazy creation, no precomputed combinations |
| Serialization format incompatibility | Low | Medium | Generic nodes serialize as standard CodeNodes |

## Conclusion

Research confirms the feature is straightforward to implement with minimal risk. The existing architecture supports extension, and clear patterns exist for all integration points. Proceed to Phase 1: Design & Contracts.
