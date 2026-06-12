package texteditmanager.undo

import androidx.compose.ui.text.AnnotatedString
import com.darkrockstudios.texteditor.CharLineOffset
import com.darkrockstudios.texteditor.markdown.MarkdownExtension
import com.darkrockstudios.texteditor.richstyle.BLOCKQUOTE_PARAGRAPH_STYLE
import com.darkrockstudios.texteditor.richstyle.BlockquoteSpanStyle
import com.darkrockstudios.texteditor.richstyle.CODE_FENCE_PARAGRAPH_STYLE
import com.darkrockstudios.texteditor.richstyle.CodeFenceSpanStyle
import com.darkrockstudios.texteditor.richstyle.ORDERED_LIST_PARAGRAPH_STYLE
import com.darkrockstudios.texteditor.richstyle.OrderedListSpanStyle
import com.darkrockstudios.texteditor.state.TextEditOperation
import com.darkrockstudios.texteditor.state.TextEditorState
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LineBlockUndoTests {
	private lateinit var scope: TestScope
	private lateinit var state: TextEditorState
	private lateinit var markdown: MarkdownExtension

	@BeforeTest
	fun setup() {
		scope = TestScope()
		state = TextEditorState(
			scope = scope.backgroundScope,
			measurer = mockk(relaxed = true)
		)
		markdown = MarkdownExtension(state)
	}

	private fun spanLines(style: Any) = state.richSpanManager.getAllRichSpans()
		.filter { it.style === style }
		.map { it.range.start.line }
		.sorted()

	private fun hasParagraphStyle(line: Int, paragraphStyle: Any) =
		state.textLines[line].paragraphStyles.any { it.item == paragraphStyle }

	@Test
	fun `undo ordered list removes span and paragraph indent`() {
		state.setText("Hello World")
		val prior = state.textLines[0]

		markdown.toggleOrderedList(0..0)
		assertEquals(listOf(0), spanLines(OrderedListSpanStyle))
		assertTrue(hasParagraphStyle(0, ORDERED_LIST_PARAGRAPH_STYLE))

		state.undo()
		assertTrue(spanLines(OrderedListSpanStyle).isEmpty())
		assertFalse(hasParagraphStyle(0, ORDERED_LIST_PARAGRAPH_STYLE))
		assertEquals(prior.text, state.textLines[0].text)
		assertEquals(prior.paragraphStyles, state.textLines[0].paragraphStyles)
	}

	@Test
	fun `type char then toggle list then undo keeps char and removes list`() {
		state.setText("Hello World")

		state.editManager.applyOperation(
			TextEditOperation.Insert(
				position = CharLineOffset(0, 11),
				text = AnnotatedString("!"),
				cursorBefore = CharLineOffset(0, 11),
				cursorAfter = CharLineOffset(0, 12)
			)
		)
		assertEquals("Hello World!", state.textLines[0].text)

		markdown.toggleOrderedList(0..0)
		assertEquals(listOf(0), spanLines(OrderedListSpanStyle))

		// Undo only the list — the typed char stays, and the list is fully gone.
		state.undo()
		assertEquals("Hello World!", state.textLines[0].text)
		assertTrue(spanLines(OrderedListSpanStyle).isEmpty())
		assertFalse(hasParagraphStyle(0, ORDERED_LIST_PARAGRAPH_STYLE))

		// A second undo reverts the prior char edit.
		state.undo()
		assertEquals("Hello World", state.textLines[0].text)
	}

	@Test
	fun `undo blockquote removes span and indent`() {
		state.setText("quote me")
		markdown.toggleBlockquote(0..0)
		assertEquals(listOf(0), spanLines(BlockquoteSpanStyle))
		assertTrue(hasParagraphStyle(0, BLOCKQUOTE_PARAGRAPH_STYLE))

		state.undo()
		assertTrue(spanLines(BlockquoteSpanStyle).isEmpty())
		assertFalse(hasParagraphStyle(0, BLOCKQUOTE_PARAGRAPH_STYLE))
		assertEquals("quote me", state.textLines[0].text)
	}

	@Test
	fun `undo code fence removes span and indent`() {
		state.setText("code line")
		markdown.toggleCodeFence(0..0)
		assertEquals(listOf(0), spanLines(CodeFenceSpanStyle))
		assertTrue(hasParagraphStyle(0, CODE_FENCE_PARAGRAPH_STYLE))

		state.undo()
		assertTrue(spanLines(CodeFenceSpanStyle).isEmpty())
		assertFalse(hasParagraphStyle(0, CODE_FENCE_PARAGRAPH_STYLE))
		assertEquals("code line", state.textLines[0].text)
	}

	@Test
	fun `undo multi-line list toggle restores every line`() {
		state.setText("one\ntwo\nthree")
		markdown.toggleOrderedList(0..2)
		assertEquals(listOf(0, 1, 2), spanLines(OrderedListSpanStyle))

		state.undo()
		assertTrue(spanLines(OrderedListSpanStyle).isEmpty())
		(0..2).forEach { line ->
			assertFalse(hasParagraphStyle(line, ORDERED_LIST_PARAGRAPH_STYLE))
		}
		assertEquals("one\ntwo\nthree", state.getAllText().text)
	}
}
