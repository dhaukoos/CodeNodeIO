/*
 * NodeTypeDefinition - Template for Node Types in Palette
 * Defines reusable node templates with ports, configuration, and code generation
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.model

/**
 * NodeTypeDefinition represents a template defining what a node can do.
 * It serves as a catalog entry for available node types in the visual editor palette.
 *
 * Based on FBP principles, NodeTypeDefinitions are reusable templates that:
 * - Define the interface (ports) a node exposes
 * - Specify configuration options for customization
 * - Provide code generation templates for target platforms
 * - Enable drag-and-drop creation of nodes from the palette
 *
 * @property id Unique identifier for this node type definition
 * @property name Display name (e.g., "HTTP GET Request", "JSON Transformer")
 * @property category Categorization for palette organization
 * @property description User-facing documentation explaining the node's purpose
 * @property portTemplates List of port specifications (defines node interface)
 * @property defaultConfiguration Default property values when node is created
 * @property configurationSchema JSON Schema for validating property values
 * @property codeTemplates Map of platform to code generation template
 *
 * @sample
 * ```kotlin
 * val httpGetNodeType = NodeTypeDefinition(
 *     id = "nodeType_http_get",
 *     name = "HTTP GET Request",
 *     category = NodeCategory.API_ENDPOINT,
 *     description = "Performs an HTTP GET request to a specified URL",
 *     portTemplates = listOf(
 *         PortTemplate("url", Port.Direction.INPUT, StringData::class, true),
 *         PortTemplate("response", Port.Direction.OUTPUT, StringData::class, false),
 *         PortTemplate("error", Port.Direction.OUTPUT, ErrorData::class, false)
 *     ),
 *     defaultConfiguration = mapOf("timeout" to "30s", "retries" to "3"),
 *     configurationSchema = """{"type": "object", "properties": {...}}""",
 *     codeTemplates = mapOf(
 *         "KMP" to "suspend fun httpGet(url: String): String { ... }",
 *         "Go" to "func httpGet(url string) (string, error) { ... }"
 *     )
 * )
 * ```
 */
