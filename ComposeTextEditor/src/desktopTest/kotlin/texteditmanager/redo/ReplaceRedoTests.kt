package texteditmanager.redo

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
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

class ReplaceRedoTests {
	private lateinit var scope: TestScope
	private lateinit var state: TextEditorState
	private val redStyle = SpanStyle(color = Color.Red)
	private val blueStyle = SpanStyle(color = Color.Blue)

	@BeforeTest
	fun setup() {
		scope = TestScope()
		state = TextEditorState(
			scope = scope.backgroundScope,
			measurer = mockk(relaxed = true)
		)
	}

	@Test
	fun `test redo basic single line replace`() {
		state.setText("Hello World")

		val range = TextEditorRange(
			start = CharLineOffset(0, 6),
			end = CharLineOffset(0, 11)
		)
		val replaceOperation = TextEditOperation.Replace(
			range = range,
			oldText = AnnotatedString("World"),
			newText = AnnotatedString("Everyone"),
			cursorBefore = CharLineOffset(0, 11),
			cursorAfter = CharLineOffset(0, 14),
			inheritStyle = false
		)

		state.editManager.applyOperation(replaceOperation)
		assertEquals("Hello Everyone", state.textLines[0].text)

		state.undo()
		assertEquals("Hello World", state.textLines[0].text)
		assertEquals(CharLineOffset(0, 11), state.cursorPosition)

		state.redo()
		assertEquals("Hello Everyone", state.textLines[0].text)
		assertEquals(CharLineOffset(0, 14), state.cursorPosition)
	}

	@Test
	fun `test redo replace with style inheritance`() {
		val initialText = buildAnnotatedString {
			append("Hello ")
			pushStyle(redStyle)
			append("World")
			pop()
		}
		state.setText(initialText)

		val range = TextEditorRange(
			start = CharLineOffset(0, 6),
			end = CharLineOffset(0, 11)
		)
		val replaceOperation = TextEditOperation.Replace(
			range = range,
			oldText = state.getTextInRange(range),
			newText = AnnotatedString("Everyone"),
			cursorBefore = CharLineOffset(0, 11),
			cursorAfter = CharLineOffset(0, 14),
			inheritStyle = true
		)

		state.editManager.applyOperation(replaceOperation)

		// Verify text and inherited style
		assertEquals("Hello Everyone", state.textLines[0].text)
		assertTrue(state.textLines[0].spanStyles.any {
			it.item == redStyle &&
					it.start == 6 &&
					it.end == 14
		})

		state.undo()
		state.redo()

		// Verify text and style after redo
		assertEquals("Hello Everyone", state.textLines[0].text)
		assertTrue(state.textLines[0].spanStyles.any {
			it.item == redStyle &&
					it.start == 6 &&
					it.end == 14
		})
	}

	@Test
	fun `test redo multi-line replace with single line`() {
		state.setText("First Line\nSecond Line\nThird Line")

		val range = TextEditorRange(
			start = CharLineOffset(0, 6),
			end = CharLineOffset(2, 5)
		)
		val replaceOperation = TextEditOperation.Replace(
			range = range,
			oldText = buildAnnotatedString {
				append("Line\nSecond Line\nThird")
			},
			newText = AnnotatedString("Text"),
			cursorBefore = CharLineOffset(2, 4),
			cursorAfter = CharLineOffset(0, 10),
			inheritStyle = false
		)

		state.editManager.applyOperation(replaceOperation)

		// Verify initial replace
		assertEquals(1, state.textLines.size)
		assertEquals("First Text Line", state.textLines[0].text)

		state.undo()
		state.redo()

		// Verify state after redo
		assertEquals(1, state.textLines.size)
		assertEquals("First Text Line", state.textLines[0].text)
		assertEquals(CharLineOffset(0, 10), state.cursorPosition)
	}

	@Test
	fun `test redo single line replace with multi-line`() {
		state.setText("Hello World")

		val range = TextEditorRange(
			start = CharLineOffset(0, 6),
			end = CharLineOffset(0, 11)
		)
		val replaceOperation = TextEditOperation.Replace(
			range = range,
			oldText = AnnotatedString("World"),
			newText = AnnotatedString("Beautiful\nWorld\nToday"),
			cursorBefore = CharLineOffset(0, 11),
			cursorAfter = CharLineOffset(2, 5),
			inheritStyle = false
		)

		state.editManager.applyOperation(replaceOperation)
		state.undo()
		state.redo()

		// Verify state after redo
		assertEquals(3, state.textLines.size)
		assertEquals("Hello Beautiful", state.textLines[0].text)
		assertEquals("World", state.textLines[1].text)
		assertEquals("Today", state.textLines[2].text)
		assertEquals(CharLineOffset(2, 5), state.cursorPosition)
	}

