/*
 * EntityModuleGenerator - Orchestrates full entity module code generation
 * License: Apache 2.0
 */

package io.codenode.kotlincompiler.generator

import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.FlowGraph

/**
 * Output data class containing all generated file contents for an entity module.
 *
 * @property moduleFiles Map of relative path → content string for entity module files
 * @property persistenceFiles Map of relative path → content string for persistence module files
 * @property flowGraph The generated FlowGraph (for further use by runtime generators)
 */
data class EntityModuleOutput(
    val moduleFiles: Map<String, String>,
    val persistenceFiles: Map<String, String>,
    val flowGraph: FlowGraph
)

/**
 * Orchestrates the complete generation of an entity CRUD module.
 * Coordinates all individual generators to produce the full set of files
 * following the UserProfiles module pattern.
 */
class EntityModuleGenerator {

    private val flowGraphBuilder = EntityFlowGraphBuilder()
    private val flowKtGenerator = FlowKtGenerator()
    private val cudCodeNodeGenerator = EntityCUDCodeNodeGenerator()
    private val repositoryCodeNodeGenerator = EntityRepositoryCodeNodeGenerator()
    private val displayCodeNodeGenerator = EntityDisplayCodeNodeGenerator()
    private val persistenceGenerator = EntityPersistenceGenerator()
    private val uiGenerator = EntityUIGenerator()
    private val viewModelGenerator = RuntimeViewModelGenerator()
    private val runtimeFlowGenerator = RuntimeFlowGenerator()
    private val runtimeControllerGenerator = RuntimeControllerGenerator()
    private val runtimeControllerInterfaceGenerator = RuntimeControllerInterfaceGenerator()
    private val runtimeControllerAdapterGenerator = RuntimeControllerAdapterGenerator()
    private val repositoryCodeGenerator = RepositoryCodeGenerator()

    /**
     * Generates a complete entity module.
     *
     * @param spec The entity module specification
     * @return EntityModuleOutput containing all generated file contents
     */
    fun generateModule(spec: EntityModuleSpec): EntityModuleOutput {
        val flowGraph = flowGraphBuilder.buildFlowGraph(spec)
        val basePackage = spec.basePackage
        val generatedPackage = "$basePackage.generated"
        val userInterfacePackage = "$basePackage.userInterface"
        val basePackagePath = basePackage.replace(".", "/")
        val generatedPath = generatedPackage.replace(".", "/")
        val userInterfacePath = userInterfacePackage.replace(".", "/")
        val pluralName = spec.pluralName

        val ipTypesPackage = "$basePackage.iptypes"
        val ipTypesPath = ipTypesPackage.replace(".", "/")

        val moduleFiles = mutableMapOf<String, String>()

        // 0. IP type data class (in iptypes/ directory)
        moduleFiles["src/commonMain/kotlin/$ipTypesPath/${spec.entityName}.kt"] =
            generateIPTypeFile(spec, ipTypesPackage)

        // 1. .flow.kt file (with IP type names for port type resolution)
        val ipTypeId = "ip_${spec.entityName.lowercase()}"
        val ipTypeNames = mapOf(ipTypeId to spec.entityName)
        val ipTypeImports = listOf("$ipTypesPackage.${spec.entityName}")
        moduleFiles["src/commonMain/kotlin/$basePackagePath/${pluralName}.flow.kt"] =
            flowKtGenerator.generateFlowKt(flowGraph, basePackage, null, ipTypeNames, ipTypeImports)

        // 2. Node definition files (in nodes/ subdirectory)
        val nodesPath = "$basePackagePath/nodes"
        moduleFiles["src/commonMain/kotlin/$nodesPath/${spec.entityName}CUDCodeNode.kt"] =
            cudCodeNodeGenerator.generate(spec)

        moduleFiles["src/commonMain/kotlin/$nodesPath/${spec.entityName}RepositoryCodeNode.kt"] =
            repositoryCodeNodeGenerator.generate(spec)

        moduleFiles["src/commonMain/kotlin/$nodesPath/${pluralName}DisplayCodeNode.kt"] =
            displayCodeNodeGenerator.generate(spec)

        // 4. Koin persistence module
        moduleFiles["src/commonMain/kotlin/$basePackagePath/${pluralName}Persistence.kt"] =
            persistenceGenerator.generate(spec)

        // 5. ViewModel
        moduleFiles["src/commonMain/kotlin/$basePackagePath/${pluralName}ViewModel.kt"] =
            viewModelGenerator.generate(flowGraph, basePackage, generatedPackage)

        // 6. 4 generated runtime files
        moduleFiles["src/commonMain/kotlin/$generatedPath/${pluralName}Flow.kt"] =
            runtimeFlowGenerator.generate(flowGraph, generatedPackage, basePackage)

        moduleFiles["src/commonMain/kotlin/$generatedPath/${pluralName}Controller.kt"] =
            runtimeControllerGenerator.generate(flowGraph, generatedPackage, basePackage)

        moduleFiles["src/commonMain/kotlin/$generatedPath/${pluralName}ControllerInterface.kt"] =
            runtimeControllerInterfaceGenerator.generate(flowGraph, generatedPackage)

        moduleFiles["src/commonMain/kotlin/$generatedPath/${pluralName}ControllerAdapter.kt"] =
            runtimeControllerAdapterGenerator.generate(flowGraph, generatedPackage)

        // 7. UI composable files
        moduleFiles["src/commonMain/kotlin/$userInterfacePath/${pluralName}.kt"] =
            uiGenerator.generateListView(spec)

        moduleFiles["src/commonMain/kotlin/$userInterfacePath/AddUpdate${spec.entityName}.kt"] =
            uiGenerator.generateFormView(spec)

        moduleFiles["src/commonMain/kotlin/$userInterfacePath/${spec.entityName}Row.kt"] =
            uiGenerator.generateRowView(spec)

        // Persistence files (go to the shared persistence module)
        val persistenceFiles = mutableMapOf<String, String>()
        val persistencePackage = spec.persistencePackage
        val persistencePath = persistencePackage.replace(".", "/")
        val tableName = spec.entityName.lowercase() + "s"

        persistenceFiles["src/commonMain/kotlin/$persistencePath/${spec.entityName}Entity.kt"] =
            repositoryCodeGenerator.generateEntity(spec.entityName, spec.properties, persistencePackage)

        persistenceFiles["src/commonMain/kotlin/$persistencePath/${spec.entityName}Dao.kt"] =
            repositoryCodeGenerator.generateDao(spec.entityName, tableName, persistencePackage)

        persistenceFiles["src/commonMain/kotlin/$persistencePath/${spec.entityName}Repository.kt"] =
            repositoryCodeGenerator.generateRepository(spec.entityName, persistencePackage)

        return EntityModuleOutput(
            moduleFiles = moduleFiles,
            persistenceFiles = persistenceFiles,
            flowGraph = flowGraph
        )
    }

