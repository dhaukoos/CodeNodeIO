/*
 * NodeDefinitionLookup - Abstraction for resolving node names to CodeNodeDefinitions
 * Decouples the dynamic pipeline builder from graphEditor's NodeDefinitionRegistry
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.runtime

/**
 * Function type for looking up a CodeNodeDefinition by node name.
 *
 * Used by DynamicPipelineBuilder to resolve node names from the FlowGraph
 * without depending on graphEditor's NodeDefinitionRegistry directly.
 *
 * Returns the CodeNodeDefinition if found, null otherwise.
 */
typealias NodeDefinitionLookup = (String) -> CodeNodeDefinition?
