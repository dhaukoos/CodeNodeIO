/*
 * ModuleRootResolver - Module directory resolution utilities
 * License: Apache 2.0
 */

package io.codenode.grapheditor.util

import java.io.File

/**
 * Resolves the .flow.kt file from a module directory.
 *
 * Looks at: {moduleDir}/src/commonMain/kotlin/io/codenode/{modulename}/{ModuleName}.flow.kt
 * The module name is derived from the directory name (lowercased for the package path,
 * original case for the filename).
 */
fun resolveFlowKtFromModule(moduleDir: File): File? {
    val moduleName = moduleDir.name
    val packageName = moduleName.lowercase()
    val flowFile = moduleDir.resolve("src/commonMain/kotlin/io/codenode/$packageName/$moduleName.flow.kt")
    return if (flowFile.exists()) flowFile else null
}

/**
 * Walks up from a starting directory to find the module root (directory containing build.gradle.kts).
 * Stops after 10 levels to avoid traversing too far up.
 *
 * @param startDir The directory to start searching from
 * @return The module root directory, or null if not found
 */
fun findModuleRoot(startDir: File?): File? {
    var dir = startDir
    var depth = 0
    while (dir != null && depth < 10) {
        if (File(dir, "build.gradle.kts").exists()) return dir
        dir = dir.parentFile
        depth++
    }
    return null
}
