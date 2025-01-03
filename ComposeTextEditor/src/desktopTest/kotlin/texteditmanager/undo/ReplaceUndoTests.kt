package texteditmanager.undo

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

class ReplaceUndoTests {
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
	fun `test undo basic single line replace`() {
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
	}


	@Test
	fun `test undo replace with style inheritance`() {
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
			oldText = AnnotatedString("World"),
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

		// Undo and verify original state
		state.undo()
		assertEquals("Hello World", state.textLines[0].text)
		assertTrue(state.textLines[0].spanStyles.any {
			it.item == redStyle &&
					it.start == 6 &&
					it.end == 11
		})
	}

	@Test
	fun `test undo multi-line replace with single line`() {
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

		// Verify collapsed to single line
		assertEquals(1, state.textLines.size)
		assertEquals("First Text Line", state.textLines[0].text)

		// Undo and verify original structure
		state.undo()

		assertEquals(3, state.textLines.size)
		assertEquals("First Line", state.textLines[0].text)
		assertEquals("Second Line", state.textLines[1].text)
		assertEquals("Third Line", state.textLines[2].text)
		assertEquals(CharLineOffset(2, 4), state.cursorPosition)
	}

	@Test
	fun `test undo single line replace with multi-line`() {
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

		// Verify expanded to multiple lines
		assertEquals(3, state.textLines.size)
		assertEquals("Hello Beautiful", state.textLines[0].text)
		assertEquals("World", state.textLines[1].text)
		assertEquals("Today", state.textLines[2].text)

		// Undo and verify original state
		state.undo()
		assertEquals(1, state.textLines.size)
		assertEquals("Hello World", state.textLines[0].text)
		assertEquals(CharLineOffset(0, 11), state.cursorPosition)
	}

	@Test
	fun `test undo replace with mixed styles and inheritance`() {
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
			end = CharLineOffset(0, 8)
		)
		val newText = buildAnnotatedString {
			pushStyle(SpanStyle(color = Color.Green))
			append("PPY WOR")
			pop()
		}
		val replaceOperation = TextEditOperation.Replace(
			range = range,
			oldText = AnnotatedString("lo Wo"),
			newText = newText,
			cursorBefore = CharLineOffset(0, 8),
			cursorAfter = CharLineOffset(0, 10),
			inheritStyle = true
		)

		state.editManager.applyOperation(replaceOperation)

		// Verify text and mixed styles
		assertEquals("HelPPY WORld Today", state.textLines[0].text)

		// Undo and verify original styles
		state.undo()
		assertEquals("Hello World Today", state.textLines[0].text)
		assertTrue(state.textLines[0].spanStyles.any {
			it.item == redStyle &&
					it.start == 0 &&
					it.end == 6
		})
		assertTrue(state.textLines[0].spanStyles.any {
			it.item == blueStyle &&
					it.start == 6 &&
					it.end == 11
		})
	}

	@Test
	fun `test undo replace entire document`() {
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

		// Verify complete replacement
		assertEquals(1, state.textLines.size)
		assertEquals("New Content", state.textLines[0].text)
		assertTrue(state.textLines[0].spanStyles.isEmpty())

		// Undo and verify original document
		state.undo()
		assertEquals(3, state.textLines.size)
		assertEquals("Complete", state.textLines[0].text)
		assertEquals("Document", state.textLines[1].text)
		assertEquals("Text", state.textLines[2].text)
		// Verify original styles
		assertTrue(state.textLines[0].spanStyles.any { it.item == redStyle })
		assertTrue(state.textLines[1].spanStyles.any { it.item == blueStyle })
		assertTrue(state.textLines[2].spanStyles.any { it.item == redStyle })
	}

	@Test
	fun `test undo replace with empty replacement`() {
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
		assertEquals("Hello", state.textLines[0].text)

		state.undo()
		assertEquals("Hello World", state.textLines[0].text)
	}

	@Test
	fun `test undo replace with complex style inheritance across lines`() {
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
			oldText = buildAnnotatedString {
				append("Line\nSecond Line\nThird")
			},
			newText = AnnotatedString("NEW\nTEXT"),
			cursorBefore = CharLineOffset(2, 5),
			cursorAfter = CharLineOffset(1, 4),
			inheritStyle = true
		)

		state.editManager.applyOperation(replaceOperation)

		// Verify text and inherited styles
		assertEquals(2, state.textLines.size)
		assertEquals("First NEW", state.textLines[0].text)
		assertEquals("TEXT Line", state.textLines[1].text)

		// Undo and verify original state
		state.undo()
		assertEquals(3, state.textLines.size)
		assertEquals("First Line", state.textLines[0].text)
		assertEquals("Second Line", state.textLines[1].text)
		assertEquals("Third Line", state.textLines[2].text)
		// Verify original styles restored
		assertTrue(state.textLines[0].spanStyles.any { it.item == redStyle })
		assertTrue(state.textLines[1].spanStyles.any { it.item == blueStyle })
		assertTrue(state.textLines[2].spanStyles.any { it.item == redStyle })
	}
}