package texteditmanager.redo

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
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

class DeleteRedoTests {
	private lateinit var scope: TestScope
	private lateinit var state: TextEditorState

	@BeforeTest
	fun setup() {
		scope = TestScope()
		state = createTestEditorState(scope)
	}

	@Test
	fun `test redo single character delete`() {
		val initialText = "Hello!"
		state.setText(initialText)

		val deleteRange = TextEditorRange(
			start = CharLineOffset(0, 5),
			end = CharLineOffset(0, 6)
		)
		val deleteOperation = TextEditOperation.Delete(
			range = deleteRange,
			cursorBefore = CharLineOffset(0, 6),
			cursorAfter = CharLineOffset(0, 5)
		)

		state.editManager.applyOperation(deleteOperation)
		state.undo()
		state.redo()

		assertEquals("Hello", state.textLines[0].text)
		assertEquals(CharLineOffset(0, 5), state.cursorPosition)
	}

	@Test
	fun `test redo multi-character delete`() {
		val initialText = "Hello World"
		state.setText(initialText)

		val deleteRange = TextEditorRange(
			start = CharLineOffset(0, 5),
			end = CharLineOffset(0, 11)
		)
		val deleteOperation = TextEditOperation.Delete(
			range = deleteRange,
			cursorBefore = CharLineOffset(0, 11),
			cursorAfter = CharLineOffset(0, 5)
		)

		state.editManager.applyOperation(deleteOperation)
		state.undo()
		state.redo()

		assertEquals("Hello", state.textLines[0].text)
		assertEquals(CharLineOffset(0, 5), state.cursorPosition)
	}

	@Test
	fun `test redo delete with styled text`() {
		val initialText = buildAnnotatedString {
			append("Hello")
			pushStyle(SpanStyle(color = Color.Red))
			append(" World")
			pop()
		}
		state.setText(initialText)

		val deleteRange = TextEditorRange(
			start = CharLineOffset(0, 5),
			end = CharLineOffset(0, 11)
		)
		val deleteOperation = TextEditOperation.Delete(
			range = deleteRange,
			cursorBefore = CharLineOffset(0, 11),
			cursorAfter = CharLineOffset(0, 5)
		)

		state.editManager.applyOperation(deleteOperation)
		state.undo()
		state.redo()

		assertEquals("Hello", state.textLines[0].text)
		assertTrue(state.textLines[0].spanStyles.isEmpty())
	}

	@Test
	fun `test redo multi-line delete`() {
		state.setText("First line")
		state.insertNewlineAtCursor()
		state.setText(buildAnnotatedString {
			append("First line")
			append("\nSecond line")
			append("\nThird line")
		})

		val deleteRange = TextEditorRange(
			start = CharLineOffset(0, 5),
			end = CharLineOffset(2, 5)
		)
		val deleteOperation = TextEditOperation.Delete(
			range = deleteRange,
			cursorBefore = CharLineOffset(2, 4),
			cursorAfter = CharLineOffset(0, 5)
		)

		state.editManager.applyOperation(deleteOperation)
		state.undo()
		state.redo()

		assertEquals("First line", state.textLines[0].text)
		assertEquals(1, state.textLines.size)
		assertEquals(CharLineOffset(0, 5), state.cursorPosition)
	}

	@Test
	fun `test redo delete preserving surrounding styles`() {
		val initialText = buildAnnotatedString {
			pushStyle(SpanStyle(color = Color.Blue))
			append("Hello")
			pop()
			append(" some ")
			pushStyle(SpanStyle(color = Color.Red))
			append("World")
			pop()
		}
		state.setText(initialText)

		val deleteRange = TextEditorRange(
			start = CharLineOffset(0, 5),
			end = CharLineOffset(0, 11)
		)
		val deleteOperation = TextEditOperation.Delete(
			range = deleteRange,
			cursorBefore = CharLineOffset(0, 11),
			cursorAfter = CharLineOffset(0, 5)
		)

		state.editManager.applyOperation(deleteOperation)
		state.undo()
		state.redo()

		val line = state.textLines[0]
		assertEquals("HelloWorld", line.text)

		// Verify surrounding styles are preserved
		assertTrue(line.spanStyles.any {
			it.item == SpanStyle(color = Color.Blue) &&
					it.start == 0 &&
					it.end == 5
		})
		assertTrue(line.spanStyles.any {
			it.item == SpanStyle(color = Color.Red) &&
					it.start == 5 &&
					it.end == 10
		})
	}

