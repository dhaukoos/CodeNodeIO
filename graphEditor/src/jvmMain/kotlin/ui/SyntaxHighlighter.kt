/*
 * SyntaxHighlighter - DSL Syntax Highlighting for Textual View
 * Provides syntax highlighting for Flow-Based Programming DSL text
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * Syntax highlighting configuration
 */
object SyntaxHighlightingTheme {
    // IntelliJ IDEA dark theme inspired colors
    val keywordColor = Color(0xFFCC7832)      // Orange for keywords
    val stringColor = Color(0xFF6A8759)       // Green for strings
    val numberColor = Color(0xFF6897BB)       // Blue for numbers
    val commentColor = Color(0xFF808080)      // Gray for comments
    val defaultTextColor = Color(0xFFA9B7C6)  // Light gray for default text

    // DSL keywords to highlight
    val keywords = setOf(
        "flowGraph", "codeNode", "graphNode",
        "input", "output", "connect",
        "version", "description", "position",
        "val", "fun", "class", "name"
    )
}

/**
 * Syntax highlighter for DSL text
 */
object SyntaxHighlighter {

    /**
     * Applies syntax highlighting to DSL text
     *
     * @param text The DSL text to highlight
     * @param theme The color theme to use (defaults to SyntaxHighlightingTheme)
     * @return Annotated string with syntax highlighting
     */
    fun highlight(
        text: String,
        theme: SyntaxHighlightingTheme = SyntaxHighlightingTheme
    ): AnnotatedString {
        return buildAnnotatedString {
            var currentIndex = 0
            var inString = false
            var stringStart = 0

            while (currentIndex < text.length) {
                val char = text[currentIndex]

                when {
                    // Handle string literals
                    char == '"' && (currentIndex == 0 || text[currentIndex - 1] != '\\') -> {
                        if (inString) {
                            // End of string
                            withStyle(SpanStyle(color = theme.stringColor)) {
                                append(text.substring(stringStart, currentIndex + 1))
                            }
                            inString = false
                        } else {
                            // Start of string
                            inString = true
                            stringStart = currentIndex
                        }
                        currentIndex++
                    }

                    inString -> {
                        // Inside string, skip
                        currentIndex++
                    }

                    // Handle single-line comments
                    char == '/' && currentIndex + 1 < text.length && text[currentIndex + 1] == '/' -> {
                        val lineEnd = text.indexOf('\n', currentIndex).let {
                            if (it == -1) text.length else it
                        }
                        withStyle(SpanStyle(color = theme.commentColor)) {
                            append(text.substring(currentIndex, lineEnd))
                        }
                        currentIndex = lineEnd
                    }

                    // Handle multi-line comments
                    char == '/' && currentIndex + 1 < text.length && text[currentIndex + 1] == '*' -> {
                        val commentEnd = text.indexOf("*/", currentIndex + 2)
                        val endIndex = if (commentEnd == -1) text.length else commentEnd + 2
                        withStyle(SpanStyle(color = theme.commentColor)) {
                            append(text.substring(currentIndex, endIndex))
                        }
                        currentIndex = endIndex
                    }

                    // Handle keywords and identifiers
                    char.isLetter() || char == '_' -> {
                        val identifierStart = currentIndex
                        while (currentIndex < text.length &&
                            (text[currentIndex].isLetterOrDigit() || text[currentIndex] == '_')
                        ) {
                            currentIndex++
                        }

                        val identifier = text.substring(identifierStart, currentIndex)

                        if (identifier in theme.keywords) {
                            withStyle(SpanStyle(color = theme.keywordColor, fontWeight = FontWeight.Bold)) {
                                append(identifier)
                            }
                        } else {
                            append(identifier)
                        }
                    }

                    // Handle numbers
                    char.isDigit() -> {
                        val numberStart = currentIndex
                        while (currentIndex < text.length &&
                            (text[currentIndex].isDigit() || text[currentIndex] == '.')
                        ) {
                            currentIndex++
                        }

                        withStyle(SpanStyle(color = theme.numberColor)) {
                            append(text.substring(numberStart, currentIndex))
                        }
                    }

                    // Handle other characters
                    else -> {
                        append(char)
                        currentIndex++
                    }
                }
            }
        }
    }

    /**
     * Quick check if a position in text is within a string literal
     * Useful for editor features like auto-completion
     */
    fun isInString(text: String, position: Int): Boolean {
        var inString = false
        var index = 0

        while (index < position && index < text.length) {
            val char = text[index]
            if (char == '"' && (index == 0 || text[index - 1] != '\\')) {
                inString = !inString
            }
            index++
        }

        return inString
    }

    /**
     * Quick check if a position in text is within a comment
     * Useful for editor features like auto-completion
     */
    fun isInComment(text: String, position: Int): Boolean {
        var inMultiLineComment = false
        var index = 0

        while (index < position && index < text.length) {
            val char = text[index]

            // Check for multi-line comment start
            if (char == '/' && index + 1 < text.length && text[index + 1] == '*') {
                inMultiLineComment = true
                index += 2
                continue
            }

            // Check for multi-line comment end
            if (inMultiLineComment && char == '*' && index + 1 < text.length && text[index + 1] == '/') {
                inMultiLineComment = false
                index += 2
                continue
            }

            // Check for single-line comment
            if (char == '/' && index + 1 < text.length && text[index + 1] == '/') {
                val lineEnd = text.indexOf('\n', index)
                if (lineEnd == -1 || lineEnd >= position) {
                    return true // Position is in single-line comment
                }
                index = lineEnd + 1
                continue
            }

            index++
        }

        return inMultiLineComment
    }

    /**
     * Get the keyword at a given position in the text
     * Returns null if position is not on a keyword
     */
    fun getKeywordAtPosition(text: String, position: Int): String? {
        if (position < 0 || position >= text.length) return null
        if (!text[position].isLetterOrDigit() && text[position] != '_') return null

        // Find start of identifier
        var start = position
        while (start > 0 && (text[start - 1].isLetterOrDigit() || text[start - 1] == '_')) {
            start--
        }

        // Find end of identifier
        var end = position
        while (end < text.length && (text[end].isLetterOrDigit() || text[end] == '_')) {
            end++
        }

        val identifier = text.substring(start, end)
        return if (identifier in SyntaxHighlightingTheme.keywords) identifier else null
    }
}
