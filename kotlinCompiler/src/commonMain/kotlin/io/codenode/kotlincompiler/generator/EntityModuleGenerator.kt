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
    private val cudGenerator = EntityCUDGenerator()
    private val displayGenerator = EntityDisplayGenerator()
    private val persistenceGenerator = EntityPersistenceGenerator()
    private val uiGenerator = EntityUIGenerator()
    private val viewModelGenerator = RuntimeViewModelGenerator()
    private val runtimeFlowGenerator = RuntimeFlowGenerator()
    private val runtimeControllerGenerator = RuntimeControllerGenerator()
    private val runtimeControllerInterfaceGenerator = RuntimeControllerInterfaceGenerator()
    private val runtimeControllerAdapterGenerator = RuntimeControllerAdapterGenerator()
    private val stubGenerator = ProcessingLogicStubGenerator()
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
        val processingLogicPackage = "$basePackage.processingLogic"
        val userInterfacePackage = "$basePackage.userInterface"
        val basePackagePath = basePackage.replace(".", "/")
        val generatedPath = generatedPackage.replace(".", "/")
        val processingLogicPath = processingLogicPackage.replace(".", "/")
        val userInterfacePath = userInterfacePackage.replace(".", "/")
        val pluralName = spec.pluralName

        val moduleFiles = mutableMapOf<String, String>()

        // 1. .flow.kt file
        moduleFiles["src/commonMain/kotlin/$basePackagePath/${pluralName}.flow.kt"] =
            flowKtGenerator.generateFlowKt(flowGraph, basePackage, processingLogicPackage)

        // 2. CUD source node stub
        moduleFiles["src/commonMain/kotlin/$basePackagePath/${spec.entityName}CUD.kt"] =
            cudGenerator.generate(spec)

        // 3. Display sink node stub
        moduleFiles["src/commonMain/kotlin/$basePackagePath/${pluralName}Display.kt"] =
            displayGenerator.generate(spec)

        // 4. Koin persistence module
        moduleFiles["src/commonMain/kotlin/$basePackagePath/${pluralName}Persistence.kt"] =
            persistenceGenerator.generate(spec)

        // 5. ViewModel
        moduleFiles["src/commonMain/kotlin/$basePackagePath/${pluralName}ViewModel.kt"] =
            viewModelGenerator.generate(flowGraph, basePackage, generatedPackage)

        // 6. 4 generated runtime files
        moduleFiles["src/commonMain/kotlin/$generatedPath/${pluralName}Flow.kt"] =
            runtimeFlowGenerator.generate(flowGraph, generatedPackage, processingLogicPackage, basePackage)

        moduleFiles["src/commonMain/kotlin/$generatedPath/${pluralName}Controller.kt"] =
            runtimeControllerGenerator.generate(flowGraph, generatedPackage, processingLogicPackage, basePackage)

        moduleFiles["src/commonMain/kotlin/$generatedPath/${pluralName}ControllerInterface.kt"] =
            runtimeControllerInterfaceGenerator.generate(flowGraph, generatedPackage)

        moduleFiles["src/commonMain/kotlin/$generatedPath/${pluralName}ControllerAdapter.kt"] =
            runtimeControllerAdapterGenerator.generate(flowGraph, generatedPackage)

        // 7. Processing logic stubs (only for nodes that need them — the repository processor)
        val codeNodes = flowGraph.getAllCodeNodes()
        for (codeNode in codeNodes) {
            if (stubGenerator.shouldGenerateStub(codeNode)) {
                val stubFileName = stubGenerator.getStubFileName(codeNode)
                moduleFiles["src/commonMain/kotlin/$processingLogicPath/$stubFileName"] =
                    stubGenerator.generateStub(codeNode, processingLogicPackage)
            }
        }

        // 8. UI composable files
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
}
