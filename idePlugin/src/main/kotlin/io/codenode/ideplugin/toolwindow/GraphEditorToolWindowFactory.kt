/*
 * Graph Editor Tool Window Factory
 * Creates the visual graph editor tool window in JetBrains IDEs
 * License: Apache 2.0
 */

package io.codenode.ideplugin.toolwindow

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

/**
 * Factory for creating the CodeNodeIO Graph Editor tool window.
 *
 * The tool window provides a visual interface for editing flow graphs.
 * It integrates with the FlowGraphManager service to load and save graphs,
 * and responds to file selection changes in the editor.
 */
class GraphEditorToolWindowFactory : ToolWindowFactory {

    private val logger = Logger.getInstance(GraphEditorToolWindowFactory::class.java)

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        logger.info("Creating Graph Editor tool window content")

        val graphEditorPanel = GraphEditorToolWindowPanel(project)

        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(
            graphEditorPanel,
            "Graph Editor",
            false
        )
        content.setDisposer(graphEditorPanel)

        toolWindow.contentManager.addContent(content)

        // Listen for file selection changes
        project.messageBus.connect(graphEditorPanel).subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
                    if (file.name.endsWith(".flow.kts")) {
                        logger.debug("Flow graph file opened: ${file.name}")
                        graphEditorPanel.loadGraph(file)
                    }
                }

                override fun selectionChanged(event: FileEditorManagerEvent) {
                    val file = event.newFile
                    if (file?.name?.endsWith(".flow.kts") == true) {
                        logger.debug("Selection changed to flow graph: ${file.name}")
                        graphEditorPanel.loadGraph(file)
                    }
                }
            }
        )

        // Load currently selected file if it's a flow graph
        val selectedFile = FileEditorManager.getInstance(project).selectedFiles.firstOrNull()
        if (selectedFile?.name?.endsWith(".flow.kts") == true) {
            graphEditorPanel.loadGraph(selectedFile)
        }
    }

    override fun shouldBeAvailable(project: Project): Boolean {
        // Tool window is always available when the plugin is installed
        return true
    }
}
