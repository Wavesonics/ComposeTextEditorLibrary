package markdown

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.darkrockstudios.texteditor.markdown.toAnnotatedStringFromMarkdown
import com.darkrockstudios.texteditor.markdown.toMarkdown
import kotlin.test.Test
import kotlin.test.assertEquals

class EscapedCharactersTest {
	@Test
	fun `Test escaped special characters`() {
		val markdownText = """
            This is not \*bold\* and not \_italic\_ and not \`code\`.
            
            This is a literal backslash: \\
            
            This is a literal \# hash.
        """.trimIndent()

		val annotatedString = markdownText.toAnnotatedStringFromMarkdown()
		println("[DEBUG_LOG] Annotated string text: ${annotatedString.text}")

		val expectedString = """
            This is not *bold* and not _italic_ and not `code`.
            
            This is a literal backslash: \
            
            This is a literal # hash.
        """.trimIndent()

		assertEquals(expectedString, annotatedString.text)

		val reconverted = annotatedString.toMarkdown()
		assertEquals(markdownText, reconverted)
	}

	@Test
	fun `Test inner escaped special characters`() {
		val markdownText = """
            This is *bold with \\ escaped \- characters* and not \_italic\_ and not \`code\`.
            
            This is a **literal backslash: \\ inside italics**
            
            This is a literal \# hash.
        """.trimIndent()

		val annotatedString = markdownText.toAnnotatedStringFromMarkdown()
		println("[DEBUG_LOG] Annotated string text: ${annotatedString.text}")

		val expectedString = """
            This is bold with \ escaped - characters and not _italic_ and not `code`.
            
            This is a literal backslash: \ inside italics
            
            This is a literal # hash.
        """.trimIndent()

		assertEquals(expectedString, annotatedString.text)

		val reconverted = annotatedString.toMarkdown()
		assertEquals(markdownText, reconverted)
	}

	@Test
	fun `Test escaped angle brackets and pipe round-trip`() {
		val markdownText = "This has \\< angle \\> brackets and a \\| pipe."

		val annotatedString = markdownText.toAnnotatedStringFromMarkdown()
		val expectedString = "This has < angle > brackets and a | pipe."
		assertEquals(expectedString, annotatedString.text, "Unescape failed for <, >, |")

		val reconverted = annotatedString.toMarkdown()
		assertEquals(markdownText, reconverted, "Round-trip failed for <, >, |")
	}

	@Test
	fun `Test all special characters round-trip from plain text`() {
		val specialChars = listOf('*', '_', '`', '#', '+', '-', '!', '[', ']', '(', ')', '{', '}', '<', '>', '|', '\\')

		specialChars.forEach { char ->
			val originalText = "Text with $char here"
			val original = AnnotatedString(originalText)
			val markdown = original.toMarkdown()
			val roundTripped = markdown.toAnnotatedStringFromMarkdown()
			assertEquals(
				originalText,
				roundTripped.text,
				"Round-trip failed for character: $char (markdown was: $markdown)"
			)
		}
	}

	@Test
	fun `Test hyphen in plain text round-trip`() {
		val originalText = "hello-world and some-other-hyphenated-words"
		val original = AnnotatedString(originalText)
		val markdown = original.toMarkdown()
		val roundTripped = markdown.toAnnotatedStringFromMarkdown()
		assertEquals(originalText, roundTripped.text, "Hyphen round-trip failed. Markdown was: $markdown")
	}

