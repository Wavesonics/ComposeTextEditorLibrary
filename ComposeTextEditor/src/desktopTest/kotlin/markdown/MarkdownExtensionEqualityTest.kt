package markdown

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import com.darkrockstudios.texteditor.markdown.MarkdownConfiguration
import com.darkrockstudios.texteditor.markdown.MarkdownExtension
import com.darkrockstudios.texteditor.state.TextEditorState
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class MarkdownExtensionEqualityTest {

	private fun TestScope.createEditorState(initialText: String?) =
		TextEditorState(
			scope = this,
			measurer = mockk(relaxed = true),
			initialText = initialText?.let { AnnotatedString(it) },
		)

	private fun TestScope.createMarkdownExtension(
		initialText: String?,
		configuration: MarkdownConfiguration = MarkdownConfiguration.DEFAULT
	): MarkdownExtension {
		val state = createEditorState(initialText)
		return MarkdownExtension(state, configuration)
	}

	@Test
	fun `test equals same object reference`() = runTest {
		val extension = createMarkdownExtension("")
		assertEquals(extension, extension)
	}

	@Test
	fun `test equals null object`() = runTest {
		val extension = createMarkdownExtension("")
		assertFalse(extension.equals(null))
	}

	@Test
	fun `test equals different type`() = runTest {
		val extension = createMarkdownExtension("")
		assertFalse(extension.equals("not a MarkdownExtension"))
	}

	@Test
	fun `test equals same content and configuration`() = runTest {
		val text = "# Header\nSome content"
		val config = MarkdownConfiguration.DEFAULT

		val extension1 = createMarkdownExtension(text, config)
		val extension2 = createMarkdownExtension(text, config)

		assertTrue(extension1 == extension2)
	}

	@Test
	fun `test equals different content same configuration`() = runTest {
		val extension1 = createMarkdownExtension("# Header 1")
		val extension2 = createMarkdownExtension("# Different Header")

		assertFalse(extension1 == extension2)
	}

	@Test
	fun `test equals same content different configuration`() = runTest {
		val text = "# Header"
		val config1 = MarkdownConfiguration.DEFAULT
		val config2 = MarkdownConfiguration.DEFAULT.copy(
			header1Style = MarkdownConfiguration.DEFAULT.header1Style.copy(color = Color.Red)
		)

		val extension1 = createMarkdownExtension(text, config1)
		val extension2 = createMarkdownExtension(text, config2)

		assertFalse(extension1 == extension2)
	}

	@Test
	fun `test hashCode consistency`() = runTest {
		val text = "# Header\nSome content"
		val config = MarkdownConfiguration.DEFAULT

		val extension1 = createMarkdownExtension(text, config)
		val extension2 = createMarkdownExtension(text, config)

		assertEquals(extension1.hashCode(), extension2.hashCode())
	}

	@Test
	fun `test hashCode different content`() = runTest {
		val extension1 = createMarkdownExtension("# Header 1")
		val extension2 = createMarkdownExtension("# Different Header")

		assertNotEquals(extension1.hashCode(), extension2.hashCode())
	}

	@Test
	fun `test hashCode different configuration`() = runTest {
		val text = "# Header"
		val config1 = MarkdownConfiguration.DEFAULT
		val config2 = MarkdownConfiguration.DEFAULT.copy(
			header1Style = MarkdownConfiguration.DEFAULT.header1Style.copy(color = Color.Red)
		)

		val extension1 = createMarkdownExtension(text, config1)
		val extension2 = createMarkdownExtension(text, config2)

		assertNotEquals(extension1.hashCode(), extension2.hashCode())
	}
}