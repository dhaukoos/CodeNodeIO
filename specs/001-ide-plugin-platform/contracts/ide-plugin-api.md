# IDE Plugin API Contract

**Feature**: CodeNodeIO IDE Plugin Platform
**Version**: 1.0.0
**Date**: 2026-01-13

## Overview

This document defines the public API contracts for the CodeNodeIO IntelliJ Platform plugin. These contracts specify how users interact with the plugin through IDE actions, tool windows, and services.

## Plugin Descriptor (plugin.xml)

### Plugin Identity

```xml
<idea-plugin>
  <id>io.codenode.ide-plugin</id>
  <name>CodeNodeIO</name>
  <version>1.0.0</version>
  <vendor email="support@codenode.io" url="https://codenode.io">CodeNodeIO</vendor>

  <description><![CDATA[
    Visual Flow-based Programming (FBP) IDE plugin for creating full-stack applications.
    Create flow graphs visually, generate Kotlin Multiplatform and Go code.
  ]]></description>

  <change-notes><![CDATA[
    <h3>1.0.0</h3>
    <ul>
      <li>Visual flow graph editor</li>
      <li>FBP DSL textual representation</li>
      <li>KMP code generation (Android, iOS, Web)</li>
      <li>Go code generation (backend services)</li>
      <li>Circuit simulator for debugging</li>
    </ul>
  ]]></change-notes>

  <depends>com.intellij.modules.platform</depends>
  <depends>com.intellij.modules.lang</depends>
  <depends>org.jetbrains.kotlin</depends>
</idea-plugin>
```

### Compatibility

- **IDE Version**: 2023.1+
- **Kotlin Plugin**: Compatible with bundled Kotlin plugin
- **Supported IDEs**: IntelliJ IDEA Ultimate, IntelliJ IDEA Community, Android Studio, GoLand

## User Actions

### 1. New Flow Graph Action

**ID**: `io.codenode.actions.NewFlowGraph`
**Location**: File → New → CodeNodeIO Flow Graph
**Shortcut**: Ctrl+Shift+Alt+N (Windows/Linux), Cmd+Shift+Alt+N (macOS)

**Contract**:
```kotlin
/**
 * Creates a new Flow Graph file in the selected directory.
 *
 * @precondition User has a project open
 * @precondition User has selected a directory in Project view
 * @postcondition New .flow.kts file created in selected directory
 * @postcondition Graph editor tool window opens with blank canvas
 */
interface NewFlowGraphAction {
    /**
     * Prompts user for flow graph name and creates file.
     *
     * @param project Current project context
     * @param directory Target directory for new file
     * @return Created VirtualFile or null if cancelled
     */
    fun createFlowGraph(project: Project, directory: VirtualFile): VirtualFile?
}
```

**User Dialog**:
- **Input**: Flow graph name (validated: alphanumeric + underscores, unique in directory)
- **Output**: Creates `<name>.flow.kts` file with template content
- **Error States**:
  - Invalid name: Show inline validation error
  - Duplicate name: Show error dialog "Flow graph already exists"
  - IO error: Show error notification with retry option

---

### 2. Open Graph Editor Action

**ID**: `io.codenode.actions.OpenGraphEditor`
**Location**: Context menu on .flow.kts files
**Shortcut**: Double-click .flow.kts file

**Contract**:
```kotlin
/**
 * Opens the visual graph editor for a flow graph file.
 *
 * @precondition User has selected a .flow.kts file
 * @postcondition Graph editor tool window opens
 * @postcondition Flow graph is loaded and rendered on canvas
 */
interface OpenGraphEditorAction {
    /**
     * Loads flow graph and opens editor.
     *
     * @param project Current project context
     * @param file Flow graph file (.flow.kts)
     * @throws FlowGraphParseException if file is malformed
     */
    fun openEditor(project: Project, file: VirtualFile)
}
```

**Error States**:
- Parse error: Show error notification with line number and error message
- File not found: Show error dialog
- Concurrent modification: Show warning with option to reload

---

### 3. Generate KMP Code Action

**ID**: `io.codenode.actions.GenerateKMPCode`
**Location**: Tools → CodeNodeIO → Generate Kotlin Multiplatform Code
**Context**: Available when graph editor is open or .flow.kts file is selected

