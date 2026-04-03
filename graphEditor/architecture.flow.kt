/*
 * Flow Graph: Target Architecture
 * Version: 2.0.0
 * Description: Vertical-slice decomposition with dedicated type catalog module — six workflow modules plus composition root
 * Generated: 2026-04-03
 *
 * Cross-validation with MIGRATION.md:
 *
 * Connection                                          | MIGRATION.md Interface                    | Seam Boundary
 * --------------------------------------------------- | ----------------------------------------- | --------------------
 * types.ipTypeMetadata → compose.ipTypeMetadata       | IPTypeRegistryService                     | compose→types
 * types.ipTypeMetadata → persist.ipTypeMetadata       | IPTypeRegistryService                     | persist→types
 * types.ipTypeMetadata → generate.ipTypeMetadata      | IPTypeRegistryService                     | generate→types
 * types.ipTypeMetadata → root.ipTypeMetadata          | IPTypeRegistryService                     | root→types
 * inspect.nodeDescriptors → compose.nodeDescriptors   | NodeRegistryService                       | compose→inspect
 * inspect.nodeDescriptors → execute.nodeDescriptors   | NodeRegistryService                       | execute→inspect
 * inspect.nodeDescriptors → generate.nodeDescriptors  | NodeRegistryService                       | generate→inspect
 * inspect.nodeDescriptors → root.nodeDescriptors      | NodeRegistryService                       | root→inspect
 * persist.serializedOutput → generate.serializedOutput| FlowGraphPersistenceService               | generate→persist
 * persist.fileService → root.fileService              | FlowGraphPersistenceService               | root→persist
 * persist.graphNodeTemplates → root.graphNodeTemplates| GraphNodeTemplateService                  | root→persist
 * compose.graphState → root.graphState                | GraphCompositionService, UndoRedoService  | root→compose
 * execute.executionState → root.executionState        | RuntimeExecutionService                   | root→execute
 * execute.animations → root.animations                | ConnectionAnimationProvider               | root→execute
 * generate.generatedOutput → root.generatedOutput     | CodeGenerationService                     | root→generate
 *
 * Total connections: 15 (DAG — no cycles)
 * Former cycles eliminated by extracting flowGraph-types:
 *   - persist.ipRepository → inspect (FileIPTypeRepository now internal to types)
 *   - generate.ipTypeFiles → inspect (IPTypeFileGenerator now internal to types)
 *
 * File movements (from 5-module to 6-module partition):
 *   From inspect → types: IPTypeDiscovery.kt, IPTypeRegistry.kt, IPProperty.kt,
 *                          IPPropertyMeta.kt, IPTypeFileMeta.kt, IPTypeMigration.kt
 *   From persist → types: FileIPTypeRepository.kt, SerializableIPType.kt
 *   From generate → types: IPTypeFileGenerator.kt
 *
 * Revised file counts: types:9, inspect:13, persist:8, compose:10, execute:7,
 *                       generate:46, root:27 = 120
 */

import io.codenode.fbpdsl.dsl.*
import io.codenode.fbpdsl.model.*

