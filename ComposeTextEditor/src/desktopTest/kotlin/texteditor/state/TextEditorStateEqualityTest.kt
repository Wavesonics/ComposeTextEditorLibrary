package texteditor.state

import androidx.compose.ui.text.AnnotatedString
import com.darkrockstudios.texteditor.TextEditorStyle
import com.darkrockstudios.texteditor.state.TextEditorState
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class TextEditorStateEqualityTest {

	private fun TestScope.createState(initialText: String?) =
		TextEditorState(
			scope = this,
			editorStyle = TextEditorStyle(),
			measurer = mockk(relaxed = true),
			initialText = initialText?.let { AnnotatedString(it) },
		)

	@Test
	fun `test equals same object reference`() = runTest {
		val state = createState("")
		assertEquals(state, state)
	}

	@Test
	fun `test equals null object`() = runTest {
		val state = createState("")
		assertFalse(state.equals(null))
	}

	@Test
	fun `test equals different type`() = runTest {
		val state = createState("")
		assertFalse(state.equals("not a TextEditorState"))
	}

	@Test
	fun `test equals same content`() = runTest {
		val lines = listOf(
			"Line 1",
			"Line 2"
		)

		// Add same content to both states
		val state1 = createState(lines.joinToString("\n"))
		val state2 = createState(lines.joinToString("\n"))

		assertTrue(state1 == state2)
	}

	@Test
	fun `test equals different content`() = runTest {
		val state1 = createState("Line 1")
		val state2 = createState("Different line")

		assertFalse(state1 == state2)
	}

	@Test
	fun `test equals different number of lines`() = runTest {
		val state1 = createState("Line 1")
		val state2 = createState("Line 1\nLine 2")

		assertFalse(state1 == state2)
	}

	@Test
	fun `test hashCode consistency`() = runTest {
		val lines = listOf(
			"Line 1",
			"Line 2"
		)

		val state1 = createState(lines.joinToString("\n"))
		val state2 = createState(lines.joinToString("\n"))

		// Equal objects should have equal hash codes
		assertEquals(state1.hashCode(), state2.hashCode())
	}

	@Test
	fun `test hashCode different content`() = runTest {
		val state1 = createState("Line 1")
		val state2 = createState("Different line")

		// Different content should (likely) have different hash codes
		assertNotEquals(state1.hashCode(), state2.hashCode())
	}
}
