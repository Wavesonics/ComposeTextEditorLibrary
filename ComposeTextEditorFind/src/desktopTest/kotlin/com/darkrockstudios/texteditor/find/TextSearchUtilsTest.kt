package com.darkrockstudios.texteditor.find

import com.darkrockstudios.texteditor.CharLineOffset
import com.darkrockstudios.texteditor.TextEditorRange
import com.darkrockstudios.texteditor.state.TextEditorState
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TextSearchUtilsTest {
	private lateinit var textState: TextEditorState

	@Before
	fun setup() {
		textState = TextEditorState(
			scope = TestScope(),
			measurer = mockk(relaxed = true),
			initialText = null
		)
	}

	@Test
	fun `test empty query returns empty list`() {
		textState.setText("hello world")
		val results = textState.findAll("")
		assertTrue(results.isEmpty())
	}

	@Test
	fun `test no matches returns empty list`() {
		textState.setText("hello world")
		val results = textState.findAll("xyz")
		assertTrue(results.isEmpty())
	}

	@Test
	fun `test single match on single line`() {
		textState.setText("hello world")
		val results = textState.findAll("world")

		assertEquals(1, results.size)
		assertEquals(CharLineOffset(0, 6), results[0].start)
		assertEquals(CharLineOffset(0, 11), results[0].end)
	}

	@Test
	fun `test multiple matches on single line`() {
		textState.setText("hello hello hello")
		val results = textState.findAll("hello")

		assertEquals(3, results.size)
		assertEquals(CharLineOffset(0, 0), results[0].start)
		assertEquals(CharLineOffset(0, 5), results[0].end)
		assertEquals(CharLineOffset(0, 6), results[1].start)
		assertEquals(CharLineOffset(0, 11), results[1].end)
		assertEquals(CharLineOffset(0, 12), results[2].start)
		assertEquals(CharLineOffset(0, 17), results[2].end)
	}

	@Test
	fun `test matches across multiple lines`() {
		textState.setText("hello world\nhello again\nhello there")
		val results = textState.findAll("hello")

		assertEquals(3, results.size)
		assertEquals(CharLineOffset(0, 0), results[0].start)
		assertEquals(CharLineOffset(1, 0), results[1].start)
		assertEquals(CharLineOffset(2, 0), results[2].start)
	}

	@Test
	fun `test case insensitive search`() {
		textState.setText("Hello HELLO hello")
		val results = textState.findAll("hello", caseSensitive = false)

		assertEquals(3, results.size)
	}

	@Test
	fun `test case sensitive search`() {
		textState.setText("Hello HELLO hello")
		val results = textState.findAll("hello", caseSensitive = true)

		assertEquals(1, results.size)
		assertEquals(CharLineOffset(0, 12), results[0].start)
	}

	@Test
	fun `test overlapping matches`() {
		textState.setText("aaa")
		val results = textState.findAll("aa")

		// Should find matches at index 0 and 1
		assertEquals(2, results.size)
		assertEquals(CharLineOffset(0, 0), results[0].start)
		assertEquals(CharLineOffset(0, 1), results[1].start)
	}

	@Test
	fun `test match at end of line`() {
		textState.setText("hello world")
		val results = textState.findAll("world")

		assertEquals(1, results.size)
		assertEquals(CharLineOffset(0, 6), results[0].start)
		assertEquals(CharLineOffset(0, 11), results[0].end)
	}

	@Test
	fun `test empty text returns empty list`() {
		textState.setText("")
		val results = textState.findAll("hello")
		assertTrue(results.isEmpty())
	}
}

class FindNearestMatchIndexTest {

	@Test
	fun `test empty matches returns -1`() {
		val result = findNearestMatchIndex(
			emptyList(),
			CharLineOffset(0, 0)
		)
		assertEquals(-1, result)
	}

	@Test
	fun `test cursor before first match returns first match`() {
		val matches = listOf(
			TextEditorRange(CharLineOffset(0, 5), CharLineOffset(0, 10)),
			TextEditorRange(CharLineOffset(0, 15), CharLineOffset(0, 20))
		)
		val result = findNearestMatchIndex(matches, CharLineOffset(0, 0))
		assertEquals(0, result)
	}

	@Test
	fun `test cursor after last match wraps to first`() {
		val matches = listOf(
			TextEditorRange(CharLineOffset(0, 5), CharLineOffset(0, 10)),
			TextEditorRange(CharLineOffset(0, 15), CharLineOffset(0, 20))
		)
		val result = findNearestMatchIndex(matches, CharLineOffset(0, 25))
		assertEquals(0, result)
	}

	@Test
	fun `test cursor at match start returns that match`() {
		val matches = listOf(
			TextEditorRange(CharLineOffset(0, 5), CharLineOffset(0, 10)),
			TextEditorRange(CharLineOffset(0, 15), CharLineOffset(0, 20))
		)
		val result = findNearestMatchIndex(matches, CharLineOffset(0, 5))
		assertEquals(0, result)
	}

	@Test
	fun `test cursor inside match returns that match`() {
		val matches = listOf(
			TextEditorRange(CharLineOffset(0, 5), CharLineOffset(0, 10)),
			TextEditorRange(CharLineOffset(0, 15), CharLineOffset(0, 20))
		)
		val result = findNearestMatchIndex(matches, CharLineOffset(0, 7))
		assertEquals(0, result)
	}

	@Test
	fun `test cursor between matches returns next match`() {
		val matches = listOf(
			TextEditorRange(CharLineOffset(0, 5), CharLineOffset(0, 10)),
			TextEditorRange(CharLineOffset(0, 15), CharLineOffset(0, 20))
		)
		val result = findNearestMatchIndex(matches, CharLineOffset(0, 12))
		assertEquals(1, result)
	}

	@Test
	fun `test multi-line matches`() {
		val matches = listOf(
			TextEditorRange(CharLineOffset(0, 0), CharLineOffset(0, 5)),
			TextEditorRange(CharLineOffset(2, 0), CharLineOffset(2, 5)),
			TextEditorRange(CharLineOffset(4, 0), CharLineOffset(4, 5))
		)
		val result = findNearestMatchIndex(matches, CharLineOffset(1, 0))
		assertEquals(1, result)
	}
}
