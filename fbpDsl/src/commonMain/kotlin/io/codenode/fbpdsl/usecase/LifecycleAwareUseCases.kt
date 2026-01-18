/*
 * LifecycleAwareUseCases - Lifecycle management for UseCases
 * Provides initialization and cleanup hooks for stateful processing
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.usecase

import io.codenode.fbpdsl.model.InformationPacket
import io.codenode.fbpdsl.model.ProcessingLogic

/**
 * Lifecycle interface for UseCases that need initialization and cleanup
 *
 * Provides hooks for:
 * - Resource allocation/initialization before processing
 * - Resource cleanup/deallocation after processing
 * - Error recovery
 */
interface Lifecycle {
    /**
     * Initialize resources before processing begins
     *
     * Called once when the CodeNode starts execution.
     * Use this to:
     * - Open database connections
     * - Initialize caches
     * - Load configuration
     * - Connect to external services
     *
     * @throws Exception if initialization fails
     */
    suspend fun initialize() {}

    /**
     * Clean up resources after processing completes or on error
     *
     * Called once when the CodeNode stops execution or encounters an error.
     * Use this to:
     * - Close database connections
     * - Flush buffers
     * - Release file handles
     * - Disconnect from services
     *
     * Guaranteed to be called even if processing throws an exception.
     */
    suspend fun cleanup() {}

    /**
     * Handle errors during processing
     *
     * Called when an exception occurs during invoke().
     * Return true to recover and continue, false to propagate the exception.
     *
     * @param error The exception that occurred
     * @param inputs The inputs that caused the error
     * @return true to recover and continue, false to propagate
     */
    suspend fun onError(error: Exception, inputs: Map<String, InformationPacket<*>>): Boolean {
        return false // Default: propagate errors
    }
}

/**
 * Abstract base class for lifecycle-aware ProcessingLogic
 *
 * Automatically manages initialization and cleanup lifecycle.
 * Subclasses should override initialize() and cleanup() as needed.
 *
 * @sample
 * ```kotlin
 * class DatabaseProcessorUseCase(
 *     private val connectionString: String
 * ) : LifecycleAwareUseCase() {
 *     private lateinit var connection: DatabaseConnection
 *
 *     override suspend fun initialize() {
 *         connection = Database.connect(connectionString)
 *     }
 *
 *     override suspend fun cleanup() {
 *         connection.close()
 *     }
 *
 *     override suspend fun process(inputs: Map<String, InformationPacket<*>>): Map<String, InformationPacket<*>> {
 *         // Use the connection
 *         return mapOf(...)
 *     }
 * }
 * ```
 */
abstract class LifecycleAwareUseCase : ProcessingLogic, Lifecycle {
    @Volatile
    private var isInitialized = false

    /**
     * Process inputs and produce outputs
     *
     * This method is called for each invocation after initialization.
     * Override this instead of invoke() to get automatic lifecycle management.
     *
     * @param inputs Map of input port name to InformationPacket
     * @return Map of output port name to InformationPacket
     */
    protected abstract suspend fun process(inputs: Map<String, InformationPacket<*>>): Map<String, InformationPacket<*>>

    final override suspend fun invoke(inputs: Map<String, InformationPacket<*>>): Map<String, InformationPacket<*>> {
        // Ensure initialization happens (note: not strictly thread-safe for concurrent first calls)
        // In a real implementation, would use Mutex from kotlinx.coroutines
        if (!isInitialized) {
            initialize()
            isInitialized = true
        }

        return try {
            process(inputs)
        } catch (e: Exception) {
            val recovered = onError(e, inputs)
            if (recovered) {
                emptyMap() // Return empty result on recovery
            } else {
                throw e // Propagate if not recovered
            }
        }
    }
}

/**
 * Lifecycle manager for manually managing UseCase lifecycles
 *
 * Use this to coordinate initialization and cleanup of multiple UseCases.
 *
 * @sample
 * ```kotlin
 * val manager = LifecycleManager()
 * val useCase1 = MyLifecycleUseCase()
 * val useCase2 = AnotherLifecycleUseCase()
 *
 * manager.register(useCase1)
 * manager.register(useCase2)
 *
 * manager.initializeAll()
 * try {
 *     // Use the use cases
 * } finally {
 *     manager.cleanupAll()
 * }
 * ```
 */
class LifecycleManager {
    private val useCases = mutableListOf<Lifecycle>()

    /**
     * Register a lifecycle-aware use case
     */
    fun register(useCase: Lifecycle) {
        useCases.add(useCase)
    }

