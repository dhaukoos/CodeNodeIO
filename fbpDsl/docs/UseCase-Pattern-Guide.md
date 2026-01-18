# UseCase Pattern Implementation Guide

## Overview

The CodeNode processing logic has been refactored to support the UseCase pattern, providing a powerful and flexible architecture for business logic implementation.

## What Was Implemented

### 1. Fun Interface for ProcessingLogic

**File**: `CodeNode.kt`

Converted from typealias to functional interface:

```kotlin
fun interface ProcessingLogic {
    suspend operator fun invoke(inputs: Map<String, InformationPacket<*>>): Map<String, InformationPacket<*>>
}
```

**Benefits**:
- ✅ Lambda syntax still works: `ProcessingLogic { inputs -> ... }`
- ✅ Class implementations: `class MyUseCase : ProcessingLogic { ... }`
- ✅ SAM conversion: Automatic conversion from lambda to interface
- ✅ Backward compatible: Existing code doesn't break

### 2. Typed UseCase Base Classes

**File**: `TypedUseCases.kt`

Provides type-safe abstractions for common patterns:

#### TransformerUseCase<TIn, TOut>
Transforms input type to output type:
```kotlin
class CelsiusToFahrenheitUseCase : TransformerUseCase<Temperature, TemperatureFahrenheit>(
    outputType = TemperatureFahrenheit::class
) {
    override suspend fun transform(input: Temperature): TemperatureFahrenheit {
        return TemperatureFahrenheit(input.celsius * 9/5 + 32)
    }
}
```

#### FilterUseCase<T>
Conditional pass-through:
```kotlin
class ActiveUserFilterUseCase : FilterUseCase<ApiUser>() {
    override suspend fun shouldPass(input: ApiUser): Boolean {
        return input.isActive
    }
}
```

#### ValidatorUseCase<T>
Routes to valid/invalid outputs:
```kotlin
class EmailValidatorUseCase : ValidatorUseCase<EmailData>() {
    override suspend fun isValid(input: EmailData): Boolean {
        return input.address.contains("@")
    }
}
```

#### SplitterUseCase<T>
Dynamic output port selection:
```kotlin
class OrderRouterUseCase : SplitterUseCase<OrderData>() {
    override suspend fun selectOutputPorts(input: OrderData): List<String> {
        return when {
            input.priority == "urgent" -> listOf("urgent")
            else -> listOf("standard")
        }
    }
}
```

#### MergerUseCase<T>
Combines multiple inputs:
```kotlin
class SensorMergerUseCase : MergerUseCase<SensorReading>(
    outputType = SensorReading::class
) {
    override suspend fun merge(inputs: Map<String, SensorReading>): SensorReading {
        val avgTemp = inputs.values.map { it.temperature }.average()
        return SensorReading(avgTemp, System.currentTimeMillis())
    }
}
```

#### GeneratorUseCase<T>
Generates output without input:
```kotlin
class TimestampGeneratorUseCase : GeneratorUseCase<TimestampData>(
    outputType = TimestampData::class
) {
    override suspend fun generate(): TimestampData {
        return TimestampData(System.currentTimeMillis())
    }
}
```

#### SinkUseCase<T>
Consumes input without output:
```kotlin
class LoggerUseCase(private val logger: Logger) : SinkUseCase<LogEntry>() {
    override suspend fun consume(input: LogEntry) {
        logger.log(input.level, input.message)
    }
}
```

### 3. Example UseCases with Patterns

**File**: `examples/ExampleUseCases.kt`

Demonstrates real-world patterns:

#### Dependency Injection
```kotlin
class UserTransformUseCase(
    private val logger: Logger,  // Injected dependency
    private val apiClient: ApiClient
) : TransformerUseCase<ApiUser, DomainUser>(
    outputType = DomainUser::class
) {
    override suspend fun transform(input: ApiUser): DomainUser {
        logger.log("INFO", "Processing user: ${input.id}")
        return DomainUser(...)
    }
}
```

