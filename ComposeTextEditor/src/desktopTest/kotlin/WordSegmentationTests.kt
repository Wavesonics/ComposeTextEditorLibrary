import com.darkrockstudios.texteditor.state.TextEditorState
import com.darkrockstudios.texteditor.state.wordSegments
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestScope
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class WordSegmentationTests {
	private lateinit var state: TextEditorState

	@Before
	fun setup() {
		state = createTestEditorState(TestScope())
	}

	@Test
	fun `empty text returns no segments`() {
		state.setText("")
		assertEquals(0, state.wordSegments().count())
	}

	@Test
	fun `single word returns one segment`() {
		state.setText("hello")
		val segments = state.wordSegments().toList()

		assertEquals(1, segments.size)
		with(segments[0]) {
			assertEquals("hello", text)
			assertEquals(0, range.start.line)
			assertEquals(0, range.start.char)
			assertEquals(0, range.end.line)
			assertEquals(5, range.end.char)
		}
	}

	@Test
	fun `multiple words on single line`() {
		state.setText("hello world test")
		val segments = state.wordSegments().toList()

		assertEquals(3, segments.size)

		// First word
		with(segments[0]) {
			assertEquals("hello", text)
			assertEquals(0, range.start.line)
			assertEquals(0, range.start.char)
			assertEquals(0, range.end.line)
			assertEquals(5, range.end.char)
		}

		// Second word
		with(segments[1]) {
			assertEquals("world", text)
			assertEquals(0, range.start.line)
			assertEquals(6, range.start.char)
			assertEquals(0, range.end.line)
			assertEquals(11, range.end.char)
		}

		// Third word
		with(segments[2]) {
			assertEquals("test", text)
			assertEquals(0, range.start.line)
			assertEquals(12, range.start.char)
			assertEquals(0, range.end.line)
			assertEquals(16, range.end.char)
		}
	}

	@Test
	fun `words across multiple lines`() {
		state.setText("first line\nsecond line")
		val segments = state.wordSegments().toList()

		assertEquals(4, segments.size)

		// Check first line words
		assertEquals("first", segments[0].text)
		assertEquals(0, segments[0].range.start.line)

		assertEquals("line", segments[1].text)
		assertEquals(0, segments[1].range.start.line)

		// Check second line words
		assertEquals("second", segments[2].text)
		assertEquals(1, segments[2].range.start.line)

		assertEquals("line", segments[3].text)
		assertEquals(1, segments[3].range.start.line)
	}

	@Test
	fun `words with underscores`() {
		state.setText("hello_world some_other_test")
		val segments = state.wordSegments().toList()

		assertEquals(2, segments.size)
		assertEquals("hello_world", segments[0].text)
		assertEquals("some_other_test", segments[1].text)
	}

	@Test
	fun `words with numbers`() {
		state.setText("word123 456test test789test")
		val segments = state.wordSegments().toList()

		assertEquals(3, segments.size)
		assertEquals("word123", segments[0].text)
		assertEquals("456test", segments[1].text)
		assertEquals("test789test", segments[2].text)
	}

	@Test
	fun `multiple spaces between words`() {
		state.setText("word1    word2     word3")
		val segments = state.wordSegments().toList()

		assertEquals(3, segments.size)
		assertEquals("word1", segments[0].text)
		assertEquals("word2", segments[1].text)
		assertEquals("word3", segments[2].text)
	}

	@Test
	fun `words with punctuation`() {
		state.setText("Hello, world! This is a test.")
		val segments = state.wordSegments().toList()

		assertEquals(6, segments.size)
		assertEquals("Hello", segments[0].text)
		assertEquals("world", segments[1].text)
		assertEquals("This", segments[2].text)
		assertEquals("is", segments[3].text)
		assertEquals("a", segments[4].text)
		assertEquals("test", segments[5].text)
	}

	private fun createTestEditorState(scope: CoroutineScope): TextEditorState {
		return TextEditorState(
			scope = scope,
			measurer = mockk(relaxed = true)
		)
	}
}