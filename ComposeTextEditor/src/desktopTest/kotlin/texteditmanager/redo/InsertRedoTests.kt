package texteditmanager.redo

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.darkrockstudios.texteditor.CharLineOffset
import com.darkrockstudios.texteditor.TextEditorStyle
import com.darkrockstudios.texteditor.state.TextEditOperation
import com.darkrockstudios.texteditor.state.TextEditorState
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InsertRedoTests {
	private lateinit var scope: TestScope
	private lateinit var state: TextEditorState

	@BeforeTest
	fun setup() {
		scope = TestScope()
		state = createTestEditorState(scope)
	}

	@Test
	fun `test redo single character insert`() {
		// Initial state
		val initialText = "Hello"
		state.setText(initialText)

		// Insert operation
		val insertPos = CharLineOffset(0, 5)
		val insertOperation = TextEditOperation.Insert(
			position = insertPos,
			text = AnnotatedString("!"),
			cursorBefore = insertPos,
			cursorAfter = CharLineOffset(0, 6)
		)

		// Apply insert, undo, then redo
		state.editManager.applyOperation(insertOperation)
		state.undo()
		state.redo()

		assertEquals("Hello!", state.textLines[0].text)
		assertEquals(CharLineOffset(0, 6), state.cursorPosition)
	}

	@Test
	fun `test redo multi-character insert`() {
		val initialText = "Hello"
		state.setText(initialText)

		val insertPos = CharLineOffset(0, 5)
		val insertOperation = TextEditOperation.Insert(
			position = insertPos,
			text = AnnotatedString(" World"),
			cursorBefore = insertPos,
			cursorAfter = CharLineOffset(0, 11)
		)

		state.editManager.applyOperation(insertOperation)
		state.undo()
		state.redo()

		assertEquals("Hello World", state.textLines[0].text)
		assertEquals(CharLineOffset(0, 11), state.cursorPosition)
	}

	@Test
	fun `test redo insert with style`() {
		val initialText = "Hello"
		state.setText(initialText)

		val boldStyle = SpanStyle(color = Color.Red)
		val insertText = buildAnnotatedString {
			pushStyle(boldStyle)
			append(" World")
			pop()
		}

		val insertPos = CharLineOffset(0, 5)
		val insertOperation = TextEditOperation.Insert(
			position = insertPos,
			text = insertText,
			cursorBefore = insertPos,
			cursorAfter = CharLineOffset(0, 11)
		)

		state.editManager.applyOperation(insertOperation)
		state.undo()
		state.redo()

		// Verify text and style after redo
		assertEquals("Hello World", state.textLines[0].text)
		assertTrue(state.textLines[0].spanStyles.any {
			it.item == boldStyle &&
					it.start == 5 &&
					it.end == 11
		})
	}

	@Test
	fun `test redo multi-line insert`() {
		val initialText = "First line"
		state.setText(initialText)

		val insertPos = CharLineOffset(0, 5)
		val insertOperation = TextEditOperation.Insert(
			position = insertPos,
			text = AnnotatedString("\nSecond\nThird"),
			cursorBefore = insertPos,
			cursorAfter = CharLineOffset(2, 5)
		)

		state.editManager.applyOperation(insertOperation)
		state.undo()
		state.redo()

		// Verify after redo
		assertEquals(3, state.textLines.size)
		assertEquals("First", state.textLines[0].text)
		assertEquals("Second", state.textLines[1].text)
		assertEquals("Third line", state.textLines[2].text)
		assertEquals(CharLineOffset(2, 5), state.cursorPosition)
	}

	@Test
	fun `test redo insert with mixed styles`() {
		val initialText = buildAnnotatedString {
			pushStyle(SpanStyle(color = Color.Blue))
			append("Hello")
			pop()
		}
		state.setText(initialText)

		val insertText = buildAnnotatedString {
			pushStyle(SpanStyle(color = Color.Red))
			append(" World")
			pop()
		}

		val insertPos = CharLineOffset(0, 5)
		val insertOperation = TextEditOperation.Insert(
			position = insertPos,
			text = insertText,
			cursorBefore = insertPos,
			cursorAfter = CharLineOffset(0, 11)
		)

		state.editManager.applyOperation(insertOperation)
		state.undo()
		state.redo()

		// Verify both styles exist after redo
		val line = state.textLines[0]
		assertEquals("Hello World", line.text)
		assertTrue(line.spanStyles.any { it.item == SpanStyle(color = Color.Blue) })
		assertTrue(line.spanStyles.any { it.item == SpanStyle(color = Color.Red) })
	}

	@Test
	fun `test redo insert at start of text`() {
		val initialText = "End"
		state.setText(initialText)

		val insertOperation = TextEditOperation.Insert(
			position = CharLineOffset(0, 0),
			text = AnnotatedString("Start "),
			cursorBefore = CharLineOffset(0, 0),
			cursorAfter = CharLineOffset(0, 6)
		)

		state.editManager.applyOperation(insertOperation)
		state.undo()
		state.redo()

		assertEquals("Start End", state.textLines[0].text)
		assertEquals(CharLineOffset(0, 6), state.cursorPosition)
	}

	@Test
	fun `test redo insert in middle of styled text`() {
		val initialText = buildAnnotatedString {
			pushStyle(SpanStyle(color = Color.Blue))
			append("HelloWorld")
			pop()
		}
		state.setText(initialText)

		val insertPos = CharLineOffset(0, 5)
		val insertOperation = TextEditOperation.Insert(
			position = insertPos,
			text = AnnotatedString(" new "),
			cursorBefore = insertPos,
			cursorAfter = CharLineOffset(0, 10)
		)

		state.editManager.applyOperation(insertOperation)
		state.undo()
		state.redo()

		// Verify style is preserved and spans entire text
		val line = state.textLines[0]
		assertEquals("Hello new World", line.text)
		assertEquals(1, line.spanStyles.size)
		assertEquals(0, line.spanStyles[0].start)
		assertEquals(line.text.length, line.spanStyles[0].end)
	}

	@Test
	fun `test redo unavailable after new operation`() {
		val initialText = "Hello"
		state.setText(initialText)

		// First operation
		val insertOperation1 = TextEditOperation.Insert(
			position = CharLineOffset(0, 5),
			text = AnnotatedString("!"),
			cursorBefore = CharLineOffset(0, 5),
			cursorAfter = CharLineOffset(0, 6)
		)

		state.editManager.applyOperation(insertOperation1)
		state.undo()

		// New operation should clear redo stack
		val insertOperation2 = TextEditOperation.Insert(
			position = CharLineOffset(0, 5),
			text = AnnotatedString("?"),
			cursorBefore = CharLineOffset(0, 5),
			cursorAfter = CharLineOffset(0, 6)
		)

		state.editManager.applyOperation(insertOperation2)

		// Attempt to redo first operation
		state.redo()

		// Should have second operation's result, not first
		assertEquals("Hello?", state.textLines[0].text)
	}

	private fun createTestEditorState(scope: TestScope): TextEditorState {
		return TextEditorState(
			scope = scope.backgroundScope,
			editorStyle = TextEditorStyle(),
			measurer = mockk(relaxed = true)
		)
	}
}