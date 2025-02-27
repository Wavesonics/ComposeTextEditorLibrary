package markdown

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import com.darkrockstudios.texteditor.markdown.MarkdownConfiguration
import com.darkrockstudios.texteditor.markdown.toMarkdown
import kotlin.test.Test
import kotlin.test.assertEquals

class MarkdownConverterTest {
	@Test
	fun `test empty string conversion`() {
		val input = AnnotatedString("")
		assertEquals("", input.toMarkdown())
	}

	@Test
	fun `test plain text conversion`() {
		val input = AnnotatedString("Hello World")
		assertEquals("Hello World", input.toMarkdown())
	}

	@Test
	fun `test bold text conversion`() {
		val input = buildAnnotatedString {
			append("Hello ")
			withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
				append("bold")
			}
			append(" world")
		}
		assertEquals("Hello **bold** world", input.toMarkdown())
	}

	@Test
	fun `test configurable bold style`() {
		val customConfig = MarkdownConfiguration(
			boldStyle = SpanStyle(fontWeight = FontWeight.Bold, color = Color.Red)
		)

		val input = buildAnnotatedString {
			append("Hello ")
			withStyle(customConfig.boldStyle) {
				append("bold")
			}
			append(" world")
		}

		assertEquals("Hello **bold** world", input.toMarkdown(customConfig))
	}

	@Test
	fun `test italic text conversion`() {
		val input = buildAnnotatedString {
			append("Hello ")
			withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
				append("italic")
			}
			append(" world")
		}
		assertEquals("Hello *italic* world", input.toMarkdown())
	}

	@Test
	fun `test code text conversion`() {
		val input = buildAnnotatedString {
			append("Here is some ")
			withStyle(
				SpanStyle(
					fontFamily = FontFamily.Monospace,
					background = Color(0xFFE0E0E0)
				)
			) {
				append("code")
			}
			append(" text")
		}
		assertEquals("Here is some `code` text", input.toMarkdown())
	}

	@Test
	fun `test nested styles conversion`() {
		val input = buildAnnotatedString {
			append("Hello ")
			withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
				append("bold ")
				withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
					append("and italic")
				}
			}
			append(" world")
		}
		assertEquals("Hello **bold *and italic*** world", input.toMarkdown())
	}

	@Test
	fun `test header conversion`() {
		val input = buildAnnotatedString {
			withStyle(SpanStyle(fontSize = 32f.sp, fontWeight = FontWeight.Bold)) {
				append("Header 1")
			}
			append("\n")
			withStyle(SpanStyle(fontSize = 24f.sp, fontWeight = FontWeight.Bold)) {
				append("Header 2")
			}
		}
		assertEquals("# Header 1\n## Header 2\n", input.toMarkdown())
	}

	@Test
	fun `test link conversion`() {
		val input = buildAnnotatedString {
			append("Click ")
			withStyle(SpanStyle(color = Color.Blue, textDecoration = TextDecoration.Underline)) {
				append("here")
			}
			append(" to continue")
		}
		assertEquals("Click [here]() to continue", input.toMarkdown())
	}

//	@Test
//	fun `test blockquote conversion`() {
//		val input = buildAnnotatedString {
//			withStyle(SpanStyle(color = Color.Gray, fontStyle = FontStyle.Italic)) {
//				append("This is a quote")
//			}
//		}
//		assertEquals("> This is a quote\n", input.toMarkdown())
//	}

	@Test
	fun `test unsupported style is dropped`() {
		val input = buildAnnotatedString {
			append("Hello ")
			withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
				append("strikethrough")
			}
			append(" world")
		}
		assertEquals("Hello strikethrough world", input.toMarkdown())
	}

	@Test
	fun `test overlapping styles are properly nested`() {
		val input = buildAnnotatedString {
			withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
				append("Hello ")
				withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
					append("beautiful ")
				}
				append("world")
			}
			withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
				append(" and universe")
			}
		}
		assertEquals("**Hello *beautiful *world*** and universe*", input.toMarkdown())
	}
}