/*
 * UIFBPSpec - Data model for UI-FBP interface generation
 * License: Apache 2.0
 */

package io.codenode.flowgraphgenerate.parser

/**
 * Typed input to the UI-FBP generator pipeline.
 *
 * Post-082/083, three potentially-distinct identifiers carry what used to be conflated as `moduleName`:
 *
 * | Identifier         | Source                                                                                  | Used for                                                                                              |
 * |--------------------|-----------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------|
 * | `flowGraphPrefix`  | The user-selected `.flow.kt` file's filename minus `.flow.kt` (PascalCase)              | Generated-file prefix; `PreviewRegistry` key (matches `RuntimePreviewPanel`'s lookup); class names    |
 * | `composableName`   | The `@Composable fun X(viewModel: ...)` function name in the qualifying UI source file  | The function the generated `PreviewProvider` invokes inside its `register { ... }` lambda             |
 * | `packageName`      | The host module's directory / Gradle project name → package path                        | On-disk path translation                                                                              |
 */
data class UIFBPSpec(
    val flowGraphPrefix: String,
    val composableName: String,
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
