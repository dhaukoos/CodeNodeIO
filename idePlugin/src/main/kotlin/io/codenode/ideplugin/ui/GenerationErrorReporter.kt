/*
 * Generation Error Reporter
 * Handles error reporting and display for code generation failures
 * License: Apache 2.0
 */

package io.codenode.ideplugin.ui

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.dsl.builder.*
import io.codenode.kotlincompiler.validator.LicenseValidator
import java.awt.Dimension
import javax.swing.JComponent

/**
 * Handles error reporting and display for code generation failures.
 *
 * Provides multiple ways to report errors:
 * - Dialog-based error display with details
 * - IDE notification balloons
 * - Log file entries
 * - Structured error reports
 */
class GenerationErrorReporter(private val project: Project) {

    private val logger = Logger.getInstance(GenerationErrorReporter::class.java)

    /**
     * Types of generation errors.
     */
    enum class ErrorType {
        VALIDATION_ERROR,
        LICENSE_VIOLATION,
        FILE_WRITE_ERROR,
        COMPILATION_ERROR,
        CONFIGURATION_ERROR,
        UNKNOWN_ERROR
    }

    /**
     * Represents a generation error with context.
     */
    data class GenerationError(
        val type: ErrorType,
        val message: String,
        val details: String? = null,
        val suggestion: String? = null,
        val exception: Throwable? = null,
        val affectedFiles: List<String> = emptyList()
    )

    /**
     * Reports a single error with a dialog.
     */
    fun reportError(error: GenerationError) {
        // Log the error
        logError(error)

        // Show dialog
        showErrorDialog(error)

        // Show notification
        showNotification(error)
    }

    /**
     * Reports multiple errors with a summary dialog.
     */
    fun reportErrors(errors: List<GenerationError>) {
        if (errors.isEmpty()) return

        // Log all errors
        errors.forEach { logError(it) }

        // Show summary dialog
        if (errors.size == 1) {
            showErrorDialog(errors.first())
        } else {
            showErrorSummaryDialog(errors)
        }
    }

    /**
     * Reports a license validation failure.
     */
    fun reportLicenseViolation(result: LicenseValidator.ValidationResult) {
        val errors = result.violations.map { violation ->
            GenerationError(
                type = ErrorType.LICENSE_VIOLATION,
                message = "License violation: ${violation.packageName}",
                details = violation.reason,
                suggestion = violation.suggestion
            )
        }

        if (errors.isNotEmpty()) {
            showLicenseViolationDialog(result)
        }
    }

    /**
     * Reports a flow graph validation failure.
     */
    fun reportValidationErrors(errors: List<String>) {
        val generationErrors = errors.map { error ->
            GenerationError(
                type = ErrorType.VALIDATION_ERROR,
                message = error,
                suggestion = "Fix the validation error and try again"
            )
        }
        reportErrors(generationErrors)
    }

    /**
     * Reports an exception during generation.
     */
    fun reportException(exception: Throwable, context: String = "Code generation") {
        val error = GenerationError(
            type = ErrorType.UNKNOWN_ERROR,
            message = "$context failed: ${exception.message}",
            details = exception.stackTraceToString().take(2000),
            exception = exception,
            suggestion = "Check the IDE log for more details"
        )
        reportError(error)
    }

    /**
     * Reports a file write error.
     */
    fun reportFileWriteError(filePath: String, exception: Throwable) {
        val error = GenerationError(
            type = ErrorType.FILE_WRITE_ERROR,
            message = "Failed to write file: $filePath",
            details = exception.message,
            exception = exception,
            affectedFiles = listOf(filePath),
            suggestion = "Check file permissions and disk space"
        )
        reportError(error)
    }

    /**
     * Reports a configuration error.
     */
    fun reportConfigurationError(message: String, suggestion: String? = null) {
        val error = GenerationError(
            type = ErrorType.CONFIGURATION_ERROR,
            message = message,
            suggestion = suggestion ?: "Review your configuration and try again"
        )
        reportError(error)
    }

