import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextLayoutResult
import com.darkrockstudios.texteditor.CharLineOffset
import com.darkrockstudios.texteditor.richstyle.RichSpan
import com.darkrockstudios.texteditor.richstyle.RichSpanStyle
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RichSpanTest {
	@Test
	fun `test containsPosition - single line span`() {
		val span = RichSpan(
			start = CharLineOffset(1, 5),
			end = CharLineOffset(1, 10),
			style = TestStyle()
		)

		// Position before span on same line
		assertFalse(span.containsPosition(CharLineOffset(1, 4)))

		// Position at start of span
		assertTrue(span.containsPosition(CharLineOffset(1, 5)))

		// Position in middle of span
		assertTrue(span.containsPosition(CharLineOffset(1, 7)))

		// Position at end of span (should be false as end is exclusive)
		assertFalse(span.containsPosition(CharLineOffset(1, 10)))

		// Position after span on same line
		assertFalse(span.containsPosition(CharLineOffset(1, 11)))
	}

	@Test
	fun `test containsPosition - multi line span`() {
		val span = RichSpan(
			start = CharLineOffset(1, 5),
			end = CharLineOffset(3, 10),
			style = TestStyle()
		)

		// Before first line
		assertFalse(span.containsPosition(CharLineOffset(0, 7)))

		// First line before start
		assertFalse(span.containsPosition(CharLineOffset(1, 4)))

		// First line at start
		assertTrue(span.containsPosition(CharLineOffset(1, 5)))

		// First line after start
		assertTrue(span.containsPosition(CharLineOffset(1, 20)))

		// Middle line - beginning
		assertTrue(span.containsPosition(CharLineOffset(2, 0)))

		// Middle line - middle
		assertTrue(span.containsPosition(CharLineOffset(2, 15)))

		// Middle line - end
		assertTrue(span.containsPosition(CharLineOffset(2, 50)))

		// Last line - beginning
		assertTrue(span.containsPosition(CharLineOffset(3, 0)))

		// Last line - before end
		assertTrue(span.containsPosition(CharLineOffset(3, 9)))

		// Last line - at end (should be false as end is exclusive)
		assertFalse(span.containsPosition(CharLineOffset(3, 10)))

		// Last line - after end
		assertFalse(span.containsPosition(CharLineOffset(3, 11)))

		// After last line
		assertFalse(span.containsPosition(CharLineOffset(4, 0)))
	}

	@Test
	fun `test containsPosition - zero width span`() {
		val span = RichSpan(
			start = CharLineOffset(1, 5),
			end = CharLineOffset(1, 5),
			style = TestStyle()
		)

		// Before position
		assertFalse(span.containsPosition(CharLineOffset(1, 4)))

		// At position (should be false since end is exclusive)
		assertFalse(span.containsPosition(CharLineOffset(1, 5)))

		// After position
		assertFalse(span.containsPosition(CharLineOffset(1, 6)))
	}

	@Test
	fun `test containsPosition - edge cases`() {
		val span = RichSpan(
			start = CharLineOffset(0, 0),
			end = CharLineOffset(2, 0),
			style = TestStyle()
		)

		// Start of document
		assertTrue(span.containsPosition(CharLineOffset(0, 0)))

		// End of span at start of line (should be false as end is exclusive)
		assertFalse(span.containsPosition(CharLineOffset(2, 0)))

		// Negative line number
		assertFalse(span.containsPosition(CharLineOffset(-1, 5)))

		// Negative character offset
		assertFalse(span.containsPosition(CharLineOffset(1, -1)))
	}

	// Simple test implementation of RichSpanStyle
	private class TestStyle : RichSpanStyle {
		override fun DrawScope.drawCustomStyle(
			layoutResult: TextLayoutResult,
			textRange: androidx.compose.ui.text.TextRange
		) {
			// Noop
		}
	}
}