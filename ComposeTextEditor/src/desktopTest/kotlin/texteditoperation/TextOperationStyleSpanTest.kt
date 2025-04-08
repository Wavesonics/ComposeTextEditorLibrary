package texteditoperation

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import com.darkrockstudios.texteditor.CharLineOffset
import com.darkrockstudios.texteditor.TextEditorRange
import com.darkrockstudios.texteditor.TextEditorStyle
import com.darkrockstudios.texteditor.state.TextEditOperation
import com.darkrockstudios.texteditor.state.TextEditorState
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TextOperationStyleSpanTest {

	private lateinit var state: TextEditorState

	@BeforeTest
	fun setup() {
		state = createTestEditorState()
		state.setText("Hello World")
	}

	@Test
	fun `test add SpanStyle`() {
		val boldStyle = SpanStyle(fontWeight = FontWeight.Bold)
		val range = TextEditorRange(
			CharLineOffset(0, 6), // Start at "World"
			CharLineOffset(0, 11) // End after "World"
		)

		val operation = TextEditOperation.StyleSpan(
			range = range,
			style = boldStyle,
			isAdd = true,
			cursorBefore = state.cursorPosition,
			cursorAfter = state.cursorPosition
		)

		state.editManager.applyOperation(operation)

		val spanStyles = state.textLines[0].spanStyles
		assertEquals(1, spanStyles.size)
		val span = spanStyles.first()
		assertEquals(6, span.start)
		assertEquals(11, span.end)
		assertEquals(boldStyle, span.item)
	}

	@Test
	fun `test remove SpanStyle`() {
		val boldStyle = SpanStyle(fontWeight = FontWeight.Bold)
		val range = TextEditorRange(
			CharLineOffset(0, 6),
			CharLineOffset(0, 11)
		)

		// Add the span first
		state.addStyleSpan(range, boldStyle)

		// Now remove it
		val operation = TextEditOperation.StyleSpan(
			range = range,
			style = boldStyle,
			isAdd = false,
			cursorBefore = state.cursorPosition,
			cursorAfter = state.cursorPosition
		)

		state.editManager.applyOperation(operation)

		val spanStyles = state.textLines[0].spanStyles
		assertTrue(spanStyles.isEmpty())
	}

	@Test
	fun `test partial SpanStyle add`() {
		val boldStyle = SpanStyle(fontWeight = FontWeight.Bold)
		val range = TextEditorRange(
			CharLineOffset(0, 3), // Start inside "Hello"
			CharLineOffset(0, 8) // End inside "World"
		)

		val operation = TextEditOperation.StyleSpan(
			range = range,
			style = boldStyle,
			isAdd = true,
			cursorBefore = state.cursorPosition,
			cursorAfter = state.cursorPosition
		)

		state.editManager.applyOperation(operation)

		val spanStyles = state.textLines[0].spanStyles
		assertEquals(1, spanStyles.size)
		val span = spanStyles.first()
		assertEquals(3, span.start)
		assertEquals(8, span.end)
		assertEquals(boldStyle, span.item)
	}

	@Test
	fun `test multi-line SpanStyle add`() {
		state.setText("Hello\nWorld")
		val boldStyle = SpanStyle(fontWeight = FontWeight.Bold)
		val range = TextEditorRange(
			CharLineOffset(0, 3), // Inside "Hello"
			CharLineOffset(1, 3) // Inside "World"
		)

		val operation = TextEditOperation.StyleSpan(
			range = range,
			style = boldStyle,
			isAdd = true,
			cursorBefore = state.cursorPosition,
			cursorAfter = state.cursorPosition
		)

		state.editManager.applyOperation(operation)

		// Check spans in both lines
		val firstLineSpans = state.textLines[0].spanStyles
		assertEquals(1, firstLineSpans.size)
		assertEquals(3, firstLineSpans.first().start)
		assertEquals(5, firstLineSpans.first().end)
		assertEquals(boldStyle, firstLineSpans.first().item)

		val secondLineSpans = state.textLines[1].spanStyles
		assertEquals(1, secondLineSpans.size)
		assertEquals(0, secondLineSpans.first().start)
		assertEquals(3, secondLineSpans.first().end)
		assertEquals(boldStyle, secondLineSpans.first().item)
	}

	private fun createTestEditorState(): TextEditorState {
		return TextEditorState(
			scope = TestScope(),
			editorStyle = TextEditorStyle(),
			measurer = mockk(relaxed = true),
			initialText = null
		)
	}
}
