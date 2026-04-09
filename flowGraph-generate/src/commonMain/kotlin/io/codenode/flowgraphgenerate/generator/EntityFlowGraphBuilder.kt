/*
 * EntityFlowGraphBuilder - Builds FlowGraph instances for entity CRUD modules
 * License: Apache 2.0
 */

package io.codenode.flowgraphgenerate.generator

import io.codenode.fbpdsl.dsl.flowGraph
import io.codenode.fbpdsl.dsl.withType
import io.codenode.fbpdsl.model.FlowGraph

/**
 * Builds a FlowGraph programmatically for an entity module, connecting three nodes:
 * {Entity}CUD (source) → {Entity}Repository (processor) → {Entity}sDisplay (sink).
 *
 * The generated FlowGraph follows the UserProfiles.flow.kt pattern with:
 * - CUD source node: 0 inputs, 3 outputs (save, update, remove)
 * - Repository processor: 3 inputs (save, update, remove), 2 outputs (result, error)
 * - Display sink: 2 inputs (result, error), 0 outputs
 * - 5 connections wiring them together
 */
class EntityFlowGraphBuilder {

    /**
     * Builds a FlowGraph for the given entity module specification.
     *
     * @param spec The entity module specification
     * @return A FlowGraph with 3 nodes and 5 connections
     */
    fun buildFlowGraph(spec: EntityModuleSpec): FlowGraph {
        val entityName = spec.entityName
        val pluralName = spec.pluralName
        val packageLower = pluralName.lowercase()

        return flowGraph(pluralName, version = "1.0.0") {
            targetPlatform(FlowGraph.TargetPlatform.KMP_ANDROID)
            targetPlatform(FlowGraph.TargetPlatform.KMP_IOS)

            val repository = codeNode("${entityName}Repository") {
                position(445.75, 398.0)
                input("save", Any::class)
                input("update", Any::class)
                input("remove", Any::class)
                output("result", Any::class)
                output("error", Any::class)
                config("_codeNodeClass", "io.codenode.$packageLower.nodes.${entityName}RepositoryCodeNode")
                config("_genericType", "in3anyout2")
                config("_repository", "true")
                config("_sourceIPTypeId", spec.sourceIPTypeId)
                config("_sourceIPTypeName", entityName)
                config("_ipTypesPackage", spec.ipTypesPackage)
            }

            val cud = codeNode("${entityName}CUD", nodeType = "SOURCE") {
                position(118.0, 394.25)
                output("save", Any::class)
                output("update", Any::class)
                output("remove", Any::class)
                config("_codeNodeClass", "io.codenode.$packageLower.nodes.${entityName}CUDCodeNode")
                config("_genericType", "in0out3")
                config("_cudSource", "true")
                config("_sourceIPTypeId", spec.sourceIPTypeId)
                config("_sourceIPTypeName", entityName)
                config("_ipTypesPackage", spec.ipTypesPackage)
            }

            val display = codeNode("${pluralName}Display", nodeType = "SINK") {
                position(799.5, 398.0)
                input("result", Any::class)
                input("error", Any::class)
                config("_codeNodeClass", "io.codenode.$packageLower.nodes.${pluralName}DisplayCodeNode")
                config("_genericType", "in2out0")
                config("_display", "true")
                config("_sourceIPTypeId", spec.sourceIPTypeId)
                config("_sourceIPTypeName", entityName)
                config("_ipTypesPackage", spec.ipTypesPackage)
            }

            val ipTypeId = "ip_${entityName.lowercase()}"
            cud.output("save") connect repository.input("save") withType ipTypeId
            cud.output("update") connect repository.input("update") withType ipTypeId
            cud.output("remove") connect repository.input("remove") withType ipTypeId
            repository.output("result") connect display.input("result")
            repository.output("error") connect display.input("error")
        }
    }
}
