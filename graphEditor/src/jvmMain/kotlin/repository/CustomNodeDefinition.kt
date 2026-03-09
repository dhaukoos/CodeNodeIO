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
    val createdAt: Long,
    val anyInput: Boolean = false,
    val isRepository: Boolean = false,
    val isCudSource: Boolean = false,
    val isDisplay: Boolean = false,
    val sourceIPTypeId: String? = null,
    val sourceIPTypeName: String? = null
) {
    companion object {
        /**
         * Factory method to create a new CustomNodeDefinition with auto-generated ID and timestamp.
         *
         * @param name User-provided display name
         * @param inputCount Number of input ports (0-3)
         * @param outputCount Number of output ports (0-3)
         * @param anyInput Whether the node uses any-input trigger mode
         * @return New CustomNodeDefinition instance
         */
        fun create(name: String, inputCount: Int, outputCount: Int, anyInput: Boolean = false): CustomNodeDefinition {
            val anyPrefix = if (anyInput) "any" else ""
            return CustomNodeDefinition(
                id = "custom_node_${UUID.randomUUID()}",
                name = name,
                inputCount = inputCount,
                outputCount = outputCount,
                genericType = "in${inputCount}${anyPrefix}out${outputCount}",
                anyInput = anyInput,
                createdAt = System.currentTimeMillis()
            )
        }

        /**
         * Factory method to create a repository node definition from a custom IP type.
         *
         * @param ipTypeName Name of the source IP type (e.g., "User")
         * @param sourceIPTypeId ID of the source custom IP type
         * @return CustomNodeDefinition configured as a repository node with 3 inputs and 2 outputs
         */
        fun createRepository(ipTypeName: String, sourceIPTypeId: String): CustomNodeDefinition {
            return CustomNodeDefinition(
                id = "custom_node_${UUID.randomUUID()}",
                name = "${ipTypeName}Repository",
                inputCount = 3,
                outputCount = 2,
                genericType = "in3out2",
                createdAt = System.currentTimeMillis(),
                isRepository = true,
                sourceIPTypeId = sourceIPTypeId,
                sourceIPTypeName = ipTypeName
            )
        }

        /**
         * Factory method to create a CUD (Create/Update/Delete) source node definition.
         * Source node with 0 inputs and 3 outputs (save, update, remove).
         *
         * @param ipTypeName Name of the source IP type (e.g., "GeoLocation")
         * @param sourceIPTypeId ID of the source custom IP type
         * @return CustomNodeDefinition configured as a CUD source node
         */
        fun createCUD(ipTypeName: String, sourceIPTypeId: String): CustomNodeDefinition {
            return CustomNodeDefinition(
                id = "custom_node_${UUID.randomUUID()}",
                name = "${ipTypeName}CUD",
                inputCount = 0,
                outputCount = 3,
                genericType = "sourceout3",
                createdAt = System.currentTimeMillis(),
                isCudSource = true,
                sourceIPTypeId = sourceIPTypeId,
                sourceIPTypeName = ipTypeName
            )
        }

        /**
         * Factory method to create a Display sink node definition.
         * Sink node with 2 inputs (entities, error) and 0 outputs.
         *
         * @param ipTypeName Name of the source IP type (e.g., "GeoLocation")
         * @param sourceIPTypeId ID of the source custom IP type
         * @return CustomNodeDefinition configured as a Display sink node
         */
        fun createDisplay(ipTypeName: String, sourceIPTypeId: String): CustomNodeDefinition {
            return CustomNodeDefinition(
                id = "custom_node_${UUID.randomUUID()}",
                name = "${ipTypeName}sDisplay",
                inputCount = 2,
                outputCount = 0,
                genericType = "in2sink",
                createdAt = System.currentTimeMillis(),
                isDisplay = true,
                sourceIPTypeId = sourceIPTypeId,
                sourceIPTypeName = ipTypeName
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
        if (isRepository && sourceIPTypeName != null) {
            return createGenericNodeType(
                numInputs = inputCount,
                numOutputs = outputCount,
                customName = name,
                customDescription = "${sourceIPTypeName} repository with save/update/remove operations",
                inputNames = listOf("save", "update", "remove"),
                outputNames = listOf("result", "error")
            )
                .addConfigurationDefault("_repository", "true")
                .addConfigurationDefault("_sourceIPTypeId", sourceIPTypeId!!)
                .addConfigurationDefault("_sourceIPTypeName", sourceIPTypeName)
        }
        if (isCudSource && sourceIPTypeName != null) {
            return createGenericNodeType(
                numInputs = inputCount,
                numOutputs = outputCount,
                customName = name,
                customDescription = "${sourceIPTypeName} CUD source with save/update/remove outputs",
                outputNames = listOf("save", "update", "remove")
            )
                .addConfigurationDefault("_cudSource", "true")
                .addConfigurationDefault("_sourceIPTypeId", sourceIPTypeId!!)
                .addConfigurationDefault("_sourceIPTypeName", sourceIPTypeName)
        }
        if (isDisplay && sourceIPTypeName != null) {
            return createGenericNodeType(
                numInputs = inputCount,
                numOutputs = outputCount,
                customName = name,
                customDescription = "${sourceIPTypeName} display sink with entities and error inputs",
                inputNames = listOf("entities", "error")
            )
                .addConfigurationDefault("_display", "true")
                .addConfigurationDefault("_sourceIPTypeId", sourceIPTypeId!!)
                .addConfigurationDefault("_sourceIPTypeName", sourceIPTypeName)
        }
        return createGenericNodeType(
            numInputs = inputCount,
            numOutputs = outputCount,
            customName = name,
            anyInput = anyInput
        )
    }
}
