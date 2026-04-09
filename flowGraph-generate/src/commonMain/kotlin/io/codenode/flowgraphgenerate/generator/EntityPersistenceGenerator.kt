/*
 * EntityPersistenceGenerator - Generates {Entity}sPersistence.kt Koin module files
 * License: Apache 2.0
 */

package io.codenode.flowgraphgenerate.generator

/**
 * Generates a {Entity}sPersistence.kt Koin module file parameterized by entity name.
 * Follows the UserProfilesPersistence.kt pattern: a Koin module providing the
 * Repository and a KoinComponent object exposing the DAO.
 */
class EntityPersistenceGenerator {

    /**
     * Generates the {Entity}sPersistence.kt Koin module content.
     *
     * @param spec The entity module specification
     * @return Complete Kotlin source file content
     */
    fun generate(spec: EntityModuleSpec): String {
        val entityName = spec.entityName
        val entityNameLower = spec.entityNameLower
        val pluralName = spec.pluralName
        val pluralNameLower = spec.pluralNameLower

        return buildString {
            appendLine("package ${spec.basePackage}")
            appendLine()
            appendLine("import ${spec.persistencePackage}.${entityName.lowercase()}.${entityName}Dao")
            appendLine("import ${spec.persistencePackage}.${entityName.lowercase()}.${entityName}Repository")
            appendLine("import org.koin.core.component.KoinComponent")
            appendLine("import org.koin.core.component.inject")
            appendLine("import org.koin.dsl.module")
            appendLine()
            appendLine("/**")
            appendLine(" * Koin module for $pluralName persistence dependencies.")
            appendLine(" * The app layer provides the [${entityName}Dao] singleton;")
            appendLine(" * this module wires it into a [${entityName}Repository].")
            appendLine(" */")
            appendLine("val ${pluralNameLower}Module = module {")
            appendLine("    single { ${entityName}Repository(get()) }")
            appendLine("}")
            appendLine()
            appendLine("/**")
            appendLine(" * Koin-backed accessor for persistence dependencies.")
            appendLine(" * Used by processing logic tick functions that cannot take constructor parameters.")
            appendLine(" */")
            appendLine("object ${pluralName}Persistence : KoinComponent {")
            appendLine("    val dao: ${entityName}Dao by inject()")
            appendLine("}")
        }
    }
}
