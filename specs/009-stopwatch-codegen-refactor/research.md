# Research: StopWatch Code Generation Refactor

**Feature**: 009-stopwatch-codegen-refactor
**Date**: 2026-02-11

## R1: File Browser Integration in Compose Desktop

### Question
How should we implement a file browser for selecting ProcessingLogic Kotlin files in a Compose Desktop application?

### Decision
Use `javax.swing.JFileChooser` from the standard JDK.

### Rationale
1. **Already in use**: The graphEditor already uses JFileChooser for Open/Save dialogs (Main.kt lines 1026-1058)
2. **No dependencies**: Part of JDK, no additional libraries needed
3. **Platform integration**: Provides native look and feel on each platform
4. **Proven pattern**: Well-tested in production applications

### Implementation Pattern
```kotlin
fun showProcessingLogicFileDialog(currentDir: File? = null): File? {
    val fileChooser = JFileChooser().apply {
        dialogTitle = "Select ProcessingLogic Implementation"
        currentDir?.let { currentDirectory = it }
        fileFilter = FileNameExtensionFilter("Kotlin Files (*.kt)", "kt")
        fileSelectionMode = JFileChooser.FILES_ONLY
    }
    return if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        fileChooser.selectedFile
    } else {
        null
    }
}
```

### Alternatives Considered

| Alternative | Pros | Cons | Why Rejected |
|-------------|------|------|--------------|
| JNA/JNI native dialogs | True native appearance | License complexity (LGPL), build complexity | License violation risk |
| Custom file picker | Full control over UI | Significant effort, inconsistent UX | Not worth development cost |
| AWT FileDialog | Simpler API | Limited customization, no filter preview | Can't filter to *.kt files reliably |

---

## R2: Property Configuration Storage

### Question
Where should the ProcessingLogic file reference be stored in the domain model?

### Decision
Store in `CodeNode.configuration` map with key `"processingLogicFile"`.

### Rationale
1. **No model changes**: Configuration map already exists and is serialized
2. **Consistent pattern**: Follows existing usage for `speedAttenuation`, `outputFormat`
3. **Serialization ready**: FlowGraphSerializer already handles configuration map
4. **Extensible**: Can add more required properties in future

### Storage Example
```kotlin
// In CodeNode
configuration = mapOf(
    "speedAttenuation" to "1000",
    "processingLogicFile" to "demos/stopwatch/TimerEmitterComponent.kt"
)
```

### Serialization (DSL)
```kotlin
val timerEmitter = codeNode("TimerEmitter", nodeType = "GENERATOR") {
    config("speedAttenuation", "1000")
    config("processingLogicFile", "demos/stopwatch/TimerEmitterComponent.kt")
    // ... ports
}
```

### Alternatives Considered

| Alternative | Pros | Cons | Why Rejected |
|-------------|------|------|--------------|
| Add CodeNode property | Type safety, explicit | Requires model change, serialization update | Breaking change, unnecessary |
| Separate metadata file | Clean separation | Complexity, sync issues | Complicates project structure |
| NodeTypeDefinition template | Centralized | Doesn't support per-instance values | Wrong abstraction level |

---

## R3: Relative vs Absolute Paths

### Question
Should ProcessingLogic file references use absolute or relative paths?

### Decision
Store **relative paths from project root**.

### Rationale
1. **Portability**: Works when cloning/sharing project across machines
2. **Version control**: No machine-specific paths in committed files
3. **IDE convention**: Follows standard project-relative reference pattern
4. **Resolution**: Convert to absolute at runtime using project root context

### Path Handling
```kotlin
// Storage (relative)
"demos/stopwatch/TimerEmitterComponent.kt"

// Resolution at compile time
val projectRoot = File("/Users/dhaukoos/CodeNodeIO")
val absolutePath = projectRoot.resolve(relativePath)

// Normalization (cross-platform)
relativePath.replace('\\', '/') // Always use forward slashes
```

### Alternatives Considered

| Alternative | Pros | Cons | Why Rejected |
|-------------|------|------|--------------|
| Absolute paths | Simple resolution | Breaks portability | Won't work on other machines |
| Package-qualified names | Language-native | Requires parsing Kotlin files | Complex implementation |
| Classpath resources | Build-system agnostic | Doesn't work for source files | Wrong abstraction |

---

## R4: Validation Architecture

### Question
Where should pre-compilation validation logic live?

### Decision
Create separate `CompilationValidator` class in `graphEditor/compilation/` package.

### Rationale
1. **Single responsibility**: Validation separate from code generation
2. **Testable**: Can unit test validator in isolation
3. **Extensible**: Easy to add more required property checks
4. **Reusable**: Could be called from multiple entry points (menu, toolbar, auto-save)

