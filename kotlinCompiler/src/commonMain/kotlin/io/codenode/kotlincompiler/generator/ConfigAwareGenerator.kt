/*
 * Configuration-Aware Code Generation Utilities
 * Provides utilities for generating configuration properties in node components
 * License: Apache 2.0
 */

package io.codenode.kotlincompiler.generator

import com.squareup.kotlinpoet.*
import io.codenode.fbpdsl.model.CodeNode

/**
 * Utility object for configuration-aware code generation.
 *
 * Provides functions to add configuration properties to generated node components,
 * ensuring that node configuration values are accessible in the generated code.
 */
object ConfigAwareGenerator {

    /**
     * Adds configuration properties from a CodeNode to a TypeSpec builder.
     *
     * Each configuration entry becomes a property in the generated class with:
     * - Property name derived from the configuration key (in camelCase)
     * - Type of String (configuration values are stored as strings)
     * - Initializer with the configuration value
     *
     * @param classBuilder The TypeSpec.Builder to add properties to
     * @param node The CodeNode containing configuration to add
     */
    fun addConfigurationProperties(classBuilder: TypeSpec.Builder, node: CodeNode) {
        if (node.configuration.isEmpty()) return

        node.configuration.forEach { (key, value) ->
            classBuilder.addProperty(
                PropertySpec.builder(key.camelCase(), String::class)
                    .initializer("%S", value)
                    .addKdoc("Configuration property: $key")
                    .build()
            )
        }
    }

    /**
     * Generates configuration properties as a list of PropertySpec.
     *
     * @param node The CodeNode containing configuration
     * @return List of PropertySpec for each configuration entry
     */
    fun generateConfigurationProperties(node: CodeNode): List<PropertySpec> {
        return node.configuration.map { (key, value) ->
            PropertySpec.builder(key.camelCase(), String::class)
                .initializer("%S", value)
                .addKdoc("Configuration property: $key")
                .build()
        }
    }

    /**
     * Generates a companion object with configuration defaults.
     *
     * Creates a companion object containing default values for all configuration
     * properties, useful for documentation and testing.
     *
     * @param node The CodeNode containing configuration
     * @return TypeSpec for the companion object, or null if no configuration
     */
    fun generateConfigurationCompanion(node: CodeNode): TypeSpec? {
        if (node.configuration.isEmpty()) return null

        val companionBuilder = TypeSpec.companionObjectBuilder()
            .addKdoc("Default configuration values.\n")

        node.configuration.forEach { (key, value) ->
            companionBuilder.addProperty(
                PropertySpec.builder("DEFAULT_${key.uppercase()}", String::class)
                    .addModifiers(KModifier.CONST)
                    .initializer("%S", value)
                    .build()
            )
        }

        return companionBuilder.build()
    }

    /**
     * Infers the type of a configuration value from its string representation.
     *
     * @param value The string value to infer type from
     * @return The inferred TypeName (Int, Double, Boolean, or String)
     */
    fun inferConfigurationType(value: String): TypeName {
        return when {
            value.toBooleanStrictOrNull() != null -> BOOLEAN
            value.toIntOrNull() != null -> INT
            value.toDoubleOrNull() != null -> DOUBLE
            else -> STRING
        }
    }

    /**
     * Generates a typed configuration property with inferred type.
     *
     * Attempts to infer the appropriate type (Int, Double, Boolean) from the
     * configuration value string. Falls back to String if no type can be inferred.
     *
     * @param key The configuration key
     * @param value The configuration value string
     * @return PropertySpec with inferred type and appropriate initializer
     */
    fun generateTypedConfigurationProperty(key: String, value: String): PropertySpec {
        val propertyName = key.camelCase()

        return when {
            value.toBooleanStrictOrNull() != null -> {
                PropertySpec.builder(propertyName, BOOLEAN)
                    .initializer("%L", value.toBoolean())
                    .addKdoc("Configuration property: $key")
                    .build()
            }
            value.toIntOrNull() != null -> {
                PropertySpec.builder(propertyName, INT)
                    .initializer("%L", value.toInt())
                    .addKdoc("Configuration property: $key")
                    .build()
            }
            value.toDoubleOrNull() != null -> {
                PropertySpec.builder(propertyName, DOUBLE)
                    .initializer("%L", value.toDouble())
                    .addKdoc("Configuration property: $key")
                    .build()
            }
            else -> {
                PropertySpec.builder(propertyName, String::class)
                    .initializer("%S", value)
                    .addKdoc("Configuration property: $key")
                    .build()
            }
        }
    }

    /**
     * Adds typed configuration properties to a TypeSpec builder.
     *
     * Similar to addConfigurationProperties but attempts to infer types for
     * numeric and boolean values.
     *
     * @param classBuilder The TypeSpec.Builder to add properties to
     * @param node The CodeNode containing configuration to add
     */
    fun addTypedConfigurationProperties(classBuilder: TypeSpec.Builder, node: CodeNode) {
        if (node.configuration.isEmpty()) return

        node.configuration.forEach { (key, value) ->
            classBuilder.addProperty(generateTypedConfigurationProperty(key, value))
        }
    }
}
