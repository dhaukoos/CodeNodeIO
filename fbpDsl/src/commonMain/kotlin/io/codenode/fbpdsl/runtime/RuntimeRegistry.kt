/*
 * RuntimeRegistry - Tracks active NodeRuntime instances for a flow
 * Enables centralized pause/resume control through RootControlNode
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.runtime

/**
 * Registry that tracks active NodeRuntime instances for a flow.
 *
 * Enables centralized pause/resume control through RootControlNode.
 * Thread-safe using a synchronized map for concurrent access from coroutines.
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
    // Using synchronizedMap for KMP compatibility (no ConcurrentHashMap in common)
    private val runtimes = mutableMapOf<String, NodeRuntime<*>>()
    private val lock = Any()

    /**
     * Register a runtime when it starts.
     *
     * @param runtime The NodeRuntime to register
     */
    fun register(runtime: NodeRuntime<*>) {
        synchronized(lock) {
            runtimes[runtime.codeNode.id] = runtime
        }
    }

    /**
     * Unregister a runtime when it stops.
     *
     * @param runtime The NodeRuntime to unregister
     */
    fun unregister(runtime: NodeRuntime<*>) {
        synchronized(lock) {
            runtimes.remove(runtime.codeNode.id)
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
        val runtimesCopy = synchronized(lock) { runtimes.values.toList() }
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
        val runtimesCopy = synchronized(lock) { runtimes.values.toList() }
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
        val runtimesCopy = synchronized(lock) { runtimes.values.toList() }
        runtimesCopy.forEach { it.stop() }
        synchronized(lock) {
            runtimes.clear()
        }
    }

    /**
     * Number of registered runtimes.
     */
    val count: Int
        get() = synchronized(lock) { runtimes.size }

    /**
     * Check if a runtime is registered by its codeNode ID.
     *
     * @param nodeId The codeNode.id to check
     * @return true if a runtime with this ID is registered
     */
    fun isRegistered(nodeId: String): Boolean {
        return synchronized(lock) { runtimes.containsKey(nodeId) }
    }

    /**
     * Get a registered runtime by its codeNode ID.
     *
     * @param nodeId The codeNode.id to look up
     * @return The NodeRuntime if found, null otherwise
     */
    fun get(nodeId: String): NodeRuntime<*>? {
        return synchronized(lock) { runtimes[nodeId] }
    }

    /**
     * Clear all registrations without stopping runtimes.
     *
     * Use stopAll() for graceful shutdown. This method is for cleanup
     * when runtimes have already been stopped externally.
     */
    fun clear() {
        synchronized(lock) {
            runtimes.clear()
        }
    }
}
