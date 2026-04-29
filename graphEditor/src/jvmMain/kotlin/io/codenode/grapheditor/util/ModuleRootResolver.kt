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

/**
 * Derives the base Kotlin package of a workspace module from its on-disk source tree.
 *
 * Strategy (in order):
 *   1. Scan `src/commonMain/kotlin` for a directory containing `flow/{flowGraphPrefix}.flow.kt`.
 *      The package is the path from `kotlin/` to the parent of `flow/` with `/` → `.`.
 *   2. Scan for any `flow/` directory; use its parent's package.
 *   3. Return null — callers fall back to the legacy `io.codenode.{moduleName.lowercase()}` derivation.
 *
 * Handles the post-082/083 reality where the workspace module's directory name (e.g.,
 * `TestModule`) is independent of its package (e.g., `io.codenode.demo`) and the
 * flow-graph prefix (e.g., `DemoUI`).
 */
fun detectModuleBasePackage(moduleDir: File, flowGraphPrefix: String): String? {
    val srcDir = File(moduleDir, "src/commonMain/kotlin")
    if (!srcDir.isDirectory) return null

    fun pathToPackage(dir: File): String? {
        val rel = dir.absoluteFile.relativeTo(srcDir.absoluteFile).path
        if (rel.isEmpty() || rel == ".") return null
        return rel.replace(File.separatorChar, '.')
    }

    val flowKtName = "$flowGraphPrefix.flow.kt"
    val matchingFlowDir = srcDir.walkTopDown()
        .filter { it.isDirectory && it.name == "flow" }
        .firstOrNull { File(it, flowKtName).isFile }
    if (matchingFlowDir != null) {
        return pathToPackage(matchingFlowDir.parentFile)
    }

    val anyFlowDir = srcDir.walkTopDown()
        .filter { it.isDirectory && it.name == "flow" }
        .firstOrNull()
    if (anyFlowDir != null) {
        return pathToPackage(anyFlowDir.parentFile)
    }

    return null
}
