/*
 * TextualView - Textual DSL Representation of Flow Graph
 * Displays flow graphs as readable Kotlin DSL text with syntax highlighting
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.codenode.fbpdsl.dsl.TextGenerator
import io.codenode.fbpdsl.model.FlowGraph

/**
 * Displays a flow graph as textual DSL representation
 *
 * @param flowGraph The flow graph to display
 * @param modifier Modifier for the view
 * @param enableSyntaxHighlighting Whether to enable syntax highlighting
 */
@Composable
fun TextualView(
    flowGraph: FlowGraph,
    modifier: Modifier = Modifier,
    enableSyntaxHighlighting: Boolean = true
) {
    // Generate DSL text from flow graph
    val dslText = remember(flowGraph) {
        TextGenerator.generate(flowGraph)
    }

    // Apply syntax highlighting if enabled
    val annotatedText = remember(dslText, enableSyntaxHighlighting) {
        if (enableSyntaxHighlighting) {
            SyntaxHighlighter.highlight(dslText)
        } else {
            AnnotatedString(dslText)
        }
    }

    // Scrollable text container
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF2B2B2B)) // Dark background for code editor feel
            .padding(16.dp)
    ) {
        SelectionContainer {
            Text(
                text = annotatedText,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .horizontalScroll(rememberScrollState()),
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = Color(0xFFA9B7C6) // Default text color (IntelliJ IDEA dark theme)
            )
        }
    }
}


/**
 * Compact textual view for side-by-side display with visual view
 *
 * @param flowGraph The flow graph to display
 * @param modifier Modifier for the view
 */
@Composable
fun CompactTextualView(
    flowGraph: FlowGraph,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF2B2B2B))
    ) {
        // Header
        Text(
            text = "Textual View: ${flowGraph.name}",
            modifier = Modifier.padding(8.dp),
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFBBBBBB)
        )

        // Textual view
        TextualView(
            flowGraph = flowGraph,
            modifier = Modifier.weight(1f)
        )
    }
}
