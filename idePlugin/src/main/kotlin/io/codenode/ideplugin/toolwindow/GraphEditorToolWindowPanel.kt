/*
 * Graph Editor Tool Window Panel
 * Swing panel that hosts the graph editor content
 * License: Apache 2.0
 */

package io.codenode.ideplugin.toolwindow

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import io.codenode.fbpdsl.model.FlowGraph
import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.Connection
import io.codenode.ideplugin.services.FlowGraphManager
import io.codenode.ideplugin.services.FlowGraphChangeListener
import java.awt.*
import java.awt.event.*
import java.awt.geom.Path2D
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D
import javax.swing.*

/**
 * Panel that hosts the visual graph editor within the IDE tool window.
 *
 * This panel provides:
 * - A canvas for rendering flow graph nodes and connections
 * - Toolbar with common actions (zoom, fit, refresh)
 * - Integration with FlowGraphManager for loading/saving
 * - Mouse interaction for selection and navigation
 */
class GraphEditorToolWindowPanel(
    private val project: Project
) : SimpleToolWindowPanel(true, true), Disposable, FlowGraphChangeListener {

    private val logger = Logger.getInstance(GraphEditorToolWindowPanel::class.java)

    private val canvas = GraphCanvas()
    private var currentFile: VirtualFile? = null
    private var currentGraph: FlowGraph? = null

    init {
        // Setup toolbar
        val toolbar = createToolbar()
        setToolbar(toolbar.component)

        // Setup canvas with scroll pane
        val scrollPane = JBScrollPane(canvas).apply {
            border = BorderFactory.createEmptyBorder()
            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
            verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
        }
        setContent(scrollPane)

        // Register as change listener
        FlowGraphManager.getInstance(project).addChangeListener(this)
    }

    /**
     * Creates the toolbar with graph editor actions.
     */
    private fun createToolbar(): ActionToolbar {
        val actionGroup = DefaultActionGroup().apply {
            add(ActionManager.getInstance().getAction("CodeNodeIO.ValidateGraph")
                ?: createPlaceholderAction("Validate"))
            addSeparator()
            add(ActionManager.getInstance().getAction("CodeNodeIO.GenerateKMPCode")
                ?: createPlaceholderAction("Generate KMP"))
            add(ActionManager.getInstance().getAction("CodeNodeIO.GenerateGoCode")
                ?: createPlaceholderAction("Generate Go"))
        }

        return ActionManager.getInstance().createActionToolbar(
            ActionPlaces.TOOLWINDOW_CONTENT,
            actionGroup,
            true
        ).apply {
            targetComponent = this@GraphEditorToolWindowPanel
        }
    }

    /**
     * Creates a placeholder action for missing actions.
     */
    private fun createPlaceholderAction(name: String): com.intellij.openapi.actionSystem.AnAction {
        return object : com.intellij.openapi.actionSystem.AnAction(name) {
            override fun actionPerformed(e: com.intellij.openapi.actionSystem.AnActionEvent) {
                // Placeholder - action not yet implemented
            }

            override fun update(e: com.intellij.openapi.actionSystem.AnActionEvent) {
                e.presentation.isEnabled = currentGraph != null
            }
        }
    }

    /**
     * Loads a flow graph from a file.
     */
    fun loadGraph(file: VirtualFile) {
        logger.info("Loading graph from: ${file.path}")

        currentFile = file

        try {
            val manager = FlowGraphManager.getInstance(project)
            val graph = manager.loadFlowGraph(file)
            currentGraph = graph

            // Update the manager's active graph
            manager.setActiveGraph(graph, file.path)

            // Update canvas
            canvas.setGraph(graph)
            canvas.repaint()

            logger.info("Graph loaded successfully: ${graph.name}")
        } catch (e: Exception) {
            logger.error("Failed to load graph: ${e.message}", e)
            currentGraph = null
            canvas.setGraph(null)
            canvas.setError("Failed to load graph: ${e.message}")
            canvas.repaint()
        }
    }

    /**
     * Refreshes the current graph from file.
     */
    fun refresh() {
        currentFile?.let { loadGraph(it) }
    }

    /**
     * Gets the currently loaded graph.
     */
    fun getGraph(): FlowGraph? = currentGraph

    /**
     * Gets the currently loaded file.
     */
    fun getFile(): VirtualFile? = currentFile

    override fun onGraphChanged(graph: FlowGraph, filePath: String) {
        if (filePath == currentFile?.path) {
            currentGraph = graph
            canvas.setGraph(graph)
            canvas.repaint()
        }
    }

    override fun dispose() {
        FlowGraphManager.getInstance(project).removeChangeListener(this)
        currentGraph = null
        currentFile = null
    }

    /**
     * Canvas component for rendering the flow graph.
     */
    private inner class GraphCanvas : JPanel() {

        private var graph: FlowGraph? = null
        private var errorMessage: String? = null
        private var selectedNodeId: String? = null

        // View transformation
        private var zoom = 1.0
        private var panX = 0.0
        private var panY = 0.0

        // Drag state
        private var lastMousePoint: Point? = null
        private var isDragging = false

        // Colors
        private val nodeColor = JBColor(Color(0x6B9BFA), Color(0x4A7BD9))
        private val nodeSelectedColor = JBColor(Color(0x59A869), Color(0x499C54))
        private val connectionColor = JBColor(Color(0x808080), Color(0xA0A0A0))
        private val backgroundColor = JBColor.background()
        private val errorColor = JBColor.RED

        init {
            preferredSize = Dimension(800, 600)
            background = backgroundColor

            // Mouse listener for pan and selection
            val mouseAdapter = object : MouseAdapter() {
                override fun mousePressed(e: MouseEvent) {
                    lastMousePoint = e.point
                    isDragging = true

                    // Check for node selection
                    graph?.let { g ->
                        val worldPoint = screenToWorld(e.point)
                        for (node in g.getAllCodeNodes()) {
                            val nodeRect = getNodeBounds(node)
                            if (nodeRect.contains(worldPoint.x, worldPoint.y)) {
                                selectedNodeId = node.id
                                repaint()
                                return
                            }
                        }
                        selectedNodeId = null
                        repaint()
                    }
                }

                override fun mouseReleased(e: MouseEvent) {
                    isDragging = false
                    lastMousePoint = null
                }

                override fun mouseDragged(e: MouseEvent) {
                    if (isDragging && selectedNodeId == null) {
                        lastMousePoint?.let { last ->
                            panX += (e.x - last.x) / zoom
                            panY += (e.y - last.y) / zoom
                            lastMousePoint = e.point
                            repaint()
                        }
                    }
                }
            }

            addMouseListener(mouseAdapter)
            addMouseMotionListener(mouseAdapter)

            // Mouse wheel for zoom
            addMouseWheelListener { e ->
                val zoomFactor = if (e.wheelRotation < 0) 1.1 else 0.9
                zoom = (zoom * zoomFactor).coerceIn(0.25, 4.0)
                repaint()
            }
        }

        fun setGraph(graph: FlowGraph?) {
            this.graph = graph
            this.errorMessage = null
            this.selectedNodeId = null

            // Auto-fit if new graph
            if (graph != null) {
                fitToContent()
            }
        }

        fun setError(message: String?) {
            this.errorMessage = message
            this.graph = null
        }

        private fun fitToContent() {
            graph?.let { g ->
                if (g.getAllCodeNodes().isEmpty()) return

                var minX = Double.MAX_VALUE
                var minY = Double.MAX_VALUE
                var maxX = Double.MIN_VALUE
                var maxY = Double.MIN_VALUE

                for (node in g.getAllCodeNodes()) {
                    val bounds = getNodeBounds(node)
                    minX = minOf(minX, bounds.minX)
                    minY = minOf(minY, bounds.minY)
                    maxX = maxOf(maxX, bounds.maxX)
                    maxY = maxOf(maxY, bounds.maxY)
                }

                val contentWidth = maxX - minX + 100
                val contentHeight = maxY - minY + 100

                val scaleX = width / contentWidth
                val scaleY = height / contentHeight
                zoom = minOf(scaleX, scaleY, 1.0).coerceIn(0.25, 2.0)

                panX = -minX + 50
                panY = -minY + 50
            }
        }

        private fun screenToWorld(point: Point): java.awt.geom.Point2D.Double {
            return java.awt.geom.Point2D.Double(
                (point.x / zoom) - panX,
                (point.y / zoom) - panY
            )
        }

        private fun getNodeBounds(node: CodeNode): Rectangle2D.Double {
            val width = 150.0
            val height = 80.0 + (node.inputPorts.size + node.outputPorts.size) * 16.0
            return Rectangle2D.Double(
                node.position.x,
                node.position.y,
                width,
                height
            )
        }

        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            val g2 = g as Graphics2D

            // Enable antialiasing
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

            // Fill background
            g2.color = backgroundColor
            g2.fillRect(0, 0, width, height)

            // Show error message if present
            if (errorMessage != null) {
                g2.color = errorColor
                g2.font = g2.font.deriveFont(14f)
                val fm = g2.fontMetrics
                val textWidth = fm.stringWidth(errorMessage)
                g2.drawString(errorMessage, (width - textWidth) / 2, height / 2)
                return
            }

            // Show placeholder if no graph
            if (graph == null) {
                g2.color = JBColor.GRAY
                g2.font = g2.font.deriveFont(14f)
                val message = "Open a .flow.kts file to view the graph"
                val fm = g2.fontMetrics
                val textWidth = fm.stringWidth(message)
                g2.drawString(message, (width - textWidth) / 2, height / 2)
                return
            }

            // Apply view transformation
            val savedTransform = g2.transform
            g2.scale(zoom, zoom)
            g2.translate(panX, panY)

            // Draw connections first (behind nodes)
            graph?.connections?.forEach { connection ->
                drawConnection(g2, connection)
            }

            // Draw nodes
            graph?.getAllCodeNodes()?.forEach { node ->
                drawNode(g2, node)
            }

            // Restore transform
            g2.transform = savedTransform

            // Draw zoom indicator
            g2.color = JBColor.GRAY
            g2.font = g2.font.deriveFont(11f)
            g2.drawString("Zoom: ${(zoom * 100).toInt()}%", 10, height - 10)
        }

        private fun drawNode(g2: Graphics2D, node: CodeNode) {
            val bounds = getNodeBounds(node)
            val isSelected = node.id == selectedNodeId

            // Draw node background
            val shape = RoundRectangle2D.Double(
                bounds.x, bounds.y, bounds.width, bounds.height, 8.0, 8.0
            )
            g2.color = if (isSelected) nodeSelectedColor else nodeColor
            g2.fill(shape)

            // Draw border
            g2.color = if (isSelected) nodeSelectedColor.darker() else nodeColor.darker()
            g2.stroke = BasicStroke(if (isSelected) 2f else 1f)
            g2.draw(shape)

            // Draw node name
            g2.color = Color.WHITE
            g2.font = g2.font.deriveFont(Font.BOLD, 12f)
            val fm = g2.fontMetrics
            val nameX = bounds.x + (bounds.width - fm.stringWidth(node.name)) / 2
            g2.drawString(node.name, nameX.toFloat(), (bounds.y + 20).toFloat())

            // Draw ports
            var portY = bounds.y + 40

            // Input ports on left
            g2.font = g2.font.deriveFont(Font.PLAIN, 10f)
            for (port in node.inputPorts) {
                // Port circle
                g2.color = JBColor(Color(0xCC7832), Color(0xCC7832))
                g2.fillOval((bounds.x - 4).toInt(), (portY - 4).toInt(), 8, 8)

                // Port name
                g2.color = Color.WHITE
                g2.drawString(port.name, (bounds.x + 8).toFloat(), (portY + 4).toFloat())

                portY += 16
            }

            // Output ports on right
            for (port in node.outputPorts) {
                // Port circle
                g2.color = JBColor(Color(0x59A869), Color(0x59A869))
                g2.fillOval((bounds.x + bounds.width - 4).toInt(), (portY - 4).toInt(), 8, 8)

                // Port name
                g2.color = Color.WHITE
                val portNameWidth = g2.fontMetrics.stringWidth(port.name)
                g2.drawString(port.name, (bounds.x + bounds.width - portNameWidth - 8).toFloat(), (portY + 4).toFloat())

                portY += 16
            }
        }

        private fun drawConnection(g2: Graphics2D, connection: Connection) {
            val sourceNode = graph?.findNode(connection.sourceNodeId) as? CodeNode ?: return
            val targetNode = graph?.findNode(connection.targetNodeId) as? CodeNode ?: return

            val sourcePort = sourceNode.outputPorts.find { it.id == connection.sourcePortId }
            val targetPort = targetNode.inputPorts.find { it.id == connection.targetPortId }

            if (sourcePort == null || targetPort == null) return

            val sourceBounds = getNodeBounds(sourceNode)
            val targetBounds = getNodeBounds(targetNode)

            // Calculate port positions
            val sourcePortIndex = sourceNode.outputPorts.indexOf(sourcePort)
            val sourceY = sourceBounds.y + 40 + sourceNode.inputPorts.size * 16 + sourcePortIndex * 16

            val targetPortIndex = targetNode.inputPorts.indexOf(targetPort)
            val targetY = targetBounds.y + 40 + targetPortIndex * 16

            val startX = sourceBounds.x + sourceBounds.width
            val startY = sourceY
            val endX = targetBounds.x
            val endY = targetY

            // Draw bezier curve
            g2.color = connectionColor
            g2.stroke = BasicStroke(2f)

            val ctrlX1 = startX + (endX - startX) / 2
            val ctrlX2 = startX + (endX - startX) / 2

            val path = Path2D.Double()
            path.moveTo(startX, startY)
            path.curveTo(ctrlX1, startY, ctrlX2, endY, endX, endY)
            g2.draw(path)

            // Draw arrow at end
            val arrowSize = 6.0
            g2.fillPolygon(
                intArrayOf(endX.toInt(), (endX - arrowSize).toInt(), (endX - arrowSize).toInt()),
                intArrayOf(endY.toInt(), (endY - arrowSize / 2).toInt(), (endY + arrowSize / 2).toInt()),
                3
            )
        }
    }
}
