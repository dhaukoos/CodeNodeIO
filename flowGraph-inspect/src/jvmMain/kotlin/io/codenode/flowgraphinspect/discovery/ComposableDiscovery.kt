/*
 * ComposableDiscovery - Discovers composable files from a module's userInterface/ directory
 * License: Apache 2.0
 */

package io.codenode.flowgraphinspect.discovery

import java.io.File

/**
 * Discovers composable .kt file names from a module's userInterface/ directory.
 *
 * Walks `src/commonMain/kotlin/.../userInterface/` and returns
 * file names (without extension), sorted alphabetically.
 *
 * @param moduleDir The root directory of the KMP module
 * @return List of composable names (file names without .kt extension), sorted
 */
fun discoverComposables(moduleDir: File): List<String> {
    val srcDir = File(moduleDir, "src/commonMain/kotlin")
    if (!srcDir.exists()) return emptyList()

    return srcDir.walkTopDown()
        .filter { it.isFile && it.extension == "kt" }
        .filter { file ->
            file.parentFile?.name == "userInterface"
        }
        .map { it.nameWithoutExtension }
        .toList()
        .sorted()
}
