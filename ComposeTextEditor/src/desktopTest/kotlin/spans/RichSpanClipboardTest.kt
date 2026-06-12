package spans

import androidx.compose.ui.text.AnnotatedString
import com.darkrockstudios.texteditor.CharLineOffset
import com.darkrockstudios.texteditor.TextEditorRange
import com.darkrockstudios.texteditor.richstyle.OrderedListSpanStyle
import com.darkrockstudios.texteditor.state.TextEditorState
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RichSpanClipboardTest {

	private fun TestScope.createState(text: String): TextEditorState =
		TextEditorState(
			scope = this,
			measurer = mockk(relaxed = true),
			initialText = AnnotatedString(text),
		)

	private fun TextEditorState.orderedLines(): List<Int> =
		richSpanManager.getAllRichSpans()
			.filter { it.style === OrderedListSpanStyle }
			.map { it.range.start.line }
			.sorted()

	@Test
	fun `copy and paste of ordered-list lines preserves the ordered-list spans`() = runTest {
		val state = createState("one\ntwo\nblank\n")
		// Mark the first two lines as ordered-list items.
		state.addRichSpan(
			CharLineOffset(0, 0),
			CharLineOffset(0, state.textLines[0].length),
			OrderedListSpanStyle,
		)
		state.addRichSpan(
			CharLineOffset(1, 0),
			CharLineOffset(1, state.textLines[1].length),
			OrderedListSpanStyle,
		)
		assertEquals(listOf(0, 1), state.orderedLines())

		// Copy the two list lines (line 0 start through end of line 1).
		val copyRange = TextEditorRange(
			start = CharLineOffset(0, 0),
			end = CharLineOffset(1, state.textLines[1].length),
		)
		val copiedText = state.getTextInRange(copyRange)
		state.copyRichSpans(copyRange)

		// Paste at the trailing empty line.
		val pasteLine = state.textLines.lastIndex
		state.cursor.updatePosition(CharLineOffset(pasteLine, 0))
		state.insertStringAtCursor(copiedText)
		state.pasteRichSpans(CharLineOffset(pasteLine, 0), copiedText)

		// Original two list lines plus the two freshly pasted ones.
		assertEquals(4, state.orderedLines().size)
		assertTrue(
			state.orderedLines().containsAll(listOf(pasteLine, pasteLine + 1)),
			"pasted lines should carry the ordered-list span",
		)
	}

	@Test
	fun `paste does not apply stale spans when pasted text differs from what was copied`() = runTest {
		val state = createState("item\nblank")
		state.addRichSpan(
			CharLineOffset(0, 0),
			CharLineOffset(0, state.textLines[0].length),
			OrderedListSpanStyle,
		)

		val copyRange = TextEditorRange(
			start = CharLineOffset(0, 0),
			end = CharLineOffset(0, state.textLines[0].length),
		)
		state.copyRichSpans(copyRange)

		// Paste different text (e.g. content that came from an external app) — the
		// remembered spans must not be re-applied.
		val external = AnnotatedString("external")
		state.cursor.updatePosition(CharLineOffset(1, 0))
		state.insertStringAtCursor(external)
		state.pasteRichSpans(CharLineOffset(1, 0), external)

		assertEquals(listOf(0), state.orderedLines())
	}
}
