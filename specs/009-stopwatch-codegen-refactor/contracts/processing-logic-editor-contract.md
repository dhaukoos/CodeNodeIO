# Contract: Processing Logic Editor

**Feature**: 009-stopwatch-codegen-refactor
**Date**: 2026-02-11
**Type**: UI Component Contract

## Overview

This contract defines the behavior and interface of the FileBrowserEditor component used for selecting ProcessingLogic implementation files in the PropertiesPanel.

## Component: FileBrowserEditor

### Purpose
Allows users to select a Kotlin file that contains the ProcessingLogic implementation for a CodeNode.

### Visual Layout

```
┌─────────────────────────────────────────────────────────┐
│ Processing Logic *                                      │
├─────────────────────────────────────────────────────────┤
│ ┌─────────────────────────────────────────┐ ┌────────┐ │
│ │ demos/stopwatch/TimerEmitterComponent.kt │ │ Browse │ │
│ └─────────────────────────────────────────┘ └────────┘ │
│ File not found: demos/stopwatch/Missing.kt (error)     │
└─────────────────────────────────────────────────────────┘
```

### Props/Parameters

```kotlin
@Composable
fun FileBrowserEditor(
    value: String,                      // Current file path
    onValueChange: (String) -> Unit,    // Called when path changes
    label: String = "File",             // Field label
    fileFilter: FileFilter? = null,     // Optional file filter (default: *.kt)
    projectRoot: File? = null,          // Project root for relative paths
    isError: Boolean = false,           // Error state styling
    errorMessage: String? = null,       // Error message to display
    enabled: Boolean = true,            // Disabled state
    modifier: Modifier = Modifier       // Compose modifier
)
```

### Behavior Specification

#### B1: Initial Render
- **Given**: Component renders with empty value
- **Then**: Text field shows placeholder text "Select a file..."
- **And**: Browse button is enabled

#### B2: Display Existing Value
- **Given**: Component renders with existing path "demos/stopwatch/TimerEmitterComponent.kt"
- **Then**: Text field displays the path
- **And**: Text is not editable directly (read-only)

#### B3: Browse Button Click
- **Given**: User clicks Browse button
- **When**: JFileChooser dialog opens
- **Then**: Dialog title is "Select ProcessingLogic Implementation"
- **And**: File filter shows "Kotlin Files (*.kt)"
- **And**: Default directory is projectRoot or last selected directory

#### B4: File Selection
- **Given**: User selects a file in the dialog
- **When**: User clicks "Open" / "Approve"
- **Then**: `onValueChange` is called with relative path from projectRoot
- **And**: Text field updates to show the relative path

#### B5: Cancel Selection
- **Given**: User opens file dialog
- **When**: User clicks "Cancel"
- **Then**: `onValueChange` is NOT called
- **And**: Text field retains previous value

#### B6: Error State Display
- **Given**: `isError = true` and `errorMessage = "File not found"`
- **Then**: Text field border shows error color (red)
- **And**: Error message displayed below the field

#### B7: Manual Text Entry
- **Given**: User types directly in text field
- **Then**: Input is allowed (alternative to browse)
- **And**: `onValueChange` called on each keystroke

### Events

| Event | Trigger | Callback |
|-------|---------|----------|
| Path Changed | File selected OR manual entry | `onValueChange(newPath)` |

### Styling

