package io.codenode.geolocations.userInterface

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
import io.codenode.persistence.GeoLocationEntity

@Composable
fun AddUpdateGeoLocation(
    existingItem: GeoLocationEntity? = null,
    onSave: (GeoLocationEntity) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var latitudeText by remember { mutableStateOf(existingItem?.latitude?.toString() ?: "") }
    var longitudeText by remember { mutableStateOf(existingItem?.longitude?.toString() ?: "") }
    var labelText by remember { mutableStateOf(existingItem?.label?.toString() ?: "") }
    var altitudeText by remember { mutableStateOf(existingItem?.altitude?.toString() ?: "") }
    var isActive by remember { mutableStateOf(existingItem?.isActive ?: true) }

    val isUpdate = existingItem != null

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (isUpdate) "Update GeoLocation" else "Add GeoLocation",
            style = MaterialTheme.typography.titleMedium
        )

        OutlinedTextField(
            value = latitudeText,
            onValueChange = { newValue ->
                if (newValue.isEmpty() || newValue.toDoubleOrNull() != null) {
                    latitudeText = newValue
                }
            },
            label = { Text("latitude") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = longitudeText,
            onValueChange = { newValue ->
                if (newValue.isEmpty() || newValue.toDoubleOrNull() != null) {
                    longitudeText = newValue
                }
            },
            label = { Text("longitude") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = labelText,
            onValueChange = { labelText = it },
            label = { Text("label") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = altitudeText,
            onValueChange = { newValue ->
                if (newValue.isEmpty() || newValue.toDoubleOrNull() != null) {
                    altitudeText = newValue
                }
            },
            label = { Text("altitude (optional)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isActive,
                onCheckedChange = { isActive = it }
            )
            Text("isActive")
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
                    val item = GeoLocationEntity(
                        id = existingItem?.id ?: 0,
                        latitude = latitudeText.toDoubleOrNull() ?: 0.0,
                        longitude = longitudeText.toDoubleOrNull() ?: 0.0,
                        label = labelText.trim(),
                        altitude = altitudeText.toDoubleOrNull(),
                        isActive = isActive
                    )
                    onSave(item)
                }
            ) {
                Text(if (isUpdate) "Update" else "Add")
            }
        }
    }
}
