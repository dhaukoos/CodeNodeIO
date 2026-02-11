# Data Model: StopWatch Code Generation Refactor

**Feature**: 009-stopwatch-codegen-refactor
**Date**: 2026-02-11

## Overview

This document defines the data entities and their relationships for the ProcessingLogic property configuration and compile validation features.

## Entities

### 1. PropertyType (Extended Enum)

**Purpose**: Defines the type of a configuration property for editor selection.

**Extension**: Add `FILE_PATH` to existing enum.

```kotlin
enum class PropertyType {
    STRING,
    NUMBER,
    BOOLEAN,
    DROPDOWN,
    FILE_PATH  // NEW: File selection property
}
```

**Usage**: When `PropertyType.FILE_PATH`, the PropertiesPanel renders a FileBrowserEditor.

---

### 2. EditorType (Extended Enum)

**Purpose**: Maps property types to UI editor components.

**Extension**: Add `FILE_BROWSER` to existing enum.

```kotlin
enum class EditorType {
    TEXT_FIELD,
    NUMBER_FIELD,
    CHECKBOX,
    DROPDOWN,
    FILE_BROWSER  // NEW: Text field + browse button
}
```

**Mapping**:
```kotlin
val editorType: EditorType
    get() = when (type) {
        PropertyType.STRING -> EditorType.TEXT_FIELD
        PropertyType.NUMBER -> EditorType.NUMBER_FIELD
        PropertyType.BOOLEAN -> EditorType.CHECKBOX
        PropertyType.DROPDOWN -> EditorType.DROPDOWN
        PropertyType.FILE_PATH -> EditorType.FILE_BROWSER  // NEW
    }
```

---

### 3. ProcessingLogicPropertyDefinition

**Purpose**: Defines the "processingLogicFile" property for CodeNodes.

**Implementation**: Static definition used for all CodeNodes.

```kotlin
val PROCESSING_LOGIC_PROPERTY = PropertyDefinition(
    name = "processingLogicFile",
    type = PropertyType.FILE_PATH,
    required = true,  // Compilation fails without it
    description = "Path to Kotlin file containing ProcessingLogic implementation"
)
```

**Location**: Can be defined as a constant in PropertiesPanel or a dedicated constants file.

---

### 4. CompilationValidationResult

**Purpose**: Result of pre-compilation validation check.

```kotlin
data class CompilationValidationResult(
    val success: Boolean,
    val nodeErrors: List<NodeValidationError>
) {
    val isValid: Boolean get() = success && nodeErrors.isEmpty()

    val errorSummary: String
        get() = if (isValid) {
            "Validation passed"
        } else {
            nodeErrors.joinToString("\n") { it.message }
        }
}
```

**Relationships**:
- Contains multiple `NodeValidationError` instances (one per node with issues)

---

### 5. NodeValidationError

**Purpose**: Describes a validation error for a specific node.

```kotlin
data class NodeValidationError(
    val nodeId: String,
    val nodeName: String,
    val missingProperties: List<String>,
    val invalidProperties: List<PropertyValidationError>
) {
    val message: String
        get() = buildString {
            append("$nodeName: ")
            if (missingProperties.isNotEmpty()) {
                append("Missing required: ${missingProperties.joinToString(", ")}")
            }
            if (invalidProperties.isNotEmpty()) {
                if (missingProperties.isNotEmpty()) append("; ")
                append("Invalid: ${invalidProperties.joinToString(", ") { it.propertyName }}")
            }
        }
}
```

**Example Error Message**:
```
TimerEmitter: Missing required: processingLogicFile
DisplayReceiver: Invalid: processingLogicFile (file not found)
```

---

### 6. PropertyValidationError

**Purpose**: Describes a validation error for a specific property.

```kotlin
data class PropertyValidationError(
    val propertyName: String,
    val reason: String  // e.g., "file not found", "invalid format"
)
```

---

### 7. RequiredPropertySpec