| State | Text Field Border | Error Text | Button |
|-------|-------------------|------------|--------|
| Normal | Default gray | Hidden | Enabled |
| Error | Red (#F44336) | Visible, red | Enabled |
| Disabled | Light gray | Hidden | Disabled |

### Accessibility

- Text field has accessible label "Processing Logic file path"
- Browse button has accessible description "Open file browser"
- Error message is announced when displayed
- Keyboard navigation: Tab to text field, Tab to Browse button, Enter to activate

---

## Component: CompilationValidationDialog

### Purpose
Displays validation errors when compilation cannot proceed due to missing required properties.

### Visual Layout

```
┌─────────────────────────────────────────────────────────┐
│ ⚠ Compilation Validation Failed                        │
├─────────────────────────────────────────────────────────┤
│                                                         │
│ The following nodes have missing or invalid properties: │
│                                                         │
│ • TimerEmitter                                          │
│   - Missing required: processingLogicFile               │
│                                                         │
│ • DisplayReceiver                                       │
│   - Missing required: processingLogicFile               │
│                                                         │
├─────────────────────────────────────────────────────────┤
│                                              [ Close ]  │
└─────────────────────────────────────────────────────────┘
```

### Props/Parameters

```kotlin
@Composable
fun CompilationValidationDialog(
    validationResult: CompilationValidationResult,
    onDismiss: () -> Unit,
    onNavigateToNode: ((String) -> Unit)? = null  // Optional: click to select node
)
```

### Behavior Specification

#### B1: Show All Errors
- **Given**: Validation result has 3 node errors
- **Then**: Dialog lists all 3 nodes with their issues

#### B2: Click Node Name
- **Given**: `onNavigateToNode` is provided
- **When**: User clicks on node name "TimerEmitter"
- **Then**: `onNavigateToNode("timer-emitter-id")` is called
- **And**: Dialog remains open

#### B3: Close Dialog
- **Given**: Dialog is open
- **When**: User clicks Close button or presses Escape
- **Then**: `onDismiss()` is called
- **And**: Dialog closes

---

## Integration: PropertiesPanel Extension

### New Property Definition

The PropertiesPanel should include the processingLogicFile property for all CodeNodes:

```kotlin
// In PropertiesPanelState or configuration
val codeNodePropertyDefinitions = listOf(
    PropertyDefinition(
        name = "processingLogicFile",
        type = PropertyType.FILE_PATH,
        required = true,
        description = "Kotlin file with ProcessingLogic implementation"
    )
)
```

### Editor Type Mapping

```kotlin
// In PropertyEditorRow
when (definition.editorType) {
    EditorType.TEXT_FIELD -> TextFieldEditor(...)
    EditorType.NUMBER_FIELD -> NumberFieldEditor(...)
    EditorType.CHECKBOX -> CheckboxEditor(...)
    EditorType.DROPDOWN -> DropdownEditor(...)
    EditorType.FILE_BROWSER -> FileBrowserEditor(  // NEW
        value = value,
        onValueChange = onValueChange,
        label = definition.name,
        isError = error != null,
        errorMessage = error
    )
}
```

---

## API Contract: CompilationValidator

### Method: validate

```kotlin
interface CompilationValidator {
    /**
     * Validates that all required properties are configured for compilation.
     *
     * @param flowGraph The flow graph to validate
     * @param projectRoot Project root for resolving relative paths
     * @return Validation result with success flag and any errors
     */
    fun validate(
        flowGraph: FlowGraph,
        projectRoot: File
    ): CompilationValidationResult
}
```

### Validation Rules

| Property | Rule | Error Code |
|----------|------|------------|
| processingLogicFile | Not blank | MISSING_REQUIRED |
| processingLogicFile | Ends with .kt | INVALID_FORMAT |
| processingLogicFile | File exists | FILE_NOT_FOUND |

### Example Usage

```kotlin
val validator = CompilationValidator()
val result = validator.validate(flowGraph, projectRoot)

if (!result.isValid) {
    showValidationDialog(result)
    return
}

// Proceed with compilation
compilationService.compile(flowGraph, outputDir)
```

---

## API Contract: FlowGraphFactoryGenerator

### Method: generateFactoryFunction

```kotlin
interface FlowGraphFactoryGenerator {
    /**
     * Generates a factory function that creates a FlowGraph instance.
     *
     * @param flowGraph The flow graph definition
     * @param packageName Target package for generated code
     * @return FunSpec for the factory function
     */
    fun generateFactoryFunction(
        flowGraph: FlowGraph,
        packageName: String
    ): FunSpec
}
```

### Generated Function Signature

```kotlin
/**
 * Creates a [graphName] FlowGraph with all nodes, ports, connections,
 * and ProcessingLogic implementations.
 *
 * @return Fully configured FlowGraph ready for execution
 */
fun create[GraphName]FlowGraph(): FlowGraph
```

### Generated Code Example

```kotlin
fun createStopWatchFlowGraph(): FlowGraph {
    val timerEmitter = CodeNode(
        id = "timer-emitter",
        name = "TimerEmitter",
        codeNodeType = CodeNodeType.GENERATOR,
        position = Node.Position(100.0, 100.0),
        inputPorts = emptyList(),
        outputPorts = listOf(
            Port(
                id = "timer-emitter_elapsedSeconds",
                name = "elapsedSeconds",
                direction = Port.Direction.OUTPUT,
                dataType = Int::class,
                owningNodeId = "timer-emitter"
            ),
            // ... more ports
        ),
        configuration = mapOf(
            "speedAttenuation" to "1000",
            "processingLogicFile" to "demos/stopwatch/TimerEmitterComponent.kt"
        ),
        processingLogic = TimerEmitterComponent(),
        controlConfig = ControlConfig(speedAttenuation = 1000L)
    )

    // ... displayReceiver definition

    val connections = listOf(
        Connection(
            id = "conn_seconds",
            sourceNodeId = "timer-emitter",
            sourcePortId = "timer-emitter_elapsedSeconds",
            targetNodeId = "display-receiver",
            targetPortId = "display-receiver_seconds",
            channelCapacity = 1
        ),
        // ... more connections
    )

    return FlowGraph(
        id = "stopwatch-flow",
        name = "StopWatch",
        version = "1.0.0",
        description = "Virtual circuit demo for stopwatch functionality",
        rootNodes = listOf(timerEmitter, displayReceiver),
        connections = connections,
        targetPlatforms = listOf(
            FlowGraph.TargetPlatform.KMP_ANDROID,
            FlowGraph.TargetPlatform.KMP_IOS,
            FlowGraph.TargetPlatform.KMP_DESKTOP
        )
    )
}
```
