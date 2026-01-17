/*
 * Go Code Generator
 * Generates Go code from FBP graphs
 * License: Apache 2.0
 */

package io.codenode.gocompiler.generator

import io.codenode.fbpdsl.model.CodeNode

class GoCodeGenerator {

    fun generateNodeComponent(node: CodeNode): String {
        return """
            |package generated
            |
            |// ${node.name} - ${node.description ?: "Generated component"}
            |type ${node.name.pascalCase()} struct {
            |    // Component implementation
            |}
        """.trimMargin()
    }
}

fun String.pascalCase(): String {
    return this.replaceFirstChar { it.uppercase() }
        .replace("_", "")
        .replace("-", "")
}

