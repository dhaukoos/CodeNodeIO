/*
 * Kotlin Code Generator
 * Generates KMP code from FBP graphs
 * License: Apache 2.0
 */

package io.codenode.kotlincompiler.generator

import com.squareup.kotlinpoet.*
import io.codenode.fbpdsl.model.CodeNode

class KotlinCodeGenerator {

    fun generateNodeComponent(node: CodeNode): FileSpec {
        val className = ClassName("io.codenode.generated", node.name.pascalCase())

        val componentClass = TypeSpec.classBuilder(className)
            .addKdoc("Generated component for node: ${node.name}\n")
            .addKdoc("Type: ${node.nodeType}\n")
            .build()

        return FileSpec.builder(className.packageName, className.simpleName)
            .addType(componentClass)
            .build()
    }
}

fun String.pascalCase(): String {
    return this.replaceFirstChar { it.uppercase() }
        .replace("_", "")
        .replace("-", "")
}

