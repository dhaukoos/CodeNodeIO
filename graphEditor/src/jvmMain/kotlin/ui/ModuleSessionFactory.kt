/*
 * ModuleSessionFactory - Creates RuntimeSession instances for discovered modules
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import io.codenode.circuitsimulator.RuntimeSession
import io.codenode.fbpdsl.model.FlowGraph
import io.codenode.fbpdsl.runtime.DynamicPipelineBuilder
import io.codenode.fbpdsl.runtime.DynamicPipelineController
import io.codenode.flowgraphinspect.registry.NodeDefinitionRegistry
import java.io.File
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy

/**
 * Factory for creating RuntimeSession instances for any discovered module.
 *
 * Uses DynamicPipelineController for all modules. Creates module-specific
 * ViewModels via reflection — no compile-time dependencies on project modules.
 */
object ModuleSessionFactory {

    /** Registry for looking up compiled CodeNodeDefinitions by name */
    var registry: NodeDefinitionRegistry? = null

    /** Project root directory for resolving module State objects */
    var projectRoot: File? = null

    /**
     * Initializes the persistence layer early so DAOs are registered in Koin
     * before any pipeline execution needs them.
     *
     * Discovers PersistenceBootstrap.registerDaos() from the project's persistence
     * module via reflection. This registers all DAOs with proper type information
     * so module Persistence objects (KoinComponent) can inject them.
     */
    fun initializePersistence() {
        try {
            val bootstrapClass = Class.forName("io.codenode.persistence.PersistenceBootstrap")
            val instance = bootstrapClass.getField("INSTANCE").get(null)
            val registerMethod = bootstrapClass.getMethod("registerDaos")
            registerMethod.invoke(instance)
        } catch (_: ClassNotFoundException) {
            // Persistence module not on classpath — expected when running without project
        } catch (e: Exception) {
            println("Warning: Could not initialize persistence: ${e.message}")
        }
    }

    fun createSession(
        moduleName: String,
        editorFlowGraph: FlowGraph? = null,
        flowGraphProvider: (() -> FlowGraph)? = null
    ): RuntimeSession? {
        val reg = registry ?: return null
        if (editorFlowGraph == null || flowGraphProvider == null) return null

        val lookup: (String) -> io.codenode.fbpdsl.runtime.CodeNodeDefinition? = { name -> reg.getByName(name) }
        if (!DynamicPipelineBuilder.canBuildDynamic(editorFlowGraph, lookup)) return null

        val controller = DynamicPipelineController(
            flowGraphProvider = flowGraphProvider,
            lookup = lookup,
            onReset = createResetCallback(moduleName)
        )

        val viewModel = createViewModel(moduleName, controller, flowGraphProvider)
            ?: return RuntimeSession(controller, Any(), editorFlowGraph, flowGraphProvider = flowGraphProvider)

        return RuntimeSession(controller, viewModel, editorFlowGraph, flowGraphProvider = flowGraphProvider)
    }

    /**
     * Creates a module-specific ViewModel via reflection.
     * Discovers the ViewModel class and ControllerInterface, creates a dynamic proxy
     * for the interface, and instantiates the ViewModel.
     */
    private fun createViewModel(
        moduleName: String,
        controller: DynamicPipelineController,
        flowGraphProvider: () -> FlowGraph
    ): Any? {
        val modulePackage = "io.codenode.${moduleName.lowercase()}"

        // Find the ControllerInterface class
        val interfaceClass = tryLoadClass("${modulePackage}.generated.${moduleName}ControllerInterface")
            ?: return null

        // Create a dynamic proxy for the ControllerInterface
        val stateObject = tryGetStateObject(modulePackage, moduleName)
        val proxy = createControllerProxy(interfaceClass, controller, flowGraphProvider, stateObject)

        // Find the ViewModel class and instantiate it
        val viewModelClass = tryLoadClass("${modulePackage}.${moduleName}ViewModel")
            ?: return null

        return tryCreateViewModel(viewModelClass, interfaceClass, proxy)
    }

