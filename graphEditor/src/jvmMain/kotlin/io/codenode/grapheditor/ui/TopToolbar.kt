/*
 * TopToolbar - Main toolbar for Graph Editor
 * File operations, undo/redo, and grouping controls
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.codenode.grapheditor.state.UndoRedoManager
import java.io.File

@Composable
fun TopToolbar(
    undoRedoManager: UndoRedoManager,
    canGroup: Boolean = false,
    canUngroup: Boolean = false,
    isInsideGraphNode: Boolean = false,
    currentGraphNodeName: String? = null,
    currentModuleName: String = "",
    mruModules: List<File> = emptyList(),
    onSwitchModule: (File) -> Unit = {},
    onOpenModule: () -> Unit = {},
    onCreateModule: () -> Unit = {},
    onModuleSettings: () -> Unit = {},
    hasModule: Boolean = false,
    onNew: () -> Unit,
    onOpen: () -> Unit,
    onSave: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onGroup: () -> Unit = {},
    onUngroup: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    // Feature 086 — per-module recompile control. Null moduleName hides the button.
    recompileModuleName: String? = null,
    isRecompiling: Boolean = false,
    onRecompileModule: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth().height(56.dp),
        color = Color(0xFF2196F3),
        elevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Back button (only visible when inside a GraphNode)
            if (isInsideGraphNode) {
                TextButton(
                    onClick = onNavigateBack,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                ) {
                    Text("\u2190 Back")  // Left arrow
                }

                Divider(
                    modifier = Modifier.width(1.dp).height(32.dp),
                    color = Color.White.copy(alpha = 0.3f)
                )
            }

            // Title with optional breadcrumb
            Text(
                text = if (isInsideGraphNode && currentGraphNodeName != null) {
                    "Inside: $currentGraphNodeName"
                } else {
                    "CodeNodeIO Graph Editor"
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            // Module dropdown (visible at root level)
            if (!isInsideGraphNode) {
                Text(
                    text = " - ",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 18.sp
                )
                ModuleDropdown(
                    currentModuleName = currentModuleName,
                    mruModules = mruModules,
                    onSwitchModule = onSwitchModule,
                    onOpenModule = onOpenModule,
                    onCreateModule = onCreateModule,
                    onModuleSettings = onModuleSettings
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // File operations
            TextButton(
                onClick = onNew,
                enabled = hasModule,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
            ) {
                Text("New")
            }

            TextButton(
                onClick = onOpen,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
            ) {
                Text("Open")
            }

            TextButton(
                onClick = onSave,
                enabled = hasModule,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
            ) {
                Text("Save")
            }

            Divider(
                modifier = Modifier.width(1.dp).height(32.dp),
                color = Color.White.copy(alpha = 0.3f)
            )

            // Undo/Redo
            TextButton(
                onClick = onUndo,
                enabled = undoRedoManager.canUndo,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
            ) {
                Text("Undo")
            }

            TextButton(
                onClick = onRedo,
                enabled = undoRedoManager.canRedo,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
            ) {
                Text("Redo")
            }

            Divider(
                modifier = Modifier.width(1.dp).height(32.dp),
                color = Color.White.copy(alpha = 0.3f)
            )

            // Group/Ungroup
            TextButton(
                onClick = onGroup,
                enabled = canGroup,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
            ) {
                Text("Group")
            }

            TextButton(
                onClick = onUngroup,
                enabled = canUngroup,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
            ) {
                Text("Ungroup")
            }

            // Feature 086 — Recompile module button. Visible only when a host module
            // is loaded; disabled while a compile is in-flight.
            val recompileState = recompileButtonState(recompileModuleName, isRecompiling)
            if (recompileState != null) {
                Divider(
                    modifier = Modifier.width(1.dp).height(32.dp),
                    color = Color.White.copy(alpha = 0.3f)
                )
                TextButton(
                    onClick = onRecompileModule,
                    enabled = recompileState.enabled,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                ) {
                    Text(text = recompileState.label)
                }
            }
        }
    }
}

/** UI state for the toolbar's "Recompile module" affordance. Pure — testable without Compose. */
data class RecompileButtonState(
    val label: String,
    val enabled: Boolean
)

/**
 * Computes the toolbar Recompile-module button's state from inputs.
 *  - Returns null when no module is loaded → button hidden entirely.
 *  - When a compile is in flight: disabled, label "Recompiling…".
 *  - Otherwise: enabled, label "Recompile module: {moduleName}" (FR-008 — scope obvious).
 */
fun recompileButtonState(moduleName: String?, isRecompiling: Boolean): RecompileButtonState? {
    if (moduleName == null) return null
    return RecompileButtonState(
        label = if (isRecompiling) "Recompiling…" else "Recompile module: $moduleName",
        enabled = !isRecompiling
    )
}
