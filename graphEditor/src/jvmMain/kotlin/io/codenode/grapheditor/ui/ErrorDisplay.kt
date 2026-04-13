/*
 * ErrorDisplay - Error Display for Invalid Connections
 * Provides UI components for displaying validation errors and warnings
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.codenode.grapheditor.state.GraphState

/**
 * Error severity levels
 */
enum class ErrorSeverity {
    ERROR,   // Blocking error
    WARNING, // Non-blocking warning
    INFO     // Informational message
}

/**
 * Error message data
 */
data class ErrorMessage(
    val message: String,
    val severity: ErrorSeverity = ErrorSeverity.ERROR,
    val details: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Inline error message display
 *
 * @param message The error message to display
 * @param severity The severity level
 * @param modifier Modifier for the error display
 * @param onDismiss Callback when dismiss is clicked
 */
@Composable
fun ErrorMessage(
    message: String,
    severity: ErrorSeverity = ErrorSeverity.ERROR,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null
) {
    val backgroundColor: Color
    val iconColor: Color
    val icon: String
    when (severity) {
        ErrorSeverity.ERROR -> {
            backgroundColor = Color(0xFFFFEBEE)
            iconColor = Color(0xFFF44336)
            icon = "⚠"
        }
        ErrorSeverity.WARNING -> {
            backgroundColor = Color(0xFFFFF8E1)
            iconColor = Color(0xFFFFC107)
            icon = "⚠"
        }
        ErrorSeverity.INFO -> {
            backgroundColor = Color(0xFFE3F2FD)
            iconColor = Color(0xFF2196F3)
            icon = "ℹ"
        }
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        backgroundColor = backgroundColor,
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = icon,
                fontSize = 20.sp,
                color = iconColor
            )

            Text(
                text = message,
                fontSize = 13.sp,
                color = Color(0xFF212121),
                modifier = Modifier.weight(1f)
            )

            if (onDismiss != null) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Text(
                        text = "×",
                        fontSize = 16.sp,
                        color = Color(0xFF757575)
                    )
                }
            }
        }
    }
}

/**
 * Toast-style error notification that appears temporarily
 *
 * @param message The error message
 * @param severity The severity level
 * @param visible Whether the toast is visible
 * @param onDismiss Callback when dismissed
 * @param modifier Modifier for the toast
 */
@Composable
fun ErrorToast(
    message: String,
    severity: ErrorSeverity = ErrorSeverity.ERROR,
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut(),
        modifier = modifier
    ) {
        ErrorMessage(
            message = message,
            severity = severity,
            onDismiss = onDismiss
        )
    }
}

/**
 * Connection validation error display
 *
 * @param graphState The graph state
 * @param modifier Modifier for the display
 */
@Composable
fun ConnectionErrorDisplay(
    graphState: GraphState,
    modifier: Modifier = Modifier
) {
    val errorMessage = graphState.errorMessage

    if (errorMessage != null) {
        ErrorMessage(
            message = errorMessage,
            severity = ErrorSeverity.ERROR,
            modifier = modifier,
            onDismiss = { graphState.clearError() }
        )
    }
}

/**
 * Error panel showing multiple errors
 *
 * @param errors List of error messages
 * @param modifier Modifier for the panel
 * @param onDismiss Callback when an error is dismissed
 * @param onClearAll Callback when clear all is clicked
 */
@Composable
fun ErrorPanel(
    errors: List<ErrorMessage>,
    modifier: Modifier = Modifier,
    onDismiss: (ErrorMessage) -> Unit = {},
    onClearAll: () -> Unit = {}
) {
    if (errors.isEmpty()) return

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        backgroundColor = Color.White,
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚠",
                        fontSize = 20.sp,
                        color = Color(0xFFF44336)
                    )
                    Text(
                        text = "Validation Errors (${errors.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF212121)
                    )
                }

                TextButton(onClick = onClearAll) {
                    Text(
                        text = "Clear All",
                        fontSize = 12.sp,
                        color = Color(0xFF2196F3)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Divider(color = Color(0xFFE0E0E0))

            Spacer(modifier = Modifier.height(12.dp))

            // Error list
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 300.dp)
            ) {
                items(errors) { error ->
                    ErrorListItem(
                        error = error,
                        onDismiss = { onDismiss(error) }
                    )
                }
            }
        }
    }
}

/**
 * Individual error list item
 */
@Composable
private fun ErrorListItem(
    error: ErrorMessage,
    onDismiss: () -> Unit
) {
    val iconColor: Color
    val icon: String
    when (error.severity) {
        ErrorSeverity.ERROR -> {
            iconColor = Color(0xFFF44336)
            icon = "⚠"
        }
        ErrorSeverity.WARNING -> {
            iconColor = Color(0xFFFFC107)
            icon = "⚠"
        }
        ErrorSeverity.INFO -> {
            iconColor = Color(0xFF2196F3)
            icon = "ℹ"
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFAFAFA), RoundedCornerShape(4.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = icon,
            fontSize = 18.sp,
            color = iconColor
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = error.message,
                fontSize = 12.sp,
                color = Color(0xFF212121)
            )

            if (error.details != null) {
                Text(
                    text = error.details,
                    fontSize = 11.sp,
                    color = Color(0xFF757575)
                )
            }
        }

        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(20.dp)
        ) {
            Text(
                text = "×",
                fontSize = 14.sp,
                color = Color(0xFF9E9E9E)
            )
        }
    }
}

/**
 * Compact error badge showing error count
 *
 * @param errorCount Number of errors
 * @param modifier Modifier for the badge
 * @param onClick Callback when clicked
 */
@Composable
fun ErrorBadge(
    errorCount: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    if (errorCount == 0) return

    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = Color(0xFFF44336)
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = "⚠",
            fontSize = 16.sp,
            color = Color.White
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = errorCount.toString(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

/**
 * Validation status indicator
 *
 * @param isValid Whether the graph is valid
 * @param errorCount Number of validation errors
 * @param warningCount Number of validation warnings
 * @param modifier Modifier for the indicator
 */
@Composable
fun ValidationStatusIndicator(
    isValid: Boolean,
    errorCount: Int = 0,
    warningCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val color: Color
    val icon: String
    val text: String
    when {
        !isValid -> {
            color = Color(0xFFF44336)
            icon = "⚠"
            text = "$errorCount error${if (errorCount != 1) "s" else ""}"
        }
        warningCount > 0 -> {
            color = Color(0xFFFFC107)
            icon = "⚠"
            text = "$warningCount warning${if (warningCount != 1) "s" else ""}"
        }
        else -> {
            color = Color(0xFF4CAF50)
            icon = "✓"
            text = "Valid"
        }
    }

    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = icon,
            fontSize = 16.sp,
            color = color
        )
        Text(
            text = text,
            fontSize = 12.sp,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Helper to manage error state
 */
@Composable
fun rememberErrorState(): MutableState<List<ErrorMessage>> {
    return remember { mutableStateOf(emptyList()) }
}

/**
 * Extension function to add an error
 */
fun MutableState<List<ErrorMessage>>.addError(
    message: String,
    severity: ErrorSeverity = ErrorSeverity.ERROR,
    details: String? = null
) {
    value = value + ErrorMessage(message, severity, details)
}

/**
 * Extension function to clear all errors
 */
fun MutableState<List<ErrorMessage>>.clearErrors() {
    value = emptyList()
}

/**
 * Extension function to remove a specific error
 */
fun MutableState<List<ErrorMessage>>.removeError(error: ErrorMessage) {
    value = value - error
}
