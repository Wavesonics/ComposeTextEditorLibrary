package spans

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextLayoutResult
import com.darkrockstudios.texteditor.CharLineOffset
import com.darkrockstudios.texteditor.LineWrap
import com.darkrockstudios.texteditor.TextEditorRange
import com.darkrockstudios.texteditor.richstyle.RichSpan
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import utils.TestStyle
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RichSpanIntersectionTest {

	// Helper function to create a mocked LineWrap
	private fun createLineWrap(
		line: Int,
		wrapStartsAtIndex: Int,
		virtualLength: Int,
		virtualLineIndex: Int,
		layoutResult: TextLayoutResult
	): LineWrap {
		return LineWrap(
			line = line,
			wrapStartsAtIndex = wrapStartsAtIndex,
			virtualLength = virtualLength,
			virtualLineIndex = virtualLineIndex,
			offset = Offset.Zero,
			textLayoutResult = layoutResult,
			richSpans = emptyList()
		)
	}

	@Test
	fun `test intersectsWith - single line span - no wrap`() {
		val span = RichSpan(
			range = TextEditorRange(
				start = CharLineOffset(1, 5),
				end = CharLineOffset(1, 10),
			),
			style = TestStyle()
		)

		// Helper function to create a mocked TextLayoutResult with proper line boundaries
		fun createMockedLayoutResult(
			virtualLineIndex: Int,
			lineStart: Int,
			lineEnd: Int
		): TextLayoutResult {
			return mockk<TextLayoutResult>().apply {
				every { getLineStart(virtualLineIndex) } returns lineStart
				every { getLineEnd(virtualLineIndex) } returns lineEnd
				every { lineCount } returns 1
			}
		}

		// Before the span
		val beforeWrap = createLineWrap(
			line = 1,
			wrapStartsAtIndex = 0,
			virtualLength = 4,
			virtualLineIndex = 0,
			layoutResult = createMockedLayoutResult(0, 0, 4)
		)
		assertFalse(span.intersectsWith(beforeWrap))

		// Overlapping start of span
		val startWrap = createLineWrap(
			line = 1,
			wrapStartsAtIndex = 0,
			virtualLength = 7,
			virtualLineIndex = 0,
			layoutResult = createMockedLayoutResult(0, 0, 7)
		)
		assertTrue(span.intersectsWith(startWrap))

		// Completely containing span
		val containingWrap = createLineWrap(
			line = 1,
			wrapStartsAtIndex = 0,
			virtualLength = 15,
			virtualLineIndex = 0,
			layoutResult = createMockedLayoutResult(0, 0, 15)
		)
		assertTrue(span.intersectsWith(containingWrap))

		// Overlapping end of span
		val endWrap = createLineWrap(
			line = 1,
			wrapStartsAtIndex = 8,
			virtualLength = 4,
			virtualLineIndex = 1,
			layoutResult = createMockedLayoutResult(1, 8, 12)
		)
		assertTrue(span.intersectsWith(endWrap))

		// After the span
		val afterWrap = createLineWrap(
			line = 1,
			wrapStartsAtIndex = 11,
			virtualLength = 4,
			virtualLineIndex = 2,
			layoutResult = createMockedLayoutResult(2, 11, 15)
		)
		assertFalse(span.intersectsWith(afterWrap))

		// Different line
		val differentLineWrap = createLineWrap(
			line = 2,
			wrapStartsAtIndex = 5,
			virtualLength = 5,
			virtualLineIndex = 0,
			layoutResult = createMockedLayoutResult(0, 5, 10)
		)
		assertFalse(span.intersectsWith(differentLineWrap))
	}

	@Test
	fun `test intersectsWith - single line span - with wrap`() {
		val span = RichSpan(
			range = TextEditorRange(
				start = CharLineOffset(1, 5),
				end = CharLineOffset(1, 15),
			),
			style = TestStyle()
		)

		// Helper function to create a mocked TextLayoutResult with proper line boundaries
		fun createMockedLayoutResult(
			virtualLineIndex: Int,
			lineStart: Int,
			lineEnd: Int
		): TextLayoutResult {
			return mockk<TextLayoutResult>().apply {
				every { getLineStart(virtualLineIndex) } returns lineStart
				every { getLineEnd(virtualLineIndex) } returns lineEnd
				every { lineCount } returns 2
			}
		}

		// First wrap segment containing start
		val firstWrap = createLineWrap(
			line = 1,
			wrapStartsAtIndex = 0,
			virtualLength = 10,
			virtualLineIndex = 0,
			layoutResult = createMockedLayoutResult(0, 0, 10)
		)
		assertTrue(span.intersectsWith(firstWrap))

		// Second wrap segment containing end
		val secondWrap = createLineWrap(
			line = 1,
			wrapStartsAtIndex = 10,
			virtualLength = 10,
			virtualLineIndex = 1,
			layoutResult = createMockedLayoutResult(1, 10, 20)
		)
		assertTrue(span.intersectsWith(secondWrap))

		// Wrap segment between start and end
		val middleWrap = createLineWrap(
			line = 1,
			wrapStartsAtIndex = 8,
			virtualLength = 4,
			virtualLineIndex = 1,
			layoutResult = createMockedLayoutResult(1, 8, 12)
		)
		assertTrue(span.intersectsWith(middleWrap))
	}

	@Test
	fun `test intersectsWith - multi line span`() {
		val span = RichSpan(
			range = TextEditorRange(
				start = CharLineOffset(1, 5),
				end = CharLineOffset(3, 10),
			),
			style = TestStyle()
		)

		// Helper function to create a mocked TextLayoutResult with proper line boundaries
		fun createMockedLayoutResult(
			virtualLineIndex: Int,
			lineStart: Int,
			lineEnd: Int,
			totalLines: Int = 1
		): TextLayoutResult {
			return mockk<TextLayoutResult>().apply {
				every { getLineStart(virtualLineIndex) } returns lineStart
				every { getLineEnd(virtualLineIndex) } returns lineEnd
				every { lineCount } returns totalLines
			}
		}

		// First line - before span start
		val beforeStartWrap = createLineWrap(
			line = 1,
			wrapStartsAtIndex = 0,
			virtualLength = 4,
			virtualLineIndex = 0,
			layoutResult = createMockedLayoutResult(0, 0, 4)
		)
		assertFalse(span.intersectsWith(beforeStartWrap))

		// First line - containing span start
		val startLineWrap = createLineWrap(
			line = 1,
			wrapStartsAtIndex = 0,
			virtualLength = 20,
			virtualLineIndex = 0,
			layoutResult = createMockedLayoutResult(0, 0, 20)
		)
		assertTrue(span.intersectsWith(startLineWrap))

		// Middle line - should always intersect
		val middleLineWrap = createLineWrap(
			line = 2,
			wrapStartsAtIndex = 0,
			virtualLength = 30,
			virtualLineIndex = 0,
			layoutResult = createMockedLayoutResult(0, 0, 30)
		)
		assertTrue(span.intersectsWith(middleLineWrap))

		// Last line - before span end
		val endLineBeforeWrap = createLineWrap(
			line = 3,
			wrapStartsAtIndex = 0,
			virtualLength = 8,
			virtualLineIndex = 0,
			layoutResult = createMockedLayoutResult(0, 0, 8)
		)
		assertTrue(span.intersectsWith(endLineBeforeWrap))

		// Last line - after span end
		val endLineAfterWrap = createLineWrap(
			line = 3,
			wrapStartsAtIndex = 11,
			virtualLength = 9,
			virtualLineIndex = 1,
			layoutResult = createMockedLayoutResult(1, 11, 20, 2)
		)
		assertFalse(span.intersectsWith(endLineAfterWrap))
	}

	@Test
	fun `test intersectsWith - edge cases`() {
		val span = RichSpan(
			range = TextEditorRange(
				start = CharLineOffset(1, 0),
				end = CharLineOffset(1, 0),
			),
			style = TestStyle()
		)

		// Helper function to create a mocked TextLayoutResult with proper line boundaries
		fun createMockedLayoutResult(
			virtualLineIndex: Int,
			lineStart: Int,
			lineEnd: Int,
			totalLines: Int = 1
		): TextLayoutResult {
			return mockk<TextLayoutResult>().apply {
				every { getLineStart(virtualLineIndex) } returns lineStart
				every { getLineEnd(virtualLineIndex) } returns lineEnd
				every { lineCount } returns totalLines
			}
		}

		// Zero-width span
		val zeroWidthWrap = createLineWrap(
			line = 1,
			wrapStartsAtIndex = 0,
			virtualLength = 10,
			virtualLineIndex = 0,
			layoutResult = createMockedLayoutResult(0, 0, 10)
		)
		assertFalse(span.intersectsWith(zeroWidthWrap))

		// Empty line
		val emptyLineWrap = createLineWrap(
			line = 1,
			wrapStartsAtIndex = 0,
			virtualLength = 0,
			virtualLineIndex = 0,
			layoutResult = createMockedLayoutResult(0, 0, 0, 0)
		)
		assertFalse(span.intersectsWith(emptyLineWrap))

		// Span at line start
		val spanAtStart = RichSpan(
			range = TextEditorRange(
				start = CharLineOffset(1, 0),
				end = CharLineOffset(1, 5),
			),
			style = TestStyle()
		)
		val wrapAtStart = createLineWrap(
			line = 1,
			wrapStartsAtIndex = 0,
			virtualLength = 10,
			virtualLineIndex = 0,
			layoutResult = createMockedLayoutResult(0, 0, 10)
		)
		assertTrue(spanAtStart.intersectsWith(wrapAtStart))

		// Span at line end
		val spanAtEnd = RichSpan(
			range = TextEditorRange(
				start = CharLineOffset(1, 95),
				end = CharLineOffset(1, 100),
			),
			style = TestStyle()
		)
		val wrapAtEnd = createLineWrap(
			line = 1,
			wrapStartsAtIndex = 90,
			virtualLength = 10,
			virtualLineIndex = 1,
			layoutResult = createMockedLayoutResult(1, 90, 100, 2)
		)
		assertTrue(spanAtEnd.intersectsWith(wrapAtEnd))
	}
}