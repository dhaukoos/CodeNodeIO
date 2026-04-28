package io.codenode.flowgraphgenerate.flow

import io.codenode.fbpdsl.dsl.*
import io.codenode.fbpdsl.model.*

// Documentary FBP graph for the Generate Repository pipeline. The runtime
// path is CodeGenerationRunner (it invokes generators directly, not via
// this graph), so this file is a structural reference, not an executed
// pipeline.
//
// Feature 085 (universal-runtime collapse) replaced the trio
// {RuntimeFlow, RuntimeController, RuntimeControllerAdapter}Generator with
// a single ModuleRuntimeGenerator. This graph reflects that.
val generateRepositoryFlowGraph = flowGraph("GenerateRepository", version = "1.0.0") {

    val configSource = codeNode("ConfigSource", nodeType = "SOURCE") {
        position(50.0, 400.0)
        output("config", Any::class)
    }

    // Module-level generators
    val flowKtGen = codeNode("FlowKtGenerator", nodeType = "TRANSFORMER") {
        position(350.0, 50.0)
        input("config", Any::class)
        output("content", String::class)
    }

    val moduleRuntimeGen = codeNode("ModuleRuntimeGenerator", nodeType = "TRANSFORMER") {
        position(350.0, 130.0)
        input("config", Any::class)
        output("content", String::class)
    }

    val controllerInterfaceGen = codeNode("RuntimeControllerInterfaceGenerator", nodeType = "TRANSFORMER") {
        position(350.0, 210.0)
        input("config", Any::class)
        output("content", String::class)
    }

    val viewModelGen = codeNode("RuntimeViewModelGenerator", nodeType = "TRANSFORMER") {
        position(350.0, 290.0)
        input("config", Any::class)
        output("content", String::class)
    }

    val uiStubGen = codeNode("UserInterfaceStubGenerator", nodeType = "TRANSFORMER") {
        position(350.0, 370.0)
        input("config", Any::class)
        output("content", String::class)
    }

    val previewProviderGen = codeNode("PreviewProviderGenerator", nodeType = "TRANSFORMER") {
        position(350.0, 450.0)
        input("config", Any::class)
        output("content", String::class)
    }

    // Entity-specific generators
    val entityCUDGen = codeNode("EntityCUDGenerator", nodeType = "TRANSFORMER") {
        position(350.0, 530.0)
        input("config", Any::class)
        output("content", String::class)
    }

    val entityRepoGen = codeNode("EntityRepositoryGenerator", nodeType = "TRANSFORMER") {
        position(350.0, 610.0)
        input("config", Any::class)
        output("content", String::class)
    }

    val entityDisplayGen = codeNode("EntityDisplayGenerator", nodeType = "TRANSFORMER") {
        position(350.0, 690.0)
        input("config", Any::class)
        output("content", String::class)
    }

    val entityPersistenceGen = codeNode("EntityPersistenceGenerator", nodeType = "TRANSFORMER") {
        position(350.0, 770.0)
        input("config", Any::class)
        output("content", String::class)
    }

    val resultCollector = codeNode("ResultCollector", nodeType = "SINK") {
        position(650.0, 400.0)
        input("flowKt", String::class)
        input("moduleRuntime", String::class)
        input("controllerInterface", String::class)
        input("viewModel", String::class)
        input("uiStub", String::class)
        input("previewProvider", String::class)
        input("entityCUD", String::class)
        input("entityRepo", String::class)
        input("entityDisplay", String::class)
        input("entityPersistence", String::class)
    }

    // Source fan-out to all generators
    configSource.output("config") connect flowKtGen.input("config")
    configSource.output("config") connect moduleRuntimeGen.input("config")
    configSource.output("config") connect controllerInterfaceGen.input("config")
    configSource.output("config") connect viewModelGen.input("config")
    configSource.output("config") connect uiStubGen.input("config")
    configSource.output("config") connect previewProviderGen.input("config")
    configSource.output("config") connect entityCUDGen.input("config")
    configSource.output("config") connect entityRepoGen.input("config")
    configSource.output("config") connect entityDisplayGen.input("config")
    configSource.output("config") connect entityPersistenceGen.input("config")

    // Generator outputs to sink
    flowKtGen.output("content") connect resultCollector.input("flowKt")
    moduleRuntimeGen.output("content") connect resultCollector.input("moduleRuntime")
    controllerInterfaceGen.output("content") connect resultCollector.input("controllerInterface")
    viewModelGen.output("content") connect resultCollector.input("viewModel")
    uiStubGen.output("content") connect resultCollector.input("uiStub")
    previewProviderGen.output("content") connect resultCollector.input("previewProvider")
    entityCUDGen.output("content") connect resultCollector.input("entityCUD")
    entityRepoGen.output("content") connect resultCollector.input("entityRepo")
    entityDisplayGen.output("content") connect resultCollector.input("entityDisplay")
    entityPersistenceGen.output("content") connect resultCollector.input("entityPersistence")
}