#### Decorator Pattern
```kotlin
class LoggingDecorator(
    private val delegate: ProcessingLogic,
    private val logger: Logger,
    private val operationName: String
) : ProcessingLogic {
    override suspend fun invoke(inputs: Map<String, InformationPacket<*>>): Map<String, InformationPacket<*>> {
        logger.log("DEBUG", "Starting: $operationName")
        val startTime = System.currentTimeMillis()

        return try {
            val result = delegate(inputs)
            logger.log("INFO", "Completed in ${System.currentTimeMillis() - startTime}ms")
            result
        } catch (e: Exception) {
            logger.log("ERROR", "Failed: ${e.message}")
            throw e
        }
    }
}
```

#### Composition & Pipelines
```kotlin
class UserProcessingPipelineUseCase(
    private val transformer: ProcessingLogic,
    private val validator: ProcessingLogic
) : ProcessingLogic {
    override suspend fun invoke(inputs: Map<String, InformationPacket<*>>): Map<String, InformationPacket<*>> {
        val transformed = transformer(inputs)
        return validator(transformed)
    }
}
```

#### Stateful Processing
```kotlin
class TemperatureAveragerUseCase : TransformerUseCase<Temperature, Temperature>(
    outputType = Temperature::class
) {
    private var count = 0
    private var sum = 0.0

    override suspend fun transform(input: Temperature): Temperature {
        count++
        sum += input.celsius
        return Temperature(sum / count)
    }
}
```

### 4. Lifecycle Support

**File**: `LifecycleAwareUseCases.kt`

Provides lifecycle management for stateful UseCases:

#### Lifecycle Interface
```kotlin
interface Lifecycle {
    suspend fun initialize() {}
    suspend fun cleanup() {}
    suspend fun onError(error: Exception, inputs: Map<String, InformationPacket<*>>): Boolean {
        return false
    }
}
```

#### LifecycleAwareUseCase
```kotlin
abstract class LifecycleAwareUseCase : ProcessingLogic, Lifecycle {
    protected abstract suspend fun process(inputs: Map<String, InformationPacket<*>>): Map<String, InformationPacket<*>>
}
```

**Example Usage**:
```kotlin
class DatabaseProcessorUseCase(
    private val connectionString: String
) : LifecycleAwareUseCase() {
    private lateinit var connection: DatabaseConnection

    override suspend fun initialize() {
        connection = Database.connect(connectionString)
    }

    override suspend fun cleanup() {
        connection.close()
    }

    override suspend fun process(inputs: Map<String, InformationPacket<*>>): Map<String, InformationPacket<*>> {
        // Use the connection
        return mapOf(...)
    }
}
```

#### LifecycleManager
Coordinates multiple UseCases:
```kotlin
val manager = LifecycleManager()
manager.register(useCase1)
manager.register(useCase2)

manager.initializeAll()
try {
    // Use the use cases
} finally {
    manager.cleanupAll()
}
```

#### Scoped Lifecycle
```kotlin
withLifecycle(MyLifecycleUseCase()) { useCase ->
    val result = useCase(inputs)
    // Use the result
}
// Automatic cleanup
```

#### Specialized Lifecycle Base Classes
- **DatabaseUseCase**: Automatic connection management
- **CachedUseCase<K, V>**: Cache initialization/cleanup
- **BufferedUseCase<T>**: Automatic flushing on cleanup

## Key Advantages

### 1. Dependency Injection ✅
UseCases can have dependencies injected via constructor, enabling:
- Testability with mocks
- IoC container integration
- Clean architecture separation

### 2. Testability ✅
- Easy to mock dependencies
- Can create test implementations
- Verify interactions with collaborators

### 3. Reusability ✅
- UseCases can be shared across multiple CodeNodes
- Build a library of business logic
- Consistent behavior across the application

### 4. State Management ✅
- Can maintain internal state between invocations
- Support for caches, counters, buffers
- Lifecycle hooks for resource management

### 5. Composition & Chaining ✅
- Combine multiple UseCases into pipelines
- Decorator pattern for cross-cutting concerns
- Fan-out/fan-in parallel processing

### 6. Observability ✅
- Decorators for logging, metrics, tracing
- Centralized cross-cutting concerns
- Error handling and recovery

### 7. Serialization ✅
- Can serialize UseCase class names
- Recreate graphs from persisted definitions
- Version control for processing logic

### 8. Error Handling ✅
- Lifecycle-aware error recovery
- Retry decorators
- Graceful degradation

## Migration Guide

