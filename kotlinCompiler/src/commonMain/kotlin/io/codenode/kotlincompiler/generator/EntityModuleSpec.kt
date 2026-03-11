/*
 * EntityModuleSpec - Specification for generating a complete entity CRUD module
 * License: Apache 2.0
 */

package io.codenode.kotlincompiler.generator

/**
 * Represents the specification for generating a complete entity module.
 * Derived from a custom IP Type's name and properties.
 *
 * @param entityName PascalCase entity name (e.g., "GeoLocation")
 * @param entityNameLower camelCase variant (e.g., "geoLocation")
 * @param pluralName PascalCase plural (e.g., "GeoLocations")
 * @param pluralNameLower camelCase plural (e.g., "geoLocations")
 * @param properties Entity fields from IP Type definition
 * @param sourceIPTypeId UUID of the source custom IP Type
 * @param basePackage Module package (e.g., "io.codenode.geolocations")
 * @param persistencePackage Always "io.codenode.persistence"
 */
data class EntityModuleSpec(
    val entityName: String,
    val entityNameLower: String,
    val pluralName: String,
    val pluralNameLower: String,
    val properties: List<EntityProperty>,
    val sourceIPTypeId: String,
    val basePackage: String,
    val persistencePackage: String = "io.codenode.persistence"
) {
    companion object {
        /**
         * Creates an EntityModuleSpec from an IP Type name, deriving all naming variants.
         *
         * @param ipTypeName PascalCase IP type name (e.g., "GeoLocation")
         * @param sourceIPTypeId UUID of the source custom IP type
         * @param properties List of entity properties from the IP type definition
         * @return Fully populated EntityModuleSpec
         */
        fun fromIPType(
            ipTypeName: String,
            sourceIPTypeId: String,
            properties: List<EntityProperty>
        ): EntityModuleSpec {
            val entityName = ipTypeName
            val entityNameLower = ipTypeName.replaceFirstChar { it.lowercase() }
            val pluralName = pluralize(ipTypeName)
            val pluralNameLower = pluralize(entityNameLower)
            val basePackage = "io.codenode.${pluralName.lowercase()}"

            return EntityModuleSpec(
                entityName = entityName,
                entityNameLower = entityNameLower,
                pluralName = pluralName,
                pluralNameLower = pluralNameLower,
                properties = properties,
                sourceIPTypeId = sourceIPTypeId,
                basePackage = basePackage
            )
        }

        private fun pluralize(name: String): String = when {
            name.endsWith("s", ignoreCase = true) -> "${name}es"
            name.endsWith("y", ignoreCase = true) && !name.endsWith("ay", ignoreCase = true)
                && !name.endsWith("ey", ignoreCase = true) && !name.endsWith("oy", ignoreCase = true)
                && !name.endsWith("uy", ignoreCase = true) -> "${name.dropLast(1)}ies"
            name.endsWith("x", ignoreCase = true) || name.endsWith("sh", ignoreCase = true)
                || name.endsWith("ch", ignoreCase = true) -> "${name}es"
            else -> "${name}s"
        }
    }
}
