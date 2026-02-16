# Quickstart: Node Generator UI Tool

**Feature**: 016-node-generator
**Date**: 2026-02-15

## Overview

This guide helps developers get started implementing the Node Generator feature for the graphEditor. The Node Generator allows users to create custom node types with configurable input/output ports.

## Prerequisites

- Kotlin 2.1.21 or later
- Compose Desktop 1.7.3
- kotlinx-serialization 1.6.0
- Existing graphEditor module compiled and running

## Key Files to Understand

Before implementing, review these existing files:

| File | Why Review |
|------|------------|
| `graphEditor/src/jvmMain/kotlin/ui/NodePalette.kt` | Pattern for composable UI panels |
| `graphEditor/src/jvmMain/kotlin/ui/PropertiesPanel.kt` | State management pattern with data class |
| `fbpDsl/src/commonMain/kotlin/.../GenericNodeTypeFactory.kt` | Factory for creating generic node types |
| `graphEditor/src/jvmMain/kotlin/Main.kt` | Integration point for new panel |

## Implementation Order

### Phase 1: Core Data Model

1. **Create `NodeGeneratorState.kt`** in `graphEditor/src/jvmMain/kotlin/state/`
   - Data class with name, inputCount, outputCount
   - Computed `isValid` and `genericType` properties
   - Helper methods: `reset()`, `withName()`, `withInputCount()`, `withOutputCount()`

2. **Create `CustomNodeDefinition.kt`** in `graphEditor/src/jvmMain/kotlin/repository/`
   - `@Serializable` data class for persistence
   - `create()` factory method
   - `toNodeTypeDefinition()` converter

### Phase 2: Repository Layer

3. **Create `CustomNodeRepository.kt`** in `graphEditor/src/jvmMain/kotlin/repository/`
   - Interface with `getAll()`, `add()`, `load()`, `save()`
   - `FileCustomNodeRepository` implementation
   - JSON file at `~/.codenode/custom-nodes.json`

### Phase 3: UI Component

4. **Create `NodeGeneratorPanel.kt`** in `graphEditor/src/jvmMain/kotlin/ui/`
   - `@Composable` function following NodePalette pattern
   - Name TextField
   - Input/Output dropdowns (0-3)
   - Create and Cancel buttons
   - Create button disabled when invalid

### Phase 4: Integration

5. **Modify `Main.kt`**
   - Initialize `FileCustomNodeRepository`
   - Load custom nodes on startup
   - Add `NodeGeneratorPanel` above `NodePalette`
   - Wire callbacks to update palette

## Code Snippets

### NodeGeneratorState Pattern

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
}
```

### Composable Panel Pattern

```kotlin
@Composable
fun NodeGeneratorPanel(
    state: NodeGeneratorState,
    onStateChange: (NodeGeneratorState) -> Unit,
    onCreateNode: (CustomNodeDefinition) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier, elevation = 4.dp) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Header
            Text("Node Generator", fontWeight = FontWeight.Bold)

            // Name input
            OutlinedTextField(
                value = state.name,
                onValueChange = { onStateChange(state.withName(it)) },
                label = { Text("Name") }
            )

            // Input/Output selectors (dropdowns)
            // ...

            // Buttons
            Row {
                Button(
                    onClick = { onStateChange(state.reset()) }
                ) { Text("Cancel") }

                Button(
                    onClick = {
                        val node = CustomNodeDefinition.create(
                            state.name, state.inputCount, state.outputCount
                        )
                        onCreateNode(node)
                        onStateChange(state.reset())
                    },
                    enabled = state.isValid
                ) { Text("Create") }
            }
        }
    }
}
```

### Repository Pattern

```kotlin
class FileCustomNodeRepository(
    private val filePath: String = "${System.getProperty("user.home")}/.codenode/custom-nodes.json"
) : CustomNodeRepository {

    private val json = Json { prettyPrint = true }
    private val nodes = mutableListOf<CustomNodeDefinition>()

    override fun getAll(): List<CustomNodeDefinition> = nodes.toList()

    override fun add(node: CustomNodeDefinition) {
        nodes.add(node)
        save()
    }

    override fun load() {
        val file = File(filePath)
        if (file.exists()) {
            try {
                nodes.clear()
                nodes.addAll(json.decodeFromString(file.readText()))
            } catch (e: Exception) {
                println("Warning: Could not load custom nodes: ${e.message}")
            }
        }
    }

    override fun save() {
        val file = File(filePath)
        file.parentFile?.mkdirs()
        file.writeText(json.encodeToString(nodes))
    }
}
```

## Testing Approach

1. **Unit Tests** (NodeGeneratorStateTest.kt)
   - Validation logic for all 16 input/output combinations
   - State immutability with copy()

2. **Repository Tests** (CustomNodeRepositoryTest.kt)
   - Save and load roundtrip
   - Handling missing file
   - Handling corrupted JSON

3. **Integration Tests** (NodeGeneratorPanelTest.kt)
   - Form validation behavior
   - Button enable/disable states
   - Create callback invocation

## Common Pitfalls

1. **Forgetting to create parent directory** for custom-nodes.json
2. **Not handling 0/0 case** in validation
3. **Mutable state in data class** - always use `copy()`
4. **Not refreshing palette** after adding custom node

## Validation Checklist

- [x] Name field validates on every keystroke
- [x] Create button disabled when name empty
- [x] Create button disabled when inputs=0 AND outputs=0
- [x] Form resets after successful create
- [x] Form resets on Cancel
- [x] Custom node appears in Generic section of palette
- [x] Custom nodes persist after app restart
- [x] Corrupted JSON file doesn't crash app

**Validated**: 2026-02-16 via unit tests and code review
