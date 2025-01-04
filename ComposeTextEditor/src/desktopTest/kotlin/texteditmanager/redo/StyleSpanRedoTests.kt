package texteditmanager.redo

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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StyleSpanRedoTests {
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
	fun `test redo add single style span`() {
		state.setText("Hello World")

		val range = TextEditorRange(
			start = CharLineOffset(0, 0),
			end = CharLineOffset(0, 5)
		)
		val styleOperation = TextEditOperation.StyleSpan(
			range = range,
			style = redStyle,
			isAdd = true,
			cursorBefore = CharLineOffset(0, 5),
			cursorAfter = CharLineOffset(0, 5)
		)

		state.editManager.applyOperation(styleOperation)
		state.undo()

		// Verify style was removed after undo
		assertTrue(state.textLines[0].spanStyles.isEmpty())

		// Redo and verify style is reapplied
		state.redo()
		assertTrue(state.textLines[0].spanStyles.any {
			it.item == redStyle &&
					it.start == 0 &&
					it.end == 5
		})
		assertEquals(CharLineOffset(0, 5), state.cursorPosition)
	}

	@Test
	fun `test redo remove style span`() {
		// Set initial text with style
		val initialText = buildAnnotatedString {
			pushStyle(redStyle)
			append("Hello World")
			pop()
		}
		state.setText(initialText)

		val range = TextEditorRange(
			start = CharLineOffset(0, 0),
			end = CharLineOffset(0, 5)
		)
		val styleOperation = TextEditOperation.StyleSpan(
			range = range,
			style = redStyle,
			isAdd = false,
			cursorBefore = CharLineOffset(0, 5),
			cursorAfter = CharLineOffset(0, 5)
		)

		state.editManager.applyOperation(styleOperation)
		state.undo()

		// Verify original style is restored after undo
		assertTrue(state.textLines[0].spanStyles.any {
			it.item == redStyle &&
					it.start == 0 &&
					it.end == 11
		})

		// Redo and verify style is partially removed again
		state.redo()
		assertFalse(state.textLines[0].spanStyles.any {
			it.item == redStyle &&
					it.start == 0 &&
					it.end == 5
		})
		assertTrue(state.textLines[0].spanStyles.any {
			it.item == redStyle &&
					it.start == 5 &&
					it.end == 11
		})
	}

	@Test
	fun `test redo style span across multiple lines`() {
		state.setText("Line 1\nLine 2\nLine 3")

		val range = TextEditorRange(
			start = CharLineOffset(0, 2),
			end = CharLineOffset(2, 3)
		)
		val styleOperation = TextEditOperation.StyleSpan(
			range = range,
			style = redStyle,
			isAdd = true,
			cursorBefore = CharLineOffset(2, 3),
			cursorAfter = CharLineOffset(2, 3)
		)

		state.editManager.applyOperation(styleOperation)
		state.undo()

		// Verify all styles were removed after undo
		state.textLines.forEach { line ->
			assertTrue(line.spanStyles.isEmpty())
		}

		// Redo and verify styles are reapplied to all lines
		state.redo()
		assertTrue(state.textLines[0].spanStyles.any { it.item == redStyle })
		assertTrue(state.textLines[1].spanStyles.any { it.item == redStyle })
		assertTrue(state.textLines[2].spanStyles.any { it.item == redStyle })
	}

	@Test
	fun `test redo overlapping style spans`() {
		state.setText("Hello World")

		// First style operation (red)
		val redRange = TextEditorRange(
			start = CharLineOffset(0, 0),
			end = CharLineOffset(0, 7)
		)
		val redOperation = TextEditOperation.StyleSpan(
			range = redRange,
			style = redStyle,
			isAdd = true,
			cursorBefore = CharLineOffset(0, 7),
			cursorAfter = CharLineOffset(0, 7)
		)

		// Second style operation (blue)
		val blueRange = TextEditorRange(
			start = CharLineOffset(0, 4),
			end = CharLineOffset(0, 11)
		)
		val blueOperation = TextEditOperation.StyleSpan(
			range = blueRange,
			style = blueStyle,
			isAdd = true,
			cursorBefore = CharLineOffset(0, 11),
			cursorAfter = CharLineOffset(0, 11)
		)

		// Apply both operations
		state.editManager.applyOperation(redOperation)
		state.editManager.applyOperation(blueOperation)

		// Undo both
		state.undo()
		state.undo()

		// Verify all styles are removed
		assertTrue(state.textLines[0].spanStyles.isEmpty())

		// Redo red style
		state.redo()
		assertTrue(state.textLines[0].spanStyles.any {
			it.item == redStyle &&
					it.start == 0 &&
					it.end == 7
		})
		assertFalse(state.textLines[0].spanStyles.any { it.item == blueStyle })

		// Redo blue style
		state.redo()
		assertTrue(state.textLines[0].spanStyles.any {
			it.item == redStyle &&
					it.start == 0 &&
					it.end == 7
		})
		assertTrue(state.textLines[0].spanStyles.any {
			it.item == blueStyle &&
					it.start == 4 &&
					it.end == 11
		})
	}

	@Test
	fun `test redo style span on empty line`() {
		state.setText("\n")

		val range = TextEditorRange(
			start = CharLineOffset(0, 0),
			end = CharLineOffset(0, 1)
		)
		val styleOperation = TextEditOperation.StyleSpan(
			range = range,
			style = redStyle,
			isAdd = true,
			cursorBefore = CharLineOffset(0, 0),
			cursorAfter = CharLineOffset(0, 0)
		)

		state.editManager.applyOperation(styleOperation)
		state.undo()

		// Verify style was removed after undo
		assertTrue(state.textLines[0].spanStyles.isEmpty())

		// Redo and verify style is reapplied
		state.redo()
		assertTrue(state.textLines[0].spanStyles.any { it.item == redStyle })
	}

	@Test
	fun `test redo multiple style operations in sequence`() {
		state.setText("Hello World")

		val range = TextEditorRange(
			start = CharLineOffset(0, 0),
			end = CharLineOffset(0, 5)
		)

		// Add style
		val addOperation = TextEditOperation.StyleSpan(
			range = range,
			style = redStyle,
			isAdd = true,
			cursorBefore = CharLineOffset(0, 5),
			cursorAfter = CharLineOffset(0, 5)
		)
		state.editManager.applyOperation(addOperation)

		// Remove style
		val removeOperation = TextEditOperation.StyleSpan(
			range = range,
			style = redStyle,
			isAdd = false,
			cursorBefore = CharLineOffset(0, 5),
			cursorAfter = CharLineOffset(0, 5)
		)
		state.editManager.applyOperation(removeOperation)

		// Undo both operations
		state.undo()
		state.undo()

		// Verify initial state
		assertTrue(state.textLines[0].spanStyles.isEmpty())

		// Redo add operation
		state.redo()
		assertTrue(state.textLines[0].spanStyles.any {
			it.item == redStyle &&
					it.start == 0 &&
					it.end == 5
		})

		// Redo remove operation
		state.redo()
		assertTrue(state.textLines[0].spanStyles.isEmpty())
	}
}