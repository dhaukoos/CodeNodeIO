/*
 * DynamicPreviewDiscovery - Discovers and registers PreviewProviders from project modules
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import java.io.File

/**
 * Discovers PreviewProvider objects in project module userInterface directories
 * and invokes their register() method via reflection.
 *
 * PreviewProvider objects must:
 * - Be Kotlin `object` declarations (have an INSTANCE field)
 * - Have a `register()` method that calls PreviewRegistry.register(...)
 * - Be in the module's userInterface package
 * - Have a source file name ending in "PreviewProvider.kt"
 *
 * This works when module classes are on the classpath (e.g., via runGraphEditor).
 */
object DynamicPreviewDiscovery {

    /**
     * Scans a userInterface directory for PreviewProvider source files and
     * invokes their register() method via reflection.
     */
    fun discoverAndRegister(uiDir: File) {
        uiDir.listFiles { file -> file.name.endsWith("PreviewProvider.kt") }?.forEach { file ->
            try {
                val content = file.readText()
                val packageMatch = Regex("^package\\s+([\\w.]+)", RegexOption.MULTILINE).find(content)
                    ?: return@forEach
                val objectMatch = Regex("object\\s+(\\w+PreviewProvider)").find(content)
                    ?: return@forEach
                val fqcn = "${packageMatch.groupValues[1]}.${objectMatch.groupValues[1]}"

                val clazz = Class.forName(fqcn)
                val instance = clazz.getField("INSTANCE").get(null)
                val registerMethod = clazz.getMethod("register")
                registerMethod.invoke(instance)
            } catch (_: ClassNotFoundException) {
                // Module not compiled / not on classpath — expected
            } catch (e: Exception) {
                println("Warning: Failed to load PreviewProvider from ${file.name}: ${e.message}")
            }
        }
    }
}
