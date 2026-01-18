/*
 * ExampleUseCases - Practical examples of UseCase patterns
 * Demonstrates dependency injection, composition, and real-world scenarios
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.usecase.examples

import io.codenode.fbpdsl.model.*
import io.codenode.fbpdsl.usecase.*
import kotlin.reflect.KClass

// ============================================================================
// Example Data Models
// ============================================================================

/**
 * User data from an API
 */
data class ApiUser(
    val id: String,
    val username: String,
    val email: String,
    val isActive: Boolean
)

/**
 * Domain user model
 */
data class DomainUser(
    val userId: String,
    val displayName: String,
    val contactEmail: String
)

/**
 * Temperature reading
 */
data class Temperature(val celsius: Double)

/**
 * Temperature in Fahrenheit
 */
data class TemperatureFahrenheit(val fahrenheit: Double)

/**
 * Log entry
 */
data class LogEntry(
    val level: String,
    val message: String,
    val timestamp: Long
)

// ============================================================================
// Example 1: Simple Transformation with Dependency Injection
// ============================================================================

/**
 * Example logger interface (would be injected)
 */
interface Logger {
    fun log(level: String, message: String)
}

/**
 * Example API client interface (would be injected)
 */
interface ApiClient {
    suspend fun fetchUserDetails(userId: String): Map<String, String>
}

/**
 * Transforms API user data to domain user model with logging
 *
 * Demonstrates:
 * - Constructor-based dependency injection
 * - Using injected dependencies in transformation logic
 */
class UserTransformUseCase(
    private val logger: Logger
) : TransformerUseCase<ApiUser, DomainUser>(
    outputType = DomainUser::class
) {
    override suspend fun transform(input: ApiUser): DomainUser {
        logger.log("INFO", "Transforming user: ${input.username}")

        return DomainUser(
            userId = input.id,
            displayName = input.username,
            contactEmail = input.email
        )
    }
}

// ============================================================================
// Example 2: Filter with Business Logic
// ============================================================================

/**
 * Filters out inactive users
 *
 * Demonstrates:
 * - Simple business rule implementation
 * - Type-safe filtering
 */
class ActiveUserFilterUseCase : FilterUseCase<ApiUser>() {
    override suspend fun shouldPass(input: ApiUser): Boolean {
        return input.isActive
    }
}

// ============================================================================
// Example 3: Validator with Multiple Rules
// ============================================================================

/**
 * Validates email format
 *
 * Demonstrates:
 * - Validation logic
 * - Routing to valid/invalid ports
 */
class EmailValidatorUseCase : ValidatorUseCase<ApiUser>() {
    override suspend fun isValid(input: ApiUser): Boolean {
        return input.email.contains("@") &&
                input.email.contains(".") &&
                input.email.length >= 5
    }
}

// ============================================================================
// Example 4: Stateful UseCase (Counter)
// ============================================================================

/**
 * Counts temperature readings and calculates running average
 *
 * Demonstrates:
 * - Stateful processing
 * - Internal state management between invocations
 */
class TemperatureAveragerUseCase : TransformerUseCase<Temperature, Temperature>(
    outputType = Temperature::class
) {
    private var count = 0
    private var sum = 0.0

    override suspend fun transform(input: Temperature): Temperature {
        count++
        sum += input.celsius

        val average = sum / count
        return Temperature(average)
    }

    /**
     * Reset the running average
     */
    fun reset() {
        count = 0
        sum = 0.0
    }
}

// ============================================================================
// Example 5: Composition - Chaining UseCases
// ============================================================================

/**
 * Composite use case that chains multiple transformations
 *
 * Demonstrates:
 * - UseCase composition
 * - Sequential processing pipeline
 */
class UserProcessingPipelineUseCase(
    private val transformer: ProcessingLogic,
    private val validator: ProcessingLogic
) : ProcessingLogic {
    override suspend fun invoke(inputs: Map<String, InformationPacket<*>>): Map<String, InformationPacket<*>> {
        // First apply transformation
        val transformedOutputs = transformer(inputs)

        // Then apply validation
        return validator(transformedOutputs)
    }
}