**Contract**:
```kotlin
/**
 * Generates Kotlin Multiplatform code from flow graph.
 *
 * @precondition Flow graph is valid (passes validation)
 * @precondition User has selected target platforms (Android, iOS, Web, Desktop)
 * @postcondition KMP project structure created in configured output directory
 * @postcondition Generated code passes license validation
 */
interface GenerateKMPCodeAction {
    /**
     * Validates graph and generates code.
     *
     * @param flowGraph Source flow graph
     * @param targets Selected KMP targets
     * @param outputDir Output directory for generated project
     * @return GenerationResult with success/failure and generated files
     * @throws ValidationException if graph validation fails
     * @throws LicenseViolationException if dependencies violate license rules
     */
    fun generate(
        flowGraph: FlowGraph,
        targets: Set<KMPTarget>,
        outputDir: VirtualFile
    ): GenerationResult
}

enum class KMPTarget {
    ANDROID, IOS, WEB_JS, WEB_WASM, DESKTOP_JVM, SERVER_JVM
}

data class GenerationResult(
    val success: Boolean,
    val generatedFiles: List<VirtualFile>,
    val errors: List<GenerationError>,
    val warnings: List<String>
)
```

**User Dialog**:
- **Target Selection**: Checkboxes for Android, iOS, Web (JS/Wasm), Desktop, Server
- **Output Directory**: Directory chooser (defaults to `generated/<graph-name>-kmp`)
- **Options**: License validation (enabled by default, cannot disable per constitution)

**Error States**:
- Validation errors: Show validation results panel with clickable errors
- License violations: Show blocking error dialog listing violating dependencies
- IO errors: Show error notification with details

---

### 4. Generate Go Code Action

**ID**: `io.codenode.actions.GenerateGoCode`
**Location**: Tools → CodeNodeIO → Generate Go Code
**Context**: Available when graph editor is open or .flow.kts file is selected

**Contract**:
```kotlin
/**
 * Generates Go code for backend services from flow graph.
 *
 * @precondition Flow graph is valid
 * @precondition Flow graph contains backend-specific nodes (API endpoints, data processing)
 * @postcondition Go module created with go.mod and implementation files
 * @postcondition Generated code passes license validation
 */
interface GenerateGoCodeAction {
    /**
     * Validates graph and generates Go code.
     *
     * @param flowGraph Source flow graph
     * @param moduleName Go module name (e.g., "github.com/user/project")
     * @param outputDir Output directory for generated module
     * @return GenerationResult with success/failure and generated files
     */
    fun generate(
        flowGraph: FlowGraph,
        moduleName: String,
        outputDir: VirtualFile
    ): GenerationResult
}
```

**User Dialog**:
- **Module Name**: Text input (validated: valid Go module path)
- **Output Directory**: Directory chooser (defaults to `generated/<graph-name>-go`)
- **Go Version**: Dropdown (1.21, 1.22, 1.23 - defaults to latest)

**Error States**:
- Invalid module name: Inline validation error
- License violations: Blocking error dialog
- No backend nodes: Warning dialog suggesting adding API/service nodes

---

### 5. Validate Flow Graph Action

**ID**: `io.codenode.actions.ValidateFlowGraph`
**Location**: Tools → CodeNodeIO → Validate Flow Graph
**Context**: Available when graph editor is open or .flow.kts file is selected
**Shortcut**: Ctrl+Shift+V (Windows/Linux), Cmd+Shift+V (macOS)

**Contract**:
```kotlin
/**
 * Validates flow graph against all rules (connections, types, cycles, etc.).
 *
 * @precondition Flow graph file is open or selected
 * @postcondition Validation results displayed in tool window
 * @postcondition Errors highlighted in visual editor
 */
interface ValidateFlowGraphAction {
    /**
     * Runs all validation rules on flow graph.
     *
     * @param flowGraph Flow graph to validate
     * @return ValidationResult with errors and warnings
     */
    fun validate(flowGraph: FlowGraph): ValidationResult
}

data class ValidationResult(
    val errors: List<ValidationError>,
    val warnings: List<ValidationWarning>,
    val isValid: Boolean
) {
    val hasErrors: Boolean get() = errors.isNotEmpty()
    val hasWarnings: Boolean get() = warnings.isNotEmpty()
}

data class ValidationError(
    val message: String,
    val nodeId: UUID?,
    val portId: UUID?,
    val connectionId: UUID?,
    val severity: Severity
)

enum class Severity { ERROR, WARNING, INFO }
```

**Validation Rules**:
1. All required INPUT ports are connected
2. Port types are compatible across connections
3. No cycles in CodeNode-to-CodeNode connections
4. All nodes have at least one port
5. Property configurations match schemas
6. No orphaned nodes (disconnected from graph)

---

### 6. Open Circuit Simulator Action

**ID**: `io.codenode.actions.OpenCircuitSimulator`
**Location**: Tools → CodeNodeIO → Open Circuit Simulator
**Context**: Available when graph editor is open

