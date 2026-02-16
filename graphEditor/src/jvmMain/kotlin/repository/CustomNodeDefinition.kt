/*
 * CustomNodeDefinition - Serializable representation of user-created custom node types
 * License: Apache 2.0
 */

package io.codenode.grapheditor.repository

import io.codenode.fbpdsl.factory.createGenericNodeType
import io.codenode.fbpdsl.model.NodeTypeDefinition
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Serializable representation of a user-created custom node type.
 * Used for persistence in CustomNodeRepository.
 *
 * @param id Unique identifier (UUID format)
 * @param name User-provided display name
 * @param inputCount Number of input ports (0-3)
 * @param outputCount Number of output ports (0-3)
 * @param genericType Type string (e.g., "in2out1")
 * @param createdAt Creation timestamp (epoch milliseconds)
 */
@Serializable
data class CustomNodeDefinition(
    val id: String,
    val name: String,
    val inputCount: Int,
    val outputCount: Int,
    val genericType: String,
    val createdAt: Long
) {
    companion object {
        /**
         * Factory method to create a new CustomNodeDefinition with auto-generated ID and timestamp.
         *
         * @param name User-provided display name
         * @param inputCount Number of input ports (0-3)
         * @param outputCount Number of output ports (0-3)
         * @return New CustomNodeDefinition instance
         */
        fun create(name: String, inputCount: Int, outputCount: Int): CustomNodeDefinition {
            return CustomNodeDefinition(
                id = "custom_node_${UUID.randomUUID()}",
                name = name,
                inputCount = inputCount,
                outputCount = outputCount,
                genericType = "in${inputCount}out${outputCount}",
                createdAt = System.currentTimeMillis()
            )
        }
    }

    /**
     * Converts this CustomNodeDefinition to a NodeTypeDefinition that can be used
     * in the Node Palette and for creating node instances.
     *
     * @return NodeTypeDefinition for use in the graph editor
     */
    fun toNodeTypeDefinition(): NodeTypeDefinition {
        return createGenericNodeType(
            numInputs = inputCount,
            numOutputs = outputCount,
            customName = name
        )
    }
}