// ============================================================================
// Example 6: Enrichment with External Data
// ============================================================================

/**
 * Enriches user data with additional details from API
 *
 * Demonstrates:
 * - Async operations with external services
 * - Combining input data with fetched data
 */
class UserEnrichmentUseCase(
    private val apiClient: ApiClient,
    private val logger: Logger
) : TransformerUseCase<ApiUser, ApiUser>(
    outputType = ApiUser::class
) {
    override suspend fun transform(input: ApiUser): ApiUser {
        logger.log("INFO", "Enriching user: ${input.id}")

        // Fetch additional data from API
        val details = apiClient.fetchUserDetails(input.id)

        // Could merge with additional fields, for now return as-is
        return input.copy(
            // Enrichment logic here
        )
    }
}

// ============================================================================
// Example 7: Error Handling with Recovery
// ============================================================================

/**
 * Safe temperature converter with error handling
 *
 * Demonstrates:
 * - Try-catch error handling
 * - Fallback values on error
 */
class SafeTemperatureConverter(
    private val logger: Logger
) : TransformerUseCase<Temperature, TemperatureFahrenheit>(
    outputType = TemperatureFahrenheit::class
) {
    override suspend fun transform(input: Temperature): TemperatureFahrenheit {
        return try {
            val fahrenheit = input.celsius * 9.0 / 5.0 + 32.0
            TemperatureFahrenheit(fahrenheit)
        } catch (e: Exception) {
            logger.log("ERROR", "Failed to convert temperature: ${e.message}")
            TemperatureFahrenheit(0.0) // Fallback value
        }
    }
}

// ============================================================================
// Example 8: Decorator Pattern for Cross-Cutting Concerns
// ============================================================================

/**
 * Decorates any ProcessingLogic with logging
 *
 * Demonstrates:
 * - Decorator pattern
 * - Cross-cutting concerns (logging, metrics, tracing)
 * - Wrapping existing logic without modification
 */
class LoggingDecorator(
    private val delegate: ProcessingLogic,
    private val logger: Logger,
    private val operationName: String
) : ProcessingLogic {
    override suspend fun invoke(inputs: Map<String, InformationPacket<*>>): Map<String, InformationPacket<*>> {
        logger.log("DEBUG", "Starting operation: $operationName")

        val startTime = System.currentTimeMillis()

        return try {
            val result = delegate(inputs)
            val duration = System.currentTimeMillis() - startTime

            logger.log("INFO", "Completed operation: $operationName in ${duration}ms")
            result
        } catch (e: Exception) {
            logger.log("ERROR", "Failed operation: $operationName - ${e.message}")
            throw e
        }
    }
}

/**
 * Decorates any ProcessingLogic with retry logic
 *
 * Demonstrates:
 * - Retry pattern for resilience
 * - Configurable retry attempts and delay
 */
class RetryDecorator(
    private val delegate: ProcessingLogic,
    private val maxAttempts: Int = 3,
    private val delayMs: Long = 1000
) : ProcessingLogic {
    override suspend fun invoke(inputs: Map<String, InformationPacket<*>>): Map<String, InformationPacket<*>> {
        var lastException: Exception? = null

        repeat(maxAttempts) { attempt ->
            try {
                return delegate(inputs)
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxAttempts - 1) {
                    // Wait before retry (would use kotlinx.coroutines.delay in real implementation)
                    // delay(delayMs)
                }
            }
        }

        throw lastException ?: IllegalStateException("Retry failed without exception")
    }
}

// ============================================================================
// Example 9: Parallel Processing with Fan-Out/Fan-In
// ============================================================================

/**
 * Parallel processor that fans out to multiple processors and aggregates results
 *
 * Demonstrates:
 * - Parallel processing pattern
 * - Multiple processing paths
 * - Result aggregation
 */