    /**
     * Creates a Java dynamic proxy implementing the ControllerInterface.
     * Delegates control methods to DynamicPipelineController, and state flow
     * getters to the module's State object via reflection.
     */
    private fun createControllerProxy(
        interfaceClass: Class<*>,
        controller: DynamicPipelineController,
        flowGraphProvider: () -> FlowGraph,
        stateObject: Any?
    ): Any {
        val handler = InvocationHandler { _, method, _ ->
            when (method.name) {
                "getExecutionState" -> controller.executionState
                "start" -> { controller.start(); flowGraphProvider() }
                "stop" -> { controller.stop(); flowGraphProvider() }
                "reset" -> { controller.reset(); flowGraphProvider() }
                "pause" -> { controller.pause(); flowGraphProvider() }
                "resume" -> { controller.resume(); flowGraphProvider() }
                else -> {
                    // Delegate state flow getters to the State object
                    if (stateObject != null && method.name.startsWith("get")) {
                        val propName = method.name.removePrefix("get")
                            .replaceFirstChar { it.lowercase() }
                        try {
                            // Try {propName}Flow field on State object
                            val flowField = stateObject.javaClass.getField("${propName}Flow")
                            flowField.get(stateObject)
                        } catch (_: NoSuchFieldException) {
                            try {
                                // Try get{PropName}Flow() method
                                val flowMethod = stateObject.javaClass.getMethod("get${propName.replaceFirstChar { it.uppercase() }}Flow")
                                flowMethod.invoke(stateObject)
                            } catch (_: Exception) {
                                null
                            }
                        }
                    } else null
                }
            }
        }

        return Proxy.newProxyInstance(
            interfaceClass.classLoader,
            arrayOf(interfaceClass),
            handler
        )
    }

    /**
     * Tries to create a ViewModel instance. Attempts constructors in order:
     * 1. (ControllerInterface) — for simple modules
     * 2. (ControllerInterface, Dao) — for entity modules (skipped if no Koin)
     */
    private fun tryCreateViewModel(
        viewModelClass: Class<*>,
        interfaceClass: Class<*>,
        proxy: Any
    ): Any? {
        // Try single-arg constructor (ControllerInterface)
        for (constructor in viewModelClass.constructors) {
            if (constructor.parameterCount == 1 && constructor.parameterTypes[0].isAssignableFrom(interfaceClass)) {
                return try {
                    constructor.newInstance(proxy)
                } catch (_: Exception) {
                    null
                }
            }
        }

        // Try two-arg constructor (ControllerInterface, Dao) — inject DAO from Koin if available
        for (constructor in viewModelClass.constructors) {
            if (constructor.parameterCount == 2 && constructor.parameterTypes[0].isAssignableFrom(interfaceClass)) {
                return try {
                    val daoClass = constructor.parameterTypes[1]
                    val dao = tryGetKoinInstance(daoClass)
                    if (dao != null) {
                        constructor.newInstance(proxy, dao)
                    } else null
                } catch (_: Exception) {
                    null
                }
            }
        }

        return null
    }

    private fun createResetCallback(moduleName: String): (() -> Unit)? {
        val stateObject = tryGetStateObject("io.codenode.${moduleName.lowercase()}", moduleName)
            ?: return null
        return try {
            val resetMethod = stateObject.javaClass.getMethod("reset")
            val callback: () -> Unit = { resetMethod.invoke(stateObject) }
            callback
        } catch (_: Exception) {
            null
        }
    }

    private fun tryGetStateObject(modulePackage: String, moduleName: String): Any? {
        val stateClass = tryLoadClass("${modulePackage}.${moduleName}State") ?: return null
        return try {
            stateClass.getField("INSTANCE").get(null)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Gets or creates the Room database instance via reflection from the persistence module.
     * Uses DatabaseModule.getDatabase() which is a singleton.
     */
    private fun getOrCreateDatabase(): Any? {
        databaseInstance?.let { return it }
        return try {
            val dbModuleClass = Class.forName("io.codenode.persistence.DatabaseModule")
            val instance = dbModuleClass.getField("INSTANCE").get(null)
            val getDbMethod = dbModuleClass.getMethod("getDatabase")
            val db = getDbMethod.invoke(instance)
            databaseInstance = db
            db
        } catch (_: Exception) {
            null
        }
    }


    private fun tryLoadClass(fqcn: String): Class<*>? {
        return try {
            Class.forName(fqcn)
        } catch (_: ClassNotFoundException) {
            null
        }
    }

    /**
     * Cached database instance (loaded once via reflection from persistence module).
     */
    private var databaseInstance: Any? = null

    private fun tryGetKoinInstance(clazz: Class<*>): Any? {
        // Resolve DAO instances via DatabaseModule reflection
        return try {
            val db = getOrCreateDatabase() ?: return null
            // Find a method on the database that returns the requested DAO type
            // e.g., userProfileDao() returns UserProfileDao
            val daoMethod = db.javaClass.methods.find { method ->
                method.parameterCount == 0 && clazz.isAssignableFrom(method.returnType)
            }
            daoMethod?.invoke(db)
        } catch (_: Exception) {
            null
        }
    }
}
