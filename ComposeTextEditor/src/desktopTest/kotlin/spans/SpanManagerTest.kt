package spans

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.darkrockstudios.texteditor.state.SpanManager
import org.junit.Test
import kotlin.test.assertEquals

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
}