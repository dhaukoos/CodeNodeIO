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

    /**
     * @param includeFlowKt When true, generates a bootstrap .flow.kt file with Source and Sink nodes
     */
    fun generateAll(spec: UIFBPSpec, includeFlowKt: Boolean = false): UIFBPGenerateResult {
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

            if (includeFlowKt) {
                files.add(UIFBPGeneratedFile(
                    relativePath = "src/commonMain/kotlin/$basePath/${spec.moduleName}.flow.kt",
                    content = generateBootstrapFlowKt(spec)
                ))
            }

            UIFBPGenerateResult(success = true, filesGenerated = files)
        } catch (e: Exception) {
            UIFBPGenerateResult(success = false, errorMessage = "Generation failed: ${e.message}")
        }
    }

    private fun generateBootstrapFlowKt(spec: UIFBPSpec): String {
        val sb = StringBuilder()
        val graphVarName = spec.moduleName.replaceFirstChar { it.lowercase() } + "FlowGraph"

        sb.appendLine("package ${spec.packageName}")
        sb.appendLine()
        sb.appendLine("import io.codenode.fbpdsl.dsl.*")
        sb.appendLine("import io.codenode.fbpdsl.model.*")
        sb.appendLine()
        sb.appendLine("val $graphVarName = flowGraph(\"${spec.moduleName}\", version = \"1.0.0\") {")

        if (spec.sourceOutputs.isNotEmpty()) {
            val sourceName = "${spec.moduleName}Source"
            sb.appendLine("    val source = codeNode(\"$sourceName\", nodeType = \"SOURCE\") {")
            sb.appendLine("        position(100.0, 300.0)")
            for (port in spec.sourceOutputs) {
                sb.appendLine("        output(\"${port.name}\", Any::class)")
            }
            sb.appendLine("    }")
        }

        if (spec.sinkInputs.isNotEmpty()) {
            sb.appendLine()
            val sinkName = "${spec.moduleName}Sink"
            sb.appendLine("    val sink = codeNode(\"$sinkName\", nodeType = \"SINK\") {")
            sb.appendLine("        position(600.0, 300.0)")
            for (port in spec.sinkInputs) {
                sb.appendLine("        input(\"${port.name}\", Any::class)")
            }
            sb.appendLine("    }")
        }

        sb.appendLine("}")

        return sb.toString()
    }
}
