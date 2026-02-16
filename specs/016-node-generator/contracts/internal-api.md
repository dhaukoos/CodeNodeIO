# Internal API Contracts: Node Generator UI Tool

**Feature**: 016-node-generator
**Date**: 2026-02-15

## Overview

This feature is a UI component within the graphEditor module. It does not expose REST/GraphQL APIs. Instead, it defines internal Kotlin interfaces and composable function signatures.

---

## Composable Function Contracts

### NodeGeneratorPanel

**Signature**:
```kotlin
@Composable
fun NodeGeneratorPanel(
    state: NodeGeneratorState,
    onStateChange: (NodeGeneratorState) -> Unit,
    onCreateNode: (CustomNodeDefinition) -> Unit,
    modifier: Modifier = Modifier
)
```

**Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| `state` | `NodeGeneratorState` | Current form state |
| `onStateChange` | `(NodeGeneratorState) -> Unit` | Callback when form values change |
| `onCreateNode` | `(CustomNodeDefinition) -> Unit` | Callback when Create is clicked with valid data |
| `modifier` | `Modifier` | Compose modifier for styling/layout |

**Behavior**:
- Renders name input field, input/output selectors, Create and Cancel buttons
- Create button disabled when `!state.isValid`
- Create button click: calls `onCreateNode` with new `CustomNodeDefinition`, then resets form
- Cancel button click: resets form to defaults via `onStateChange`

---

## Data Class Contracts

### NodeGeneratorState

```kotlin
data class NodeGeneratorState(
    val name: String = "",
    val inputCount: Int = 1,
    val outputCount: Int = 1
) {
    val isValid: Boolean
        get() = name.isNotBlank() && !(inputCount == 0 && outputCount == 0)

    val genericType: String
        get() = "in${inputCount}out${outputCount}"

    fun reset(): NodeGeneratorState = NodeGeneratorState()

    fun withName(name: String): NodeGeneratorState = copy(name = name)
    fun withInputCount(count: Int): NodeGeneratorState = copy(inputCount = count.coerceIn(0, 3))
    fun withOutputCount(count: Int): NodeGeneratorState = copy(outputCount = count.coerceIn(0, 3))
}
```

---

### CustomNodeDefinition

```kotlin
@Serializable
data class CustomNodeDefinition(
    val id: String,
    val name: String,
    val inputCount: Int,
    val outputCount: Int,
    val genericType: String,
    val createdAt: Long
) {
    companion object {
        fun create(name: String, inputCount: Int, outputCount: Int): CustomNodeDefinition {
            return CustomNodeDefinition(
                id = "custom_node_${java.util.UUID.randomUUID()}",
                name = name,
                inputCount = inputCount,
                outputCount = outputCount,
                genericType = "in${inputCount}out${outputCount}",
                createdAt = System.currentTimeMillis()
            )
        }
    }

    fun toNodeTypeDefinition(): NodeTypeDefinition {
        return GenericNodeTypeFactory.createGenericNodeType(
            numInputs = inputCount,
            numOutputs = outputCount,
            customName = name
        )
    }
}
```

---

## Repository Contract

### CustomNodeRepository

```kotlin
interface CustomNodeRepository {
    /**
     * Returns all saved custom node definitions.
     */
    fun getAll(): List<CustomNodeDefinition>

    /**
     * Adds a new custom node definition and persists to storage.
     */
    fun add(node: CustomNodeDefinition)

    /**
     * Loads custom nodes from storage file.
     * Called on application startup.
     * If file missing or corrupted, initializes empty list.
     */
    fun load()

    /**
     * Saves current custom nodes to storage file.
     * Called after each add() operation.
     */
    fun save()
}
```

**Default Implementation**: `FileCustomNodeRepository`

```kotlin
class FileCustomNodeRepository(
    private val filePath: String = "${System.getProperty("user.home")}/.codenode/custom-nodes.json"
) : CustomNodeRepository {
    // Implementation details in tasks
}
```

---

## Integration Points

### Main.kt Integration

```kotlin
// In GraphEditorApp composable:

// 1. Initialize repository and load saved nodes
val customNodeRepository = remember { FileCustomNodeRepository() }
LaunchedEffect(Unit) { customNodeRepository.load() }

// 2. State for custom nodes
var customNodes by remember { mutableStateOf(customNodeRepository.getAll()) }

// 3. Combine with built-in node types
val allNodeTypes = remember(customNodes) {
    createSampleNodeTypes() + customNodes.map { it.toNodeTypeDefinition() }
}

// 4. Node Generator state
var nodeGeneratorState by remember { mutableStateOf(NodeGeneratorState()) }

// 5. Layout - add NodeGeneratorPanel above NodePalette
Column {
    NodeGeneratorPanel(
        state = nodeGeneratorState,
        onStateChange = { nodeGeneratorState = it },
        onCreateNode = { node ->
            customNodeRepository.add(node)
            customNodes = customNodeRepository.getAll()
        }
    )
    NodePalette(
        nodeTypes = allNodeTypes,
        onNodeSelected = { ... }
    )
}
```

---

## Error Handling Contracts

| Scenario | Behavior |
|----------|----------|
| File read error | Log warning, return empty list |
| JSON parse error | Log warning, return empty list |
| File write error | Log error, throw exception (caller decides retry) |
| Invalid input count | Coerce to valid range [0, 3] |
| Invalid output count | Coerce to valid range [0, 3] |
| Empty name on Create | Button disabled, no action |
| 0/0 configuration | Button disabled, no action |
