/*
 * Flow Graph: Target Architecture
 * Version: 1.0.0
 * Description: Vertical-slice decomposition of graphEditor, kotlinCompiler, and circuitSimulator into five workflow-oriented modules plus composition root
 * Generated: 2026-04-02
 *
 * Cross-validation with MIGRATION.md:
 *
 * Connection                                          | MIGRATION.md Interface                    | Seam Boundary
 * --------------------------------------------------- | ----------------------------------------- | --------------------
 * inspect.nodeDescriptors → compose.nodeRegistry      | NodeRegistryService                       | compose→inspect (4)
 * inspect.ipTypeMetadata → compose.ipTypeRegistry     | IPTypeRegistryService                     | compose→inspect (4)
 * inspect.nodeDescriptors → execute.nodeRegistry      | NodeRegistryService                       | execute→inspect (1)
 * inspect.nodeDescriptors → generate.nodeRegistry     | NodeRegistryService                       | generate→inspect (3)
 * inspect.ipTypeMetadata → generate.ipTypeRegistry    | IPTypeRegistryService                     | generate→inspect (3)
 * inspect.ipTypeMetadata → persist.ipTypeRegistry     | IPTypeRegistryService                     | persist→inspect (2)
 * persist.serializedOutput → generate.serializer      | FlowGraphPersistenceService               | generate→persist (1)
 * persist.ipRepository → inspect.ipRepository         | FileIPTypeRepository (inspect→persist)     | inspect→persist (2)
 * generate.ipTypeFiles → inspect.generatedTypes       | IPTypeGenerationService                   | inspect→generate (1)
 * compose.graphState → root.graphState                | GraphCompositionService, UndoRedoService  | root→compose (8)
 * persist.fileService → root.fileService              | FlowGraphPersistenceService               | root→persist (2)
 * persist.templateService → root.templateService      | GraphNodeTemplateService                  | root→persist (2)
 * execute.executionState → root.executionState        | RuntimeExecutionService                   | root→execute (via UI)
 * execute.animations → root.animations                | ConnectionAnimationProvider               | root→execute (1 seam)
 * generate.generatedOutput → root.generatedOutput     | CodeGenerationService                     | root→generate (via UI)
 * inspect.nodeDescriptors → root.nodeDescriptors      | NodeRegistryService                       | root→inspect (3)
 * inspect.ipTypeMetadata → root.ipTypeMetadata        | IPTypeRegistryService                     | root→inspect (3)
 *
 * Total connections: 17 (matches 28 cross-boundary seams consolidated into interface-level flows)
 * All connections verified against MIGRATION.md Interface Summary table
 */

import io.codenode.fbpdsl.dsl.*
import io.codenode.fbpdsl.model.*

val graph = flowGraph("Target Architecture", version = "1.0.0", description = "Vertical-slice module decomposition — five workflow modules plus composition root") {
    // Target Platforms
    targetPlatform(FlowGraph.TargetPlatform.KMP_DESKTOP)
    targetPlatform(FlowGraph.TargetPlatform.KMP_ANDROID)
    targetPlatform(FlowGraph.TargetPlatform.KMP_IOS)

    // Nodes
    val inspect = codeNode("flowGraph-inspect", nodeType = "SOURCE") {
        description = "Understanding available components: node palette, IP type registry, filesystem scanner, CodeNode text editor. 19 files. Absorbs no external modules."
        position(100.0, 300.0)
        input("filesystemPaths", String::class)
        input("classpathEntries", String::class)
        input("ipRepository", String::class)
        input("generatedTypes", String::class)
        output("nodeDescriptors", String::class)
        output("ipTypeMetadata", String::class)
        output("ipRepository", String::class)
    }

    val persist = codeNode("flowGraph-persist") {
        description = "Saving and loading flow graphs: FlowGraphSerializer, FlowKtParser, template registry, file I/O. 10 files. Absorbs no external modules."
        position(400.0, 100.0)
        input("flowGraph", String::class)
        input("ipTypeRegistry", String::class)
        output("serializedOutput", String::class)
        output("fileService", String::class)
        output("templateService", String::class)
        output("ipRepository", String::class)
    }

    val compose = codeNode("flowGraph-compose") {
        description = "Building a flow graph interactively: graph mutations, port connections, validation, undo/redo. 10 files. Absorbs no external modules."
        position(400.0, 300.0)
        input("nodeRegistry", String::class)
        input("ipTypeRegistry", String::class)
        output("graphState", String::class)
    }

    val execute = codeNode("flowGraph-execute") {
        description = "Running and observing flow graphs: runtime pipeline, execution control, animation, debugging. 7 files. Absorbs circuitSimulator (5 files)."
        position(400.0, 500.0)
        input("flowGraph", String::class)
        input("nodeRegistry", String::class)
        output("executionState", String::class)
        output("animations", String::class)
        output("debugSnapshots", String::class)
    }

    val generate = codeNode("flowGraph-generate") {
        description = "Producing deployable code: all generators, templates, validators, module save. 47 files. Absorbs kotlinCompiler (38 files)."
        position(700.0, 300.0)
        input("flowGraph", String::class)
        input("serializer", String::class)
        input("nodeRegistry", String::class)
        input("ipTypeRegistry", String::class)
        output("generatedOutput", String::class)
        output("ipTypeFiles", String::class)
    }

    val root = codeNode("graphEditor", nodeType = "SINK") {
        description = "Composition root: Compose UI, ViewModels, renderers, DI wiring. 27 files. Orchestrates all slices."
        position(1000.0, 300.0)
        input("graphState", String::class)
        input("fileService", String::class)
        input("templateService", String::class)
        input("executionState", String::class)
        input("animations", String::class)
        input("generatedOutput", String::class)
        input("nodeDescriptors", String::class)
        input("ipTypeMetadata", String::class)
    }

    // Connections: inspect → consumers (inspect is the most depended-upon module)
    inspect.output("nodeDescriptors") connect compose.input("nodeRegistry")
    inspect.output("ipTypeMetadata") connect compose.input("ipTypeRegistry")
    inspect.output("nodeDescriptors") connect execute.input("nodeRegistry")
    inspect.output("nodeDescriptors") connect generate.input("nodeRegistry")
    inspect.output("ipTypeMetadata") connect generate.input("ipTypeRegistry")
    inspect.output("ipTypeMetadata") connect persist.input("ipTypeRegistry")
    inspect.output("nodeDescriptors") connect root.input("nodeDescriptors")
    inspect.output("ipTypeMetadata") connect root.input("ipTypeMetadata")

    // Connections: persist → consumers
    persist.output("serializedOutput") connect generate.input("serializer")
    persist.output("fileService") connect root.input("fileService")
    persist.output("templateService") connect root.input("templateService")

    // Connections: persist ↔ inspect (bidirectional)
    persist.output("ipRepository") connect inspect.input("ipRepository")

    // Connections: generate → inspect (bidirectional)
    generate.output("ipTypeFiles") connect inspect.input("generatedTypes")

    // Connections: compose → root
    compose.output("graphState") connect root.input("graphState")

    // Connections: execute → root
    execute.output("executionState") connect root.input("executionState")
    execute.output("animations") connect root.input("animations")

    // Connections: generate → root
    generate.output("generatedOutput") connect root.input("generatedOutput")
}
