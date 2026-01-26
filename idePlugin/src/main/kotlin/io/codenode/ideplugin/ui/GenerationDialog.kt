/*
 * Generation Dialog
 * Dialog for configuring KMP code generation options
 * License: Apache 2.0
 */

package io.codenode.ideplugin.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import io.codenode.fbpdsl.model.FlowGraph
import io.codenode.kotlincompiler.generator.BuildScriptGenerator
import javax.swing.JComponent

/**
 * Dialog for configuring KMP code generation options.
 *
 * Allows users to:
 * - Select target platforms (Android, iOS, Desktop, Web JS, Web Wasm)
 * - Configure platform-specific settings (SDK versions, deployment targets)
 * - Review flow graph information before generation
 * - Validate settings before proceeding
 */
class GenerationDialog(
    private val project: Project,
    private val flowGraph: FlowGraph
) : DialogWrapper(project) {

    // Platform selection checkboxes
    private val androidCheckbox = JBCheckBox("Android")
    private val iosCheckbox = JBCheckBox("iOS")
    private val desktopCheckbox = JBCheckBox("Desktop (JVM)")
    private val jsCheckbox = JBCheckBox("Web (JavaScript)")
    private val wasmCheckbox = JBCheckBox("Web (Wasm)")

    // Android configuration
    private val androidMinSdkField = JBTextField("24")
    private val androidTargetSdkField = JBTextField("34")
    private val androidCompileSdkField = JBTextField("34")

    // iOS configuration
    private val iosDeploymentTargetField = JBTextField("14.0")

    // Output configuration
    private val packageNameField = JBTextField("io.codenode.generated")

    init {
        title = "Generate KMP Code"
        setOKButtonText("Generate")
        setCancelButtonText("Cancel")

        // Initialize checkboxes based on flow graph's target platforms
        androidCheckbox.isSelected = flowGraph.targetsPlatform(FlowGraph.TargetPlatform.KMP_ANDROID)
        iosCheckbox.isSelected = flowGraph.targetsPlatform(FlowGraph.TargetPlatform.KMP_IOS)
        desktopCheckbox.isSelected = flowGraph.targetsPlatform(FlowGraph.TargetPlatform.KMP_DESKTOP)
        jsCheckbox.isSelected = flowGraph.targetsPlatform(FlowGraph.TargetPlatform.KMP_WEB)
        wasmCheckbox.isSelected = flowGraph.targetsPlatform(FlowGraph.TargetPlatform.KMP_WASM)

        // Enable/disable platform-specific fields based on selection
        androidCheckbox.addActionListener { updateFieldsState() }
        iosCheckbox.addActionListener { updateFieldsState() }

        init()
        updateFieldsState()
    }

    override fun createCenterPanel(): JComponent {
        return panel {
            // Flow Graph Information
            group("Flow Graph") {
                row("Name:") {
                    label(flowGraph.name)
                        .bold()
                }
                row("Version:") {
                    label(flowGraph.version)
                }
                row("Nodes:") {
                    label("${flowGraph.getAllCodeNodes().size} code nodes")
                }
                row("Connections:") {
                    label("${flowGraph.connections.size} connections")
                }
                flowGraph.description?.let { desc ->
                    row("Description:") {
                        label(desc)
                    }
                }
            }

            // Target Platform Selection
            group("Target Platforms") {
                row {
                    cell(androidCheckbox)
                        .comment("Generate Android-compatible Kotlin code")
                }
                row {
                    cell(iosCheckbox)
                        .comment("Generate iOS framework (Kotlin/Native)")
                }
                row {
                    cell(desktopCheckbox)
                        .comment("Generate JVM desktop application code")
                }
                row {
                    cell(jsCheckbox)
                        .comment("Generate JavaScript/browser code")
                }
                row {
                    cell(wasmCheckbox)
                        .comment("Generate WebAssembly code (experimental)")
                }
            }

            // Android Configuration
            collapsibleGroup("Android Configuration") {
                row("Min SDK:") {
                    cell(androidMinSdkField)
                        .columns(COLUMNS_SHORT)
                        .comment("Minimum Android SDK version (e.g., 24)")
                }
                row("Target SDK:") {
                    cell(androidTargetSdkField)
                        .columns(COLUMNS_SHORT)
                        .comment("Target Android SDK version (e.g., 34)")
                }
                row("Compile SDK:") {
                    cell(androidCompileSdkField)
                        .columns(COLUMNS_SHORT)
                        .comment("Compile SDK version (e.g., 34)")
                }
            }

            // iOS Configuration
            collapsibleGroup("iOS Configuration") {
                row("Deployment Target:") {
                    cell(iosDeploymentTargetField)
                        .columns(COLUMNS_SHORT)
                        .comment("Minimum iOS version (e.g., 14.0)")
                }
            }

            // Output Configuration
            group("Output Configuration") {
                row("Package Name:") {
                    cell(packageNameField)
                        .columns(COLUMNS_LARGE)
                        .comment("Base package for generated code")
                }
            }

            // Warnings
            row {
                cell(JBLabel())
                comment("Generated code will comply with Apache 2.0 license requirements")
            }
        }
    }

    override fun doValidate(): ValidationInfo? {
        // At least one platform must be selected
        if (!androidCheckbox.isSelected &&
            !iosCheckbox.isSelected &&
            !desktopCheckbox.isSelected &&
            !jsCheckbox.isSelected &&
            !wasmCheckbox.isSelected) {
            return ValidationInfo("Please select at least one target platform", androidCheckbox)
        }

        // Validate Android SDK versions
        if (androidCheckbox.isSelected) {
            val minSdk = androidMinSdkField.text.toIntOrNull()
            val targetSdk = androidTargetSdkField.text.toIntOrNull()
            val compileSdk = androidCompileSdkField.text.toIntOrNull()

            if (minSdk == null || minSdk < 21) {
                return ValidationInfo("Min SDK must be at least 21", androidMinSdkField)
            }
            if (targetSdk == null || targetSdk < minSdk) {
                return ValidationInfo("Target SDK must be >= Min SDK", androidTargetSdkField)
            }
            if (compileSdk == null || compileSdk < targetSdk) {
                return ValidationInfo("Compile SDK must be >= Target SDK", androidCompileSdkField)
            }
        }

        // Validate iOS deployment target
        if (iosCheckbox.isSelected) {
            val iosTarget = iosDeploymentTargetField.text
            if (!iosTarget.matches(Regex("\\d+\\.\\d+"))) {
                return ValidationInfo("Invalid iOS deployment target (use format: 14.0)", iosDeploymentTargetField)
            }
            val majorVersion = iosTarget.split(".").firstOrNull()?.toIntOrNull() ?: 0
            if (majorVersion < 13) {
                return ValidationInfo("iOS deployment target must be at least 13.0", iosDeploymentTargetField)
            }
        }

        // Validate package name
        val packageName = packageNameField.text
        if (packageName.isBlank()) {
            return ValidationInfo("Package name cannot be empty", packageNameField)
        }
        if (!packageName.matches(Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)*$"))) {
            return ValidationInfo("Invalid package name format", packageNameField)
        }

        return null // Validation passed
    }

    /**
     * Updates the enabled state of platform-specific configuration fields.
     */
    private fun updateFieldsState() {
        androidMinSdkField.isEnabled = androidCheckbox.isSelected
        androidTargetSdkField.isEnabled = androidCheckbox.isSelected
        androidCompileSdkField.isEnabled = androidCheckbox.isSelected

        iosDeploymentTargetField.isEnabled = iosCheckbox.isSelected
    }

    /**
     * Returns the configured target platforms.
     */
    fun getTargetConfig(): BuildScriptGenerator.TargetConfig {
        return BuildScriptGenerator.TargetConfig(
            android = androidCheckbox.isSelected,
            ios = iosCheckbox.isSelected,
            desktop = desktopCheckbox.isSelected,
            js = jsCheckbox.isSelected,
            wasm = wasmCheckbox.isSelected,
            androidMinSdk = androidMinSdkField.text.toIntOrNull() ?: 24,
            androidTargetSdk = androidTargetSdkField.text.toIntOrNull() ?: 34,
            androidCompileSdk = androidCompileSdkField.text.toIntOrNull() ?: 34,
            iosDeploymentTarget = iosDeploymentTargetField.text
        )
    }

    /**
     * Returns the configured package name.
     */
    fun getPackageName(): String {
        return packageNameField.text
    }

    /**
     * Returns true if any target platform is selected.
     */
    fun hasSelectedTargets(): Boolean {
        return androidCheckbox.isSelected ||
               iosCheckbox.isSelected ||
               desktopCheckbox.isSelected ||
               jsCheckbox.isSelected ||
               wasmCheckbox.isSelected
    }

    /**
     * Returns a summary of selected targets for display.
     */
    fun getTargetSummary(): String {
        val targets = mutableListOf<String>()
        if (androidCheckbox.isSelected) targets.add("Android")
        if (iosCheckbox.isSelected) targets.add("iOS")
        if (desktopCheckbox.isSelected) targets.add("Desktop")
        if (jsCheckbox.isSelected) targets.add("Web (JS)")
        if (wasmCheckbox.isSelected) targets.add("Web (Wasm)")
        return targets.joinToString(", ")
    }

    companion object {
        /**
         * Shows the generation dialog and returns the result.
         *
         * @param project The current project
         * @param flowGraph The flow graph to generate code for
         * @return GenerationDialogResult if OK was clicked, null if cancelled
         */
        fun showAndGetResult(project: Project, flowGraph: FlowGraph): GenerationDialogResult? {
            val dialog = GenerationDialog(project, flowGraph)
            return if (dialog.showAndGet()) {
                GenerationDialogResult(
                    targetConfig = dialog.getTargetConfig(),
                    packageName = dialog.getPackageName()
                )
            } else {
                null
            }
        }
    }
}

/**
 * Result from the generation dialog.
 */
data class GenerationDialogResult(
    val targetConfig: BuildScriptGenerator.TargetConfig,
    val packageName: String
)
