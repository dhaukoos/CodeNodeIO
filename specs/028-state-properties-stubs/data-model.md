# Data Model: State Properties Stubs

## Entities

### StatePropertiesGenerator

New generator class in `kotlinCompiler` module. Follows the same pattern as `ProcessingLogicStubGenerator`.

**Responsibilities:**
- Determine if a node needs a state properties file (`shouldGenerate(codeNode)`)
- Generate state properties filename (`getStatePropertiesFileName(codeNode)`)
- Generate state properties object name (`getStatePropertiesObjectName(codeNode)`)
- Generate complete state properties file content (`generateStateProperties(codeNode, packageName)`)

**Methods:**

| Method | Input | Output | Logic |
|--------|-------|--------|-------|
| `shouldGenerate(CodeNode)` | CodeNode | Boolean | Returns `true` if node has >= 1 port (input or output) |
| `getStatePropertiesFileName(CodeNode)` | CodeNode | String | `"{NodeName}StateProperties.kt"` |
| `getStatePropertiesObjectName(CodeNode)` | CodeNode | String | `"{NodeName}StateProperties"` (PascalCase) |
| `generateStateProperties(CodeNode, String)` | CodeNode, packageName | String | Full Kotlin source for the state properties object |

### Generated State Properties Object (output artifact)

A Kotlin `object` generated per node, placed in `stateProperties/` sub-package.

**Structure per port:**

| Property | Visibility | Type | Description |
|----------|-----------|------|-------------|
| `_{portName}` | `internal` | `MutableStateFlow<{PortType}>` | Mutable state, accessible within module |
| `{portName}Flow` | `public` | `StateFlow<{PortType}>` | Read-only accessor via `.asStateFlow()` |

**Methods:**

| Method | Visibility | Description |
|--------|-----------|-------------|
| `reset()` | `public` | Resets all `_portName.value` to initial defaults |

### ModuleSaveService Changes

**New constant:**
- `STATE_PROPERTIES_SUBPACKAGE = "stateProperties"`

**New method:**
- `generateStatePropertiesFiles(FlowGraph, File, String, MutableList<String>)` — Generates state properties files with don't-overwrite semantics

**Modified methods:**
- `compileModule()` — Adds calls to create stateProperties directory, generate files, detect orphans
- `detectOrphanedComponents()` — Extended to also scan `stateProperties/` directory

### ProcessingLogicStubGenerator Changes

**Modified method:**
- `generateStub(CodeNode, String)` — Accepts additional `statePropertiesPackage` parameter; adds import for state properties object

### RuntimeFlowGenerator Changes

**Modified methods:**
- `generateImports()` — Adds imports for state properties objects from `stateProperties` package
- `generateObservableState()` — Changed from creating MutableStateFlow to delegating from state properties objects
- `generateSinkConsumeBlock()` — References state properties object for `_portName.value` updates
- `generateResetMethod()` — Calls `reset()` on each state properties object instead of directly resetting MutableStateFlow values

## Relationships

```text
StatePropertiesGenerator ──generates──> {NodeName}StateProperties.kt
ProcessingLogicStubGenerator ──imports──> {NodeName}StateProperties (in generated stub)
RuntimeFlowGenerator ──delegates from──> {NodeName}StateProperties (in generated Flow class)
ModuleSaveService ──orchestrates──> StatePropertiesGenerator (alongside ProcessingLogicStubGenerator)
ObservableStateResolver ──still used by──> RuntimeFlowGenerator (to identify which sink ports to delegate)
```