**Purpose**: Specification for a required property that must be validated before compilation.

```kotlin
data class RequiredPropertySpec(
    val key: String,                    // e.g., "processingLogicFile"
    val displayName: String,            // e.g., "Processing Logic"
    val validator: ((String) -> String?)? // Returns error message or null if valid
)
```

**Example**:
```kotlin
val PROCESSING_LOGIC_SPEC = RequiredPropertySpec(
    key = "processingLogicFile",
    displayName = "Processing Logic",
    validator = { path ->
        when {
            path.isBlank() -> "Path is required"
            !File(projectRoot, path).exists() -> "File not found: $path"
            !path.endsWith(".kt") -> "Must be a Kotlin file (.kt)"
            else -> null  // Valid
        }
    }
)
```

---

### 8. FileBrowserEditorState

**Purpose**: Transient UI state for the file browser editor.

```kotlin
data class FileBrowserEditorState(
    val value: String,
    val isError: Boolean,
    val errorMessage: String?,
    val lastDirectory: File?  // Remember last browsed directory
)
```

**Note**: This is UI-only state, not persisted.

---

## Entity Relationships

```
┌─────────────────────────┐
│      FlowGraph          │
│  - rootNodes: List<Node>│
│  - connections          │
└───────────┬─────────────┘
            │ contains
            ▼
┌─────────────────────────┐
│       CodeNode          │
│  - configuration: Map   │──────▶ "processingLogicFile" → "path/to/File.kt"
│  - processingLogic      │
└───────────┬─────────────┘
            │ validated by
            ▼
┌─────────────────────────┐
│  CompilationValidator   │
│  - requiredSpecs: List  │
└───────────┬─────────────┘
            │ produces
            ▼
┌─────────────────────────────────┐
│   CompilationValidationResult   │
│   - success: Boolean            │
│   - nodeErrors: List            │
└─────────────┬───────────────────┘
              │ contains
              ▼
┌─────────────────────────────────┐
│      NodeValidationError        │
│   - nodeId, nodeName            │
│   - missingProperties           │
│   - invalidProperties           │
└─────────────────────────────────┘
```

---

## Configuration Key Convention

### Reserved Configuration Keys

| Key | Type | Required | Description |
|-----|------|----------|-------------|
| `processingLogicFile` | FILE_PATH | Yes (for compile) | Path to ProcessingLogic implementation |
| `speedAttenuation` | NUMBER | No | Timer tick interval (ms) |
| `_genericType` | STRING | Internal | Marker for generic node types |

### Key Naming Convention
- Use camelCase
- Internal keys prefixed with `_`
- Required keys validated by CompilationValidator

---

## Validation Rules

### processingLogicFile Property

| Rule | Error Message |
|------|---------------|
| Must not be blank | "Processing Logic is required" |
| Must end with .kt | "Must be a Kotlin file (.kt)" |
| File must exist | "File not found: {path}" |
| Must be relative path | "Use relative path from project root" |

### Validation Timing

| Event | Validation |
|-------|------------|
| Property change | Format validation (ends with .kt) |
| Save flow graph | None (allow incomplete) |
| Compile trigger | Full validation (required + existence) |

---

## State Transitions

### Compile Validation Flow

```
┌──────────────┐
│  User clicks │
│   Compile    │
└──────┬───────┘
       │
       ▼
┌──────────────────────┐
│ CompilationValidator │
│  .validate(graph)    │
└──────────┬───────────┘
       │
       ├─── Success ───▶ CompilationService.compile()
       │
       └─── Failure ───▶ Display errors, abort compile
```

### UI State Based on Validation

```
┌─────────────────────────────────────────────────────┐
│ Validation State │ Compile Button │ Node Badge     │
├──────────────────┼────────────────┼────────────────┤
│ All valid        │ Enabled        │ None           │
│ Some missing     │ Disabled       │ Warning icon   │
│ File not found   │ Disabled       │ Error icon     │
└─────────────────────────────────────────────────────┘
```
