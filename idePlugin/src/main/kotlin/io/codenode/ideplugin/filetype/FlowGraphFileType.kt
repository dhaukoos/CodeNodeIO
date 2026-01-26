/*
 * Flow Graph File Type
 * Registers .flow.kts as a recognized file type in JetBrains IDEs
 * License: Apache 2.0
 */

package io.codenode.ideplugin.filetype

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.NlsSafe
import javax.swing.Icon

/**
 * File type definition for CodeNodeIO Flow Graph files (.flow.kts).
 *
 * Flow graphs are stored as Kotlin Script files using the FBP DSL syntax.
 * This file type enables:
 * - Custom icon display in project view
 * - Association with the graph editor
 * - Recognition by IDE actions and services
 */
object FlowGraphFileType : FileType {

    /**
     * Unique identifier for this file type.
     */
    @NlsSafe
    override fun getName(): String = "FlowGraph"

    /**
     * Human-readable description shown in file type settings.
     */
    @NlsContexts.Label
    override fun getDescription(): String = "CodeNodeIO Flow Graph (FBP DSL)"

    /**
     * Default file extension (without the dot).
     */
    override fun getDefaultExtension(): String = "flow.kts"

    /**
     * Icon displayed in project view and tabs.
     */
    override fun getIcon(): Icon = FLOW_GRAPH_ICON

    /**
     * Whether this file type is binary.
     */
    override fun isBinary(): Boolean = false

    /**
     * Whether this file type is read-only.
     */
    override fun isReadOnly(): Boolean = false
}

/**
 * Custom icon for flow graph files.
 * Uses a flow/graph-like icon from the platform.
 */
private val FLOW_GRAPH_ICON: Icon by lazy {
    try {
        // Try to load custom icon
        IconLoader.getIcon("/icons/flowgraph.svg", FlowGraphFileType::class.java)
    } catch (e: Exception) {
        // Fall back to platform icon
        com.intellij.icons.AllIcons.FileTypes.Diagram
    }
}