class ParallelProcessorUseCase(
    private val processors: List<ProcessingLogic>
) : ProcessingLogic {
    override suspend fun invoke(inputs: Map<String, InformationPacket<*>>): Map<String, InformationPacket<*>> {
        // In a real implementation, would use coroutines for parallel execution
        // For now, sequential demonstration

        val allResults = mutableMapOf<String, InformationPacket<*>>()

        processors.forEach { processor ->
            val results = processor(inputs)
            allResults.putAll(results)
        }

        return allResults
    }
}

// ============================================================================
// Example 10: Conditional Routing
// ============================================================================

/**
 * Routes users to different processing paths based on status
 *
 * Demonstrates:
 * - Conditional logic
 * - Dynamic output port selection
 */
class UserRouterUseCase : SplitterUseCase<ApiUser>() {
    override suspend fun selectOutputPorts(input: ApiUser): List<String> {
        return when {
            !input.isActive -> listOf("inactive")
            input.email.endsWith("@admin.com") -> listOf("admin")
            else -> listOf("regular")
        }
    }
}

// ============================================================================
// Example 11: Batch Processing
// ============================================================================

/**
 * Accumulates items and processes them in batches
 *
 * Demonstrates:
 * - Stateful batch accumulation
 * - Conditional output based on batch size
 */
class BatchProcessorUseCase<T : Any>(
    private val batchSize: Int,
    private val outputType: KClass<T>
) : ProcessingLogic {
    private val batch = mutableListOf<T>()

    override suspend fun invoke(inputs: Map<String, InformationPacket<*>>): Map<String, InformationPacket<*>> {
        @Suppress("UNCHECKED_CAST")
        val inputPacket = inputs["input"] as? InformationPacket<T>
            ?: throw IllegalArgumentException("Missing or invalid input packet")

        batch.add(inputPacket.payload)

        return if (batch.size >= batchSize) {
            val batchData = batch.toList()
            batch.clear()

            // Return batch as list data
            mapOf("output" to InformationPacketFactory.createWithType(
                ListData::class,
                ListData(batchData)
            ))
        } else {
            emptyMap() // Accumulating, no output yet
        }
    }

    /**
     * Flush remaining items in batch
     */
    suspend fun flush(): Map<String, InformationPacket<*>> {
        return if (batch.isNotEmpty()) {
            val batchData = batch.toList()
            batch.clear()
            mapOf("output" to InformationPacketFactory.createWithType(
                ListData::class,
                ListData(batchData)
            ))
        } else {
            emptyMap()
        }
    }
}

// ============================================================================
// Usage Example in CodeNode Construction
// ============================================================================

/**
 * Example of creating CodeNodes with UseCases
 */
object UseCaseExamples {
    /**
     * Create a user transformation node with dependency injection
     */
    fun createUserTransformNode(logger: Logger): CodeNode {
        val useCase = UserTransformUseCase(logger)

        return CodeNodeFactory.create(
            name = "UserTransformer",
            codeNodeType = CodeNodeType.TRANSFORMER,
            processingLogic = useCase
        )
    }

    /**
     * Create a decorated node with logging
     */
    fun createDecoratedNode(
        innerLogic: ProcessingLogic,
        logger: Logger,
        operationName: String
    ): CodeNode {
        val decoratedLogic = LoggingDecorator(innerLogic, logger, operationName)

        return CodeNodeFactory.create(
            name = operationName,
            codeNodeType = CodeNodeType.CUSTOM,
            processingLogic = decoratedLogic
        )
    }

    /**
     * Create a pipeline of multiple UseCases
     */
    fun createUserProcessingPipeline(
        logger: Logger
    ): CodeNode {
        val transformer = UserTransformUseCase(logger)
        val validator = EmailValidatorUseCase()
        val pipeline = UserProcessingPipelineUseCase(transformer, validator)

        return CodeNodeFactory.create(
            name = "UserPipeline",
            codeNodeType = CodeNodeType.CUSTOM,
            processingLogic = pipeline
        )
    }
}
