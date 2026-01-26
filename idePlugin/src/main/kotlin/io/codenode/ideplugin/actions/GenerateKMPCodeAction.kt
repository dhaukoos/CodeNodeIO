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
import io.codenode.kotlincompiler.generator.BuildScriptGenerator
import io.codenode.kotlincompiler.generator.KotlinCodeGenerator
import io.codenode.kotlincompiler.validator.LicenseValidator
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

    private val codeGenerator = KotlinCodeGenerator()
    private val buildScriptGenerator = BuildScriptGenerator()
    private val licenseValidator = LicenseValidator()

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

        // Run generation in background
        ProgressManager.getInstance().run(object : Task.Backgroundable(
            project,
            "Generating KMP Code",
            true
        ) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    indicator.text = "Validating flow graph..."
                    indicator.fraction = 0.1

                    // Validate the flow graph
                    val validation = flowGraph.validate()
                    if (!validation.success) {
                        ApplicationManager.getApplication().invokeLater {
                            Messages.showErrorDialog(
                                project,
                                "Flow graph validation failed:\n${validation.errors.joinToString("\n")}",
                                "Generation Error"
                            )
                        }
                        return
                    }

                    indicator.text = "Generating component classes..."
                    indicator.fraction = 0.3

                    // Generate the project
                    val generatedProject = codeGenerator.generateProject(flowGraph)

                    indicator.text = "Generating build scripts..."
                    indicator.fraction = 0.5

                    // Generate build scripts
                    val buildScript = buildScriptGenerator.generateBuildScript(flowGraph, config)
                    val settingsScript = buildScriptGenerator.generateSettingsScript(flowGraph.name)
                    val gradleProperties = buildScriptGenerator.generateGradleProperties()

                    indicator.text = "Validating licenses..."
                    indicator.fraction = 0.7

                    // Validate licenses
                    val licenseResult = licenseValidator.validateBuildScript(buildScript)
                    if (!licenseResult.isValid) {
                        val report = licenseValidator.generateReport(licenseResult)
                        ApplicationManager.getApplication().invokeLater {
                            Messages.showErrorDialog(
                                project,
                                "License validation failed:\n\n$report",
                                "License Violation"
                            )
                        }
                        return
                    }

                    indicator.text = "Writing files..."
                    indicator.fraction = 0.9

                    // Write files to output directory
                    writeGeneratedFiles(
                        outputDir,
                        flowGraph.name,
                        generatedProject,
                        buildScript,
                        settingsScript,
                        gradleProperties
                    )

                    indicator.fraction = 1.0

                    ApplicationManager.getApplication().invokeLater {
                        Messages.showInfoMessage(
                            project,
                            "KMP code generated successfully!\n\n" +
                                "Output: ${outputDir.path}/${flowGraph.name}\n" +
                                "Files: ${generatedProject.files.size} source files\n" +
                                "Targets: ${getTargetSummary(config)}",
                            "Generation Complete"
                        )
                    }
                } catch (ex: Exception) {
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showErrorDialog(
                            project,
                            "Code generation failed: ${ex.message}",
                            "Generation Error"
                        )
                    }
                }
            }
        })
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
        // Try to get from editor
        val editor = e.getData(CommonDataKeys.EDITOR)
        val psiFile = e.getData(CommonDataKeys.PSI_FILE)

        // Check if it's a .flow.kts file
        if (psiFile?.name?.endsWith(".flow.kts") == true) {
            // TODO: Parse the DSL file to get FlowGraph
            // For now, return a placeholder
            return createPlaceholderFlowGraph(psiFile.name.removeSuffix(".flow.kts"))
        }

        // Try to get from project service
        val project = e.project ?: return null
        // TODO: Get from FlowGraphManager service
        // val flowGraphManager = project.getService(FlowGraphManager::class.java)
        // return flowGraphManager.currentGraph

        return null
    }

    /**
     * Creates a placeholder flow graph for testing.
     */
    private fun createPlaceholderFlowGraph(name: String): FlowGraph {
        return FlowGraph(
            id = "graph_$name",
            name = name,
            version = "1.0.0",
            description = "Generated from $name.flow.kts",
            targetPlatforms = listOf(
                FlowGraph.TargetPlatform.KMP_ANDROID,
                FlowGraph.TargetPlatform.KMP_IOS
            )
        )
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
     * Writes all generated files to the output directory.
     */
    private fun writeGeneratedFiles(
        outputDir: VirtualFile,
        projectName: String,
        generatedProject: io.codenode.kotlincompiler.generator.GeneratedProject,
        buildScript: String,
        settingsScript: String,
        gradleProperties: String
    ) {
        val projectDir = File(outputDir.path, projectName)
        projectDir.mkdirs()

        // Write build.gradle.kts
        File(projectDir, "build.gradle.kts").writeText(buildScript)

        // Write settings.gradle.kts
        File(projectDir, "settings.gradle.kts").writeText(settingsScript)

        // Write gradle.properties
        File(projectDir, "gradle.properties").writeText(gradleProperties)

        // Create source directories
        val srcDir = File(projectDir, "src/commonMain/kotlin/io/codenode/generated")
        srcDir.mkdirs()

        // Write generated source files using the GeneratedProject's file writing capability
        generatedProject.writeTo(srcDir)

        // Refresh the file system
        outputDir.refresh(true, true)
    }

    /**
     * Returns a summary of selected targets.
     */
    private fun getTargetSummary(config: BuildScriptGenerator.TargetConfig): String {
        val targets = mutableListOf<String>()
        if (config.android) targets.add("Android")
        if (config.ios) targets.add("iOS")
        if (config.desktop) targets.add("Desktop")
        if (config.js) targets.add("Web (JS)")
        if (config.wasm) targets.add("Web (Wasm)")
        return targets.joinToString(", ")
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