	@Test
	fun `Test hyphen in bold text round-trip`() {
		val original = buildAnnotatedString {
			append("Some ")
			withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
				append("bold-hyphenated-text")
			}
			append(" here")
		}
		val markdown = original.toMarkdown()
		val roundTripped = markdown.toAnnotatedStringFromMarkdown()
		assertEquals(
			original.text,
			roundTripped.text,
			"Hyphen in bold round-trip failed. Markdown was: $markdown"
		)
	}

	@Test
	fun `Test hyphen in italic text round-trip`() {
		val original = buildAnnotatedString {
			append("Some ")
			withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
				append("italic-hyphenated-text")
			}
			append(" here")
		}
		val markdown = original.toMarkdown()
		val roundTripped = markdown.toAnnotatedStringFromMarkdown()
		assertEquals(
			original.text,
			roundTripped.text,
			"Hyphen in italic round-trip failed. Markdown was: $markdown"
		)
	}

	@Test
	fun `Test hyphen at start of line round-trip`() {
		// A hyphen at line start could be parsed as an unordered list marker
		val originalText = "- this looks like a list item"
		val original = AnnotatedString(originalText)
		val markdown = original.toMarkdown()
		val roundTripped = markdown.toAnnotatedStringFromMarkdown()
		assertEquals(
			originalText,
			roundTripped.text,
			"Hyphen at line start round-trip failed. Markdown was: $markdown"
		)
	}

	@Test
	fun `Test multiple hyphens at start of line round-trip`() {
		// Three hyphens could be parsed as a thematic break / horizontal rule
		val originalText = "--- this starts with triple hyphens"
		val original = AnnotatedString(originalText)
		val markdown = original.toMarkdown()
		val roundTripped = markdown.toAnnotatedStringFromMarkdown()
		assertEquals(
			originalText,
			roundTripped.text,
			"Triple hyphen at line start round-trip failed. Markdown was: $markdown"
		)
	}

	@Test
	fun `Test standalone hyphen line round-trip`() {
		// A lone hyphen on a line
		val originalText = "-"
		val original = AnnotatedString(originalText)
		val markdown = original.toMarkdown()
		val roundTripped = markdown.toAnnotatedStringFromMarkdown()
		assertEquals(
			originalText,
			roundTripped.text,
			"Standalone hyphen round-trip failed. Markdown was: $markdown"
		)
	}

	@Test
	fun `Test hyphen after newline round-trip`() {
		// Hyphen at start of second line — could trigger list parsing
		val originalText = "First line\n- second line looks like a list"
		val original = AnnotatedString(originalText)
		val markdown = original.toMarkdown()
		val roundTripped = markdown.toAnnotatedStringFromMarkdown()
		assertEquals(
			originalText,
			roundTripped.text,
			"Hyphen after newline round-trip failed. Markdown was: $markdown"
		)
	}

	@Test
	fun `Test multiple hyphenated lines round-trip`() {
		val originalText = "key-value\nsome-thing\nanother-item"
		val original = AnnotatedString(originalText)
		val markdown = original.toMarkdown()
		val roundTripped = markdown.toAnnotatedStringFromMarkdown()
		assertEquals(
			originalText,
			roundTripped.text,
			"Multiple hyphenated lines round-trip failed. Markdown was: $markdown"
		)
	}

	@Test
	fun `Test hyphen with spaces round-trip`() {
		// "- " is the canonical unordered list marker in CommonMark
		val originalText = "- item one\n- item two"
		val original = AnnotatedString(originalText)
		val markdown = original.toMarkdown()
		val roundTripped = markdown.toAnnotatedStringFromMarkdown()
		assertEquals(
			originalText,
			roundTripped.text,
			"Hyphen-space list-like lines round-trip failed. Markdown was: $markdown"
		)
	}

	@Test
	fun `Test en-dash and em-dash round-trip`() {
		// Users might type double or triple hyphens for dashes
		val originalText = "value -- something --- else"
		val original = AnnotatedString(originalText)
		val markdown = original.toMarkdown()
		val roundTripped = markdown.toAnnotatedStringFromMarkdown()
		assertEquals(
			originalText,
			roundTripped.text,
			"En/em-dash round-trip failed. Markdown was: $markdown"
		)
	}

	@Test
	fun `Test hyphen surrounded by special chars round-trip`() {
		val originalText = "*-* and _-_ and (-) and [-]"
		val original = AnnotatedString(originalText)
		val markdown = original.toMarkdown()
		val roundTripped = markdown.toAnnotatedStringFromMarkdown()
		assertEquals(
			originalText,
			roundTripped.text,
			"Hyphen with surrounding special chars round-trip failed. Markdown was: $markdown"
		)
	}

	@Test
	fun `Test plus at start of line round-trip`() {
		// Plus is also a list marker in CommonMark
		val originalText = "+ this looks like a list item"
		val original = AnnotatedString(originalText)
		val markdown = original.toMarkdown()
		val roundTripped = markdown.toAnnotatedStringFromMarkdown()
		assertEquals(
			originalText,
			roundTripped.text,
			"Plus at line start round-trip failed. Markdown was: $markdown"
		)
	}

	@Test
	fun `Test exclamation mark round-trip`() {
		// "![" starts an image in markdown
		val originalText = "Look at this ![not an image] thing"
		val original = AnnotatedString(originalText)
		val markdown = original.toMarkdown()
		val roundTripped = markdown.toAnnotatedStringFromMarkdown()
		assertEquals(
			originalText,
			roundTripped.text,
			"Exclamation mark round-trip failed. Markdown was: $markdown"
		)
	}

	@Test
	fun `Test hash at start of line round-trip`() {
		// "#" at line start is a header
		val originalText = "# not a header"
		val original = AnnotatedString(originalText)
		val markdown = original.toMarkdown()
		val roundTripped = markdown.toAnnotatedStringFromMarkdown()
		assertEquals(
			originalText,
			roundTripped.text,
			"Hash at line start round-trip failed. Markdown was: $markdown"
		)
	}
}