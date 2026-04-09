/*
 * SourceFileResolver - Resolves source file paths for CodeNodeDefinition objects
 * JVM-only utility for locating .kt source files from compiled class locations
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.runtime

import java.io.File

/**
 * Resolves the absolute source file path for a CodeNodeDefinition object.
 *
 * Uses the compiled class file location to navigate back to the Gradle module root,
 * then resolves the source path from the package structure. Works across composite
 * builds where the source may be in a different project root than the running application.
 *
 * @param clazz The Java class of the CodeNodeDefinition object
 * @param sourceSet The Gradle source set name ("jvmMain", "commonMain", etc.)
 * @return Absolute path to the .kt source file, or null if not found
 */
fun resolveSourceFilePath(clazz: Class<*>, sourceSet: String = "jvmMain"): String? {
    // Derive the expected source file path from the class's package and name
    val packagePath = clazz.`package`?.name?.replace('.', File.separatorChar) ?: return null
    val className = clazz.simpleName.removeSuffix("\$Companion")
    val sourceFileName = "$className.kt"

    // Strategy 1: Navigate from compiled class output back to source
    // Class location is typically: <module>/build/classes/kotlin/jvm/main/<package>/<Class>.class
    val codeSource = clazz.protectionDomain?.codeSource?.location ?: return null
    val classesDir = File(codeSource.toURI())

    // Walk up from classes dir to find the module root (contains build.gradle.kts)
    var moduleRoot = classesDir
    repeat(10) {
        if (File(moduleRoot, "build.gradle.kts").exists()) {
            val sourceFile = File(moduleRoot, "src/$sourceSet/kotlin/$packagePath/$sourceFileName")
            if (sourceFile.exists()) {
                return sourceFile.absolutePath
            }
            // Also check commonMain as fallback
            if (sourceSet != "commonMain") {
                val commonFile = File(moduleRoot, "src/commonMain/kotlin/$packagePath/$sourceFileName")
                if (commonFile.exists()) {
                    return commonFile.absolutePath
                }
            }
        }
        moduleRoot = moduleRoot.parentFile ?: return null
    }

    return null
}
