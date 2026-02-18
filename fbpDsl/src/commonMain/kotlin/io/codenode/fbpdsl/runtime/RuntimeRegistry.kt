/*
 * RuntimeRegistry - Tracks active NodeRuntime instances for a flow
 * Enables centralized pause/resume control through RootControlNode
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.runtime

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.runBlocking

/**
 * Registry that tracks active NodeRuntime instances for a flow.
 *
 * Enables centralized pause/resume control through RootControlNode.
 * Thread-safe using a Mutex for concurrent access from coroutines.
 *
 * Usage:
 * ```kotlin
 * val registry = RuntimeRegistry()
 *
 * // Runtimes register themselves on start
 * registry.register(generatorRuntime)
 * registry.register(sinkRuntime)
 *
 * // Controller can pause all runtimes
 * registry.pauseAll()
 *
 * // And resume them
 * registry.resumeAll()
 *
 * // On stop, runtimes unregister
 * registry.unregister(generatorRuntime)
 * ```
 */
class RuntimeRegistry {

    // Thread-safe map for concurrent access from coroutines
    private val runtimes = mutableMapOf<String, NodeRuntime<*>>()
    private val mutex = Mutex()

    /**
     * Register a runtime when it starts.
     *
     * @param runtime The NodeRuntime to register
     */
    fun register(runtime: NodeRuntime<*>) {
        runBlocking {
            mutex.withLock {
                runtimes[runtime.codeNode.id] = runtime
            }
        }
    }

    /**
     * Unregister a runtime when it stops.
     *
     * @param runtime The NodeRuntime to unregister
     */
    fun unregister(runtime: NodeRuntime<*>) {
        runBlocking {
            mutex.withLock {
                runtimes.remove(runtime.codeNode.id)
            }
        }
    }

    /**
     * Pause all registered runtimes.
     *
     * Calls pause() on each registered runtime.
     * Runtimes with independentControl=true on their codeNode
     * will be skipped (they manage their own state).
     */
    fun pauseAll() {
        val runtimesCopy = runBlocking {
            mutex.withLock { runtimes.values.toList() }
        }
        runtimesCopy.forEach { runtime ->
            // Respect independentControl flag
            if (!runtime.codeNode.controlConfig.independentControl) {
                runtime.pause()
            }
        }
    }

    /**
     * Resume all registered runtimes.
     *
     * Calls resume() on each registered runtime.
     * Runtimes with independentControl=true on their codeNode
     * will be skipped (they manage their own state).
     */
    fun resumeAll() {
        val runtimesCopy = runBlocking {
            mutex.withLock { runtimes.values.toList() }
        }
        runtimesCopy.forEach { runtime ->
            // Respect independentControl flag
            if (!runtime.codeNode.controlConfig.independentControl) {
                runtime.resume()
            }
        }
    }

    /**
     * Stop all registered runtimes and clear registry.
     *
     * Calls stop() on each registered runtime, then clears all registrations.
     */
    fun stopAll() {
        val runtimesCopy = runBlocking {
            mutex.withLock { runtimes.values.toList() }
        }
        runtimesCopy.forEach { it.stop() }
        runBlocking {
            mutex.withLock {
                runtimes.clear()
            }
        }
    }

    /**
     * Number of registered runtimes.
     */
    val count: Int
        get() = runBlocking {
            mutex.withLock { runtimes.size }
        }

    /**
     * Check if a runtime is registered by its codeNode ID.
     *
     * @param nodeId The codeNode.id to check
     * @return true if a runtime with this ID is registered
     */
    fun isRegistered(nodeId: String): Boolean {
        return runBlocking {
            mutex.withLock { runtimes.containsKey(nodeId) }
        }
    }

    /**
     * Get a registered runtime by its codeNode ID.
     *
     * @param nodeId The codeNode.id to look up
     * @return The NodeRuntime if found, null otherwise
     */
    fun get(nodeId: String): NodeRuntime<*>? {
        return runBlocking {
            mutex.withLock { runtimes[nodeId] }
        }
    }

    /**
     * Clear all registrations without stopping runtimes.
     *
     * Use stopAll() for graceful shutdown. This method is for cleanup
     * when runtimes have already been stopped externally.
     */
    fun clear() {
        runBlocking {
            mutex.withLock {
                runtimes.clear()
            }
        }
    }
}