val graph = flowGraph("Target Architecture", version = "2.0.0", description = "Vertical-slice module decomposition with dedicated type catalog module — six workflow modules plus composition root") {
    // Target Platforms
    targetPlatform(FlowGraph.TargetPlatform.KMP_DESKTOP)
    targetPlatform(FlowGraph.TargetPlatform.KMP_ANDROID)
    targetPlatform(FlowGraph.TargetPlatform.KMP_IOS)

    // Nodes (GraphNodes — empty containers representing target vertical-slice modules)

    val types = graphNode("flowGraph-types") {
        description = "IP type lifecycle: discovery, registry, repository, file generation, migration. 9 files. Consolidates IP type concerns from inspect, persist, and generate."
        position(100.0, 100.0)
        exposeInput("filesystemPaths", String::class)
        exposeInput("classpathEntries", String::class)
        exposeOutput("ipTypeMetadata", String::class)
    }

    val inspect = graphNode("flowGraph-inspect") {
        description = "Understanding available components: node palette, filesystem node scanning, CodeNode text editor. 13 files."
        position(100.0, 400.0)
        exposeInput("filesystemPaths", String::class)
        exposeInput("classpathEntries", String::class)
        exposeOutput("nodeDescriptors", String::class)
    }

    val persist = graphNode("flowGraph-persist") {
        description = "Saving and loading flow graphs: FlowGraphSerializer, FlowKtParser, template registry, file I/O. 8 files."
        position(400.0, 100.0)
        exposeInput("flowGraph", String::class)
        exposeInput("ipTypeMetadata", String::class)
        exposeOutput("serializedOutput", String::class)
        exposeOutput("fileService", String::class)
        exposeOutput("graphNodeTemplates", String::class)
    }

    val compose = graphNode("flowGraph-compose") {
        description = "Building a flow graph interactively: graph mutations, port connections, validation, undo/redo. 10 files."
        position(400.0, 400.0)
        exposeInput("nodeDescriptors", String::class)
        exposeInput("ipTypeMetadata", String::class)
        exposeOutput("graphState", String::class)
    }

    val execute = graphNode("flowGraph-execute") {
        description = "Running and observing flow graphs: runtime pipeline, execution control, animation, debugging. 7 files. Absorbs circuitSimulator (5 files)."
        position(400.0, 600.0)
        exposeInput("flowGraph", String::class)
        exposeInput("nodeDescriptors", String::class)
        exposeOutput("executionState", String::class)
        exposeOutput("animations", String::class)
        exposeOutput("debugSnapshots", String::class)
    }

    val generate = graphNode("flowGraph-generate") {
        description = "Producing deployable code: all generators, templates, validators, module save. 46 files. Absorbs kotlinCompiler (38 files)."
        position(700.0, 400.0)
        exposeInput("flowGraph", String::class)
        exposeInput("serializedOutput", String::class)
        exposeInput("nodeDescriptors", String::class)
        exposeInput("ipTypeMetadata", String::class)
        exposeOutput("generatedOutput", String::class)
    }

    val root = graphNode("graphEditor") {
        description = "Composition root: Compose UI, ViewModels, renderers, DI wiring. 27 files. Orchestrates all slices."
        position(1000.0, 400.0)
        exposeInput("graphState", String::class)
        exposeInput("fileService", String::class)
        exposeInput("graphNodeTemplates", String::class)
        exposeInput("executionState", String::class)
        exposeInput("animations", String::class)
        exposeInput("generatedOutput", String::class)
        exposeInput("nodeDescriptors", String::class)
        exposeInput("ipTypeMetadata", String::class)
    }

    // Connections: types → consumers (IP type metadata for all modules that need type info)
    types.output("ipTypeMetadata") connect compose.input("ipTypeMetadata")
    types.output("ipTypeMetadata") connect persist.input("ipTypeMetadata")
    types.output("ipTypeMetadata") connect generate.input("ipTypeMetadata")
    types.output("ipTypeMetadata") connect root.input("ipTypeMetadata")

    // Connections: inspect → consumers (node descriptors only — IP type data now from types)
    inspect.output("nodeDescriptors") connect compose.input("nodeDescriptors")
    inspect.output("nodeDescriptors") connect execute.input("nodeDescriptors")
    inspect.output("nodeDescriptors") connect generate.input("nodeDescriptors")
    inspect.output("nodeDescriptors") connect root.input("nodeDescriptors")

    // Connections: persist → consumers
    persist.output("serializedOutput") connect generate.input("serializedOutput")
    persist.output("fileService") connect root.input("fileService")
    persist.output("graphNodeTemplates") connect root.input("graphNodeTemplates")

    // Connections: compose → root
    compose.output("graphState") connect root.input("graphState")

    // Connections: execute → root
    execute.output("executionState") connect root.input("executionState")
    execute.output("animations") connect root.input("animations")

    // Connections: generate → root
    generate.output("generatedOutput") connect root.input("generatedOutput")
}
