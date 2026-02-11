# Quickstart: StopWatch Code Generation Refactor

**Feature**: 009-stopwatch-codegen-refactor
**Date**: 2026-02-11

## Prerequisites

- Kotlin 2.1.21+
- JDK 17+
- Gradle 8.x
- CodeNodeIO repository cloned

## Development Environment Setup

### 1. Checkout Feature Branch

```bash
cd /path/to/CodeNodeIO
git checkout 009-stopwatch-codegen-refactor
```

### 2. Build All Modules

```bash
./gradlew build
```

### 3. Run Graph Editor

```bash
./gradlew :graphEditor:run
```

## Key Files to Understand

### Properties Panel (UI)

```
graphEditor/src/jvmMain/kotlin/ui/
├── PropertiesPanel.kt      # Main panel component, state management
└── PropertyEditors.kt      # Individual editor components (TEXT_FIELD, etc.)
```

**Key classes**:
- `PropertiesPanelState` - Manages selected node, properties, validation
- `PropertyDefinition` - Describes a property (name, type, required)
- `PropertyType` / `EditorType` - Enums for property/editor types

### Code Generation

```
kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/
├── ModuleGenerator.kt      # Full module generation (build.gradle, sources)
├── FlowGenerator.kt        # Flow orchestrator class generation
└── ComponentGenerator.kt   # Component class generation
```

**Key classes**:
- `ModuleGenerator` - Coordinates full module generation
- Uses KotlinPoet for code generation (`FunSpec`, `TypeSpec`, `FileSpec`)

### Compilation Service

```
graphEditor/src/jvmMain/kotlin/compilation/
└── CompilationService.kt   # Triggers code generation from UI
```

### Serialization

```
graphEditor/src/jvmMain/kotlin/serialization/
└── FlowGraphSerializer.kt  # FlowGraph → .flow.kts file
```

**Note**: Configuration properties serialized via `config(key, value)` DSL calls.

## Implementation Tasks Overview

### Task Group 1: File Browser Editor

Add new editor type for file selection:

1. Add `PropertyType.FILE_PATH` enum value
2. Add `EditorType.FILE_BROWSER` enum value
3. Create `FileBrowserEditor` composable
4. Wire into `PropertyEditorRow` switch

**Test file**: `graphEditor/src/jvmTest/kotlin/ui/FileBrowserEditorTest.kt`

### Task Group 2: Compilation Validator

Create pre-compile validation:

1. Create `CompilationValidator` class
2. Define `RequiredPropertySpec` for processingLogicFile
3. Implement validation logic
4. Integrate into compile flow

**Test file**: `graphEditor/src/jvmTest/kotlin/compilation/CompilationValidatorTest.kt`

### Task Group 3: Factory Generator

Generate `createXXXFlowGraph()` function:

1. Create `FlowGraphFactoryGenerator` class
2. Generate node creation code with KotlinPoet
3. Generate port and connection code
4. Integrate into `ModuleGenerator`

**Test file**: `kotlinCompiler/src/commonTest/kotlin/.../FlowGraphFactoryGeneratorTest.kt`

### Task Group 4: Integration

Update StopWatch demo:

1. Add processingLogicFile config to StopWatch.flow.kts
2. Regenerate StopWatch module
3. Update KMPMobileApp to use generated factory

## Running Tests

### All Tests
```bash
./gradlew test
```

### Specific Module
```bash
./gradlew :graphEditor:test
./gradlew :kotlinCompiler:test
```

### Single Test Class
```bash
./gradlew :graphEditor:test --tests "io.codenode.grapheditor.ui.FileBrowserEditorTest"
```

## Debugging Tips

### GraphEditor UI
1. Run with `./gradlew :graphEditor:run`
2. Open `demos/stopwatch/StopWatch.flow.kts`
3. Select a node to see PropertiesPanel
4. Use IntelliJ debugger attached to Gradle process

### Code Generation
1. Write test in `FlowGraphFactoryGeneratorTest`
2. Print generated code: `println(funSpec.toString())`
3. Verify output compiles by adding to integration test

### Validation
1. Create test graph with missing properties
2. Call validator and inspect result
3. Check error messages are actionable

## Code Style Notes

### Compose Desktop Patterns
- Use `mutableStateOf()` for reactive state
- Immutable data classes with `copy()` for updates
- Remember composables where appropriate

### KotlinPoet Patterns
```kotlin
// Building a function
FunSpec.builder("createFoo")
    .returns(FlowGraph::class)
    .addCode(/* code block */)
    .build()

// Building code blocks
CodeBlock.builder()
    .addStatement("val x = %T()", SomeClass::class)
    .build()
```

### File Path Handling
```kotlin
// Always use forward slashes for storage
path.replace('\\', '/')

// Resolve relative to project root
projectRoot.resolve(relativePath)
```

## Common Issues

### "File not found" validation error
- Ensure path is relative to project root
- Check file exists: `ls -la demos/stopwatch/TimerEmitterComponent.kt`

### "ProcessingLogic class not found" at runtime
- Verify class name matches file name
- Check import statement in generated code

### JFileChooser not showing
- Running on headless server? Compose Desktop requires display
- Check DISPLAY env var on Linux

## Useful Commands

```bash
# Check current branch
git branch --show-current

# View StopWatch.flow.kts
cat demos/stopwatch/StopWatch.flow.kts

# View generated code
cat StopWatch/src/commonMain/kotlin/io/codenode/generated/stopwatch/StopWatchFlow.kt

# Run StopWatch module tests
./gradlew :StopWatch:test

# Run KMPMobileApp
./gradlew :KMPMobileApp:run
```

## References

- [Spec](./spec.md) - Feature requirements
- [Plan](./plan.md) - Implementation approach
- [Data Model](./data-model.md) - Entity definitions
- [Contracts](./contracts/) - API and UI contracts
- [KotlinPoet docs](https://square.github.io/kotlinpoet/)
- [Compose Desktop docs](https://www.jetbrains.com/lp/compose-mpp/)