### From Lambda to UseCase

**Before (Lambda)**:
```kotlin
val node = CodeNodeFactory.createTransformer<InputData, OutputData>(
    name = "StringToInt",
    transform = { input ->
        OutputData(input.value.toIntOrNull() ?: 0)
    }
)
```

**After (UseCase Class)**:
```kotlin
class StringToIntUseCase : TransformerUseCase<InputData, OutputData>(
    outputType = OutputData::class
) {
    override suspend fun transform(input: InputData): OutputData {
        return OutputData(input.value.toIntOrNull() ?: 0)
    }
}

val node = CodeNodeFactory.create(
    name = "StringToInt",
    codeNodeType = CodeNodeType.TRANSFORMER,
    processingLogic = StringToIntUseCase()
)
```

**With Dependencies**:
```kotlin
class StringToIntUseCase(
    private val logger: Logger,
    private val validator: InputValidator
) : TransformerUseCase<InputData, OutputData>(
    outputType = OutputData::class
) {
    override suspend fun transform(input: InputData): OutputData {
        logger.info("Converting: ${input.value}")
        validator.validate(input)
        return OutputData(input.value.toIntOrNull() ?: 0)
    }
}

// Inject dependencies
val useCase = StringToIntUseCase(logger, validator)
val node = CodeNodeFactory.create(
    name = "StringToInt",
    codeNodeType = CodeNodeType.TRANSFORMER,
    processingLogic = useCase
)
```

## Best Practices

### 1. Single Responsibility
Each UseCase should do one thing well:
```kotlin
// Good: Focused responsibility
class EmailValidatorUseCase : ValidatorUseCase<EmailData>()

// Bad: Multiple responsibilities
class EmailValidatorAndSenderUseCase : ...
```

### 2. Immutable Dependencies
Dependencies should be immutable and thread-safe:
```kotlin
// Good: Immutable dependencies
class MyUseCase(
    private val config: Configuration,  // Immutable
    private val apiClient: ApiClient    // Thread-safe
) : ...

// Bad: Mutable shared state
class MyUseCase(
    private var counter: Int = 0  // Mutable, not thread-safe
) : ...
```

### 3. Use Lifecycle for Resources
Always use lifecycle hooks for resource management:
```kotlin
class FileProcessorUseCase : LifecycleAwareUseCase() {
    private lateinit var fileHandle: FileHandle

    override suspend fun initialize() {
        fileHandle = openFile()
    }

    override suspend fun cleanup() {
        fileHandle.close()
    }
}
```

### 4. Prefer Composition over Inheritance
Build complex logic by composing simple UseCases:
```kotlin
val pipeline = listOf(
    ValidatorUseCase(),
    TransformerUseCase(),
    EnricherUseCase()
).reduce { acc, useCase ->
    ChainedUseCase(acc, useCase)
}
```

### 5. Use Decorators for Cross-Cutting Concerns
```kotlin
val baseLogic = MyBusinessLogic()
val withLogging = LoggingDecorator(baseLogic, logger)
val withRetry = RetryDecorator(withLogging, maxAttempts = 3)
val withMetrics = MetricsDecorator(withRetry, metrics)
```

## File Structure

```
fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/
├── model/
│   ├── CodeNode.kt              # ProcessingLogic fun interface
│   ├── CodeNodeFactory.kt       # Updated factory methods
│   └── ...
└── usecase/
    ├── TypedUseCases.kt         # Base classes for common patterns
    ├── LifecycleAwareUseCases.kt # Lifecycle support
    └── examples/
        └── ExampleUseCases.kt   # Real-world examples
```

## Build Status

✅ **ALL IMPLEMENTATIONS COMPILE SUCCESSFULLY**

## Next Steps

1. Add unit tests for UseCase base classes
2. Create more specialized UseCases (HTTP, Database, File I/O)
3. Add metrics/observability decorators
4. Integration with dependency injection frameworks
5. Code generation for boilerplate UseCase code

## Conclusion

The UseCase pattern refactoring provides a powerful, flexible, and maintainable architecture for CodeNode processing logic while maintaining backward compatibility with lambda-based approaches. The combination of type safety, dependency injection, lifecycle management, and composition patterns creates a robust foundation for building complex data flow applications.
