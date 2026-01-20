/*
 * PortValidator - Port Type Compatibility Validation
 * Validates port connections and type compatibility for Flow-Based Programming graphs
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.validation

import io.codenode.fbpdsl.model.Port
import io.codenode.fbpdsl.model.Node
import io.codenode.fbpdsl.model.Connection
import io.codenode.fbpdsl.model.ValidationResult
import kotlin.reflect.KClass

/**
 * Validator for port type compatibility and connection validation
 * Provides comprehensive checks for port connections in FBP graphs
 */
object PortValidator {

    /**
     * Validates whether two ports can be connected
     *
     * @param sourcePort The output port (source of data)
     * @param targetPort The input port (destination of data)
     * @return PortCompatibilityResult with compatibility status and detailed messages
     */
    fun validatePortConnection(
        sourcePort: Port<*>,
        targetPort: Port<*>
    ): PortCompatibilityResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        // Check 1: Port directions must be OUTPUT → INPUT
        if (sourcePort.direction != Port.Direction.OUTPUT) {
            errors.add("Source port '${sourcePort.name}' must be an OUTPUT port, got ${sourcePort.direction}")
        }
        if (targetPort.direction != Port.Direction.INPUT) {
            errors.add("Target port '${targetPort.name}' must be an INPUT port, got ${targetPort.direction}")
        }

        // Check 2: Ports must belong to different nodes
        if (sourcePort.owningNodeId == targetPort.owningNodeId) {
            errors.add("Cannot connect ports from the same node (self-connection not allowed)")
        }

        // Check 3: Data type compatibility
        val typeCompatibility = checkTypeCompatibility(sourcePort.dataType, targetPort.dataType)
        if (!typeCompatibility.isCompatible) {
            errors.add("Type mismatch: ${sourcePort.typeName} → ${targetPort.typeName}")
            errors.addAll(typeCompatibility.errors)
        } else if (typeCompatibility.warnings.isNotEmpty()) {
            warnings.addAll(typeCompatibility.warnings)
        }

        // Check 4: Required port validation
        if (targetPort.required) {
            warnings.add("Target port '${targetPort.name}' is required - ensure it receives data")
        }

