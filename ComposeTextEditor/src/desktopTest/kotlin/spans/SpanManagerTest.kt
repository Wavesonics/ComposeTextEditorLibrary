package spans

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.darkrockstudios.texteditor.state.SpanManager
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SpanManagerTest {
	@Test
	fun `test overlapping spans are merged`() {
		val spanManager = SpanManager()
		val boldStyle = SpanStyle(fontWeight = FontWeight.Bold)

		// Create text with two overlapping spans
		val originalText = buildAnnotatedString {
			append("Hello World")
			addStyle(boldStyle, 0, 5) // Span A: "Hello"
			addStyle(boldStyle, 3, 7) // Span B: "lo W"
		}

		val processedSpans = spanManager.processSpans(originalText)

		assertEquals(1, processedSpans.size, "Should have merged into a single span")
		assertEquals(0, processedSpans[0].start, "Merged span should start at 0")
		assertEquals(7, processedSpans[0].end, "Merged span should end at 7")
	}

	@Test
	fun `test multiple overlapping spans are merged`() {
		val spanManager = SpanManager()
		val boldStyle = SpanStyle(fontWeight = FontWeight.Bold)

		// Create text with multiple overlapping spans
		val originalText = buildAnnotatedString {
			append("Hello World")
			addStyle(boldStyle, 0, 5)  // "Hello"
			addStyle(boldStyle, 3, 7)  // "lo W"
			addStyle(boldStyle, 6, 9)  // "Wor"
		}

		val processedSpans = spanManager.processSpans(originalText)

		assertEquals(1, processedSpans.size, "Should have merged into a single span")
		assertEquals(0, processedSpans[0].start, "Merged span should start at 0")
		assertEquals(9, processedSpans[0].end, "Merged span should end at 9")
	}

	@Test
	fun `test deduplicate and merge spans`() {
		val spanManager = SpanManager()
		val originalText = AnnotatedString(
			text = "Hello World",
			spanStyles = listOf(
				AnnotatedString.Range(
					SpanStyle(color = androidx.compose.ui.graphics.Color.Red),
					0,
					5
				),
				AnnotatedString.Range(
					SpanStyle(color = androidx.compose.ui.graphics.Color.Red),
					5,
					11
				)
			)
		)

		val result = spanManager.processSpans(originalText)

		val expected = listOf(
			AnnotatedString.Range(SpanStyle(color = androidx.compose.ui.graphics.Color.Red), 0, 11)
		)

		assertEquals(expected, result)
	}

	@Test
	fun `test span insertion`() {
		val spanManager = SpanManager()
		val originalText = AnnotatedString(
			text = "Hello",
			spanStyles = listOf(
				AnnotatedString.Range(
					SpanStyle(color = androidx.compose.ui.graphics.Color.Red),
					0,
					5
				)
			)
		)
		val insertedText = AnnotatedString(
			text = " World",
			spanStyles = listOf(
				AnnotatedString.Range(
					SpanStyle(color = androidx.compose.ui.graphics.Color.Blue),
					0,
					6
				)
			)
		)

		val result =
			spanManager.processSpans(originalText, insertionPoint = 5, insertedText = insertedText)

		val expected = listOf(
			AnnotatedString.Range(SpanStyle(color = androidx.compose.ui.graphics.Color.Red), 0, 5),
			AnnotatedString.Range(SpanStyle(color = androidx.compose.ui.graphics.Color.Blue), 5, 11)
		)

		assertEquals(expected.size, result.size)
		assertEquals(expected, result)
	}

	@Test
	fun `test span deletion`() {
		val spanManager = SpanManager()
		val originalText = AnnotatedString(
			text = "Hello World",
			spanStyles = listOf(
				AnnotatedString.Range(
					SpanStyle(color = androidx.compose.ui.graphics.Color.Red),
					0,
					5
				),
				AnnotatedString.Range(
					SpanStyle(color = androidx.compose.ui.graphics.Color.Blue),
					6,
					11
				)
			)
		)

		val result = spanManager.processSpans(originalText, deletionStart = 3, deletionEnd = 8)

		val expected = listOf(
			AnnotatedString.Range(SpanStyle(color = androidx.compose.ui.graphics.Color.Red), 0, 3),
			AnnotatedString.Range(SpanStyle(color = androidx.compose.ui.graphics.Color.Blue), 3, 6)
		)

		assertEquals(expected, result)
	}

	@Test
	fun `test merge adjacent spans`() {
		val spanManager = SpanManager()
		val originalText = AnnotatedString(
			text = "Hello World",
			spanStyles = listOf(
				AnnotatedString.Range(SpanStyle(fontWeight = FontWeight.Bold), 0, 5),
				AnnotatedString.Range(SpanStyle(fontWeight = FontWeight.Bold), 5, 11)
			)
		)

		val result = spanManager.processSpans(originalText)

		val expected = listOf(
			AnnotatedString.Range(
				SpanStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
				0,
				11
			)
		)

		assertEquals(expected, result)
	}

	@Test
	fun `test inserting non-bold character in middle of bold text`() {
		val spanManager = SpanManager()
		val boldStyle = SpanStyle(fontWeight = FontWeight.Bold)

		// Create text with a bold span
		val originalText = AnnotatedString(
			text = "HelloWorld",
			spanStyles = listOf(
				AnnotatedString.Range(boldStyle, 0, 10)
			)
		)

		// Insert a character with no explicit style in the middle
		val insertedText = AnnotatedString(
			text = " ",
			spanStyles = emptyList()
		)

		val result = spanManager.processSpans(originalText, insertionPoint = 5, insertedText = insertedText)

		// Expected: Bold span expands to cover the entire text including inserted character
		// This is the typical text editor behavior where typing in the middle of styled text
		// inherits the surrounding style
		val expected = listOf(
			AnnotatedString.Range(boldStyle, 0, 11)  // "Hello World" - entire text is bold
		)

		assertEquals(expected.size, result.size, "Should have one span covering entire text")
		assertEquals(expected, result, "Bold span should expand to include inserted text")
	}

	@Test
	fun `test empty text with spans`() {
		val spanManager = SpanManager()
		val boldStyle = SpanStyle(fontWeight = FontWeight.Bold)

		// Create empty text with a span (technically invalid but should be handled gracefully)
		val originalText = AnnotatedString(
			text = "",
			spanStyles = listOf(
				AnnotatedString.Range(boldStyle, 0, 0)
			)
		)

		val result = spanManager.processSpans(originalText)

		// Empty text should have no spans
		assertEquals(0, result.size, "Empty text should have no spans")
	}

	@Test
	fun `test spans at text boundaries`() {
		val spanManager = SpanManager()
		val boldStyle = SpanStyle(fontWeight = FontWeight.Bold)
		val italicStyle = SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)

		// Create text with spans at the beginning and end
		val originalText = AnnotatedString(
			text = "Hello World",
			spanStyles = listOf(
				AnnotatedString.Range(boldStyle, 0, 2),      // "He" at the beginning
				AnnotatedString.Range(italicStyle, 9, 11)    // "ld" at the end
			)
		)

		val result = spanManager.processSpans(originalText)

		// Both spans should be preserved
		assertEquals(2, result.size, "Should preserve both boundary spans")

		// Verify first span (at beginning)
		assertEquals(0, result[0].start, "First span should start at beginning of text")
		assertEquals(2, result[0].end, "First span should end at position 2")
		assertEquals(boldStyle, result[0].item, "First span should have bold style")

		// Verify second span (at end)
		assertEquals(9, result[1].start, "Second span should start at position 9")
		assertEquals(11, result[1].end, "Second span should end at end of text")
		assertEquals(italicStyle, result[1].item, "Second span should have italic style")
	}

	@Test
	fun `test deletion across multiple spans with different styles`() {
		val spanManager = SpanManager()
		val boldStyle = SpanStyle(fontWeight = FontWeight.Bold)
		val italicStyle = SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
		val underlineStyle = SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)

		// Create text with multiple spans of different styles
		val originalText = AnnotatedString(
			text = "Hello Beautiful World",
			spanStyles = listOf(
				AnnotatedString.Range(boldStyle, 0, 5),          // "Hello"
				AnnotatedString.Range(italicStyle, 6, 15),       // "Beautiful"
				AnnotatedString.Range(underlineStyle, 16, 21)    // "World"
			)
		)

		// Delete text that spans across all three styles (from "lo Be" to "ful W")
		val result = spanManager.processSpans(originalText, deletionStart = 3, deletionEnd = 17)

		// Should have two spans after deletion (based on actual behavior)
		assertEquals(2, result.size, "Should have two spans after deletion")

		// Verify first span (beginning of text)
		assertEquals(0, result[0].start, "First span should start at beginning of text")
		assertEquals(3, result[0].end, "First span should end at deletion start")
		assertEquals(boldStyle, result[0].item, "First span should have bold style")

		// Verify second span (end of text)
		assertEquals(3, result[1].start, "Second span should start at deletion point")
		assertEquals(7, result[1].end, "Second span should end at end of text")
		assertEquals(underlineStyle, result[1].item, "Second span should have underline style")
	}

	@Test
	fun `test inserting styled text into text with different styles`() {
		val spanManager = SpanManager()
		val boldStyle = SpanStyle(fontWeight = FontWeight.Bold)
		val italicStyle = SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)

		// Create original text with bold style
		val originalText = AnnotatedString(
			text = "Hello World",
			spanStyles = listOf(
				AnnotatedString.Range(boldStyle, 0, 11)  // Entire text is bold
			)
		)

		// Create inserted text with italic style
		val insertedText = AnnotatedString(
			text = "Beautiful ",
			spanStyles = listOf(
				AnnotatedString.Range(italicStyle, 0, 10)  // Entire inserted text is italic
			)
		)

		// Insert the italic text in the middle of the bold text
		val result = spanManager.processSpans(originalText, insertionPoint = 6, insertedText = insertedText)

		// Should have two spans after insertion:
		// - Bold expands to cover entire text (including inserted portion)
		// - Italic applies to the inserted portion
		// The inserted text will be both bold AND italic
		assertEquals(2, result.size, "Should have two spans after insertion")

		// Verify bold span covers entire text
		assertEquals(0, result[0].start, "Bold span should start at beginning of text")
		assertEquals(21, result[0].end, "Bold span should cover entire text")
		assertEquals(boldStyle, result[0].item, "First span should have bold style")

		// Verify italic span covers inserted text
		assertEquals(6, result[1].start, "Italic span should start at insertion point")
		assertEquals(16, result[1].end, "Italic span should end after inserted text")
		assertEquals(italicStyle, result[1].item, "Second span should have italic style")
	}

	@Test
	fun `test removing specific span styles`() {
		val spanManager = SpanManager()
		val boldStyle = SpanStyle(fontWeight = FontWeight.Bold)
		val italicStyle = SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
		val redStyle = SpanStyle(color = androidx.compose.ui.graphics.Color.Red)

		// Create text with multiple overlapping styles
		val originalText = AnnotatedString(
			text = "Hello Beautiful World",
			spanStyles = listOf(
				AnnotatedString.Range(boldStyle, 0, 21),     // Entire text is bold
				AnnotatedString.Range(italicStyle, 6, 15),   // "Beautiful" is italic
				AnnotatedString.Range(redStyle, 0, 21)       // Entire text is red
			)
		)

		// Remove the italic style from the middle section
		val result = spanManager.removeSingleLineSpanStyle(originalText, 6, 15, italicStyle)

		// Should still have bold and red styles
		assertEquals(2, result.spanStyles.size, "Should have two span styles after removal")

		// Check that the bold style is still applied to the entire text
		val boldSpan = result.spanStyles.find { it.item == boldStyle }
		assertNotNull(boldSpan, "Bold style should still be present")
		assertEquals(0, boldSpan.start, "Bold span should start at beginning of text")
		assertEquals(21, boldSpan.end, "Bold span should end at end of text")

		// Check that the red style is still applied to the entire text
		val redSpan = result.spanStyles.find { it.item == redStyle }
		assertNotNull(redSpan, "Red style should still be present")
		assertEquals(0, redSpan.start, "Red span should start at beginning of text")
		assertEquals(21, redSpan.end, "Red span should end at end of text")

		// Check that the italic style is no longer present
		val italicSpan = result.spanStyles.find { it.item == italicStyle }
		assertNull(italicSpan, "Italic style should be removed")
	}

	@Test
	fun `test applying span styles to already styled text`() {
		val spanManager = SpanManager()
		val boldStyle = SpanStyle(fontWeight = FontWeight.Bold)
		val italicStyle = SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)

		// Create text with bold style
		val originalText = AnnotatedString(
			text = "Hello Beautiful World",
			spanStyles = listOf(
				AnnotatedString.Range(boldStyle, 0, 21)  // Entire text is bold
			)
		)

		// Apply italic style to a portion of the text
		val result = spanManager.applySingleLineSpanStyle(originalText, 6, 15, italicStyle)

		// Should have both bold and italic styles
		assertEquals(2, result.spanStyles.size, "Should have two span styles after applying new style")

		// Check that the bold style is still applied to the entire text
		val boldSpan = result.spanStyles.find { it.item == boldStyle }
		assertNotNull(boldSpan, "Bold style should still be present")
		assertEquals(0, boldSpan.start, "Bold span should start at beginning of text")
		assertEquals(21, boldSpan.end, "Bold span should end at end of text")

		// Check that the italic style is applied to the middle section
		val italicSpan = result.spanStyles.find { it.item == italicStyle }
		assertNotNull(italicSpan, "Italic style should be present")
		assertEquals(6, italicSpan.start, "Italic span should start at position 6")
		assertEquals(15, italicSpan.end, "Italic span should end at position 15")
	}

	@Test
	fun `test handling empty spans`() {
		val spanManager = SpanManager()
		val boldStyle = SpanStyle(fontWeight = FontWeight.Bold)

		// Create text with an empty span (start equals end)
		val originalText = AnnotatedString(
			text = "Hello World",
			spanStyles = listOf(
				AnnotatedString.Range(boldStyle, 5, 5)  // Empty span: start equals end
			)
		)

		val result = spanManager.processSpans(originalText)

		// Empty spans should be filtered out
		assertEquals(0, result.size, "Empty spans should be filtered out")
	}

	@Test
	fun `test handling spans that extend beyond text boundaries`() {
		val spanManager = SpanManager()
		val boldStyle = SpanStyle(fontWeight = FontWeight.Bold)
		val italicStyle = SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)

		// Create text with spans that extend beyond text boundaries
		val originalText = AnnotatedString(
			text = "Hello World",  // Length is 11
			spanStyles = listOf(
				AnnotatedString.Range(boldStyle, -2, 5),     // Start is negative
				AnnotatedString.Range(italicStyle, 8, 15)    // End is beyond text length
			)
		)

		val result = spanManager.processSpans(originalText)

		// Spans should be coerced to valid ranges
		assertEquals(2, result.size, "Should have two spans after coercing to valid ranges")

		// First span should be coerced to start at 0
		assertEquals(0, result[0].start, "First span should start at 0 (coerced from negative)")
		assertEquals(5, result[0].end, "First span should end at 5")
		assertEquals(boldStyle, result[0].item, "First span should have bold style")

		// Second span should be coerced to end at text length
		assertEquals(8, result[1].start, "Second span should start at 8")
		assertEquals(11, result[1].end, "Second span should end at text length (coerced from beyond)")
		assertEquals(italicStyle, result[1].item, "Second span should have italic style")
	}
}