    /**
     * Shows a success notification.
     */
    fun reportSuccess(message: String, details: String? = null) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("CodeNodeIO")
            .createNotification(
                "Code Generation Successful",
                if (details != null) "$message\n$details" else message,
                NotificationType.INFORMATION
            )
            .notify(project)
    }

    /**
     * Logs an error to the IDE log.
     */
    private fun logError(error: GenerationError) {
        val message = buildString {
            append("[${error.type}] ${error.message}")
            error.details?.let { append("\nDetails: $it") }
            error.suggestion?.let { append("\nSuggestion: $it") }
            if (error.affectedFiles.isNotEmpty()) {
                append("\nAffected files: ${error.affectedFiles.joinToString()}")
            }
        }

        if (error.exception != null) {
            logger.error(message, error.exception)
        } else {
            logger.error(message)
        }
    }

    /**
     * Shows an error dialog.
     */
    private fun showErrorDialog(error: GenerationError) {
        val dialog = ErrorDetailsDialog(project, error)
        dialog.show()
    }

    /**
     * Shows a summary dialog for multiple errors.
     */
    private fun showErrorSummaryDialog(errors: List<GenerationError>) {
        val dialog = ErrorSummaryDialog(project, errors)
        dialog.show()
    }

    /**
     * Shows a license violation dialog.
     */
    private fun showLicenseViolationDialog(result: LicenseValidator.ValidationResult) {
        val dialog = LicenseViolationDialog(project, result)
        dialog.show()
    }

    /**
     * Shows a notification balloon.
     */
    private fun showNotification(error: GenerationError) {
        val notificationType = when (error.type) {
            ErrorType.LICENSE_VIOLATION -> NotificationType.ERROR
            ErrorType.VALIDATION_ERROR -> NotificationType.WARNING
            else -> NotificationType.ERROR
        }

        try {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("CodeNodeIO")
                .createNotification(
                    "Code Generation Failed",
                    error.message,
                    notificationType
                )
                .notify(project)
        } catch (e: Exception) {
            // Notification group may not be registered, fall back to message
            logger.warn("Could not show notification: ${e.message}")
        }
    }

    /**
     * Dialog showing detailed error information.
     */
    private class ErrorDetailsDialog(
        project: Project,
        private val error: GenerationError
    ) : DialogWrapper(project) {

        init {
            title = "Generation Error"
            setOKButtonText("Close")
            init()
        }

        override fun createCenterPanel(): JComponent {
            return panel {
                row {
                    icon(Messages.getErrorIcon())
                    label(error.message)
                        .bold()
                }

                if (error.details != null) {
                    group("Details") {
                        row {
                            val textArea = JBTextArea(error.details)
                            textArea.isEditable = false
                            textArea.lineWrap = true
                            textArea.wrapStyleWord = true
                            val scrollPane = JBScrollPane(textArea)
                            scrollPane.preferredSize = Dimension(500, 150)
                            cell(scrollPane)
                                .align(Align.FILL)
                        }
                    }
                }

                if (error.suggestion != null) {
                    group("Suggestion") {
                        row {
                            label(error.suggestion!!)
                        }
                    }
                }

                if (error.affectedFiles.isNotEmpty()) {
                    group("Affected Files") {
                        error.affectedFiles.forEach { file ->
                            row {
                                label(file)
                            }
                        }
                    }
                }
            }
        }

        override fun createActions() = arrayOf(okAction)
    }

    /**
     * Dialog showing summary of multiple errors.
     */
    private class ErrorSummaryDialog(
        project: Project,
        private val errors: List<GenerationError>
    ) : DialogWrapper(project) {

        init {
            title = "Generation Errors (${errors.size})"
            setOKButtonText("Close")
            init()
        }

        override fun createCenterPanel(): JComponent {
            return panel {
                row {
                    icon(Messages.getErrorIcon())
                    label("${errors.size} errors occurred during code generation")
                        .bold()
                }

                group("Errors") {
                    errors.forEachIndexed { index, error ->
                        row {
                            label("${index + 1}. [${error.type}] ${error.message}")
                        }
                        if (error.suggestion != null) {
                            row {
                                label("   → ${error.suggestion}")
                                    .comment("")
                            }
                        }
                    }
                }
            }
        }

        override fun createActions() = arrayOf(okAction)
    }

    /**
     * Dialog showing license violation details.
     */
    private class LicenseViolationDialog(
        project: Project,
        private val result: LicenseValidator.ValidationResult
    ) : DialogWrapper(project) {

        init {
            title = "License Violations Detected"
            setOKButtonText("Close")
            init()
        }

        override fun createCenterPanel(): JComponent {
            return panel {
                row {
                    icon(Messages.getErrorIcon())
                    label("Code generation blocked due to license violations")
                        .bold()
                }

                row {
                    label("Per project constitution: NO GPL/LGPL/AGPL dependencies allowed")
                        .comment("")
                }

                group("Violations") {
                    result.violations.forEach { violation ->
                        row {
                            label("• ${violation.packageName}")
                                .bold()
                        }
                        row {
                            label("  License: ${violation.licenseType}")
                        }
                        row {
                            label("  Reason: ${violation.reason}")
                        }
                        violation.suggestion?.let { suggestion ->
                            row {
                                label("  Suggestion: $suggestion")
                                    .comment("")
                            }
                        }
                    }
                }

                if (result.warnings.isNotEmpty()) {
                    group("Warnings") {
                        result.warnings.forEach { warning ->
                            row {
                                label("⚠ $warning")
                            }
                        }
                    }
                }

                row {
                    label("Please remove or replace the restricted dependencies before generating code.")
                }
            }
        }

        override fun createActions() = arrayOf(okAction)
    }

    companion object {
        /**
         * Gets the error reporter for a project.
         */
        fun getInstance(project: Project): GenerationErrorReporter {
            return GenerationErrorReporter(project)
        }
    }
}
