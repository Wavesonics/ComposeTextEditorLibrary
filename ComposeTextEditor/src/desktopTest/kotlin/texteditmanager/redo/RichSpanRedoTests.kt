package texteditmanager.redo

import androidx.compose.ui.text.AnnotatedString
import com.darkrockstudios.texteditor.CharLineOffset
import com.darkrockstudios.texteditor.TextEditorRange
import com.darkrockstudios.texteditor.richstyle.OrderedListSpanStyle
import com.darkrockstudios.texteditor.state.TextEditOperation
import com.darkrockstudios.texteditor.state.TextEditorState
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RichSpanRedoTests {
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

	private fun hasOrderedListSpan() = state.richSpanManager.getAllRichSpans()
		.any { it.style == OrderedListSpanStyle }

	@Test
	fun `test redo add rich span re-applies it`() {
		state.setText("Hello World")

		val range = TextEditorRange(
			start = CharLineOffset(0, 0),
			end = CharLineOffset(0, 11)
		)
		state.addRichSpan(range, OrderedListSpanStyle)
		state.undo()
		assertTrue(state.richSpanManager.getAllRichSpans().isEmpty())

		state.redo()
		assertTrue(hasOrderedListSpan())
	}

	@Test
	fun `test redo remove rich span re-removes it`() {
		state.setText("Hello World")

		val range = TextEditorRange(
			start = CharLineOffset(0, 0),
			end = CharLineOffset(0, 11)
		)
		state.addRichSpan(range, OrderedListSpanStyle)
		state.removeRichSpan(range.start, range.end, OrderedListSpanStyle)
		assertTrue(state.richSpanManager.getAllRichSpans().isEmpty())

		state.undo()
		assertTrue(hasOrderedListSpan())

		state.redo()
		assertTrue(state.richSpanManager.getAllRichSpans().isEmpty())
	}

	@Test
	fun `test type char then add list then undo redo ends in correct state`() {
		state.setText("Hello World")

		state.editManager.applyOperation(
			TextEditOperation.Insert(
				position = CharLineOffset(0, 11),
				text = AnnotatedString("!"),
				cursorBefore = CharLineOffset(0, 11),
				cursorAfter = CharLineOffset(0, 12)
			)
		)

		val range = TextEditorRange(
			start = CharLineOffset(0, 0),
			end = CharLineOffset(0, 12)
		)
		state.addRichSpan(range, OrderedListSpanStyle)

		// Undo the list: char stays, list gone
		state.undo()
		assertEquals("Hello World!", state.textLines[0].text)
		assertTrue(state.richSpanManager.getAllRichSpans().isEmpty())

		// Redo the list: char stays, list back
		state.redo()
		assertEquals("Hello World!", state.textLines[0].text)
		assertTrue(hasOrderedListSpan())
	}
}
