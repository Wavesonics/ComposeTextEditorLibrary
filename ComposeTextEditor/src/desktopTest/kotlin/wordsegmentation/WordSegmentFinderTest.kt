package wordsegmentation

import com.darkrockstudios.texteditor.CharLineOffset
import com.darkrockstudios.texteditor.state.TextEditorState
import com.darkrockstudios.texteditor.state.findWordSegmentAt
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WordSegmentFinderTest {
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
	fun `test empty state returns null`() {
		textState.setText("")
		val result = textState.findWordSegmentAt(CharLineOffset(0, 0))
		assertNull(result)
	}

	@Test
	fun `test position within simple word`() {
		textState.setText("hello")
		val result = textState.findWordSegmentAt(CharLineOffset(0, 2))
		assertEquals("hello", result?.text)
		assertEquals(0, result?.range?.start?.char)
		assertEquals(5, result?.range?.end?.char)
	}

	@Test
	fun `test position at start of word`() {
		textState.setText("hello world")
		val result = textState.findWordSegmentAt(CharLineOffset(0, 0))
		assertEquals("hello", result?.text)
		assertEquals(0, result?.range?.start?.char)
		assertEquals(5, result?.range?.end?.char)
	}

	@Test
	fun `test position at end of word`() {
		textState.setText("hello world")
		val result = textState.findWordSegmentAt(CharLineOffset(0, 4))
		assertEquals("hello", result?.text)
		assertEquals(0, result?.range?.start?.char)
		assertEquals(5, result?.range?.end?.char)
	}

	@Test
	fun `test position between words`() {
		textState.setText("hello world")
		val result = textState.findWordSegmentAt(CharLineOffset(0, 5))
		assertEquals("hello", result?.text)
		assertEquals(0, result?.range?.start?.char)
		assertEquals(5, result?.range?.end?.char)
	}

	@Test
	fun `test position at space after word`() {
		textState.setText("hello world")
		val result = textState.findWordSegmentAt(CharLineOffset(0, 5))
		assertEquals("hello", result?.text)
		assertEquals(0, result?.range?.start?.char)
		assertEquals(5, result?.range?.end?.char)
	}

	@Test
	fun `test position at space before word`() {
		textState.setText("hello world")
		val result = textState.findWordSegmentAt(CharLineOffset(0, 6))
		assertEquals("world", result?.text)
		assertEquals(6, result?.range?.start?.char)
		assertEquals(11, result?.range?.end?.char)
	}

	@Test
	fun `test word with apostrophe`() {
		textState.setText("don't worry")
		val result = textState.findWordSegmentAt(CharLineOffset(0, 3))
		assertEquals("don't", result?.text)
		assertEquals(0, result?.range?.start?.char)
		assertEquals(5, result?.range?.end?.char)
	}

	@Test
	fun `test word with hyphen`() {
		textState.setText("well-known")
		val result = textState.findWordSegmentAt(CharLineOffset(0, 5))
		assertEquals("well-known", result?.text)
		assertEquals(0, result?.range?.start?.char)
		assertEquals(10, result?.range?.end?.char)
	}

	@Test
	fun `test word with period`() {
		textState.setText("U.S.A.")
		val result = textState.findWordSegmentAt(CharLineOffset(0, 3))
		assertEquals("U.S.A.", result?.text)
		assertEquals(0, result?.range?.start?.char)
		assertEquals(6, result?.range?.end?.char)
	}

	@Test
	fun `test position at end of line`() {
		textState.setText("hello")
		val result = textState.findWordSegmentAt(CharLineOffset(0, 5))
		assertEquals("hello", result?.text)
		assertEquals(0, result?.range?.start?.char)
		assertEquals(5, result?.range?.end?.char)
	}

	@Test
	fun `test position beyond end of line returns null`() {
		textState.setText("hello")
		val result = textState.findWordSegmentAt(CharLineOffset(0, 6))
		assertNull(result)
	}

	@Test
	fun `test position at invalid line returns null`() {
		textState.setText("hello")
		val result = textState.findWordSegmentAt(CharLineOffset(1, 0))
		assertNull(result)
	}

	@Test
	fun `test multiple lines finds correct word`() {
		textState.setText("hello world\ntest line")
		val result = textState.findWordSegmentAt(CharLineOffset(1, 2))
		assertEquals("test", result?.text)
		assertEquals(0, result?.range?.start?.char)
		assertEquals(4, result?.range?.end?.char)
		assertEquals(1, result?.range?.start?.line)
		assertEquals(1, result?.range?.end?.line)
	}

	@Test
	fun `test word with numbers`() {
		textState.setText("hello123world")
		val result = textState.findWordSegmentAt(CharLineOffset(0, 5))
		assertEquals("hello123world", result?.text)
		assertEquals(0, result?.range?.start?.char)
		assertEquals(13, result?.range?.end?.char)
	}

	@Test
	fun `test underscore in word`() {
		textState.setText("hello_world")
		val result = textState.findWordSegmentAt(CharLineOffset(0, 5))
		assertEquals("hello_world", result?.text)
		assertEquals(0, result?.range?.start?.char)
		assertEquals(11, result?.range?.end?.char)
	}
}