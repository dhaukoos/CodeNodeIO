# Validation Interface Contract

**Feature**: 011-stopwatch-refactor
**Date**: 2026-02-12

## RequiredPropertyValidator Interface

### Purpose

Validates that all CodeNodes in a FlowGraph have their required configuration properties defined.

### Interface Definition

```kotlin
/**
 * Validates required properties for CodeNodes in a FlowGraph.
 * Used by CompilationService before module generation.
 */
interface RequiredPropertyValidator {

    /**
     * Validates all nodes in the flow graph have required properties.
     *
     * @param flowGraph The flow graph to validate
     * @return PropertyValidationResult with success status and any errors
     */
    fun validate(flowGraph: FlowGraph): PropertyValidationResult

    /**
     * Gets the required properties for a specific node type.
     *
     * @param nodeType The CodeNodeType to check
     * @return Set of required property names, empty if none required
     */
    fun getRequiredProperties(nodeType: CodeNodeType): Set<String>
}
```

### Default Implementation

```kotlin
/**
 * Default implementation using hardcoded required property specs.
 */
class DefaultRequiredPropertyValidator : RequiredPropertyValidator {

    private val requiredSpecs = mapOf(
        CodeNodeType.GENERIC to setOf("_useCaseClass", "_genericType")
    )

    override fun validate(flowGraph: FlowGraph): PropertyValidationResult {
        val errors = mutableListOf<PropertyValidationError>()

        flowGraph.getAllCodeNodes().forEach { node ->
            val required = getRequiredProperties(node.codeNodeType)
            required.forEach { propertyName ->
                val value = node.configuration[propertyName]
                if (value.isNullOrBlank()) {
                    errors.add(PropertyValidationError(
                        nodeId = node.id,
                        nodeName = node.name,
                        propertyName = propertyName,
                        reason = "required for code generation"
                    ))
                }
            }
        }

        return PropertyValidationResult(
            success = errors.isEmpty(),
            errors = errors
        )
    }

    override fun getRequiredProperties(nodeType: CodeNodeType): Set<String> {
        return requiredSpecs[nodeType] ?: emptySet()
    }
}
```

### Usage Contract

**Caller**: CompilationService.compileToModule()

**Preconditions**:
- FlowGraph has passed structure validation (FlowGraph.validate())
- FlowGraph is non-null

**Postconditions**:
- If success=true: All GENERIC nodes have `_useCaseClass` and `_genericType` defined
- If success=false: `errors` list contains all missing properties across all nodes

**Error Handling**:
- Returns validation errors, never throws exceptions
- Empty graph returns success (no nodes to validate)

### Test Contract

```kotlin
@Test
fun `validate returns success when all required properties present`()

@Test
fun `validate returns error for GENERIC node missing _useCaseClass`()

@Test
fun `validate returns error for GENERIC node missing _genericType`()

@Test
fun `validate returns multiple errors for multiple nodes with missing properties`()

@Test
fun `validate ignores non-GENERIC node types`()

@Test
fun `validate returns success for empty graph`()
```

---

## CompilationService Integration Contract

### Modified Method Signature

```kotlin
fun compileToModule(
    flowGraph: FlowGraph,
    outputDir: File,
    moduleName: String = flowGraph.name.lowercase().replace(" ", "-"),
    packageName: String = ModuleGenerator.DEFAULT_PACKAGE
): CompilationResult
```

### Behavior Contract

**Before** (existing):
1. Validate FlowGraph structure
2. Generate module
3. Write files

**After** (with property validation):
1. Validate FlowGraph structure
2. **Validate required properties** ← NEW
3. Generate module
4. Write files

### Return Value Contract

| Condition | Return |
|-----------|--------|
| Structure invalid | `CompilationResult(success=false, errorMessage="Flow graph validation failed: ...")` |
| Properties invalid | `CompilationResult(success=false, errorMessage="Required properties missing: ...")` |
| All valid | `CompilationResult(success=true, outputPath=..., fileCount=...)` |

---

## PropertiesPanel Enhancement Contract

### Modified Behavior

**For GENERIC nodes**:
1. Show `_useCaseClass` field with required indicator (*)
2. Show `_genericType` as read-only (set when node created)
3. Show other configuration properties as normal

**Field Display**:
```
Name
[TimerEmitter                    ]

Use Case Class *
[io.codenode.usecases.Timer     ]
Fully qualified class implementing ProcessingLogic

Generic Type (read-only)
[in0out2                         ]

Configuration
─────────────────────────────────
speedAttenuation
[1000                            ]
```

### Validation Feedback

- Empty required field: Red border, error text "Use Case Class is required"
- Valid field: Normal styling
