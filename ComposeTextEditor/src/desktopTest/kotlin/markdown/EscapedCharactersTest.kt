package markdown

import com.darkrockstudios.texteditor.markdown.toAnnotatedStringFromMarkdown
import com.darkrockstudios.texteditor.markdown.toMarkdown
import kotlin.test.Test
import kotlin.test.assertEquals

class EscapedCharactersTest {
	@Test
	fun `Test escaped special characters`() {
		val markdownText = """
            This is not \*bold\* and not \_italic\_ and not \`code\`.
            
            This is a literal backslash: \\
            
            This is a literal \# hash.
        """.trimIndent()

		val annotatedString = markdownText.toAnnotatedStringFromMarkdown()
		println("[DEBUG_LOG] Annotated string text: ${annotatedString.text}")

		val expectedString = """
            This is not *bold* and not _italic_ and not `code`.
            
            This is a literal backslash: \
            
            This is a literal # hash.
        """.trimIndent()

		assertEquals(expectedString, annotatedString.text)

		val reconverted = annotatedString.toMarkdown()
		assertEquals(markdownText, reconverted)
	}

	@Test
	fun `Test inner escaped special characters`() {
		val markdownText = """
            This is *bold with \\ escaped \- characters* and not \_italic\_ and not \`code\`.
            
            This is a **literal backslash: \\ inside italics**
            
            This is a literal \# hash.
        """.trimIndent()

		val annotatedString = markdownText.toAnnotatedStringFromMarkdown()
		println("[DEBUG_LOG] Annotated string text: ${annotatedString.text}")

		val expectedString = """
            This is bold with \ escaped - characters and not _italic_ and not `code`.
            
            This is a literal backslash: \ inside italics
            
            This is a literal # hash.
        """.trimIndent()

		assertEquals(expectedString, annotatedString.text)

		val reconverted = annotatedString.toMarkdown()
		assertEquals(markdownText, reconverted)
	}
}