data class NodeTypeDefinition(
    val id: String,
    val name: String,
    val category: NodeCategory,
    val description: String,
    val portTemplates: List<PortTemplate> = emptyList(),
    val defaultConfiguration: Map<String, String> = emptyMap(),
    val configurationSchema: String? = null,
    val codeTemplates: Map<String, String> = emptyMap()
) {
    /**
     * Category for organizing node types in the palette
     */
    enum class NodeCategory {
        /** UI components (buttons, forms, displays) */
        UI_COMPONENT,

        /** Backend services (authentication, data processing) */
        SERVICE,

        /** Data transformation nodes (map, filter, format) */
        TRANSFORMER,

        /** Validation nodes (schema validation, business rules) */
        VALIDATOR,

        /** API endpoint nodes (REST, GraphQL, WebSocket) */
        API_ENDPOINT,

        /** Database operation nodes (query, insert, update) */
        DATABASE,

        /** Generic nodes with configurable inputs/outputs (0-5 each) */
        GENERIC
    }

    init {
        require(id.isNotBlank()) { "NodeTypeDefinition ID cannot be blank" }
        require(name.isNotBlank()) { "NodeTypeDefinition name cannot be blank" }
        require(description.isNotBlank()) { "NodeTypeDefinition description cannot be blank" }
    }

    /**
     * Validates that this NodeTypeDefinition is well-formed
     *
     * @return Validation result with success flag and error messages
     */
    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()

        // Validate basic attributes
        if (id.isBlank()) {
            errors.add("NodeTypeDefinition ID cannot be blank")
        }
        if (name.isBlank()) {
            errors.add("NodeTypeDefinition name cannot be blank")
        }
        if (description.isBlank()) {
            errors.add("NodeTypeDefinition description cannot be blank")
        }

        // Validate port templates have unique names
        val portNames = portTemplates.map { it.name }
        val duplicatePortNames = portNames.groupingBy { it }.eachCount().filter { it.value > 1 }
        if (duplicatePortNames.isNotEmpty()) {
            errors.add("Port templates must have unique names, found duplicates: ${duplicatePortNames.keys.joinToString(", ")}")
        }

        // Validate each port template
        portTemplates.forEach { portTemplate ->
            val templateValidation = portTemplate.validate()
            if (!templateValidation.success) {
                errors.add("Invalid port template '${portTemplate.name}': ${templateValidation.errors.joinToString(", ")}")
            }
        }

        // Validate configuration schema if present
        if (configurationSchema != null && configurationSchema.isBlank()) {
            errors.add("Configuration schema cannot be blank if specified")
        }

        // Validate code templates
        if (codeTemplates.isEmpty()) {
            // Warning: No code templates means no code generation possible
            // This might be acceptable for design-only nodes
        }
        codeTemplates.forEach { (platform, template) ->
            if (template.isBlank()) {
                errors.add("Code template for platform '$platform' cannot be blank")
            }
        }

        return ValidationResult(
            success = errors.isEmpty(),
            errors = errors
        )
    }

    /**
     * Checks if this node type definition supports a specific target platform
     *
     * @param platform The platform identifier (e.g., "KMP", "Go")
     * @return true if code template exists for the platform
     */
    fun supportsCodeGeneration(platform: String): Boolean {
        return codeTemplates.containsKey(platform)
    }

    /**
     * Gets the code template for a specific platform
     *
     * @param platform The platform identifier
     * @return Code template string or null if not found
     */
    fun getCodeTemplate(platform: String): String? {
        return codeTemplates[platform]
    }

    /**
     * Gets all input port templates
     *
     * @return List of input port templates
     */
    fun getInputPortTemplates(): List<PortTemplate> {
        return portTemplates.filter { it.direction == Port.Direction.INPUT }
    }

    /**
     * Gets all output port templates
     *
     * @return List of output port templates
     */
    fun getOutputPortTemplates(): List<PortTemplate> {
        return portTemplates.filter { it.direction == Port.Direction.OUTPUT }
    }

    /**
     * Finds a port template by name
     *
     * @param portName The name of the port template to find
     * @return The port template if found, null otherwise
     */
    fun findPortTemplate(portName: String): PortTemplate? {
        return portTemplates.find { it.name == portName }
    }

    /**
     * Creates a copy of this NodeTypeDefinition with updated port templates
     *
     * @param newPortTemplates The new list of port templates
     * @return New NodeTypeDefinition instance with updated port templates
     */
    fun withPortTemplates(newPortTemplates: List<PortTemplate>): NodeTypeDefinition {
        return copy(portTemplates = newPortTemplates)
    }

    /**
     * Creates a copy of this NodeTypeDefinition with an added port template
     *
     * @param portTemplate The port template to add
     * @return New NodeTypeDefinition instance with added port template
     */
    fun addPortTemplate(portTemplate: PortTemplate): NodeTypeDefinition {
        return copy(portTemplates = portTemplates + portTemplate)
    }

    /**
     * Creates a copy of this NodeTypeDefinition with updated default configuration
     *
     * @param newConfiguration The new default configuration map
     * @return New NodeTypeDefinition instance with updated configuration
     */
    fun withDefaultConfiguration(newConfiguration: Map<String, String>): NodeTypeDefinition {
        return copy(defaultConfiguration = newConfiguration)
    }

    /**
     * Creates a copy of this NodeTypeDefinition with an added configuration entry
     *
     * @param key Configuration key
     * @param value Configuration value
     * @return New NodeTypeDefinition instance with added configuration
     */
    fun addConfigurationDefault(key: String, value: String): NodeTypeDefinition {
        return copy(defaultConfiguration = defaultConfiguration + (key to value))
    }

    /**
     * Creates a copy of this NodeTypeDefinition with updated code templates
     *
     * @param newCodeTemplates The new code templates map
     * @return New NodeTypeDefinition instance with updated code templates
     */
    fun withCodeTemplates(newCodeTemplates: Map<String, String>): NodeTypeDefinition {
        return copy(codeTemplates = newCodeTemplates)
    }

    /**
     * Creates a copy of this NodeTypeDefinition with an added code template
     *
     * @param platform Target platform identifier
     * @param template Code generation template
     * @return New NodeTypeDefinition instance with added code template
     */
    fun addCodeTemplate(platform: String, template: String): NodeTypeDefinition {
        return copy(codeTemplates = codeTemplates + (platform to template))
    }

    /**
     * Gets a configuration default value by key
     *
     * @param key Configuration key
     * @return Configuration value or null if not found
     */
    fun getConfigurationDefault(key: String): String? {
        return defaultConfiguration[key]
    }

    /**
     * Checks if this node type has any required input ports
     *
     * @return true if at least one input port is required
     */
    fun hasRequiredInputs(): Boolean {
        return portTemplates.any { it.direction == Port.Direction.INPUT && it.required }
    }

    /**
     * Gets the total number of port templates
     *
     * @return Total port template count
     */
    fun getPortTemplateCount(): Int {
        return portTemplates.size
    }

    /**
     * Checks if this node type is a valid filename for DSL persistence
     *
     * @return true if name contains only valid filename characters
     */
    fun hasValidFilename(): Boolean {
        val validFilenamePattern = Regex("^[a-zA-Z0-9_\\-. ]+$")
        return validFilenamePattern.matches(name)
    }
}
