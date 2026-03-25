package io.codenode.addresses.userInterface

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
import io.codenode.addresses.iptypes.Address

@Composable
fun AddUpdateAddress(
    existingItem: Address? = null,
    onSave: (Address) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var StreetText by remember { mutableStateOf(existingItem?.street?.toString() ?: "") }
    var CityText by remember { mutableStateOf(existingItem?.city?.toString() ?: "") }
    var StateText by remember { mutableStateOf(existingItem?.state?.toString() ?: "") }
    var ZipText by remember { mutableStateOf(existingItem?.zip?.toString() ?: "") }

    val isUpdate = existingItem != null

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (isUpdate) "Update Address" else "Add Address",
            style = MaterialTheme.typography.titleMedium
        )

        OutlinedTextField(
            value = StreetText,
            onValueChange = { StreetText = it },
            label = { Text("Street") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = CityText,
            onValueChange = { CityText = it },
            label = { Text("City") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = StateText,
            onValueChange = { StateText = it },
            label = { Text("State") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = ZipText,
            onValueChange = { newValue ->
                if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                    ZipText = newValue
                }
            },
            label = { Text("Zip") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                    val item = Address(
                        id = existingItem?.id ?: 0,
                        street = StreetText.trim(),
                        city = CityText.trim(),
                        state = StateText.trim(),
                        zip = ZipText.toIntOrNull() ?: 0
                    )
                    onSave(item)
                }
            ) {
                Text(if (isUpdate) "Update" else "Add")
            }
        }
    }
}