	@Test
	fun `test redo partial style delete`() {
		val initialText = buildAnnotatedString {
			pushStyle(SpanStyle(color = Color.Blue))
			append("HelloWorld")
			pop()
		}
		state.setText(initialText)

		val deleteRange = TextEditorRange(
			start = CharLineOffset(0, 2),
			end = CharLineOffset(0, 7)
		)
		val deleteOperation = TextEditOperation.Delete(
			range = deleteRange,
			cursorBefore = CharLineOffset(0, 7),
			cursorAfter = CharLineOffset(0, 2)
		)

		state.editManager.applyOperation(deleteOperation)
		state.undo()
		state.redo()

		val line = state.textLines[0]
		assertEquals("Herld", line.text)

		// Verify style spans entire remaining text
		assertEquals(1, line.spanStyles.size)
		assertEquals(0, line.spanStyles[0].start)
		assertEquals(5, line.spanStyles[0].end)
		assertEquals(SpanStyle(color = Color.Blue), line.spanStyles[0].item)
	}

	@Test
	fun `test redo delete with multiple style boundaries`() {
		val initialText = buildAnnotatedString {
			pushStyle(SpanStyle(color = Color.Blue))
			append("Hello")
			pop()
			pushStyle(SpanStyle(color = Color.Red))
			append(" some ")
			pop()
			pushStyle(SpanStyle(color = Color.Green))
			append("text")
			pop()
			append(" here")
		}
		state.setText(initialText)

		val deleteRange = TextEditorRange(
			start = CharLineOffset(0, 3),
			end = CharLineOffset(0, 11)
		)
		val deleteOperation = TextEditOperation.Delete(
			range = deleteRange,
			cursorBefore = CharLineOffset(0, 12),
			cursorAfter = CharLineOffset(0, 3)
		)

		state.editManager.applyOperation(deleteOperation)
		state.undo()
		state.redo()

		val line = state.textLines[0]
		assertEquals("Heltext here", line.text)

		// Verify remaining styles are preserved and properly positioned
		assertTrue(line.spanStyles.any {
			it.item == SpanStyle(color = Color.Blue) &&
					it.start == 0 &&
					it.end == 3
		})
		assertTrue(line.spanStyles.any {
			it.item == SpanStyle(color = Color.Green) &&
					it.start == 3 &&
					it.end == 7
		})
	}

	@Test
	fun `test redo unavailable after new operation`() {
		val initialText = "Hello World"
		state.setText(initialText)

		// First delete operation
		val deleteRange1 = TextEditorRange(
			start = CharLineOffset(0, 5),
			end = CharLineOffset(0, 6)
		)
		val deleteOperation1 = TextEditOperation.Delete(
			range = deleteRange1,
			cursorBefore = CharLineOffset(0, 6),
			cursorAfter = CharLineOffset(0, 5)
		)

		state.editManager.applyOperation(deleteOperation1)
		state.undo()
		assertEquals("Hello World", state.textLines[0].text)

		// New operation should clear redo stack
		val deleteRange2 = TextEditorRange(
			start = CharLineOffset(0, 10),
			end = CharLineOffset(0, 11)
		)
		val deleteOperation2 = TextEditOperation.Delete(
			range = deleteRange2,
			cursorBefore = CharLineOffset(0, 11),
			cursorAfter = CharLineOffset(0, 10)
		)

		// This will clear the redo stack
		state.editManager.applyOperation(deleteOperation2)

		// Attempt redo of first operation, but will do nothing because it was cleared
		state.redo()

		// Should maintain second operation's result
		assertEquals("Hello Worl", state.textLines[0].text)
	}

	private fun createTestEditorState(scope: TestScope): TextEditorState {
		return TextEditorState(
			scope = scope.backgroundScope,
			editorStyle = TextEditorStyle(),
			measurer = mockk(relaxed = true)
		)
	}
}