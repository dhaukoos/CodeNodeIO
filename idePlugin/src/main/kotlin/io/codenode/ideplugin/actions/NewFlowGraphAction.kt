/*
 * New Flow Graph Action
 * IDE action for creating new flow graph files
 * License: Apache 2.0
 */

package io.codenode.ideplugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.text
import io.codenode.fbpdsl.dsl.flowGraph
import io.codenode.grapheditor.serialization.FlowGraphSerializer
import io.codenode.ideplugin.services.FlowGraphManager
import java.io.File
import javax.swing.JComponent

/**
 * IDE action for creating a new flow graph file.
 *
 * This action:
 * 1. Shows a dialog for entering graph name and selecting location
 * 2. Creates a new .flow.kts file with a basic flow graph template
 * 3. Opens the file in the editor
 * 4. Activates the Graph Editor tool window
 */
class NewFlowGraphAction : AnAction() {

    private val logger = Logger.getInstance(NewFlowGraphAction::class.java)

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        // Get the current directory context (for default save location)
        val currentDir = getCurrentDirectory(e, project)

        // Show the new flow graph dialog
        val dialog = NewFlowGraphDialog(project, currentDir)
        if (!dialog.showAndGet()) {
            return // User cancelled
        }

        val config = dialog.getConfig()
        createFlowGraph(project, config)
    }

    override fun update(e: AnActionEvent) {
        // Enable action only when a project is open
        e.presentation.isEnabledAndVisible = e.project != null
    }

    /**
     * Gets the current directory for the new file.
     */
    private fun getCurrentDirectory(e: AnActionEvent, project: Project): VirtualFile? {
        // Try to get from selected file/directory
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
        if (virtualFile != null) {
            return if (virtualFile.isDirectory) virtualFile else virtualFile.parent
        }

        // Fall back to project base directory
        return project.baseDir
    }

    /**
     * Creates the flow graph file.
     */
    private fun createFlowGraph(project: Project, config: NewFlowGraphConfig) {
        ApplicationManager.getApplication().runWriteAction {
            try {
                // Create the flow graph
                val graph = flowGraph(
                    name = config.graphName,
                    version = config.version
                ) {
                    // Empty graph - user will add nodes
                }

                // Serialize to DSL format
                val content = FlowGraphSerializer.serialize(graph)

                // Determine file path
                val fileName = "${config.fileName}.flow.kts"
                val targetDir = config.targetDirectory
                val filePath = File(targetDir.path, fileName).absolutePath

                // Create the file
                val file = File(filePath)
                if (file.exists()) {
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showErrorDialog(
                            project,
                            "A file named '$fileName' already exists in this directory.",
                            "File Exists"
                        )
                    }
                    return@runWriteAction
                }

                file.writeText(content)
                logger.info("Created new flow graph file: $filePath")

                // Refresh VFS to make the file visible
                targetDir.refresh(false, false)

                // Find the created file in VFS
                val virtualFile = VirtualFileManager.getInstance()
                    .refreshAndFindFileByUrl("file://$filePath")

                if (virtualFile != null) {
                    // Set as active graph
                    val flowGraphManager = FlowGraphManager.getInstance(project)
                    flowGraphManager.setActiveGraph(graph, filePath)

                    ApplicationManager.getApplication().invokeLater {
                        // Open the file in the editor
                        FileEditorManager.getInstance(project).openFile(virtualFile, true)

                        // Show success message
                        Messages.showInfoMessage(
                            project,
                            "Flow graph '${config.graphName}' created successfully.\n\n" +
                                "File: $fileName\n" +
                                "Location: ${targetDir.path}",
                            "Flow Graph Created"
                        )
                    }
                }
            } catch (ex: Exception) {
                logger.error("Failed to create flow graph", ex)
                ApplicationManager.getApplication().invokeLater {
                    Messages.showErrorDialog(
                        project,
                        "Failed to create flow graph: ${ex.message}",
                        "Error"
                    )
                }
            }
        }
    }

    /**
     * Dialog for configuring new flow graph.
     */
    private class NewFlowGraphDialog(
        project: Project,
        private val defaultDirectory: VirtualFile?
    ) : DialogWrapper(project) {

        private var graphName = "NewFlowGraph"
        private var fileName = "new_graph"
        private var version = "1.0.0"
        private var targetDirectory: VirtualFile? = defaultDirectory

        init {
            title = "New Flow Graph"
            init()
        }

        override fun createCenterPanel(): JComponent {
            return panel {
                row("Graph Name:") {
                    textField()
                        .bindText(::graphName)
                        .comment("Display name for the flow graph")
                        .focused()
                }
                row("File Name:") {
                    textField()
                        .bindText(::fileName)
                        .comment("File will be saved as <name>.flow.kts")
                }
                row("Version:") {
                    textField()
                        .bindText(::version)
                        .comment("Semantic version (e.g., 1.0.0)")
                }
                row("Location:") {
                    label(targetDirectory?.path ?: "Project root")
                }
            }
        }

        override fun doValidate(): ValidationInfo? {
            if (graphName.isBlank()) {
                return ValidationInfo("Graph name cannot be empty")
            }
            if (fileName.isBlank()) {
                return ValidationInfo("File name cannot be empty")
            }
            if (!fileName.matches(Regex("^[a-zA-Z][a-zA-Z0-9_-]*$"))) {
                return ValidationInfo("File name must start with a letter and contain only letters, numbers, underscores, and hyphens")
            }
            if (!version.matches(Regex("^\\d+\\.\\d+\\.\\d+(-[a-zA-Z0-9]+)?$"))) {
                return ValidationInfo("Version must be in format X.Y.Z (e.g., 1.0.0)")
            }
            return null
        }

        fun getConfig(): NewFlowGraphConfig {
            return NewFlowGraphConfig(
                graphName = graphName,
                fileName = fileName,
                version = version,
                targetDirectory = targetDirectory ?: throw IllegalStateException("No target directory")
            )
        }
    }

    /**
     * Configuration for new flow graph creation.
     */
    private data class NewFlowGraphConfig(
        val graphName: String,
        val fileName: String,
        val version: String,
        val targetDirectory: VirtualFile
    )
}
