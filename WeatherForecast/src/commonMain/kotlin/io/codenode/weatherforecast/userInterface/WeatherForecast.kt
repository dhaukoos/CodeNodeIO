/*
 * WeatherForecastUI - Main composable for the WeatherForecast module
 * License: Apache 2.0
 */

package io.codenode.weatherforecast.userInterface

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.codenode.fbpdsl.model.ExecutionState
import io.codenode.weatherforecast.WeatherForecastViewModel

/**
 * Main composable for the WeatherForecast module.
 *
 * Displays a scrollable list of 7-day forecast entries with date, high/low
 * temperatures, a loading indicator, error messages, and a Refresh button.
 */
@Composable
fun WeatherForecastUI(
    viewModel: WeatherForecastViewModel,
    modifier: Modifier = Modifier
) {
    val entries by viewModel.forecastEntries.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val executionState by viewModel.executionState.collectAsState()
    val isRunning = executionState == ExecutionState.RUNNING

    Column(
        modifier = modifier.fillMaxSize().padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Pipeline status
        Text(
            text = when (executionState) {
                ExecutionState.RUNNING -> "Pipeline Running"
                ExecutionState.PAUSED -> "Pipeline Paused"
                else -> "Pipeline Idle"
            },
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // Refresh button
        Button(
            onClick = { viewModel.refresh() },
            enabled = isRunning && !isLoading,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Text(if (isLoading) "Loading..." else "Refresh Forecast")
        }

        // Error message
        if (errorMessage != null) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        // Loading indicator
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.padding(16.dp)
            )
        }

        // Forecast list
        if (entries.isNotEmpty()) {
            Text(
                text = "7-Day Forecast",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                entries.forEach { entry ->
                    ForecastEntryRow(
                        date = entry.date,
                        high = entry.high,
                        low = entry.low,
                        unit = entry.unit
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                }
            }
        } else if (!isLoading && errorMessage == null) {
            Text(
                text = "Press Refresh to load forecast data",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp)
            )
        }
    }
}

@Composable
private fun ForecastEntryRow(
    date: String,
    high: Double,
    low: Double,
    unit: String
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = date,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(60.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "H: ${"%.1f".format(high)}$unit",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFE53935) // Red for high
            )
            Text(
                text = "L: ${"%.1f".format(low)}$unit",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF1E88E5) // Blue for low
            )
        }
    }
}
