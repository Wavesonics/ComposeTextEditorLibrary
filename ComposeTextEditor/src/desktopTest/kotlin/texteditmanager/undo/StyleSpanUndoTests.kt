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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StyleSpanUndoTests {
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
	fun `test undo add single style span`() {
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

		// Verify style was applied
		assertTrue(state.textLines[0].spanStyles.any {
			it.item == redStyle &&
					it.start == 0 &&
					it.end == 5
		})

		// Undo and verify style is removed
		state.undo()
		assertTrue(state.textLines[0].spanStyles.isEmpty())
		assertEquals(CharLineOffset(0, 5), state.cursorPosition)
	}

	@Test
	fun `test undo remove style span`() {
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

		// Verify style was partially removed
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

		// Undo and verify original style is restored
		state.undo()
		assertTrue(state.textLines[0].spanStyles.any {
			it.item == redStyle &&
					it.start == 0 &&
					it.end == 11
		})
	}

	@Test
	fun `test undo style span across multiple lines`() {
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

		// Verify styles were applied to all lines
		assertTrue(state.textLines[0].spanStyles.any { it.item == redStyle })
		assertTrue(state.textLines[1].spanStyles.any { it.item == redStyle })
		assertTrue(state.textLines[2].spanStyles.any { it.item == redStyle })

		// Undo and verify all styles are removed
		state.undo()
		state.textLines.forEach { line ->
			assertTrue(line.spanStyles.isEmpty())
		}
	}

	@Test
	fun `test undo overlapping style spans`() {
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

		state.editManager.applyOperation(redOperation)
		state.editManager.applyOperation(blueOperation)

		// Verify both styles exist
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

		// Undo blue style
		state.undo()
		assertTrue(state.textLines[0].spanStyles.any {
			it.item == redStyle &&
					it.start == 0 &&
					it.end == 7
		})
		assertFalse(state.textLines[0].spanStyles.any { it.item == blueStyle })

		// Undo red style
		state.undo()
		assertTrue(state.textLines[0].spanStyles.isEmpty())
	}

	@Test
	fun `test undo style span on empty line`() {
		state.setText("\n")

		val range = TextEditorRange(
			start = CharLineOffset(0, 0),
			end = CharLineOffset(0, 1)  // Changed to include the newline
		)
		val styleOperation = TextEditOperation.StyleSpan(
			range = range,
			style = redStyle,
			isAdd = true,
			cursorBefore = CharLineOffset(0, 0),
			cursorAfter = CharLineOffset(0, 0)
		)

		state.editManager.applyOperation(styleOperation)

		// Verify style was applied
		assertTrue(state.textLines[0].spanStyles.any { it.item == redStyle })

		// Undo and verify
		state.undo()
		assertTrue(state.textLines[0].spanStyles.isEmpty())
	}

	@Test
	fun `test undo add and remove style operations in sequence`() {
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

		// Verify style is gone
		assertTrue(state.textLines[0].spanStyles.isEmpty())

		// Undo remove
		state.undo()
		assertTrue(state.textLines[0].spanStyles.any {
			it.item == redStyle &&
					it.start == 0 &&
					it.end == 5
		})

		// Undo add
		state.undo()
		assertTrue(state.textLines[0].spanStyles.isEmpty())
	}
}