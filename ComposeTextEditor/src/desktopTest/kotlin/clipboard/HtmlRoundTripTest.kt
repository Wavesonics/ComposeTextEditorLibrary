package clipboard

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
import com.darkrockstudios.texteditor.clipboard.htmlToAnnotatedString
import com.darkrockstudios.texteditor.clipboard.toHtml
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class HtmlRoundTripTest {

	// ---------- Serialization ----------

	@Test
	fun `empty string`() {
		assertEquals("", AnnotatedString("").toHtml())
	}

	@Test
	fun `plain text passes through`() {
		assertEquals("Hello world", AnnotatedString("Hello world").toHtml())
	}

	@Test
	fun `escapes special characters`() {
		assertEquals(
			"a &lt;b&gt; &amp; c &quot;d&quot;",
			AnnotatedString("a <b> & c \"d\"").toHtml()
		)
	}

	@Test
	fun `newlines become br`() {
		assertEquals("line1<br/>line2", AnnotatedString("line1\nline2").toHtml())
	}

	@Test
	fun `bold span emits strong`() {
		val input = buildAnnotatedString {
			append("a ")
			withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("bold") }
			append(" b")
		}
		assertEquals("a <strong>bold</strong> b", input.toHtml())
	}

	@Test
	fun `italic span emits em`() {
		val input = buildAnnotatedString {
			withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append("italic") }
		}
		assertEquals("<em>italic</em>", input.toHtml())
	}

	@Test
	fun `underline span emits u`() {
		val input = buildAnnotatedString {
			withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) { append("under") }
		}
		assertEquals("<u>under</u>", input.toHtml())
	}

	@Test
	fun `strikethrough span emits s`() {
		val input = buildAnnotatedString {
			withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) { append("strike") }
		}
		assertEquals("<s>strike</s>", input.toHtml())
	}

	@Test
	fun `monospace family emits code`() {
		val input = buildAnnotatedString {
			withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) { append("code") }
		}
		assertEquals("<code>code</code>", input.toHtml())
	}

	@Test
	fun `color emits inline style`() {
		val input = buildAnnotatedString {
			withStyle(SpanStyle(color = Color(0xFFFF0000))) { append("red") }
		}
		assertEquals("<span style=\"color:#ff0000\">red</span>", input.toHtml())
	}

	@Test
	fun `font size emits inline style`() {
		val input = buildAnnotatedString {
			withStyle(SpanStyle(fontSize = 24.sp)) { append("big") }
		}
		assertEquals("<span style=\"font-size:24px\">big</span>", input.toHtml())
	}

	// ---------- Parsing ----------

	@Test
	fun `parse empty string`() {
		assertEquals("", "".htmlToAnnotatedString().text)
	}

	@Test
	fun `parse plain text`() {
		assertEquals("Hello", "Hello".htmlToAnnotatedString().text)
	}

	@Test
	fun `parse bold and strong as Bold`() {
		val a = "<b>x</b>".htmlToAnnotatedString()
		val b = "<strong>x</strong>".htmlToAnnotatedString()
		assertEquals(FontWeight.Bold, a.spanStyles.single().item.fontWeight)
		assertEquals(FontWeight.Bold, b.spanStyles.single().item.fontWeight)
	}

	@Test
	fun `parse italic and em as Italic`() {
		val a = "<i>x</i>".htmlToAnnotatedString()
		val b = "<em>x</em>".htmlToAnnotatedString()
		assertEquals(FontStyle.Italic, a.spanStyles.single().item.fontStyle)
		assertEquals(FontStyle.Italic, b.spanStyles.single().item.fontStyle)
	}

	@Test
	fun `parse br as newline`() {
		assertEquals("a\nb", "a<br/>b".htmlToAnnotatedString().text)
		assertEquals("a\nb", "a<br>b".htmlToAnnotatedString().text)
	}

	@Test
	fun `parse decodes entities`() {
		assertEquals("a < b & c > d", "a &lt; b &amp; c &gt; d".htmlToAnnotatedString().text)
		assertEquals("é", "&#233;".htmlToAnnotatedString().text)
		assertEquals("é", "&#xE9;".htmlToAnnotatedString().text)
	}

	@Test
	fun `parse unknown tag drops tag keeps content`() {
		val r = "<custom>hello</custom>".htmlToAnnotatedString()
		assertEquals("hello", r.text)
		assertTrue(r.spanStyles.isEmpty())
	}

	@Test
	fun `parse paragraph adds newline`() {
		assertEquals("a\nb\n", "<p>a</p><p>b</p>".htmlToAnnotatedString().text)
	}

	@Test
	fun `parse skips script content`() {
		assertEquals("after", "<script>alert(1)</script>after".htmlToAnnotatedString().text)
	}

	@Test
	fun `parse style attribute color`() {
		val r = "<span style=\"color:#ff0000\">x</span>".htmlToAnnotatedString()
		assertEquals(Color(0xFFFF0000), r.spanStyles.single().item.color)
	}

	@Test
	fun `parse style attribute font-size in px`() {
		val r = "<span style=\"font-size:24px\">x</span>".htmlToAnnotatedString()
		assertEquals(24.sp, r.spanStyles.single().item.fontSize)
	}

	// ---------- Round-trips ----------

	@Test
	fun `roundtrip preserves bold`() {
		val input = buildAnnotatedString {
			append("a ")
			withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("bold") }
			append(" b")
		}
		val out = input.toHtml().htmlToAnnotatedString()
		assertEquals("a bold b", out.text)
		val span = out.spanStyles.single { it.item.fontWeight == FontWeight.Bold }
		assertEquals("bold", out.text.substring(span.start, span.end))
	}

	@Test
	fun `roundtrip preserves italic`() {
		val input = buildAnnotatedString {
			withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append("hi") }
		}
		val out = input.toHtml().htmlToAnnotatedString()
		assertEquals("hi", out.text)
		assertEquals(FontStyle.Italic, out.spanStyles.single().item.fontStyle)
	}

	@Test
	fun `roundtrip preserves color`() {
		val input = buildAnnotatedString {
			withStyle(SpanStyle(color = Color(0xFF336699))) { append("blue") }
		}
		val out = input.toHtml().htmlToAnnotatedString()
		assertEquals("blue", out.text)
		assertEquals(Color(0xFF336699), out.spanStyles.single().item.color)
	}

	@Test
	fun `roundtrip preserves font size`() {
		val input = buildAnnotatedString {
			withStyle(SpanStyle(fontSize = 18.sp)) { append("size") }
		}
		val out = input.toHtml().htmlToAnnotatedString()
		assertEquals(18.sp, out.spanStyles.single().item.fontSize)
	}

	@Test
	fun `roundtrip preserves multiline`() {
		val input = AnnotatedString("line1\nline2\nline3")
		val out = input.toHtml().htmlToAnnotatedString()
		assertEquals("line1\nline2\nline3", out.text)
	}

	@Test
	fun `roundtrip preserves nested styles`() {
		val input = buildAnnotatedString {
			withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
				append("bold ")
				withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append("italic") }
				append(" more")
			}
		}
		val out = input.toHtml().htmlToAnnotatedString()
		assertEquals("bold italic more", out.text)
		val boldSpan = out.spanStyles.firstOrNull { it.item.fontWeight == FontWeight.Bold }
		val italicSpan = out.spanStyles.firstOrNull { it.item.fontStyle == FontStyle.Italic }
		assertNotNull(boldSpan)
		assertNotNull(italicSpan)
		assertEquals("bold italic more", out.text.substring(boldSpan.start, boldSpan.end))
		assertEquals("italic", out.text.substring(italicSpan.start, italicSpan.end))
	}

	@Test
	fun `roundtrip preserves underline and strikethrough together`() {
		val input = buildAnnotatedString {
			withStyle(
				SpanStyle(textDecoration = TextDecoration.Underline + TextDecoration.LineThrough)
			) { append("both") }
		}
		val out = input.toHtml().htmlToAnnotatedString()
		assertEquals("both", out.text)
		// Both decorations should appear (as nested <u><s> in HTML, then merged on parse)
		val hasUnderline = out.spanStyles.any { it.item.textDecoration?.contains(TextDecoration.Underline) == true }
		val hasStrike = out.spanStyles.any { it.item.textDecoration?.contains(TextDecoration.LineThrough) == true }
		assertTrue(hasUnderline, "underline missing")
		assertTrue(hasStrike, "strikethrough missing")
	}
}
