package texteditmanager.undo

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

class InsertUndoTests {
	private lateinit var scope: TestScope
	private lateinit var state: TextEditorState

	@BeforeTest
	fun setup() {
		scope = TestScope()
		state = createTestEditorState(scope)
	}

	@Test
	fun `test undo single character insert`() {
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

		// Apply insert
		state.editManager.applyOperation(insertOperation)
		assertEquals("Hello!", state.textLines[0].text)

		// Undo
		state.undo()
		assertEquals(initialText, state.textLines[0].text)
		assertEquals(insertPos, state.cursorPosition)
	}

	@Test
	fun `test undo multi-character insert`() {
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
		assertEquals("Hello World", state.textLines[0].text)

		state.undo()
		assertEquals(initialText, state.textLines[0].text)
		assertEquals(insertPos, state.cursorPosition)
	}

	@Test
	fun `test undo insert with style`() {
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

		// Verify text and style after insert
		assertEquals("Hello World", state.textLines[0].text)
		assertTrue(state.textLines[0].spanStyles.any {
			it.item == boldStyle &&
					it.start == 5 &&
					it.end == 11
		})

		// Undo and verify style is removed
		state.undo()
		assertEquals(initialText, state.textLines[0].text)
		assertTrue(state.textLines[0].spanStyles.isEmpty())
	}

	@Test
	fun `test undo multi-line insert`() {
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

		// Verify after insert
		assertEquals(3, state.textLines.size)
		assertEquals("First", state.textLines[0].text)
		assertEquals("Second", state.textLines[1].text)
		assertEquals("Third line", state.textLines[2].text)

		// Undo and verify
		state.undo()
		assertEquals(1, state.textLines.size)
		assertEquals(initialText, state.textLines[0].text)
		assertEquals(insertPos, state.cursorPosition)
	}

	@Test
	fun `test undo insert with mixed styles`() {
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

		// Verify both styles exist after insert
		val line = state.textLines[0]
		assertEquals("Hello World", line.text)
		assertTrue(line.spanStyles.any { it.item == SpanStyle(color = Color.Blue) })
		assertTrue(line.spanStyles.any { it.item == SpanStyle(color = Color.Red) })

		// Undo and verify only original style remains
		state.undo()
		assertEquals("Hello", state.textLines[0].text)
		assertEquals(1, state.textLines[0].spanStyles.size)
		assertTrue(state.textLines[0].spanStyles.any {
			it.item == SpanStyle(color = Color.Blue)
		})
	}

	@Test
	fun `test undo insert at start of text`() {
		val initialText = "End"
		state.setText(initialText)

		val insertOperation = TextEditOperation.Insert(
			position = CharLineOffset(0, 0),
			text = AnnotatedString("Start "),
			cursorBefore = CharLineOffset(0, 0),
			cursorAfter = CharLineOffset(0, 6)
		)

		state.editManager.applyOperation(insertOperation)
		assertEquals("Start End", state.textLines[0].text)

		state.undo()
		assertEquals(initialText, state.textLines[0].text)
		assertEquals(CharLineOffset(0, 0), state.cursorPosition)
	}

	@Test
	fun `test undo insert in middle of styled text`() {
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

		// Verify style is preserved and spans entire text
		val line = state.textLines[0]
		assertEquals("Hello new World", line.text)
		assertEquals(1, line.spanStyles.size)
		assertEquals(0, line.spanStyles[0].start)
		assertEquals(line.text.length, line.spanStyles[0].end)

		// Undo and verify original style is intact
		state.undo()
		assertEquals("HelloWorld", state.textLines[0].text)
		assertEquals(1, state.textLines[0].spanStyles.size)
		assertEquals(0, state.textLines[0].spanStyles[0].start)
		assertEquals(10, state.textLines[0].spanStyles[0].end)
	}

	private fun createTestEditorState(scope: TestScope): TextEditorState {
		return TextEditorState(
			scope = scope.backgroundScope,
			editorStyle = TextEditorStyle(),
			measurer = mockk(relaxed = true)
		)
	}
}