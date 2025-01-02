package texteditor

import com.darkrockstudios.texteditor.CharLineOffset
import com.darkrockstudios.texteditor.TextEditorRange
import org.junit.Test
import kotlin.test.assertEquals

class TextEditorRangeExtTest {
	@Test
	fun `test single line range`() {
		val range = TextEditorRange(
			start = CharLineOffset(5, 0),
			end = CharLineOffset(5, 10)
		)
		assertEquals(5..5, range.affectedLines())
	}

	@Test
	fun `test multi line range`() {
		val range = TextEditorRange(
			start = CharLineOffset(5, 0),
			end = CharLineOffset(8, 10)
		)
		assertEquals(5..8, range.affectedLines())
	}

	@Test
	fun `test buffered single line range`() {
		val range = TextEditorRange(
			start = CharLineOffset(5, 0),
			end = CharLineOffset(5, 10)
		)
		assertEquals(3..7, range.affectedLines(buffer = 2, maxLines = 100))
	}

	@Test
	fun `test buffered range at start of document`() {
		val range = TextEditorRange(
			start = CharLineOffset(0, 0),
			end = CharLineOffset(2, 10)
		)
		assertEquals(0..4, range.affectedLines(buffer = 2, maxLines = 100))
	}

	@Test
	fun `test buffered range at end of document`() {
		val range = TextEditorRange(
			start = CharLineOffset(98, 0),
			end = CharLineOffset(100, 10)
		)
		assertEquals(96..100, range.affectedLines(buffer = 2, maxLines = 100))
	}

	@Test
	fun `test buffered range with zero buffer`() {
		val range = TextEditorRange(
			start = CharLineOffset(5, 0),
			end = CharLineOffset(8, 10)
		)
		assertEquals(5..8, range.affectedLines(buffer = 0, maxLines = 100))
	}

	@Test
	fun `test reverse line range returns correct order`() {
		val range = TextEditorRange(
			start = CharLineOffset(8, 0),
			end = CharLineOffset(5, 10)
		)
		assertEquals(5..8, range.affectedLines())
	}
}