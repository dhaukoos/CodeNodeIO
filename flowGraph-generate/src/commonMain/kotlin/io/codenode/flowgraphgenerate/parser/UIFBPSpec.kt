/*
 * UIFBPSpec - Data model for UI-FBP interface generation
 * License: Apache 2.0
 */

package io.codenode.flowgraphgenerate.parser

data class UIFBPSpec(
    val moduleName: String,
    val viewModelTypeName: String,
    val packageName: String,
    val sourceOutputs: List<PortInfo>,
    val sinkInputs: List<PortInfo>,
    val ipTypeImports: List<String> = emptyList()
)

data class PortInfo(
    val name: String,
    val typeName: String,
    val isNullable: Boolean = false
)

data class UIFBPParseResult(
    val spec: UIFBPSpec? = null,
    val isSuccess: Boolean = spec != null,
    val errorMessage: String? = null
)