        return PortCompatibilityResult(
            isCompatible = errors.isEmpty(),
            errors = errors,
            warnings = warnings,
            typeCompatibility = typeCompatibility
        )
    }

    /**
     * Validates a complete connection between two nodes
     *
     * @param connection The connection to validate
     * @param sourceNode The source node
     * @param targetNode The target node
     * @return PortCompatibilityResult with validation details
     */
    fun validateConnection(
        connection: Connection,
        sourceNode: Node,
        targetNode: Node
    ): PortCompatibilityResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        // Find the ports
        val sourcePort = sourceNode.outputPorts.find { it.id == connection.sourcePortId }
        val targetPort = targetNode.inputPorts.find { it.id == connection.targetPortId }

        if (sourcePort == null) {
            errors.add("Source port '${connection.sourcePortId}' not found on node '${sourceNode.name}'")
        }
        if (targetPort == null) {
            errors.add("Target port '${connection.targetPortId}' not found on node '${targetNode.name}'")
        }

        if (sourcePort != null && targetPort != null) {
            val portResult = validatePortConnection(sourcePort, targetPort)
            return portResult.copy(
                errors = errors + portResult.errors,
                warnings = warnings + portResult.warnings
            )
        }

        return PortCompatibilityResult(
            isCompatible = false,
            errors = errors,
            warnings = warnings,
            typeCompatibility = TypeCompatibilityResult(
                isCompatible = false,
                errors = errors,
                warnings = emptyList()
            )
        )
    }

    /**
     * Checks type compatibility between source and target types
     *
     * @param sourceType The type from the OUTPUT port
     * @param targetType The type from the INPUT port
     * @return TypeCompatibilityResult with detailed compatibility information
     */
    fun checkTypeCompatibility(
        sourceType: KClass<*>,
        targetType: KClass<*>
    ): TypeCompatibilityResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        // Rule 1: Exact type match (always compatible)
        if (sourceType == targetType) {
            return TypeCompatibilityResult(
                isCompatible = true,
                errors = emptyList(),
                warnings = emptyList()
            )
        }

        // Rule 2: Any type (universal compatibility)
        if (targetType == Any::class) {
            warnings.add("Target accepts Any type - runtime type checking recommended")
            return TypeCompatibilityResult(
                isCompatible = true,
                errors = emptyList(),
                warnings = warnings
            )
        }
        if (sourceType == Any::class) {
            warnings.add("Source provides Any type - runtime type validation recommended")
            return TypeCompatibilityResult(
                isCompatible = true,
                errors = emptyList(),
                warnings = warnings
            )
        }

        // Rule 3: Nullable types
        if (sourceType.simpleName?.endsWith("?") == true ||
            targetType.simpleName?.endsWith("?") == true) {
            warnings.add("Nullable types detected - ensure null safety")
        }

        // Rule 4: Collection types compatibility
        val sourceTypeName = sourceType.simpleName ?: sourceType.toString()
        val targetTypeName = targetType.simpleName ?: targetType.toString()

        if (isCollectionType(sourceType) && isCollectionType(targetType)) {
            warnings.add("Collection type compatibility: verify element types match")
            return TypeCompatibilityResult(
                isCompatible = true,
                errors = emptyList(),
                warnings = warnings
            )
        }

        // Rule 5: String compatibility (common serialization format)
        if (sourceType == String::class || targetType == String::class) {
            warnings.add("String conversion may be required")
            return TypeCompatibilityResult(
                isCompatible = true,
                errors = emptyList(),
                warnings = warnings
            )
        }

        // Rule 6: Number type compatibility
        if (isNumericType(sourceType) && isNumericType(targetType)) {
            val narrowing = isNarrowingConversion(sourceType, targetType)
            if (narrowing) {
                warnings.add("Narrowing conversion from ${sourceTypeName} to ${targetTypeName} - precision loss possible")
            }
            return TypeCompatibilityResult(
                isCompatible = true,
                errors = emptyList(),
                warnings = warnings
            )
        }

        // Rule 7: Subtype compatibility (requires reflection)
        try {
            // Check if sourceType is assignable to targetType
            // Note: This may not work fully in Kotlin/Common due to reflection limitations
            if (targetType.isInstance(sourceType)) {
                return TypeCompatibilityResult(
                    isCompatible = true,
                    errors = emptyList(),
                    warnings = listOf("Subtype relationship detected")
                )
            }
        } catch (e: Exception) {
            // Reflection not available or failed
            warnings.add("Unable to verify subtype relationship - platform limitations")
        }

        // Rule 8: Default - types are incompatible
        errors.add("Incompatible types: ${sourceTypeName} cannot be assigned to ${targetTypeName}")
        return TypeCompatibilityResult(
            isCompatible = false,
            errors = errors,
            warnings = warnings
        )
    }

    /**
     * Checks if a type is a collection type
     */
    private fun isCollectionType(type: KClass<*>): Boolean {
        val typeName = type.simpleName ?: type.toString()
        return typeName.startsWith("List") ||
               typeName.startsWith("Set") ||
               typeName.startsWith("Collection") ||
               typeName.startsWith("Array") ||
               typeName.startsWith("Iterable") ||
               typeName.startsWith("Sequence")
    }

    /**
     * Checks if a type is a numeric type
     */
    private fun isNumericType(type: KClass<*>): Boolean {
        return type == Int::class ||
               type == Long::class ||
               type == Short::class ||
               type == Byte::class ||
               type == Float::class ||
               type == Double::class ||
               type == Number::class
    }

    /**
     * Checks if a numeric conversion is narrowing (may lose precision)
     */
    private fun isNarrowingConversion(sourceType: KClass<*>, targetType: KClass<*>): Boolean {
        // Conversion matrix for narrowing
        return when (sourceType) {
            Double::class -> targetType in listOf(Float::class, Long::class, Int::class, Short::class, Byte::class)
            Float::class -> targetType in listOf(Long::class, Int::class, Short::class, Byte::class)
            Long::class -> targetType in listOf(Int::class, Short::class, Byte::class)
            Int::class -> targetType in listOf(Short::class, Byte::class)
            Short::class -> targetType == Byte::class
            else -> false
        }
    }

    /**
     * Validates that all required input ports on a node have connections
     *
     * @param node The node to validate
     * @param connections List of all connections in the graph
     * @return ValidationResult with missing required port information
     */
    fun validateRequiredPorts(
        node: Node,
        connections: List<Connection>
    ): ValidationResult {
        val errors = mutableListOf<String>()

        // Find all required input ports
        val requiredInputPorts = node.inputPorts.filter { it.required }

        // Check which ones have connections
        val connectedPortIds = connections
            .filter { it.targetNodeId == node.id }
            .map { it.targetPortId }
            .toSet()

        // Find unconnected required ports
        val unconnectedRequired = requiredInputPorts.filter { it.id !in connectedPortIds }

        if (unconnectedRequired.isNotEmpty()) {
            errors.add(
                "Node '${node.name}' has unconnected required input ports: " +
                unconnectedRequired.joinToString(", ") { it.name }
            )
        }

        return ValidationResult(
            success = errors.isEmpty(),
            errors = errors
        )
    }

    /**
     * Validates that a port doesn't have too many connections (cardinality check)
     *
     * @param node The node containing the port
     * @param portId The port ID to check
     * @param connections List of all connections in the graph
     * @param maxConnections Maximum allowed connections (default: 1 for input, unlimited for output)
     * @return ValidationResult with cardinality violation information
     */
    fun validatePortCardinality(
        node: Node,
        portId: String,
        connections: List<Connection>,
        maxConnections: Int? = null
    ): ValidationResult {
        val errors = mutableListOf<String>()

        // Find the port
        val port = node.getAllPorts().find { it.id == portId }
        if (port == null) {
            errors.add("Port '$portId' not found on node '${node.name}'")
            return ValidationResult(success = false, errors = errors)
        }

        // Count connections to this port
        val connectionCount = connections.count { conn ->
            when (port.direction) {
                Port.Direction.INPUT -> conn.targetNodeId == node.id && conn.targetPortId == portId
                Port.Direction.OUTPUT -> conn.sourceNodeId == node.id && conn.sourcePortId == portId
            }
        }

        // Determine max connections
        val max = maxConnections ?: when (port.direction) {
            Port.Direction.INPUT -> 1  // Input ports typically accept one connection
            Port.Direction.OUTPUT -> Int.MAX_VALUE  // Output ports can have unlimited connections
        }

        if (connectionCount > max) {
            errors.add(
                "Port '${port.name}' on node '${node.name}' has too many connections: " +
                "$connectionCount (max: $max)"
            )
        }

        return ValidationResult(
            success = errors.isEmpty(),
            errors = errors
        )
    }

    /**
     * Validates all connections for a node
     *
     * @param node The node to validate
     * @param connections List of all connections in the graph
     * @param allNodes Map of node ID to Node for looking up connected nodes
     * @return ValidationResult with all validation errors
     */
    fun validateNodeConnections(
        node: Node,
        connections: List<Connection>,
        allNodes: Map<String, Node>
    ): ValidationResult {
        val errors = mutableListOf<String>()

        // Get all connections involving this node
        val nodeConnections = connections.filter { conn ->
            conn.sourceNodeId == node.id || conn.targetNodeId == node.id
        }

        // Validate each connection
        nodeConnections.forEach { conn ->
            if (conn.sourceNodeId == node.id) {
                // This node is the source
                val targetNode = allNodes[conn.targetNodeId]
                if (targetNode != null) {
                    val result = validateConnection(conn, node, targetNode)
                    if (!result.isCompatible) {
                        errors.addAll(result.errors)
                    }
                } else {
                    errors.add("Target node '${conn.targetNodeId}' not found")
                }
            } else {
                // This node is the target
                val sourceNode = allNodes[conn.sourceNodeId]
                if (sourceNode != null) {
                    val result = validateConnection(conn, sourceNode, node)
                    if (!result.isCompatible) {
                        errors.addAll(result.errors)
                    }
                } else {
                    errors.add("Source node '${conn.sourceNodeId}' not found")
                }
            }
        }

        return ValidationResult(
            success = errors.isEmpty(),
            errors = errors
        )
    }
}

/**
 * Result of port compatibility validation
 *
 * @property isCompatible Whether the ports are compatible
 * @property errors List of error messages (blocking issues)
 * @property warnings List of warning messages (non-blocking issues)
 * @property typeCompatibility Detailed type compatibility information
 */
data class PortCompatibilityResult(
    val isCompatible: Boolean,
    val errors: List<String>,
    val warnings: List<String>,
    val typeCompatibility: TypeCompatibilityResult
)

/**
 * Result of type compatibility checking
 *
 * @property isCompatible Whether the types are compatible
 * @property errors List of type compatibility errors
 * @property warnings List of type compatibility warnings
 */
data class TypeCompatibilityResult(
    val isCompatible: Boolean,
    val errors: List<String>,
    val warnings: List<String>
)

/**
 * Extension function to easily check if two ports are compatible
 */
fun Port<*>.isCompatibleWith(other: Port<*>): PortCompatibilityResult {
    return PortValidator.validatePortConnection(this, other)
}

/**
 * Extension function to get type compatibility between two ports
 */
fun Port<*>.getTypeCompatibility(other: Port<*>): TypeCompatibilityResult {
    return PortValidator.checkTypeCompatibility(this.dataType, other.dataType)
}
