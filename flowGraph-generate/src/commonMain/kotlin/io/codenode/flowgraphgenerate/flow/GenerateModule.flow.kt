package io.codenode.flowgraphgenerate.flow

import io.codenode.fbpdsl.dsl.*
import io.codenode.fbpdsl.model.*

// Documentary FBP graph for the Generate Module pipeline. The runtime path
// is CodeGenerationRunner (it invokes generators directly, not via this
// graph), so this file is a structural reference, not an executed pipeline.
//
// Feature 085 (universal-runtime collapse) replaced the trio
// {RuntimeFlow, RuntimeController, RuntimeControllerAdapter}Generator with
// a single ModuleRuntimeGenerator. This graph reflects that.
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

    val moduleRuntimeGen = codeNode("ModuleRuntimeGenerator", nodeType = "TRANSFORMER") {
        position(350.0, 150.0)
        input("config", Any::class)
        output("content", String::class)
    }

    val controllerInterfaceGen = codeNode("RuntimeControllerInterfaceGenerator", nodeType = "TRANSFORMER") {
        position(350.0, 250.0)
        input("config", Any::class)
        output("content", String::class)
    }

    val viewModelGen = codeNode("RuntimeViewModelGenerator", nodeType = "TRANSFORMER") {
        position(350.0, 350.0)
        input("config", Any::class)
        output("content", String::class)
    }

    val uiStubGen = codeNode("UserInterfaceStubGenerator", nodeType = "TRANSFORMER") {
        position(350.0, 450.0)
        input("config", Any::class)
        output("content", String::class)
    }

    val previewProviderGen = codeNode("PreviewProviderGenerator", nodeType = "TRANSFORMER") {
        position(350.0, 550.0)
        input("config", Any::class)
        output("content", String::class)
    }

    val resultCollector = codeNode("ResultCollector", nodeType = "SINK") {
        position(650.0, 300.0)
        input("flowKt", String::class)
        input("moduleRuntime", String::class)
        input("controllerInterface", String::class)
        input("viewModel", String::class)
        input("uiStub", String::class)
        input("previewProvider", String::class)
    }

    configSource.output("config") connect flowKtGen.input("config")
    configSource.output("config") connect moduleRuntimeGen.input("config")
    configSource.output("config") connect controllerInterfaceGen.input("config")
    configSource.output("config") connect viewModelGen.input("config")
    configSource.output("config") connect uiStubGen.input("config")
    configSource.output("config") connect previewProviderGen.input("config")

    flowKtGen.output("content") connect resultCollector.input("flowKt")
    moduleRuntimeGen.output("content") connect resultCollector.input("moduleRuntime")
    controllerInterfaceGen.output("content") connect resultCollector.input("controllerInterface")
    viewModelGen.output("content") connect resultCollector.input("viewModel")
    uiStubGen.output("content") connect resultCollector.input("uiStub")
    previewProviderGen.output("content") connect resultCollector.input("previewProvider")
}