	@Test
	fun `test redo replace with mixed styles and inheritance`() {
		val initialText = buildAnnotatedString {
			pushStyle(redStyle)
			append("Hello ")
			pop()
			pushStyle(blueStyle)
			append("World")
			pop()
			append(" Today")
		}
		state.setText(initialText)

		val range = TextEditorRange(
			start = CharLineOffset(0, 3),
			end = CharLineOffset(0, 9)
		)
		val newText = buildAnnotatedString {
			pushStyle(SpanStyle(color = Color.Green))
			append("PPY WOR")
			pop()
		}
		val replaceOperation = TextEditOperation.Replace(
			range = range,
			oldText = state.getTextInRange(range),
			newText = newText,
			cursorBefore = CharLineOffset(0, 9),
			cursorAfter = CharLineOffset(0, 10),
			inheritStyle = true
		)

		state.editManager.applyOperation(replaceOperation)
		state.undo()
		state.redo()

		// Verify text and mixed styles after redo
		assertEquals("HelPPY WORld Today", state.textLines[0].text)
		assertEquals(CharLineOffset(0, 10), state.cursorPosition)
	}

	@Test
	fun `test redo replace entire document`() {
		val initialText = buildAnnotatedString {
			pushStyle(redStyle)
			append("Complete\n")
			pushStyle(blueStyle)
			append("Document\n")
			pop()
			append("Text")
			pop()
		}
		state.setText(initialText)

		val range = TextEditorRange(
			start = CharLineOffset(0, 0),
			end = CharLineOffset(2, 4)
		)
		val replaceOperation = TextEditOperation.Replace(
			range = range,
			oldText = initialText,
			newText = AnnotatedString("New Content"),
			cursorBefore = CharLineOffset(2, 4),
			cursorAfter = CharLineOffset(0, 11),
			inheritStyle = false
		)

		state.editManager.applyOperation(replaceOperation)
		state.undo()
		state.redo()

		// Verify state after redo
		assertEquals(1, state.textLines.size)
		assertEquals("New Content", state.textLines[0].text)
		assertTrue(state.textLines[0].spanStyles.isEmpty())
		assertEquals(CharLineOffset(0, 11), state.cursorPosition)
	}

	@Test
	fun `test redo replace with empty replacement`() {
		state.setText("Hello World")

		val range = TextEditorRange(
			start = CharLineOffset(0, 5),
			end = CharLineOffset(0, 11)
		)
		val replaceOperation = TextEditOperation.Replace(
			range = range,
			oldText = AnnotatedString(" World"),
			newText = AnnotatedString(""),
			cursorBefore = CharLineOffset(0, 11),
			cursorAfter = CharLineOffset(0, 5),
			inheritStyle = false
		)

		state.editManager.applyOperation(replaceOperation)
		state.undo()
		state.redo()

		// Verify state after redo
		assertEquals("Hello", state.textLines[0].text)
		assertEquals(CharLineOffset(0, 5), state.cursorPosition)
	}

	@Test
	fun `test redo replace with complex style inheritance across lines`() {
		val initialText = buildAnnotatedString {
			pushStyle(redStyle)
			append("First Line\n")
			pushStyle(blueStyle)
			append("Second Line\n")
			pop()
			append("Third Line")
			pop()
		}
		state.setText(initialText)

		val range = TextEditorRange(
			start = CharLineOffset(0, 6),
			end = CharLineOffset(2, 5)
		)
		val replaceOperation = TextEditOperation.Replace(
			range = range,
			oldText = state.getTextInRange(range),
			newText = AnnotatedString("NEW\nTEXT"),
			cursorBefore = CharLineOffset(2, 5),
			cursorAfter = CharLineOffset(1, 4),
			inheritStyle = true
		)

		state.editManager.applyOperation(replaceOperation)
		state.undo()
		state.redo()

		// Verify final state after redo
		assertEquals(2, state.textLines.size)
		assertEquals("First NEW", state.textLines[0].text)
		assertEquals("TEXT Line", state.textLines[1].text)
		assertEquals(CharLineOffset(1, 4), state.cursorPosition)
	}
}