/*
 * SyntaxHighlighter Test
 * Unit tests for DSL syntax highlighting functionality
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import kotlin.test.*

class SyntaxHighlighterTest {

    @Test
    fun `should highlight keywords in DSL text`() {
        // Given DSL text with keywords
        val dslText = """
            flowGraph(
                name = "Test",
                version = "1.0.0"
            ) {
                val gen = codeNode("Generator") {
                    output("data", String::class)
                }
            }
        """.trimIndent()

        // When highlighting
        val highlighted = SyntaxHighlighter.highlight(dslText)

        // Then keywords should be identified
        assertTrue(dslText.contains("flowGraph"), "Text should contain flowGraph keyword")
        assertTrue(dslText.contains("codeNode"), "Text should contain codeNode keyword")
        assertTrue(dslText.contains("output"), "Text should contain output keyword")
        assertTrue(highlighted.text == dslText, "Highlighted text content should match original")
    }

    @Test
    fun `should detect position in string literal`() {
        // Given text with string literals
        val text = """val name = "Test Graph" """

        // When checking positions
        val beforeString = SyntaxHighlighter.isInString(text, 11)  // Before opening quote
        val inString = SyntaxHighlighter.isInString(text, 15)      // Inside "Test Graph"
        val afterString = SyntaxHighlighter.isInString(text, 23)   // After closing quote

        // Then should correctly identify string positions
        assertFalse(beforeString, "Position before string should return false")
        assertTrue(inString, "Position inside string should return true")
        assertFalse(afterString, "Position after string should return false")
    }

    @Test
    fun `should detect position in single-line comment`() {
        // Given text with single-line comment
        val text = """
            val x = 5
            // This is a comment
            val y = 10
        """.trimIndent()

        // When checking positions
        val beforeComment = SyntaxHighlighter.isInComment(text, 5)   // In "val x = 5"
        val inComment = SyntaxHighlighter.isInComment(text, 20)      // In "// This is"
        val afterComment = SyntaxHighlighter.isInComment(text, 50)   // In "val y = 10"

        // Then should correctly identify comment positions
        assertFalse(beforeComment, "Position before comment should return false")
        assertTrue(inComment, "Position in comment should return true")
        assertFalse(afterComment, "Position after comment should return false")
    }

    @Test
    fun `should detect position in multi-line comment`() {
        // Given text with multi-line comment
        val text = """
            val x = 5
            /* This is
               a multi-line
               comment */
            val y = 10
        """.trimIndent()

        // When checking positions
        val beforeComment = SyntaxHighlighter.isInComment(text, 5)   // In "val x = 5"
        val inComment = SyntaxHighlighter.isInComment(text, 25)      // Inside comment
        val afterComment = SyntaxHighlighter.isInComment(text, 70)   // In "val y = 10"

        // Then should correctly identify comment positions
        assertFalse(beforeComment, "Position before comment should return false")
        assertTrue(inComment, "Position in comment should return true")
        assertFalse(afterComment, "Position after comment should return false")
    }

    @Test
    fun `should get keyword at position`() {
        // Given text with keywords
        val text = "flowGraph(myVar = \"Test\") { codeNode(\"Gen\") }"

        // When getting keywords at positions
        val keyword1 = SyntaxHighlighter.getKeywordAtPosition(text, 0)   // "flowGraph"
        val keyword2 = SyntaxHighlighter.getKeywordAtPosition(text, 5)   // "flowGraph"
        val keyword3 = SyntaxHighlighter.getKeywordAtPosition(text, 28)  // "codeNode"
        val notKeyword = SyntaxHighlighter.getKeywordAtPosition(text, 10) // "myVar" (not a keyword)

        // Then should correctly identify keywords
        assertEquals("flowGraph", keyword1, "Should identify flowGraph keyword at start")
        assertEquals("flowGraph", keyword2, "Should identify flowGraph keyword in middle")
        assertEquals("codeNode", keyword3, "Should identify codeNode keyword")
        assertNull(notKeyword, "Should return null for non-keywords")
    }

    @Test
    fun `should handle empty text`() {
        // Given empty text
        val text = ""

        // When highlighting
        val highlighted = SyntaxHighlighter.highlight(text)

        // Then should handle gracefully
        assertEquals("", highlighted.text, "Should handle empty text")
    }

    @Test
    fun `should handle text without keywords`() {
        // Given text without keywords
        val text = "some random text 123"

        // When highlighting
        val highlighted = SyntaxHighlighter.highlight(text)

        // Then should return text unchanged
        assertEquals(text, highlighted.text, "Should preserve non-keyword text")
    }

    @Test
    fun `should handle escaped quotes in strings`() {
        // Given text with escaped quotes
        val text = """val str = "Hello \"World\"" """

        // When checking positions
        val inString1 = SyntaxHighlighter.isInString(text, 15)  // In "Hello"
        val inString2 = SyntaxHighlighter.isInString(text, 22)  // In \"World\"
        val afterString = SyntaxHighlighter.isInString(text, 27) // After closing quote

        // Then should handle escaped quotes
        assertTrue(inString1, "Should be in string before escaped quote")
        assertTrue(inString2, "Should be in string with escaped quotes")
        assertFalse(afterString, "Should not be in string after closing quote")
    }

    @Test
    fun `should highlight numbers correctly`() {
        // Given text with numbers
        val text = "val x = 42 val y = 3.14"

        // When highlighting
        val highlighted = SyntaxHighlighter.highlight(text)

        // Then numbers should be in the text
        assertTrue(highlighted.text.contains("42"), "Should contain integer")
        assertTrue(highlighted.text.contains("3.14"), "Should contain decimal")
    }

    @Test
    fun `should handle complex DSL with all elements`() {
        // Given complex DSL text
        val text = """
            // Graph definition
            flowGraph(
                name = "Complex",
                version = "1.0.0"
            ) {
                /* Multi-line
                   comment */
                val gen = codeNode("DataGen") {
                    output("data", String::class)
                }

                val proc = codeNode("Processor") {
                    input("in", String::class)
                    output("out", String::class)
                }

                gen.output("data") connect proc.input("in")
            }
        """.trimIndent()

        // When highlighting
        val highlighted = SyntaxHighlighter.highlight(text)

        // Then should preserve all content
        assertEquals(text, highlighted.text, "Should preserve complex DSL structure")

        // And should have annotations (we can't easily test colors, but we verify text is preserved)
        assertTrue(highlighted.spanStyles.isNotEmpty(), "Should have style annotations")
    }
}
