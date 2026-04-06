/*
 * FlowGraphTypesCodeNode - Coarse-grained CodeNode wrapping IP type lifecycle
 * 3 inputs (filesystemPaths, classpathEntries, ipTypeCommands), 1 output (ipTypeMetadata)
 * License: Apache 2.0
 */

package io.codenode.flowgraphtypes.node

import io.codenode.fbpdsl.model.CodeNodeFactory
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.model.IPColor
import io.codenode.fbpdsl.model.InformationPacketType
import io.codenode.fbpdsl.runtime.CodeNodeDefinition
import io.codenode.fbpdsl.runtime.NodeRuntime
import io.codenode.fbpdsl.runtime.PortSpec
import io.codenode.flowgraphtypes.discovery.IPTypeDiscovery
import io.codenode.flowgraphtypes.generator.IPTypeFileGenerator
import io.codenode.flowgraphtypes.model.CustomIPTypeDefinition
import io.codenode.flowgraphtypes.registry.IPTypeRegistry
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Serializable command for mutating IP type registry state.
 */
@Serializable
private data class IPTypeCommand(
    val command: String,
    val typeName: String? = null,
    val typeId: String? = null,
    val colorRed: Int? = null,
    val colorGreen: Int? = null,
    val colorBlue: Int? = null
)

/**
 * Coarse-grained CodeNode that wraps the entire IP type lifecycle:
 * discovery, registry, repository, and file generation.
 *
 * Ports:
 * - Input 1: filesystemPaths — project root path for scanning IP type files
 * - Input 2: classpathEntries — classpath for KClass resolution
 * - Input 3: ipTypeCommands — JSON commands for register/unregister/generate/updateColor
 * - Output 1: ipTypeMetadata — serialized registry state (all registered types as JSON)
 *
 * Uses anyInput mode: re-emits updated metadata whenever ANY input changes.
 */
object FlowGraphTypesCodeNode : CodeNodeDefinition {
    override val name = "FlowGraphTypes"
    override val category = CodeNodeType.TRANSFORMER
    override val description = "IP type lifecycle: discovery, registry, and file generation"
    override val inputPorts = listOf(
        PortSpec("filesystemPaths", String::class),
        PortSpec("classpathEntries", String::class),
        PortSpec("ipTypeCommands", String::class)
    )
    override val outputPorts = listOf(
        PortSpec("ipTypeMetadata", String::class)
    )
    override val anyInput = true

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override fun createRuntime(name: String): NodeRuntime {
        val registry = IPTypeRegistry.withDefaults()

        return CodeNodeFactory.createIn3AnyOut1Processor<String, String, String, String>(
            name = name,
            initialValue1 = "",
            initialValue2 = "",
            initialValue3 = ""
        ) { filesystemPaths, classpathEntries, ipTypeCommands ->
            // Handle filesystem discovery when paths change
            if (filesystemPaths.isNotEmpty()) {
                try {
                    val projectRoot = File(filesystemPaths)
                    if (projectRoot.isDirectory) {
                        val discovery = IPTypeDiscovery(projectRoot)
                        val discovered = discovery.discoverAll()
                        registry.registerFromFilesystem(discovered) { meta ->
                            discovery.resolveKClass(meta)
                        }
                    }
                } catch (_: Exception) {
                    // Invalid path — skip discovery, keep current state
                }
            }

            // Handle mutation commands
            if (ipTypeCommands.isNotEmpty()) {
                try {
                    val cmd = json.decodeFromString<IPTypeCommand>(ipTypeCommands)
                    when (cmd.command) {
                        "register" -> {
                            val typeName = cmd.typeName ?: ""
                            val typeId = cmd.typeId ?: "ip_${typeName.lowercase()}"
                            if (typeName.isNotEmpty()) {
                                val definition = CustomIPTypeDefinition(
                                    id = typeId,
                                    typeName = typeName,
                                    color = if (cmd.colorRed != null && cmd.colorGreen != null && cmd.colorBlue != null) {
                                        IPColor(cmd.colorRed, cmd.colorGreen, cmd.colorBlue)
                                    } else {
                                        CustomIPTypeDefinition.nextColor(registry.customTypeCount())
                                    }
                                )
                                registry.registerCustomType(definition)
                            }
                        }
                        "unregister" -> {
                            val typeId = cmd.typeId
                            if (typeId != null) {
                                registry.unregister(typeId)
                            }
                        }
                        "updateColor" -> {
                            val typeId = cmd.typeId
                            if (typeId != null && cmd.colorRed != null && cmd.colorGreen != null && cmd.colorBlue != null) {
                                registry.updateColor(typeId, IPColor(cmd.colorRed, cmd.colorGreen, cmd.colorBlue))
                            }
                        }
                        "generate" -> {
                            val typeName = cmd.typeName ?: ""
                            if (typeName.isNotEmpty() && filesystemPaths.isNotEmpty()) {
                                val projectRoot = File(filesystemPaths)
                                val generator = IPTypeFileGenerator(projectRoot)
                                val typeId = cmd.typeId ?: "ip_${typeName.lowercase()}"
                                val definition = CustomIPTypeDefinition(
                                    id = typeId,
                                    typeName = typeName,
                                    color = if (cmd.colorRed != null && cmd.colorGreen != null && cmd.colorBlue != null) {
                                        IPColor(cmd.colorRed, cmd.colorGreen, cmd.colorBlue)
                                    } else {
                                        CustomIPTypeDefinition.nextColor(registry.customTypeCount())
                                    }
                                )
                                generator.generateIPTypeFile(
                                    definition,
                                    io.codenode.fbpdsl.model.PlacementLevel.UNIVERSAL
                                )
                                registry.registerCustomType(definition)
                            }
                        }
                    }
                } catch (_: Exception) {
                    // Malformed command — skip, keep current state
                }
            }

            // Serialize current registry state as output
            serializeRegistryState(registry)
        }
    }

    /**
     * Serializes the current registry state to a JSON string.
     */
    private fun serializeRegistryState(registry: IPTypeRegistry): String {
        val types = registry.getAllTypes()
        return buildString {
            append("{\"types\":[")
            types.forEachIndexed { index, type ->
                if (index > 0) append(",")
                append("{\"id\":\"${type.id}\",\"typeName\":\"${type.typeName}\"")
                append(",\"color\":{\"red\":${type.color.red},\"green\":${type.color.green},\"blue\":${type.color.blue}}")
                type.description?.let { append(",\"description\":\"$it\"") }
                append("}")
            }
            append("]}")
        }
    }
}
