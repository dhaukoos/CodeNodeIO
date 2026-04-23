package io.codenode.flowgraphgenerate.flow

import io.codenode.fbpdsl.dsl.*
import io.codenode.fbpdsl.model.*

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

    val runtimeFlowGen = codeNode("RuntimeFlowGenerator", nodeType = "TRANSFORMER") {
        position(350.0, 130.0)
        input("config", Any::class)
        output("content", String::class)
    }

    val controllerGen = codeNode("RuntimeControllerGenerator", nodeType = "TRANSFORMER") {
        position(350.0, 210.0)
        input("config", Any::class)
        output("content", String::class)
    }

    val controllerInterfaceGen = codeNode("RuntimeControllerInterfaceGenerator", nodeType = "TRANSFORMER") {
        position(350.0, 290.0)
        input("config", Any::class)
        output("content", String::class)
    }

    val controllerAdapterGen = codeNode("RuntimeControllerAdapterGenerator", nodeType = "TRANSFORMER") {
        position(350.0, 370.0)
        input("config", Any::class)
        output("content", String::class)
    }

    val viewModelGen = codeNode("RuntimeViewModelGenerator", nodeType = "TRANSFORMER") {
        position(350.0, 450.0)
        input("config", Any::class)
        output("content", String::class)
    }

    val uiStubGen = codeNode("UserInterfaceStubGenerator", nodeType = "TRANSFORMER") {
        position(350.0, 530.0)
        input("config", Any::class)
        output("content", String::class)
    }

    // Entity-specific generators
    val entityCUDGen = codeNode("EntityCUDGenerator", nodeType = "TRANSFORMER") {
        position(350.0, 630.0)
        input("config", Any::class)
        output("content", String::class)
    }

    val entityRepoGen = codeNode("EntityRepositoryGenerator", nodeType = "TRANSFORMER") {
        position(350.0, 710.0)
        input("config", Any::class)
        output("content", String::class)
    }

    val entityDisplayGen = codeNode("EntityDisplayGenerator", nodeType = "TRANSFORMER") {
        position(350.0, 790.0)
        input("config", Any::class)
        output("content", String::class)
    }

    val entityPersistenceGen = codeNode("EntityPersistenceGenerator", nodeType = "TRANSFORMER") {
        position(350.0, 870.0)
        input("config", Any::class)
        output("content", String::class)
    }

    val resultCollector = codeNode("ResultCollector", nodeType = "SINK") {
        position(650.0, 400.0)
        input("flowKt", String::class)
        input("runtimeFlow", String::class)
        input("controller", String::class)
        input("controllerInterface", String::class)
        input("controllerAdapter", String::class)
        input("viewModel", String::class)
        input("uiStub", String::class)
        input("entityCUD", String::class)
        input("entityRepo", String::class)
        input("entityDisplay", String::class)
        input("entityPersistence", String::class)
    }

    // Source fan-out to all generators
    configSource.output("config") connect flowKtGen.input("config")
    configSource.output("config") connect runtimeFlowGen.input("config")
    configSource.output("config") connect controllerGen.input("config")
    configSource.output("config") connect controllerInterfaceGen.input("config")
    configSource.output("config") connect controllerAdapterGen.input("config")
    configSource.output("config") connect viewModelGen.input("config")
    configSource.output("config") connect uiStubGen.input("config")
    configSource.output("config") connect entityCUDGen.input("config")
    configSource.output("config") connect entityRepoGen.input("config")
    configSource.output("config") connect entityDisplayGen.input("config")
    configSource.output("config") connect entityPersistenceGen.input("config")

    // Generator outputs to sink
    flowKtGen.output("content") connect resultCollector.input("flowKt")
    runtimeFlowGen.output("content") connect resultCollector.input("runtimeFlow")
    controllerGen.output("content") connect resultCollector.input("controller")
    controllerInterfaceGen.output("content") connect resultCollector.input("controllerInterface")
    controllerAdapterGen.output("content") connect resultCollector.input("controllerAdapter")
    viewModelGen.output("content") connect resultCollector.input("viewModel")
    uiStubGen.output("content") connect resultCollector.input("uiStub")
    entityCUDGen.output("content") connect resultCollector.input("entityCUD")
    entityRepoGen.output("content") connect resultCollector.input("entityRepo")
    entityDisplayGen.output("content") connect resultCollector.input("entityDisplay")
    entityPersistenceGen.output("content") connect resultCollector.input("entityPersistence")
}
