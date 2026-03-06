package io.codenode.userprofiles.userInterface

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.codenode.userprofiles.persistence.UserProfileEntity

@Composable
fun AddUpdateUserProfile(
    existingProfile: UserProfileEntity? = null,
    onSave: (UserProfileEntity) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf(existingProfile?.name ?: "") }
    var ageText by remember { mutableStateOf(existingProfile?.age?.toString() ?: "") }
    var isActive by remember { mutableStateOf(existingProfile?.isActive ?: true) }

    val isUpdate = existingProfile != null

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (isUpdate) "Update Profile" else "Add Profile",
            style = MaterialTheme.typography.titleMedium
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = ageText,
            onValueChange = { newValue ->
                if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                    ageText = newValue
                }
            },
            label = { Text("Age (optional)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isActive,
                onCheckedChange = { isActive = it }
            )
            Text("Active")
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = onCancel) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    val age = ageText.toIntOrNull()
                    val profile = UserProfileEntity(
                        id = existingProfile?.id ?: 0,
                        name = name.trim(),
                        age = age,
                        isActive = isActive
                    )
                    onSave(profile)
                },
                enabled = name.isNotBlank()
            ) {
                Text(if (isUpdate) "Update" else "Add")
            }
        }
    }
}
