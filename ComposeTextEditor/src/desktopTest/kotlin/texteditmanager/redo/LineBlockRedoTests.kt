package texteditmanager.redo

import androidx.compose.ui.text.AnnotatedString
import com.darkrockstudios.texteditor.CharLineOffset
import com.darkrockstudios.texteditor.markdown.MarkdownExtension
import com.darkrockstudios.texteditor.richstyle.BlockquoteSpanStyle
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
import kotlin.test.assertTrue

class LineBlockRedoTests {
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
	fun `redo ordered list re-applies span and indent`() {
		state.setText("Hello World")
		markdown.toggleOrderedList(0..0)
		state.undo()
		assertTrue(spanLines(OrderedListSpanStyle).isEmpty())

		state.redo()
		assertEquals(listOf(0), spanLines(OrderedListSpanStyle))
		assertTrue(hasParagraphStyle(0, ORDERED_LIST_PARAGRAPH_STYLE))
	}

	@Test
	fun `type char then toggle list then undo redo ends in correct state`() {
		state.setText("Hello World")

		state.editManager.applyOperation(
			TextEditOperation.Insert(
				position = CharLineOffset(0, 11),
				text = AnnotatedString("!"),
				cursorBefore = CharLineOffset(0, 11),
				cursorAfter = CharLineOffset(0, 12)
			)
		)

		markdown.toggleOrderedList(0..0)

		state.undo()
		assertEquals("Hello World!", state.textLines[0].text)
		assertTrue(spanLines(OrderedListSpanStyle).isEmpty())

		state.redo()
		assertEquals("Hello World!", state.textLines[0].text)
		assertEquals(listOf(0), spanLines(OrderedListSpanStyle))
		assertTrue(hasParagraphStyle(0, ORDERED_LIST_PARAGRAPH_STYLE))
	}

	@Test
	fun `redo blockquote re-applies it`() {
		state.setText("quote me")
		markdown.toggleBlockquote(0..0)
		state.undo()
		assertTrue(spanLines(BlockquoteSpanStyle).isEmpty())

		state.redo()
		assertEquals(listOf(0), spanLines(BlockquoteSpanStyle))
	}

	@Test
	fun `redo code fence re-applies it`() {
		state.setText("code line")
		markdown.toggleCodeFence(0..0)
		state.undo()
		assertTrue(spanLines(CodeFenceSpanStyle).isEmpty())

		state.redo()
		assertEquals(listOf(0), spanLines(CodeFenceSpanStyle))
	}

	@Test
	fun `redo multi-line list toggle re-applies every line`() {
		state.setText("one\ntwo\nthree")
		markdown.toggleOrderedList(0..2)
		state.undo()
		assertTrue(spanLines(OrderedListSpanStyle).isEmpty())

		state.redo()
		assertEquals(listOf(0, 1, 2), spanLines(OrderedListSpanStyle))
		(0..2).forEach { line ->
			assertTrue(hasParagraphStyle(line, ORDERED_LIST_PARAGRAPH_STYLE))
		}
	}

	@Test
	fun `undo demote restores the list`() {
		state.setText("item")
		markdown.toggleOrderedList(0..0)
		assertEquals(listOf(0), spanLines(OrderedListSpanStyle))

		// Toggle off (demote) — this is its own atomic, undoable entry.
		markdown.toggleOrderedList(0..0)
		assertTrue(spanLines(OrderedListSpanStyle).isEmpty())

		state.undo()
		assertEquals(listOf(0), spanLines(OrderedListSpanStyle))
		assertTrue(hasParagraphStyle(0, ORDERED_LIST_PARAGRAPH_STYLE))

		state.redo()
		assertTrue(spanLines(OrderedListSpanStyle).isEmpty())
	}
}
