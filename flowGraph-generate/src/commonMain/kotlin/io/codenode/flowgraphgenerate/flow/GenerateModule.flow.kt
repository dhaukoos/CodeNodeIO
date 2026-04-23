package io.codenode.flowgraphgenerate.flow

import io.codenode.fbpdsl.dsl.*
import io.codenode.fbpdsl.model.*

val generateModuleFlowGraph = flowGraph("GenerateModule", version = "1.0.0") {

    val configSource = codeNode("ConfigSource", nodeType = "SOURCE") {
        position(50.0, 300.0)
        output("config", Any::class)
    }

    val flowKtGen = codeNode("FlowKtGenerator", nodeType = "TRANSFORMER") {
        position(350.0, 50.0)
        input("config", Any::class)
        output("content", String::class)
    }

    val runtimeFlowGen = codeNode("RuntimeFlowGenerator", nodeType = "TRANSFORMER") {
        position(350.0, 150.0)
        input("config", Any::class)
        output("content", String::class)
    }

    val controllerGen = codeNode("RuntimeControllerGenerator", nodeType = "TRANSFORMER") {
        position(350.0, 250.0)
        input("config", Any::class)
        output("content", String::class)
    }

    val controllerInterfaceGen = codeNode("RuntimeControllerInterfaceGenerator", nodeType = "TRANSFORMER") {
        position(350.0, 350.0)
        input("config", Any::class)
        output("content", String::class)
    }

    val controllerAdapterGen = codeNode("RuntimeControllerAdapterGenerator", nodeType = "TRANSFORMER") {
        position(350.0, 450.0)
        input("config", Any::class)
        output("content", String::class)
    }

    val viewModelGen = codeNode("RuntimeViewModelGenerator", nodeType = "TRANSFORMER") {
        position(350.0, 550.0)
        input("config", Any::class)
        output("content", String::class)
    }

    val uiStubGen = codeNode("UserInterfaceStubGenerator", nodeType = "TRANSFORMER") {
        position(350.0, 650.0)
        input("config", Any::class)
        output("content", String::class)
    }

    val resultCollector = codeNode("ResultCollector", nodeType = "SINK") {
        position(650.0, 300.0)
        input("flowKt", String::class)
        input("runtimeFlow", String::class)
        input("controller", String::class)
        input("controllerInterface", String::class)
        input("controllerAdapter", String::class)
        input("viewModel", String::class)
        input("uiStub", String::class)
    }

    configSource.output("config") connect flowKtGen.input("config")
    configSource.output("config") connect runtimeFlowGen.input("config")
    configSource.output("config") connect controllerGen.input("config")
    configSource.output("config") connect controllerInterfaceGen.input("config")
    configSource.output("config") connect controllerAdapterGen.input("config")
    configSource.output("config") connect viewModelGen.input("config")
    configSource.output("config") connect uiStubGen.input("config")

    flowKtGen.output("content") connect resultCollector.input("flowKt")
    runtimeFlowGen.output("content") connect resultCollector.input("runtimeFlow")
    controllerGen.output("content") connect resultCollector.input("controller")
    controllerInterfaceGen.output("content") connect resultCollector.input("controllerInterface")
    controllerAdapterGen.output("content") connect resultCollector.input("controllerAdapter")
    viewModelGen.output("content") connect resultCollector.input("viewModel")
    uiStubGen.output("content") connect resultCollector.input("uiStub")
}
