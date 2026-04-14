/*
 * GenerationContext - Custom IP Type
 * @IPType
 * @TypeName GenerationContext
 * @TypeId ip_generationcontext
 * @Color rgb(3, 169, 244)
 * License: Apache 2.0
 */

package io.codenode.iptypes

data class GenerationContext(
    val flowGraphModel: String,
    val serializedOutput: String
)
