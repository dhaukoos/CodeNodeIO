/*
 * ModuleDropdown - Toolbar dropdown for workspace module selection
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

@Composable
fun ModuleDropdown(
    currentModuleName: String,
    mruModules: List<File>,
    onSwitchModule: (File) -> Unit,
    onOpenModule: () -> Unit,
    onCreateModule: () -> Unit,
    onModuleSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val displayName = currentModuleName.ifEmpty { "No Module" }

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            colors = ButtonDefaults.outlinedButtonColors(
                backgroundColor = Color.Transparent,
                contentColor = Color.White
            ),
            border = null
        ) {
            Text(
                text = displayName,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "\u25BC",
                fontSize = 10.sp
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            if (currentModuleName.isNotEmpty()) {
                DropdownMenuItem(
                    onClick = { expanded = false },
                    enabled = false
                ) {
                    Text("\u2713  $currentModuleName")
                }
            }

            val otherModules = mruModules.filter { it.name != currentModuleName && it.isDirectory }
            otherModules.forEach { moduleDir ->
                DropdownMenuItem(
                    onClick = {
                        expanded = false
                        onSwitchModule(moduleDir)
                    }
                ) {
                    Text("     ${moduleDir.name}")
                }
            }

            if (currentModuleName.isNotEmpty() || otherModules.isNotEmpty()) {
                Divider()
            }

            DropdownMenuItem(onClick = {
                expanded = false
                onOpenModule()
            }) {
                Text("Open Module...")
            }

            DropdownMenuItem(onClick = {
                expanded = false
                onCreateModule()
            }) {
                Text("Create New Module...")
            }

            if (currentModuleName.isNotEmpty()) {
                Divider()

                DropdownMenuItem(onClick = {
                    expanded = false
                    onModuleSettings()
                }) {
                    Text("Module Settings...")
                }
            }
        }
    }
}
