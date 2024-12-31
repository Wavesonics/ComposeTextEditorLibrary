package com.darkrockstudios.texteditor.state

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.darkrockstudios.texteditor.CharLineOffset
import com.darkrockstudios.texteditor.LineWrap
import com.darkrockstudios.texteditor.TextEditorRange
import com.darkrockstudios.texteditor.richstyle.RichSpanStyle
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class TextOperationInsertTest {
	private lateinit var state: TextEditorState
	private lateinit var testRichSpanStyle: TestRichSpanStyle

	@BeforeTest
	fun setup() {
		state = createTestEditorState()
		testRichSpanStyle = TestRichSpanStyle()
	}

	@Test
	fun `test simple insert into empty document`() {
		val operation = TextEditOperation.Insert(
			position = CharLineOffset(0, 0),
			text = AnnotatedString("Hello"),
			cursorBefore = CharLineOffset(0, 0),
			cursorAfter = CharLineOffset(0, 5)
		)

		state.editManager.applyOperation(operation)

		assertEquals("Hello", state.textLines[0].text)
		assertEquals(1, state.textLines.size)
	}

	@Test
	fun `test insert with existing content`() {
		state.setText("Hello World")

		val operation = TextEditOperation.Insert(
			position = CharLineOffset(0, 5),
			text = AnnotatedString(" Beautiful"),
			cursorBefore = CharLineOffset(0, 5),
			cursorAfter = CharLineOffset(0, 14)
		)

		state.editManager.applyOperation(operation)

		assertEquals("Hello Beautiful World", state.textLines[0].text)
	}

	@Test
	fun `test insert with newline`() {
		state.setText("Hello World")

		val operation = TextEditOperation.Insert(
			position = CharLineOffset(0, 5),
			text = AnnotatedString("\n"),
			cursorBefore = CharLineOffset(0, 5),
			cursorAfter = CharLineOffset(1, 0)
		)

		state.editManager.applyOperation(operation)

		assertEquals(2, state.textLines.size)
		assertEquals("Hello", state.textLines[0].text)
		assertEquals(" World", state.textLines[1].text)
	}

	@Test
	fun `test multi-line insert`() {
		state.setText("Start End")

		val operation = TextEditOperation.Insert(
			position = CharLineOffset(0, 5),
			text = AnnotatedString("Line 1\nLine 2\nLine 3"),
			cursorBefore = CharLineOffset(0, 5),
			cursorAfter = CharLineOffset(2, 6)
		)

		state.editManager.applyOperation(operation)

		assertEquals(3, state.textLines.size)
		assertEquals("StartLine 1", state.textLines[0].text)
		assertEquals("Line 2", state.textLines[1].text)
		assertEquals("Line 3 End", state.textLines[2].text)
	}

	@Test
	fun `test insert with SpanStyle`() {
		state.setText("Hello World")
		val boldStyle = SpanStyle(fontWeight = FontWeight.Bold)

		// Create text with a bold span
		val textToInsert = buildAnnotatedString {
			append("Bold ")
			addStyle(boldStyle, 0, 4)
		}

		val operation = TextEditOperation.Insert(
			position = CharLineOffset(0, 6),
			text = textToInsert,
			cursorBefore = CharLineOffset(0, 6),
			cursorAfter = CharLineOffset(0, 10)
		)

		state.editManager.applyOperation(operation)

		assertEquals("Hello Bold World", state.textLines[0].text)
		assertEquals(1, state.textLines[0].spanStyles.size)

		val resultSpan = state.textLines[0].spanStyles[0]
		assertEquals(6, resultSpan.start)
		assertEquals(10, resultSpan.end)
		assertEquals(boldStyle, resultSpan.item)
	}

	@Test
	fun `test insert with RichSpan`() {
		state.setText("Hello World")

		// Add a RichSpan to the initial text
		state.richSpanManager.addRichSpan(
			range = TextEditorRange(
				start = CharLineOffset(0, 0),
				end = CharLineOffset(0, 5)
			),
			style = testRichSpanStyle
		)

		// Insert text in the middle of the span
		val operation = TextEditOperation.Insert(
			position = CharLineOffset(0, 2),
			text = AnnotatedString("NEW"),
			cursorBefore = CharLineOffset(0, 2),
			cursorAfter = CharLineOffset(0, 5)
		)

		state.editManager.applyOperation(operation)

		assertEquals("HeNEWllo World", state.textLines[0].text)

		// Verify the RichSpan was adjusted
		val spans = state.richSpanManager.getAllRichSpans()
		assertEquals(1, spans.size)

		val adjustedSpan = spans.first()
		assertEquals(0, adjustedSpan.range.start.char)
		assertEquals(8, adjustedSpan.range.end.char) // Original 5 + 3 inserted chars
	}

	@Test
	fun `test insert splitting RichSpan with newline`() {
		state.setText("HelloWorld")

		// Add a RichSpan covering the whole text
		state.richSpanManager.addRichSpan(
			range = TextEditorRange(
				start = CharLineOffset(0, 0),
				end = CharLineOffset(0, 10)
			),
			style = testRichSpanStyle
		)

		// Insert newline in the middle
		val operation = TextEditOperation.Insert(
			position = CharLineOffset(0, 5),
			text = AnnotatedString("\n"),
			cursorBefore = CharLineOffset(0, 5),
			cursorAfter = CharLineOffset(1, 0)
		)

		state.editManager.applyOperation(operation)

		assertEquals(2, state.textLines.size)
		assertEquals("Hello", state.textLines[0].text)
		assertEquals("World", state.textLines[1].text)

		// Verify the RichSpan was split
		val spans = state.richSpanManager.getAllRichSpans()
		assertEquals(2, spans.size)

		val firstSpan = spans.find { it.range.start.line == 0 }!!
		assertEquals(0, firstSpan.range.start.char)
		assertEquals(5, firstSpan.range.end.char)

		val secondSpan = spans.find { it.range.start.line == 1 }!!
		assertEquals(0, secondSpan.range.start.char)
		assertEquals(5, secondSpan.range.end.char)
	}

	@Test
	fun `test insert preserves existing SpanStyles`() {
		// Create initial text with a span
		val boldStyle = SpanStyle(fontWeight = FontWeight.Bold)
		val initialText = buildAnnotatedString {
			append("Hello World")
			addStyle(boldStyle, 0, 5) // "Hello" is bold
		}
		state.setText(initialText)

		// Insert text after the span
		val operation = TextEditOperation.Insert(
			position = CharLineOffset(0, 6),
			text = AnnotatedString("Beautiful "),
			cursorBefore = CharLineOffset(0, 7),
			cursorAfter = CharLineOffset(0, 16)
		)

		state.editManager.applyOperation(operation)

		assertEquals("Hello Beautiful World", state.textLines[0].text)
		assertEquals(1, state.textLines[0].spanStyles.size)

		assertEquals(1, state.textLines[0].spanStyles.size)
		val resultSpan = state.textLines[0].spanStyles[0]
		assertEquals(0, resultSpan.start)
		assertEquals(5, resultSpan.end)
		assertEquals(boldStyle, resultSpan.item)
	}

	private class TestRichSpanStyle : RichSpanStyle {
		override fun DrawScope.drawCustomStyle(
			layoutResult: TextLayoutResult,
			lineWrap: LineWrap,
			textRange: TextRange
		) {
			// No-op for testing
		}
	}

	private fun createTestEditorState(): TextEditorState {
		return TextEditorState(
			scope = TestScope(),
			measurer = mockk(relaxed = true),
			initialText = AnnotatedString("")
		)
	}
}