**Contract**:
```kotlin
/**
 * Opens circuit simulator for debugging and visualizing flow execution.
 *
 * @precondition Flow graph is valid
 * @postcondition Circuit simulator tool window opens
 * @postcondition Simulation can be started, paused, resumed, and speed-controlled
 */
interface OpenCircuitSimulatorAction {
    /**
     * Launches simulator with flow graph.
     *
     * @param flowGraph Flow graph to simulate
     * @return Simulator instance for controlling execution
     */
    fun openSimulator(flowGraph: FlowGraph): CircuitSimulator
}

interface CircuitSimulator {
    fun start()
    fun pause()
    fun resume()
    fun stop()
    fun setSpeed(multiplier: Double) // 0.1x to 10x
    fun stepForward() // Execute one IP processing cycle
    fun getExecutionState(): ExecutionState
}

data class ExecutionState(
    val running: Boolean,
    val paused: Boolean,
    val currentIP: InformationPacket?,
    val activeNodes: Set<UUID>,
    val queuedIPs: Map<UUID, List<InformationPacket>> // Port ID → queued IPs
)
```

---

## Tool Windows

### 1. Graph Editor Tool Window

**ID**: `GraphEditorToolWindow`
**Location**: Right sidebar
**Icon**: Flow graph icon

**Components**:
- **Canvas**: Main visual editing area
- **Node Palette**: Sidebar with available node types (categorized)
- **Properties Panel**: Bottom panel showing selected node/connection properties
- **Minimap**: Top-right corner showing full graph overview

**Actions**:
- Pan: Click+drag on canvas background
- Zoom: Mouse wheel or pinch gesture
- Add Node: Drag from palette to canvas
- Connect Ports: Click output port, then click input port
- Select: Click node/connection
- Multi-select: Ctrl+click or drag selection box
- Delete: Select + Delete key
- Undo/Redo: Ctrl+Z / Ctrl+Shift+Z

---

### 2. Validation Results Tool Window

**ID**: `ValidationResultsToolWindow`
**Location**: Bottom panel
**Icon**: Warning/error icon

**Content**:
- Table of errors and warnings
- Columns: Severity, Message, Location (node/port/connection)
- Click error → navigate to element in graph editor
- Filter by severity
- Auto-refresh on graph changes

---

### 3. Circuit Simulator Tool Window

**ID**: `CircuitSimulatorToolWindow`
**Location**: Bottom panel
**Icon**: Play/debug icon

**Controls**:
- Start/Stop buttons
- Pause/Resume button
- Speed slider (0.1x - 10x)
- Step Forward button
- Current execution state display

**Visualization**:
- Active nodes highlighted
- IPs shown flowing through connections (animated)
- Port queues displayed with IP counts
- Timeline showing IP processing history

---

## File Types

### Flow Graph File (.flow.kts)

**Extension**: `.flow.kts`
**Description**: CodeNodeIO Flow Graph (Kotlin DSL)
**Icon**: Custom flow graph icon
**Editor**: Graph Editor Tool Window (default), Text Editor (secondary)

**Template Content**:
```kotlin
import io.codenode.fbpdsl.*

flowGraph("NewGraph") {
    version = "1.0.0"
    description = "Flow graph description"

    // Define nodes and connections here
}
```

**Syntax Highlighting**: Kotlin syntax with custom DSL keywords highlighted

---

## Services

### 1. FlowGraphManager Service

**Scope**: Project-level service

**Contract**:
```kotlin
/**
 * Manages flow graph lifecycle and persistence.
 */
interface FlowGraphManager {
    /**
     * Loads flow graph from file.
     *
     * @param file .flow.kts file
     * @return Parsed FlowGraph
     * @throws FlowGraphParseException if file is malformed
     */
    fun loadFlowGraph(file: VirtualFile): FlowGraph

    /**
     * Saves flow graph to file.
     *
     * @param flowGraph Flow graph to save
     * @param file Target .flow.kts file
     */
    fun saveFlowGraph(flowGraph: FlowGraph, file: VirtualFile)

    /**
     * Gets all flow graphs in project.
     *
     * @return List of flow graph files
     */
    fun getAllFlowGraphs(): List<VirtualFile>
}
```

---

### 2. CodeGenerationService

**Scope**: Project-level service

**Contract**:
```kotlin
/**
 * Handles code generation for KMP and Go targets.
 */
interface CodeGenerationService {
    /**
     * Generates Kotlin Multiplatform code.
     *
     * @param flowGraph Source flow graph
     * @param config Generation configuration
     * @return Generation result
     */
    fun generateKMP(flowGraph: FlowGraph, config: KMPGenerationConfig): GenerationResult

    /**
     * Generates Go code.
     *
     * @param flowGraph Source flow graph
     * @param config Generation configuration
     * @return Generation result
     */
    fun generateGo(flowGraph: FlowGraph, config: GoGenerationConfig): GenerationResult

    /**
     * Validates generated code dependencies against license rules.
     *
     * @param dependencies List of dependencies
     * @return Validation result with violations
     */
    fun validateLicenses(dependencies: List<Dependency>): LicenseValidationResult
}
```