    /**
     * Initialize all registered use cases in order
     *
     * If any initialization fails, previously initialized use cases are cleaned up.
     */
    suspend fun initializeAll() {
        val initialized = mutableListOf<Lifecycle>()

        try {
            useCases.forEach { useCase ->
                useCase.initialize()
                initialized.add(useCase)
            }
        } catch (e: Exception) {
            // Cleanup any that were initialized before the failure
            initialized.reversed().forEach { useCase ->
                try {
                    useCase.cleanup()
                } catch (cleanupError: Exception) {
                    // Log but don't propagate cleanup errors
                }
            }
            throw e
        }
    }

    /**
     * Clean up all registered use cases in reverse order
     *
     * Cleanup is attempted for all use cases even if some fail.
     */
    suspend fun cleanupAll() {
        val errors = mutableListOf<Exception>()

        useCases.reversed().forEach { useCase ->
            try {
                useCase.cleanup()
            } catch (e: Exception) {
                errors.add(e)
            }
        }

        // If any cleanup failed, throw the first error
        errors.firstOrNull()?.let { throw it }
    }

    /**
     * Clear all registered use cases
     */
    fun clear() {
        useCases.clear()
    }
}

/**
 * Lifecycle decorator that adds lifecycle management to any ProcessingLogic
 *
 * Wraps existing ProcessingLogic to provide initialization and cleanup hooks.
 *
 * @sample
 * ```kotlin
 * val baseLogic: ProcessingLogic = MySimpleUseCase()
 * val lifecycleLogic = LifecycleDecorator(
 *     delegate = baseLogic,
 *     onInit = { println("Initializing...") },
 *     onCleanup = { println("Cleaning up...") }
 * )
 * ```
 */
class LifecycleDecorator(
    private val delegate: ProcessingLogic,
    private val onInit: suspend () -> Unit = {},
    private val onCleanup: suspend () -> Unit = {},
    private val onError: suspend (Exception, Map<String, InformationPacket<*>>) -> Boolean = { _, _ -> false }
) : LifecycleAwareUseCase() {

    override suspend fun initialize() {
        onInit()
        if (delegate is Lifecycle) {
            delegate.initialize()
        }
    }

    override suspend fun cleanup() {
        if (delegate is Lifecycle) {
            delegate.cleanup()
        }
        onCleanup()
    }

    override suspend fun onError(error: Exception, inputs: Map<String, InformationPacket<*>>): Boolean {
        return if (delegate is Lifecycle) {
            delegate.onError(error, inputs)
        } else {
            onError(error, inputs)
        }
    }

    override suspend fun process(inputs: Map<String, InformationPacket<*>>): Map<String, InformationPacket<*>> {
        return delegate(inputs)
    }
}

/**
 * Scoped lifecycle that ensures cleanup happens automatically
 *
 * Similar to Kotlin's `use` function for Closeable resources.
 *
 * @sample
 * ```kotlin
 * withLifecycle(MyLifecycleUseCase()) { useCase ->
 *     val result = useCase(inputs)
 *     // Use the result
 * }
 * // Cleanup is automatic
 * ```
 */
suspend fun <T : Lifecycle, R> withLifecycle(useCase: T, block: suspend (T) -> R): R {
    useCase.initialize()
    return try {
        block(useCase)
    } finally {
        useCase.cleanup()
    }
}

/**
 * Example: Database-connected use case with lifecycle
 */
abstract class DatabaseUseCase : LifecycleAwareUseCase() {
    protected var connection: Any? = null // Would be actual DB connection type

    override suspend fun initialize() {
        // connection = Database.connect(...)
        println("Database connection opened")
    }

    override suspend fun cleanup() {
        // connection?.close()
        connection = null
        println("Database connection closed")
    }

    override suspend fun onError(error: Exception, inputs: Map<String, InformationPacket<*>>): Boolean {
        // Could implement retry logic or transaction rollback
        println("Database error: ${error.message}")
        return false
    }
}

/**
 * Example: Cached use case with lifecycle for cache initialization
 */
abstract class CachedUseCase<K : Any, V : Any> : LifecycleAwareUseCase() {
    protected val cache = mutableMapOf<K, V>()

    override suspend fun initialize() {
        // Could load cache from disk/redis
        println("Cache initialized")
    }

    override suspend fun cleanup() {
        // Could persist cache to disk
        cache.clear()
        println("Cache cleared")
    }
}

/**
 * Example: Buffered use case with automatic flushing
 */
abstract class BufferedUseCase<T : Any> : LifecycleAwareUseCase() {
    protected val buffer = mutableListOf<T>()
    protected val bufferSize: Int = 100

    override suspend fun cleanup() {
        flush()
    }

    protected open suspend fun flush() {
        if (buffer.isNotEmpty()) {
            // Process buffered items
            buffer.clear()
        }
    }
}