    /**
     * Generates the IP type data class file with @IPType header and entity conversion extensions.
     */
    private fun generateIPTypeFile(spec: EntityModuleSpec, ipTypesPackage: String): String {
        val entityName = spec.entityName
        val entityNameLower = spec.entityNameLower
        val typeId = "ip_${entityName.lowercase()}"

        return buildString {
            appendLine("/*")
            appendLine(" * $entityName - Custom IP Type")
            appendLine(" * @IPType")
            appendLine(" * @TypeName $entityName")
            appendLine(" * @TypeId $typeId")
            appendLine(" * @Color rgb(0, 188, 212)")
            appendLine(" * Generated by CodeNodeIO EntityModuleGenerator")
            appendLine(" * License: Apache 2.0")
            appendLine(" */")
            appendLine()
            appendLine("package $ipTypesPackage")
            appendLine()
            appendLine("import ${spec.persistencePackage}.${entityName}Entity")
            appendLine()
            appendLine("data class $entityName(")
            appendLine("    val id: Long = 0,")
            for ((index, prop) in spec.properties.withIndex()) {
                val nullable = if (!prop.isRequired) "?" else ""
                val default = if (!prop.isRequired) " = null" else ""
                val comma = if (index < spec.properties.size - 1) "," else ""
                appendLine("    val ${prop.name}: ${prop.kotlinType}$nullable$default$comma")
            }
            appendLine(")")
            appendLine()
            appendLine("/** Convert IP type to persistence entity */")
            append("fun $entityName.toEntity() = ${entityName}Entity(id = id")
            for (prop in spec.properties) {
                append(", ${prop.name} = ${prop.name}")
            }
            appendLine(")")
            appendLine()
            appendLine("/** Convert persistence entity to IP type */")
            append("fun ${entityName}Entity.to$entityName() = $entityName(id = id")
            for (prop in spec.properties) {
                append(", ${prop.name} = ${prop.name}")
            }
            appendLine(")")
        }
    }
}
