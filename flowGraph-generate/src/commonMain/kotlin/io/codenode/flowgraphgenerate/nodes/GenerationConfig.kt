/*
 * GenerationConfig - Input configuration for generator CodeNode wrappers
 * License: Apache 2.0
 */

package io.codenode.flowgraphgenerate.nodes

import io.codenode.fbpdsl.model.FlowGraph

data class GenerationConfig(
    val flowGraph: FlowGraph,
    val basePackage: String,
    val flowPackage: String,
    val controllerPackage: String,
    val viewModelPackage: String,
    val userInterfacePackage: String,
    val moduleName: String
)