### Architecture
```
┌─────────────┐     ┌─────────────────────┐     ┌──────────────────┐
│ Compile UI  │────▶│ CompilationValidator │────▶│ CompilationService │
└─────────────┘     └─────────────────────┘     └──────────────────┘
                              │                          │
                    ValidationResult              CompilationResult
                    - success: Boolean            - success: Boolean
                    - errors: List<NodeError>     - outputPath: String
```

### Alternatives Considered

| Alternative | Pros | Cons | Why Rejected |
|-------------|------|------|--------------|
| Inline in CompilationService | Simpler call | Mixed responsibilities, harder to test | Violates SRP |
| PropertiesPanel validation | Immediate feedback | Wrong layer (edit-time vs compile-time) | Scope mismatch |
| FlowGraph.validate() | Centralized | FlowGraph is domain model, not compile tool | Layer violation |

---

## R5: Factory Function Placement

### Question
Where should the generated `createXXXFlowGraph()` function be placed?

### Decision
Generate as **top-level function** in the same file as the Flow class (e.g., `StopWatchFlow.kt`).

### Rationale
1. **Kotlin idiom**: Factory functions are commonly top-level in Kotlin
2. **Single import**: User imports one file, gets both class and factory
3. **Discoverable**: Adjacent to the class it creates
4. **Consistent**: Same pattern as existing generated code

### Generated Code Pattern
```kotlin
// File: StopWatchFlow.kt
package io.codenode.generated.stopwatch

/**
 * Creates a StopWatch FlowGraph with all nodes, ports, and connections.
 * ProcessingLogic implementations are instantiated from configured files.
 */
fun createStopWatchFlowGraph(): FlowGraph {
    val timerEmitter = CodeNode(
        id = "timer-emitter",
        name = "TimerEmitter",
        // ... full definition
        processingLogic = TimerEmitterComponent()
    )
    // ... other nodes, connections
    return FlowGraph(/* ... */)
}

class StopWatchFlow {
    // ... existing class
}
```

### Alternatives Considered

| Alternative | Pros | Cons | Why Rejected |
|-------------|------|------|--------------|
| Companion object method | Explicit ownership | Less idiomatic Kotlin | Java-style pattern |
| Separate file | Clean separation | Extra import, harder to discover | Unnecessary complexity |
| FlowGraph extension function | Follows Kotlin style | FlowGraph in different module | Package boundary issue |

---

## R6: ProcessingLogic Class Resolution

### Question
How should generated code reference ProcessingLogic classes from file paths?

### Decision
Derive class/object name from file name, import from inferred package.

### Resolution Algorithm
1. Parse file path: `demos/stopwatch/TimerEmitterComponent.kt`
2. Extract file name without extension: `TimerEmitterComponent`
3. Assume class/object name matches file name (Kotlin convention)
4. Infer package from directory structure + project configuration

### Implementation
```kotlin
fun resolveProcessingLogicClassName(filePath: String): String {
    val fileName = File(filePath).nameWithoutExtension
    return fileName // Assumes class name matches file name
}

// Generated import
import io.codenode.demos.stopwatch.TimerEmitterComponent

// Generated instantiation
processingLogic = TimerEmitterComponent()
```

### Edge Cases
- **Object declaration**: Works same as class (Kotlin singleton pattern)
- **Multiple classes in file**: Use file name as primary class
- **Nested classes**: Not supported (require explicit configuration)

---

## R7: Visual Feedback for Incomplete Configuration

### Question
How should the UI indicate that a flow graph has incomplete required properties?

### Decision
Multi-level visual feedback:
1. **Property level**: Red asterisk on required fields, inline error message
2. **Node level**: Warning icon badge on canvas node
3. **Graph level**: Compile button disabled, status bar message

### Implementation

```kotlin
// PropertiesPanel - inline error
@Composable
fun PropertyEditorRow(
    definition: PropertyDefinition,
    value: String,
    error: String?, // null if valid
    // ...
)

// Node badge (via NodeRenderer)
if (hasValidationErrors) {
    Icon(
        Icons.Default.Warning,
        tint = Color(0xFFFF9800) // Orange warning
    )
}

// Compile button state
Button(
    onClick = onCompile,
    enabled = !hasValidationErrors
)
```

---

## Summary

| Research Area | Decision | Key Benefit |
|---------------|----------|-------------|
| File browser | JFileChooser (JDK) | No dependencies, consistent UX |
| Storage location | CodeNode.configuration map | No model changes |
| Path format | Relative from project root | Portability |
| Validation architecture | Separate CompilationValidator class | Testable, SRP |
| Factory placement | Top-level function in Flow file | Idiomatic Kotlin |
| Class resolution | File name = class name convention | Simple, follows Kotlin standards |
| Visual feedback | Multi-level (property, node, graph) | Clear user guidance |
