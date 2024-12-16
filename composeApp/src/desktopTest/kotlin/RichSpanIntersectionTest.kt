import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import com.darkrockstudios.texteditor.CharLineOffset
import com.darkrockstudios.texteditor.LineWrap
import com.darkrockstudios.texteditor.richstyle.RichSpan
import com.darkrockstudios.texteditor.richstyle.RichSpanStyle
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RichSpanIntersectionTest {

	// Helper function to create a mocked LineWrap
	private fun createLineWrap(
		line: Int,
		wrapStartsAtIndex: Int,
		virtualLength: Int,
		lineEnd: Int,
		lineCount: Int = 1
	): LineWrap {
		val mockTextLayoutResult = mockk<TextLayoutResult>()
		every { mockTextLayoutResult.lineCount } returns lineCount
		every { mockTextLayoutResult.getLineEnd(0, any()) } returns lineEnd

		return LineWrap(
			line = line,
			wrapStartsAtIndex = wrapStartsAtIndex,
			virtualLength = virtualLength,
			offset = Offset(0f, 0f),
			textLayoutResult = mockTextLayoutResult,
			richSpans = emptyList(),
			virtualLineIndex = virtualLineIndex
		)
	}

	@Test
	fun `test intersectsWith - single line span - no wrap`() {
		val span = RichSpan(
			start = CharLineOffset(1, 5),
			end = CharLineOffset(1, 10),
			style = TestStyle()
		)

		// Before the span
		val beforeWrap = createLineWrap(line = 1, wrapStartsAtIndex = 0, lineEnd = 4)
		assertFalse(span.intersectsWith(beforeWrap))

		// Overlapping start of span
		val startWrap = createLineWrap(line = 1, wrapStartsAtIndex = 0, lineEnd = 7)
		assertTrue(span.intersectsWith(startWrap))

		// Completely containing span
		val containingWrap = createLineWrap(line = 1, wrapStartsAtIndex = 0, lineEnd = 15)
		assertTrue(span.intersectsWith(containingWrap))

		// Overlapping end of span
		val endWrap = createLineWrap(line = 1, wrapStartsAtIndex = 8, lineEnd = 12)
		assertTrue(span.intersectsWith(endWrap))

		// After the span
		val afterWrap = createLineWrap(line = 1, wrapStartsAtIndex = 11, lineEnd = 15)
		assertFalse(span.intersectsWith(afterWrap))

		// Different line
		val differentLineWrap = createLineWrap(line = 2, wrapStartsAtIndex = 5, lineEnd = 10)
		assertFalse(span.intersectsWith(differentLineWrap))
	}

	@Test
	fun `test intersectsWith - single line span - with wrap`() {
		val span = RichSpan(
			start = CharLineOffset(1, 5),
			end = CharLineOffset(1, 15),
			style = TestStyle()
		)

		// First wrap segment containing start
		val firstWrap = createLineWrap(line = 1, wrapStartsAtIndex = 0, lineEnd = 10)
		assertTrue(span.intersectsWith(firstWrap))

		// Second wrap segment containing end
		val secondWrap = createLineWrap(line = 1, wrapStartsAtIndex = 10, lineEnd = 20)
		assertTrue(span.intersectsWith(secondWrap))

		// Wrap segment between start and end
		val middleWrap = createLineWrap(line = 1, wrapStartsAtIndex = 8, lineEnd = 12)
		assertTrue(span.intersectsWith(middleWrap))
	}

	@Test
	fun `test intersectsWith - multi line span`() {
		val span = RichSpan(
			start = CharLineOffset(1, 5),
			end = CharLineOffset(3, 10),
			style = TestStyle()
		)

		// First line - before span start
		val beforeStartWrap = createLineWrap(line = 1, wrapStartsAtIndex = 0, lineEnd = 4)
		assertFalse(span.intersectsWith(beforeStartWrap))

		// First line - containing span start
		val startLineWrap = createLineWrap(line = 1, wrapStartsAtIndex = 0, lineEnd = 20)
		assertTrue(span.intersectsWith(startLineWrap))

		// Middle line - should always intersect
		val middleLineWrap = createLineWrap(line = 2, wrapStartsAtIndex = 0, lineEnd = 30)
		assertTrue(span.intersectsWith(middleLineWrap))

		// Last line - before span end
		val endLineBeforeWrap = createLineWrap(line = 3, wrapStartsAtIndex = 0, lineEnd = 8)
		assertTrue(span.intersectsWith(endLineBeforeWrap))

		// Last line - after span end
		val endLineAfterWrap = createLineWrap(line = 3, wrapStartsAtIndex = 11, lineEnd = 20)
		assertFalse(span.intersectsWith(endLineAfterWrap))
	}

	@Test
	fun `test intersectsWith - edge cases`() {
		val span = RichSpan(
			start = CharLineOffset(1, 0),
			end = CharLineOffset(1, 0),
			style = TestStyle()
		)

		// Zero-width span
		val zeroWidthWrap = createLineWrap(line = 1, wrapStartsAtIndex = 0, lineEnd = 10)
		assertFalse(span.intersectsWith(zeroWidthWrap))

		// Empty line
		val emptyLineWrap =
			createLineWrap(line = 1, wrapStartsAtIndex = 0, lineEnd = 0, lineCount = 0)
		assertFalse(span.intersectsWith(emptyLineWrap))

		// Span at line start
		val spanAtStart = RichSpan(
			start = CharLineOffset(1, 0),
			end = CharLineOffset(1, 5),
			style = TestStyle()
		)
		val wrapAtStart = createLineWrap(line = 1, wrapStartsAtIndex = 0, lineEnd = 10)
		assertTrue(spanAtStart.intersectsWith(wrapAtStart))

		// Span at line end
		val spanAtEnd = RichSpan(
			start = CharLineOffset(1, 95),
			end = CharLineOffset(1, 100),
			style = TestStyle()
		)
		val wrapAtEnd = createLineWrap(line = 1, wrapStartsAtIndex = 90, lineEnd = 100)
		assertTrue(spanAtEnd.intersectsWith(wrapAtEnd))
	}

	private class TestStyle : RichSpanStyle {
		override fun DrawScope.drawCustomStyle(
			layoutResult: TextLayoutResult,
			textRange: TextRange
		) {
			// No-op implementation for testing
		}
	}
}