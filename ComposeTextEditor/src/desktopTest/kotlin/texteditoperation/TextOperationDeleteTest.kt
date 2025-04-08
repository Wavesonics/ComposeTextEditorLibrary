package texteditoperation

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import com.darkrockstudios.texteditor.CharLineOffset
import com.darkrockstudios.texteditor.LineWrap
import com.darkrockstudios.texteditor.TextEditorRange
import com.darkrockstudios.texteditor.TextEditorStyle
import com.darkrockstudios.texteditor.richstyle.RichSpanStyle
import com.darkrockstudios.texteditor.state.TextEditOperation
import com.darkrockstudios.texteditor.state.TextEditorState
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class TextOperationDeleteTest {
	private lateinit var state: TextEditorState
	private lateinit var testRichSpanStyle: TestRichSpanStyle

	@BeforeTest
	fun setup() {
		state = createTestEditorState()
		testRichSpanStyle = TestRichSpanStyle()
	}

	@Test
	fun `test delete single character`() {
		state.setText("Hello World")

		val operation = TextEditOperation.Delete(
			range = TextEditorRange(
				start = CharLineOffset(0, 5),
				end = CharLineOffset(0, 6)
			),
			cursorBefore = CharLineOffset(0, 6),
			cursorAfter = CharLineOffset(0, 5)
		)

		state.editManager.applyOperation(operation)

		assertEquals("HelloWorld", state.textLines[0].text)
	}

	@Test
	fun `test delete across lines`() {
		state.setText("Hello\nWorld")

		val operation = TextEditOperation.Delete(
			range = TextEditorRange(
				start = CharLineOffset(0, 5),
				end = CharLineOffset(1, 3)
			),
			cursorBefore = CharLineOffset(1, 3),
			cursorAfter = CharLineOffset(0, 5)
		)

		state.editManager.applyOperation(operation)

		assertEquals("Hellold", state.textLines[0].text)
		assertEquals(1, state.textLines.size)
	}

	@Test
	fun `test delete entire line`() {
		state.setText("Line1\nLine2\nLine3")

		val operation = TextEditOperation.Delete(
			range = TextEditorRange(
				start = CharLineOffset(1, 0),
				end = CharLineOffset(2, 0)
			),
			cursorBefore = CharLineOffset(2, 0),
			cursorAfter = CharLineOffset(1, 0)
		)

		state.editManager.applyOperation(operation)

		assertEquals(2, state.textLines.size)
		assertEquals("Line1", state.textLines[0].text)
		assertEquals("Line3", state.textLines[1].text)
	}

	@Test
	fun `test delete with rich spans`() {
		state.setText("Hello World")
		state.richSpanManager.addRichSpan(
			range = TextEditorRange(
				start = CharLineOffset(0, 0),
				end = CharLineOffset(0, 5)
			),
			style = testRichSpanStyle
		)

		val operation = TextEditOperation.Delete(
			range = TextEditorRange(
				start = CharLineOffset(0, 2),
				end = CharLineOffset(0, 4)
			),
			cursorBefore = CharLineOffset(0, 4),
			cursorAfter = CharLineOffset(0, 2)
		)

		state.editManager.applyOperation(operation)

		assertEquals("Heo World", state.textLines[0].text)

		val spans = state.richSpanManager.getAllRichSpans()
		assertEquals(1, spans.size)
		val span = spans.first()
		assertEquals(0, span.range.start.char)
		assertEquals(3, span.range.end.char) // Adjusted after deletion
	}

	@Test
	fun `test delete splitting rich spans across lines`() {
		state.setText("Hello\nWorld")

		state.richSpanManager.addRichSpan(
			range = TextEditorRange(
				start = CharLineOffset(0, 0),
				end = CharLineOffset(1, 5)
			),
			style = testRichSpanStyle
		)

		val operation = TextEditOperation.Delete(
			range = TextEditorRange(
				start = CharLineOffset(0, 3),
				end = CharLineOffset(1, 2)
			),
			cursorBefore = CharLineOffset(1, 2),
			cursorAfter = CharLineOffset(0, 3)
		)

		state.editManager.applyOperation(operation)

		assertEquals("Helrld", state.textLines[0].text)
		assertEquals(1, state.textLines.size)

		val spans = state.richSpanManager.getAllRichSpans()
		assertEquals(1, spans.size)

		val span = spans.first()
		assertEquals(0, span.range.start.char)
		assertEquals(6, span.range.end.char)
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
			editorStyle = TextEditorStyle(),
			measurer = mockk(relaxed = true),
			initialText = AnnotatedString("")
		)
	}
}
