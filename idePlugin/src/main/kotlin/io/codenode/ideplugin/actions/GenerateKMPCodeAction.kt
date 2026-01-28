/*
 * Generate KMP Code Action
 * IDE action for generating Kotlin Multiplatform code from flow graphs
 * License: Apache 2.0
 */

package io.codenode.ideplugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.panel
import io.codenode.fbpdsl.model.FlowGraph
import io.codenode.ideplugin.services.CodeGenerationService
import io.codenode.ideplugin.services.FlowGraphManager
import io.codenode.ideplugin.services.GenerationErrorType
import io.codenode.kotlincompiler.generator.BuildScriptGenerator
import java.io.File
import javax.swing.JComponent

/**
 * IDE action that generates Kotlin Multiplatform code from the current flow graph.
 *
 * This action:
 * 1. Retrieves the current flow graph from the editor or project
 * 2. Shows a dialog for target platform selection
 * 3. Validates the graph before generation
 * 4. Generates KMP code using KotlinCodeGenerator
 * 5. Generates build scripts using BuildScriptGenerator
 * 6. Validates licenses using LicenseValidator
 * 7. Writes generated files to the selected output directory
 */
class GenerateKMPCodeAction : AnAction() {

    private val logger = Logger.getInstance(GenerateKMPCodeAction::class.java)

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val flowGraph = getFlowGraphFromContext(e) ?: run {
            Messages.showWarningDialog(
                project,
                "No flow graph found. Please open a flow graph file first.",
                "Generate KMP Code"
            )
            return
        }

        // Show target selection dialog
        val dialog = TargetSelectionDialog(project, flowGraph)
        if (!dialog.showAndGet()) {
            return // User cancelled
        }

        // Get selected targets and output directory
        val config = dialog.getTargetConfig()
        val outputDir = selectOutputDirectory(project) ?: return

        // Use the CodeGenerationService
        val codeGenerationService = CodeGenerationService.getInstance(project)

        codeGenerationService.generateKMPCodeAsync(
            flowGraph = flowGraph,
            config = config,
            outputDir = File(outputDir.path)
        ) { result ->
            if (result.success) {
                Messages.showInfoMessage(
                    project,
                    "KMP code generated successfully!\n\n" +
                        "Output: ${result.outputPath}\n" +
                        "Files: ${result.fileCount} files\n" +
                        "Targets: ${result.targetSummary}",
                    "Generation Complete"
                )
            } else {
                val title = when (result.errorType) {
                    GenerationErrorType.LICENSE_VIOLATION -> "License Violation"
                    GenerationErrorType.VALIDATION_FAILED -> "Validation Error"
                    else -> "Generation Error"
                }
                Messages.showErrorDialog(
                    project,
                    result.errorMessage ?: "Unknown error occurred",
                    title
                )
            }
        }
    }

    override fun update(e: AnActionEvent) {
        // Enable action only when a project is open
        val project = e.project
        e.presentation.isEnabledAndVisible = project != null
    }

    /**
     * Retrieves the flow graph from the current context.
     */
    private fun getFlowGraphFromContext(e: AnActionEvent): FlowGraph? {
        val project = e.project ?: return null
        val flowGraphManager = FlowGraphManager.getInstance(project)

        // First, check if there's an active graph in the manager
        val activeGraph = flowGraphManager.getActiveGraph()
        if (activeGraph != null) {
            logger.info("Using active graph from FlowGraphManager: ${activeGraph.name}")
            return activeGraph
        }

        // Try to get from the current file in editor
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
        if (virtualFile?.name?.endsWith(".flow.kts") == true) {
            logger.info("Loading flow graph from file: ${virtualFile.path}")
            return try {
                val graph = flowGraphManager.loadFlowGraph(virtualFile)
                flowGraphManager.setActiveGraph(graph, virtualFile.path)
                graph
            } catch (ex: Exception) {
                logger.warn("Failed to load flow graph: ${ex.message}")
                null
            }
        }

        // Try to get from PSI file
        val psiFile = e.getData(CommonDataKeys.PSI_FILE)
        if (psiFile?.name?.endsWith(".flow.kts") == true) {
            val file = psiFile.virtualFile
            if (file != null) {
                logger.info("Loading flow graph from PSI file: ${file.path}")
                return try {
                    val graph = flowGraphManager.loadFlowGraph(file)
                    flowGraphManager.setActiveGraph(graph, file.path)
                    graph
                } catch (ex: Exception) {
                    logger.warn("Failed to load flow graph from PSI: ${ex.message}")
                    null
                }
            }
        }

        logger.debug("No flow graph found in current context")
        return null
    }

    /**
     * Shows directory chooser for output location.
     */
    private fun selectOutputDirectory(project: Project): VirtualFile? {
        val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
            .withTitle("Select Output Directory")
            .withDescription("Choose where to generate the KMP project")

        return FileChooser.chooseFile(descriptor, project, project.baseDir)
    }

    /**
     * Dialog for selecting target platforms.
     */
    private class TargetSelectionDialog(
        project: Project,
        private val flowGraph: FlowGraph
    ) : DialogWrapper(project) {

        private val androidCheckbox = JBCheckBox("Android", flowGraph.targetsPlatform(FlowGraph.TargetPlatform.KMP_ANDROID))
        private val iosCheckbox = JBCheckBox("iOS", flowGraph.targetsPlatform(FlowGraph.TargetPlatform.KMP_IOS))
        private val desktopCheckbox = JBCheckBox("Desktop (JVM)", flowGraph.targetsPlatform(FlowGraph.TargetPlatform.KMP_DESKTOP))
        private val jsCheckbox = JBCheckBox("Web (JavaScript)", flowGraph.targetsPlatform(FlowGraph.TargetPlatform.KMP_WEB))
        private val wasmCheckbox = JBCheckBox("Web (Wasm)", flowGraph.targetsPlatform(FlowGraph.TargetPlatform.KMP_WASM))

        init {
            title = "Generate KMP Code"
            init()
        }

        override fun createCenterPanel(): JComponent {
            return panel {
                group("Target Platforms") {
                    row {
                        cell(androidCheckbox)
                    }
                    row {
                        cell(iosCheckbox)
                    }
                    row {
                        cell(desktopCheckbox)
                    }
                    row {
                        cell(jsCheckbox)
                    }
                    row {
                        cell(wasmCheckbox)
                    }
                }
                group("Project Info") {
                    row("Name:") {
                        label(flowGraph.name)
                    }
                    row("Version:") {
                        label(flowGraph.version)
                    }
                    row("Nodes:") {
                        label("${flowGraph.getAllCodeNodes().size}")
                    }
                }
            }
        }

        fun getTargetConfig(): BuildScriptGenerator.TargetConfig {
            return BuildScriptGenerator.TargetConfig(
                android = androidCheckbox.isSelected,
                ios = iosCheckbox.isSelected,
                desktop = desktopCheckbox.isSelected,
                js = jsCheckbox.isSelected,
                wasm = wasmCheckbox.isSelected
            )
        }
    }
}
