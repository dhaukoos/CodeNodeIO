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
    var nameText by remember { mutableStateOf(existingItem?.name?.toString() ?: "") }
    var latText by remember { mutableStateOf(existingItem?.lat?.toString() ?: "") }
    var lonText by remember { mutableStateOf(existingItem?.lon?.toString() ?: "") }

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
            value = nameText,
            onValueChange = { nameText = it },
            label = { Text("name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = latText,
            onValueChange = { newValue ->
                if (newValue.isEmpty() || newValue.toDoubleOrNull() != null) {
                    latText = newValue
                }
            },
            label = { Text("lat") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = lonText,
            onValueChange = { newValue ->
                if (newValue.isEmpty() || newValue.toDoubleOrNull() != null) {
                    lonText = newValue
                }
            },
            label = { Text("lon") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

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
                        name = nameText.trim(),
                        lat = latText.toDoubleOrNull() ?: 0.0,
                        lon = lonText.toDoubleOrNull() ?: 0.0
                    )
                    onSave(item)
                }
            ) {
                Text(if (isUpdate) "Update" else "Add")
            }
        }
    }
}
