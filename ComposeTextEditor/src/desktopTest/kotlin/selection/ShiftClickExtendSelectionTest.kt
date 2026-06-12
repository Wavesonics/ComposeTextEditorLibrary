package selection

import androidx.compose.ui.text.AnnotatedString
import com.darkrockstudios.texteditor.CharLineOffset
import com.darkrockstudios.texteditor.state.TextEditorState
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Regression for issue #23 — shift+click should reliably extend the selection.
 *
 * Exercises the selection math the pointer handlers drive on a shift+click: a plain
 * click positions the cursor and clears any selection; a shift+click extends from the
 * cursor to the click while holding the original anchor fixed.
 */
class ShiftClickExtendSelectionTest {

	private lateinit var state: TextEditorState

	@BeforeTest
	fun setup() {
		state = TextEditorState(
			scope = TestScope(),
			measurer = mockk(relaxed = true),
			initialText = AnnotatedString(""),
		)
		state.setText("The quick brown fox jumps over the lazy dog")
	}

	private fun offset(char: Int) = CharLineOffset(0, char)

	/** Mirrors a plain primary click: move cursor, drop any selection. */
	private fun click(char: Int) {
		state.cursor.updatePosition(offset(char))
		state.selector.clearSelection()
	}

	/** Mirrors a shift+primary click: extend from the cursor to the click, then move the cursor. */
	private fun shiftClick(char: Int) {
		val anchor = state.cursorPosition
		state.selector.extendSelection(anchor, offset(char))
		state.cursor.updatePosition(offset(char))
	}

	@Test
	fun `shift+click with no selection selects from cursor to click`() {
		click(4)
		shiftClick(15)

		val selection = state.selector.selection
		assertEquals(offset(4), selection?.start)
		assertEquals(offset(15), selection?.end)
	}

	@Test
	fun `shift+click works the same selecting backward`() {
		click(15)
		shiftClick(4)

		val selection = state.selector.selection
		assertEquals(offset(4), selection?.start)
		assertEquals(offset(15), selection?.end)
	}

	@Test
	fun `a second shift+click keeps the original anchor fixed`() {
		click(4)
		shiftClick(9)
		shiftClick(19)

		val selection = state.selector.selection
		assertEquals(offset(4), selection?.start, "anchor from the first click must stay put")
		assertEquals(offset(19), selection?.end)
	}

	@Test
	fun `shift+click can shrink the selection back toward the anchor`() {
		click(4)
		shiftClick(19)
		shiftClick(9)

		val selection = state.selector.selection
		assertEquals(offset(4), selection?.start)
		assertEquals(offset(9), selection?.end)
	}

	@Test
	fun `shift+click crossing the anchor flips the selection around it`() {
		click(10)
		shiftClick(19)
		shiftClick(4)

		val selection = state.selector.selection
		assertEquals(offset(4), selection?.start)
		assertEquals(offset(10), selection?.end, "anchor stays at the original click")
	}

	@Test
	fun `shift+click back onto the anchor collapses the selection`() {
		click(4)
		shiftClick(19)
		shiftClick(4)

		assertNull(state.selector.selection)
	}

	@Test
	fun `a plain click after extending clears the selection`() {
		click(4)
		shiftClick(19)
		click(9)

		assertNull(state.selector.selection)
	}
}
