/*
 * UIFBPInterfaceGenerator - Orchestrates generation of all UI-FBP interface files
 * License: Apache 2.0
 */

package io.codenode.flowgraphgenerate.generator

import io.codenode.flowgraphgenerate.parser.UIFBPSpec

data class UIFBPGenerateResult(
    val success: Boolean,
    val filesGenerated: List<UIFBPGeneratedFile> = emptyList(),
    val errorMessage: String? = null
)

data class UIFBPGeneratedFile(
    val relativePath: String,
    val content: String
)

class UIFBPInterfaceGenerator {

    private val stateGenerator = UIFBPStateGenerator()
    private val viewModelGenerator = UIFBPViewModelGenerator()
    private val sourceGenerator = UIFBPSourceCodeNodeGenerator()
    private val sinkGenerator = UIFBPSinkCodeNodeGenerator()

    fun generateAll(spec: UIFBPSpec): UIFBPGenerateResult {
        return try {
            val files = mutableListOf<UIFBPGeneratedFile>()
            val basePath = spec.packageName.replace(".", "/")

            files.add(UIFBPGeneratedFile(
                relativePath = "src/commonMain/kotlin/$basePath/${spec.moduleName}State.kt",
                content = stateGenerator.generate(spec)
            ))

            files.add(UIFBPGeneratedFile(
                relativePath = "src/commonMain/kotlin/$basePath/${spec.viewModelTypeName}.kt",
                content = viewModelGenerator.generate(spec)
            ))

            val sourceContent = sourceGenerator.generate(spec)
            if (sourceContent != null) {
                files.add(UIFBPGeneratedFile(
                    relativePath = "src/commonMain/kotlin/$basePath/nodes/${spec.moduleName}SourceCodeNode.kt",
                    content = sourceContent
                ))
            }

            val sinkContent = sinkGenerator.generate(spec)
            if (sinkContent != null) {
                files.add(UIFBPGeneratedFile(
                    relativePath = "src/commonMain/kotlin/$basePath/nodes/${spec.moduleName}SinkCodeNode.kt",
                    content = sinkContent
                ))
            }

            UIFBPGenerateResult(success = true, filesGenerated = files)
        } catch (e: Exception) {
            UIFBPGenerateResult(success = false, errorMessage = "Generation failed: ${e.message}")
        }
    }
}