---

### 3. ValidationService

**Scope**: Application-level service (shared across projects)

**Contract**:
```kotlin
/**
 * Validates flow graphs against all rules.
 */
interface ValidationService {
    /**
     * Validates flow graph.
     *
     * @param flowGraph Flow graph to validate
     * @return Validation result
     */
    fun validate(flowGraph: FlowGraph): ValidationResult

    /**
     * Registers custom validation rule.
     *
     * @param rule Validation rule implementation
     */
    fun registerRule(rule: ValidationRule)

    /**
     * Gets all registered validation rules.
     *
     * @return List of validation rules
     */
    fun getRules(): List<ValidationRule>
}

interface ValidationRule {
    val id: String
    val severity: Severity
    fun validate(flowGraph: FlowGraph): List<ValidationError>
}
```

---

## Extension Points

### 1. Node Type Provider

Allows third-party plugins to contribute custom node types.

**Extension Point ID**: `io.codenode.nodeTypeProvider`

**Contract**:
```kotlin
interface NodeTypeProvider {
    /**
     * Provides custom node type definitions.
     *
     * @return List of node type definitions
     */
    fun getNodeTypes(): List<NodeTypeDefinition>
}
```

---

### 2. Code Generator Extension

Allows third-party plugins to add code generation for additional targets.

**Extension Point ID**: `io.codenode.codeGenerator`

**Contract**:
```kotlin
interface CodeGeneratorExtension {
    val targetPlatform: String // e.g., "Rust", "Python", "TypeScript"

    /**
     * Generates code for target platform.
     *
     * @param flowGraph Source flow graph
     * @param outputDir Output directory
     * @return Generation result
     */
    fun generate(flowGraph: FlowGraph, outputDir: VirtualFile): GenerationResult
}
```

---

## Error Handling

### Error Notification Strategy

1. **Validation Errors**: Show in Validation Results tool window + highlight in editor
2. **Generation Errors**: Show error balloon notification + detailed log in Event Log
3. **License Violations**: Show blocking error dialog (cannot proceed)
4. **Parse Errors**: Show error balloon with line number + highlight in text editor
5. **IO Errors**: Show error balloon with retry option

### Error Message Format

**User-Facing Errors**:
- Clear, actionable messages
- No stack traces (logged separately)
- Suggest corrective actions

**Example**:
```
❌ Validation Error: Unconnected Input Port

Node "EmailValidator" has required input port "email" that is not connected.

Action: Connect a source node's output to this input port, or mark the port as optional.

Location: EmailValidator:12
```

---

## Performance Contracts

### UI Responsiveness

- **Graph Rendering**: Must render 50-node graphs in <100ms
- **Validation**: Must complete in <500ms for typical graphs
- **Code Generation**: Must complete in <30s for 10-15 node graphs (background task)
- **File Operations**: Load/save must complete in <1s for typical graphs

### Memory Constraints

- **Plugin Heap**: Must operate within <500MB heap allocation
- **Graph Size**: Support graphs up to 100 nodes (beyond 50 may have degraded performance)

---

## Accessibility

### Keyboard Navigation

- **All actions**: Must have keyboard shortcuts
- **Graph editor**: Arrow keys for navigation, Tab for element selection
- **Dialogs**: Standard tab order, Enter/Esc for confirm/cancel

### Screen Reader Support

- **Node labels**: Must be announced by screen readers
- **Validation errors**: Must be readable in Validation Results window
- **Status updates**: Must announce state changes (e.g., "Code generation complete")

---

## Security Considerations

### Input Validation

- **File paths**: Validate to prevent path traversal
- **DSL parsing**: Sandbox Kotlin script execution to prevent code injection
- **Module names**: Validate format to prevent command injection

### License Validation

- **CRITICAL**: ALL dependencies MUST pass license check before code generation completes
- **Blocking**: License violations prevent code generation (cannot be overridden)
- **Audit Trail**: Log all license validation results for compliance audit

---

## Version Compatibility

### Backward Compatibility

- **Flow Graph Format**: Minor version changes must be backward compatible
- **Plugin API**: Maintain binary compatibility within major version
- **Generated Code**: Generated code targets minimum platform versions (Kotlin 1.9+, Go 1.21+)

### Migration

- **Format Changes**: Provide automatic migration for flow graph format changes
- **Deprecation**: Deprecated APIs must be supported for at least 2 minor versions
