package texteditmanager.undo

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.darkrockstudios.texteditor.CharLineOffset
import com.darkrockstudios.texteditor.TextEditorRange
import com.darkrockstudios.texteditor.state.TextEditOperation
import com.darkrockstudios.texteditor.state.TextEditorState
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeleteUndoTests {
	private lateinit var scope: TestScope
	private lateinit var state: TextEditorState

	@BeforeTest
	fun setup() {
		scope = TestScope()
		state = TextEditorState(
			scope = scope.backgroundScope,
			measurer = mockk(relaxed = true)
		)
	}

	@Test
	fun `test undo single character delete`() {
		val initialText = "Hello World"
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
		assertEquals("HelloWorld", state.textLines[0].text)

		state.undo()
		assertEquals(initialText, state.textLines[0].text)
		assertEquals(CharLineOffset(0, 6), state.cursorPosition)
	}

	@Test
	fun `test undo multi-character delete`() {
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
		assertEquals("Hello", state.textLines[0].text)

		state.undo()
		assertEquals(initialText, state.textLines[0].text)
		assertEquals(CharLineOffset(0, 11), state.cursorPosition)
	}

	@Test
	fun `test undo delete of styled text`() {
		val initialText = buildAnnotatedString {
			append("Hello ")
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

		// Verify text and style after delete
		assertEquals("Hello", state.textLines[0].text)
		assertTrue(state.textLines[0].spanStyles.isEmpty())

		// Undo and verify style is restored
		state.undo()
		assertEquals("Hello World", state.textLines[0].text)
		assertTrue(state.textLines[0].spanStyles.any {
			it.item == SpanStyle(color = Color.Red) &&
					it.start == 6 &&
					it.end == 11
		})
	}

	@Test
	fun `test undo multi-line delete`() {
		state.setText("Line 1\nLine 2\nLine 3")

		val deleteRange = TextEditorRange(
			start = CharLineOffset(0, 3),
			end = CharLineOffset(2, 3)
		)
		val deleteOperation = TextEditOperation.Delete(
			range = deleteRange,
			cursorBefore = CharLineOffset(2, 3),
			cursorAfter = CharLineOffset(0, 3)
		)

		state.editManager.applyOperation(deleteOperation)

		// Verify after delete
		assertEquals(1, state.textLines.size)
		assertEquals("Line 3", state.textLines[0].text)

		// Undo and verify
		state.undo()
		assertEquals(3, state.textLines.size)
		assertEquals("Line 1", state.textLines[0].text)
		assertEquals("Line 2", state.textLines[1].text)
		assertEquals("Line 3", state.textLines[2].text)
		assertEquals(CharLineOffset(2, 3), state.cursorPosition)
	}

	@Test
	fun `test undo delete across styled regions`() {
		val initialText = buildAnnotatedString {
			pushStyle(SpanStyle(color = Color.Blue))
			append("Blue")
			pop()
			append(" ")
			pushStyle(SpanStyle(color = Color.Red))
			append("Red")
			pop()
		}
		state.setText(initialText)

		val deleteRange = TextEditorRange(
			start = CharLineOffset(0, 3),
			end = CharLineOffset(0, 6)
		)
		val deleteOperation = TextEditOperation.Delete(
			range = deleteRange,
			cursorBefore = CharLineOffset(0, 6),
			cursorAfter = CharLineOffset(0, 3)
		)

		state.editManager.applyOperation(deleteOperation)

		// Verify partial styles remain
		val line = state.textLines[0]
		assertEquals("Blued", line.text)
		assertTrue(line.spanStyles.any { it.item == SpanStyle(color = Color.Blue) })
		assertTrue(line.spanStyles.any { it.item == SpanStyle(color = Color.Red) })

		// Undo and verify original styles are restored
		state.undo()
		assertEquals("Blue Red", state.textLines[0].text)
		assertEquals(2, state.textLines[0].spanStyles.size)
		assertTrue(state.textLines[0].spanStyles.any {
			it.item == SpanStyle(color = Color.Blue) &&
					it.start == 0 &&
					it.end == 4
		})
		assertTrue(state.textLines[0].spanStyles.any {
			it.item == SpanStyle(color = Color.Red) &&
					it.start == 5 &&
					it.end == 8
		})
	}

	@Test
	fun `test undo delete at line boundaries`() {
		state.setText("Line 1\nLine 2\nLine 3")

		val deleteRange = TextEditorRange(
			start = CharLineOffset(0, 6),
			end = CharLineOffset(1, 0)
		)
		val deleteOperation = TextEditOperation.Delete(
			range = deleteRange,
			cursorBefore = CharLineOffset(1, 0),
			cursorAfter = CharLineOffset(0, 6)
		)

		state.editManager.applyOperation(deleteOperation)

		// Verify lines were joined
		assertEquals(2, state.textLines.size)
		assertEquals("Line 1Line 2", state.textLines[0].text)

		// Undo and verify line break is restored
		state.undo()
		assertEquals(3, state.textLines.size)
		assertEquals("Line 1", state.textLines[0].text)
		assertEquals("Line 2", state.textLines[1].text)
		assertEquals(CharLineOffset(1, 0), state.cursorPosition)
	}

	@Test
	fun `test undo delete of empty line`() {
		state.setText("Line 1\n\nLine 3")

		val deleteRange = TextEditorRange(
			start = CharLineOffset(1, 0),
			end = CharLineOffset(2, 0)
		)
		val deleteOperation = TextEditOperation.Delete(
			range = deleteRange,
			cursorBefore = CharLineOffset(2, 0),
			cursorAfter = CharLineOffset(1, 0)
		)

		state.editManager.applyOperation(deleteOperation)
		assertEquals(2, state.textLines.size)
		assertEquals("Line 1", state.textLines[0].text)
		assertEquals("Line 3", state.textLines[1].text)

		state.undo()
		assertEquals(3, state.textLines.size)
		assertEquals("Line 1", state.textLines[0].text)
		assertEquals("", state.textLines[1].text)
		assertEquals("Line 3", state.textLines[2].text)
		assertEquals(CharLineOffset(2, 0), state.cursorPosition)
	}
}