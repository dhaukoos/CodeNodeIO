/*
 * Open Graph Editor Action
 * IDE action for opening the visual graph editor tool window
 * License: Apache 2.0
 */

package io.codenode.ideplugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindowManager
import io.codenode.ideplugin.services.FlowGraphManager

/**
 * IDE action for opening the Graph Editor tool window.
 *
 * This action:
 * 1. Opens the CodeNodeIO Graph Editor tool window
 * 2. If a .flow.kts file is currently selected, loads it into the editor
 * 3. If no flow graph is selected, shows an empty editor with instructions
 */
class OpenGraphEditorAction : AnAction() {

    private val logger = Logger.getInstance(OpenGraphEditorAction::class.java)

    companion object {
        const val TOOL_WINDOW_ID = "CodeNodeIO Graph Editor"
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        // Get the tool window
        val toolWindowManager = ToolWindowManager.getInstance(project)
        val toolWindow = toolWindowManager.getToolWindow(TOOL_WINDOW_ID)

        if (toolWindow == null) {
            logger.error("Graph Editor tool window not found")
            Messages.showErrorDialog(
                project,
                "The Graph Editor tool window could not be found.\n\n" +
                    "Please ensure the CodeNodeIO plugin is properly installed.",
                "Tool Window Not Found"
            )
            return
        }

        // Activate the tool window
        toolWindow.show {
            logger.info("Graph Editor tool window activated")

            // Try to load the current flow graph file
            val flowGraphFile = getFlowGraphFromContext(e, project)
            if (flowGraphFile != null) {
                loadFlowGraphIntoEditor(project, flowGraphFile)
            }
        }
    }

    override fun update(e: AnActionEvent) {
        // Enable action only when a project is open
        e.presentation.isEnabledAndVisible = e.project != null
    }

    /**
     * Gets the flow graph file from the current context.
     */
    private fun getFlowGraphFromContext(e: AnActionEvent, project: Project): VirtualFile? {
        // Check if current file is a flow graph
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
        if (virtualFile?.name?.endsWith(".flow.kts") == true) {
            return virtualFile
        }

        // Check if current editor has a flow graph
        val selectedFile = FileEditorManager.getInstance(project).selectedFiles.firstOrNull()
        if (selectedFile?.name?.endsWith(".flow.kts") == true) {
            return selectedFile
        }

        // Check FlowGraphManager for active graph
        val flowGraphManager = FlowGraphManager.getInstance(project)
        val activeFilePath = flowGraphManager.getActiveFilePath()
        if (activeFilePath != null) {
            val file = project.baseDir?.fileSystem?.findFileByPath(activeFilePath)
            if (file != null) {
                return file
            }
        }

        return null
    }

    /**
     * Loads a flow graph file into the Graph Editor.
     */
    private fun loadFlowGraphIntoEditor(project: Project, file: VirtualFile) {
        try {
            val flowGraphManager = FlowGraphManager.getInstance(project)
            val graph = flowGraphManager.loadFlowGraph(file)
            flowGraphManager.setActiveGraph(graph, file.path)
            logger.info("Loaded flow graph '${graph.name}' into Graph Editor")
        } catch (ex: Exception) {
            logger.warn("Failed to load flow graph: ${ex.message}")
            // Don't show error - the tool window will handle empty state
        }
    }
}

/**
 * Action to open the Graph Editor for the currently selected file.
 *
 * This action is specifically for the context menu on .flow.kts files.
 */
class OpenCurrentFileInGraphEditorAction : AnAction() {

    private val logger = Logger.getInstance(OpenCurrentFileInGraphEditorAction::class.java)

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        if (!file.name.endsWith(".flow.kts")) {
            Messages.showWarningDialog(
                project,
                "Please select a .flow.kts file to open in the Graph Editor.",
                "Invalid File"
            )
            return
        }

        // Load the flow graph
        val flowGraphManager = FlowGraphManager.getInstance(project)
        try {
            val graph = flowGraphManager.loadFlowGraph(file)
            flowGraphManager.setActiveGraph(graph, file.path)
        } catch (ex: Exception) {
            Messages.showErrorDialog(
                project,
                "Failed to load flow graph: ${ex.message}",
                "Load Error"
            )
            return
        }

        // Open the tool window
        val toolWindowManager = ToolWindowManager.getInstance(project)
        val toolWindow = toolWindowManager.getToolWindow(OpenGraphEditorAction.TOOL_WINDOW_ID)

        if (toolWindow != null) {
            toolWindow.show {
                logger.info("Opened '${file.name}' in Graph Editor")
            }
        }
    }

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible =
            e.project != null && file?.name?.endsWith(".flow.kts") == true
    }
}
