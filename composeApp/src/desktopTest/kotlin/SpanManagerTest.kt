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
}