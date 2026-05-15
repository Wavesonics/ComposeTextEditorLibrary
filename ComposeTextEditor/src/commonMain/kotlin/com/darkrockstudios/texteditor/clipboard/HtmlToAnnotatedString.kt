package com.darkrockstudios.texteditor.clipboard

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Parses an HTML clipboard fragment into an [AnnotatedString].
 *
 * Handles the inline-style tag set [AnnotatedString.toHtml] emits, plus the
 * common alternates other rich-text apps use (`<b>`, `<i>`, `<strike>`,
 * `<font color>`, `<a>`). Block-level tags are reduced to text plus newlines.
 * Unknown tags are dropped — their text content is kept.
 *
 * This parser is intentionally forgiving: malformed HTML, mismatched tags,
 * unknown tags, and unknown style declarations all degrade gracefully rather
 * than throwing.
 */
fun String.htmlToAnnotatedString(): AnnotatedString {
	if (isEmpty()) return AnnotatedString("")

	val tokens = tokenizeHtml(this)
	return buildAnnotatedString {
		// Stack entry = how many styles were pushed for the matching open tag,
		// plus a marker to emit a newline (for block-level tags) on close.
		data class StackEntry(val tagName: String, val pushed: Int, val trailingNewline: Boolean)

		val stack = ArrayDeque<StackEntry>()

		for (token in tokens) {
			when (token) {
				is HtmlToken.Text -> append(decodeEntities(token.content))
				is HtmlToken.StartTag -> {
					val voidText = handleVoid(token.name, token.attrs)
					if (voidText != null) {
						append(voidText)
					} else {
						val pushed = stylesForTag(token.name, token.attrs).onEach { pushStyle(it) }.size
						if (!token.selfClose) {
							stack.addLast(StackEntry(token.name, pushed, isBlockTag(token.name)))
						} else {
							repeat(pushed) { pop() }
						}
					}
				}

				is HtmlToken.EndTag -> {
					// Pop stack entries until we find the matching tag (or empty).
					// This forgives mismatched tags (closes everything above the match).
					var matched = false
					while (stack.isNotEmpty()) {
						val top = stack.removeLast()
						repeat(top.pushed) { pop() }
						if (top.tagName == token.name) {
							if (top.trailingNewline) append('\n')
							matched = true
							break
						}
					}
					if (!matched && isBlockTag(token.name)) append('\n')
				}
			}
		}
		// Any unclosed tags: pop their styles. Block-trailing newlines are skipped
		// because the document ended without an explicit close.
		while (stack.isNotEmpty()) {
			val top = stack.removeLast()
			repeat(top.pushed) { pop() }
		}
	}
}

// ----- Tag → SpanStyle mapping -----

private fun stylesForTag(name: String, attrs: Map<String, String>): List<SpanStyle> {
	val styles = mutableListOf<SpanStyle>()
	when (name) {
		"b", "strong" -> styles += SpanStyle(fontWeight = FontWeight.Bold)
		"i", "em", "cite", "dfn", "var" -> styles += SpanStyle(fontStyle = FontStyle.Italic)
		"u", "ins" -> styles += SpanStyle(textDecoration = TextDecoration.Underline)
		"s", "strike", "del" -> styles += SpanStyle(textDecoration = TextDecoration.LineThrough)
		"code", "kbd", "samp", "tt", "pre" -> styles += SpanStyle(fontFamily = FontFamily.Monospace)
		"mark" -> styles += SpanStyle(background = Color(0xFFFFFF00))
		"a" -> styles += SpanStyle(color = Color(0xFF1A0DAB), textDecoration = TextDecoration.Underline)
		"font" -> {
			attrs["color"]?.let { parseCssColor(it)?.let { c -> styles += SpanStyle(color = c) } }
		}
	}
	parseStyleAttribute(attrs["style"])?.let { styles += it }
	return styles
}

private fun parseStyleAttribute(raw: String?): SpanStyle? {
	if (raw.isNullOrBlank()) return null
	var color: Color? = null
	var background: Color? = null
	var fontWeight: FontWeight? = null
	var fontStyle: FontStyle? = null
	var fontFamily: FontFamily? = null
	var fontSize: androidx.compose.ui.unit.TextUnit? = null
	var decoration: TextDecoration? = null

	raw.split(';').forEach { decl ->
		val colon = decl.indexOf(':')
		if (colon <= 0) return@forEach
		val prop = decl.substring(0, colon).trim().lowercase()
		val value = decl.substring(colon + 1).trim()
		if (value.isEmpty()) return@forEach
		when (prop) {
			"color" -> parseCssColor(value)?.let { color = it }
			"background-color", "background" -> parseCssColor(value)?.let { background = it }
			"font-weight" -> fontWeight = parseCssFontWeight(value)
			"font-style" -> if (value.equals("italic", true) || value.equals("oblique", true)) {
				fontStyle = FontStyle.Italic
			}

			"font-family" -> if (value.contains("monospace", true) ||
				value.contains("courier", true) ||
				value.contains("mono", true)
			) fontFamily = FontFamily.Monospace

			"font-size" -> fontSize = parseCssFontSize(value)
			"text-decoration", "text-decoration-line" -> decoration = parseCssTextDecoration(value)
		}
	}

	if (color == null && background == null && fontWeight == null && fontStyle == null &&
		fontFamily == null && fontSize == null && decoration == null
	) return null

	return SpanStyle(
		color = color ?: Color.Unspecified,
		background = background ?: Color.Unspecified,
		fontWeight = fontWeight,
		fontStyle = fontStyle,
		fontFamily = fontFamily,
		fontSize = fontSize ?: androidx.compose.ui.unit.TextUnit.Unspecified,
		textDecoration = decoration,
	)
}

private fun parseCssColor(raw: String): Color? {
	val v = raw.trim().lowercase()
	if (v.startsWith("#")) {
		val hex = v.substring(1)
		return when (hex.length) {
			3 -> {
				val r = hex[0].digitToIntOrNull(16) ?: return null
				val g = hex[1].digitToIntOrNull(16) ?: return null
				val b = hex[2].digitToIntOrNull(16) ?: return null
				Color(red = (r * 17) / 255f, green = (g * 17) / 255f, blue = (b * 17) / 255f)
			}

			6 -> {
				val r = hex.substring(0, 2).toIntOrNull(16) ?: return null
				val g = hex.substring(2, 4).toIntOrNull(16) ?: return null
				val b = hex.substring(4, 6).toIntOrNull(16) ?: return null
				Color(red = r / 255f, green = g / 255f, blue = b / 255f)
			}

			8 -> {
				// CSS #RRGGBBAA — alpha last.
				val r = hex.substring(0, 2).toIntOrNull(16) ?: return null
				val g = hex.substring(2, 4).toIntOrNull(16) ?: return null
				val b = hex.substring(4, 6).toIntOrNull(16) ?: return null
				val a = hex.substring(6, 8).toIntOrNull(16) ?: return null
				Color(red = r / 255f, green = g / 255f, blue = b / 255f, alpha = a / 255f)
			}

			else -> null
		}
	}
	if (v.startsWith("rgb")) {
		val open = v.indexOf('(')
		val close = v.indexOf(')')
		if (open < 0 || close < open) return null
		val parts = v.substring(open + 1, close).split(',').map { it.trim() }
		if (parts.size < 3) return null
		val r = parts[0].toFloatOrNull() ?: return null
		val g = parts[1].toFloatOrNull() ?: return null
		val b = parts[2].toFloatOrNull() ?: return null
		val a = if (parts.size >= 4) parts[3].toFloatOrNull() ?: 1f else 1f
		return Color(red = r / 255f, green = g / 255f, blue = b / 255f, alpha = a)
	}
	return namedColors[v]
}

private val namedColors = mapOf(
	"black" to Color.Black, "white" to Color.White, "red" to Color.Red, "green" to Color.Green,
	"blue" to Color.Blue, "yellow" to Color.Yellow, "cyan" to Color.Cyan, "magenta" to Color.Magenta,
	"gray" to Color.Gray, "grey" to Color.Gray, "transparent" to Color.Transparent,
)

private fun parseCssFontWeight(raw: String): FontWeight? {
	val v = raw.trim().lowercase()
	return when (v) {
		"bold", "bolder" -> FontWeight.Bold
		"normal" -> FontWeight.Normal
		"lighter" -> FontWeight.Light
		else -> v.toIntOrNull()?.let { FontWeight(it) }
	}
}

private fun parseCssFontSize(raw: String): androidx.compose.ui.unit.TextUnit? {
	val v = raw.trim().lowercase()
	val numEnd = v.indexOfFirst { !(it.isDigit() || it == '.' || it == '-') }
	val (numStr, unit) = if (numEnd < 0) v to "px" else v.substring(0, numEnd) to v.substring(numEnd)
	val num = numStr.toFloatOrNull() ?: return null
	return when (unit) {
		"px", "pt" -> num.sp
		"em", "rem" -> num.em
		"%" -> (num / 100f).em
		else -> num.sp
	}
}

private fun parseCssTextDecoration(raw: String): TextDecoration? {
	val v = raw.lowercase()
	val u = v.contains("underline")
	val s = v.contains("line-through")
	return when {
		u && s -> TextDecoration.Underline + TextDecoration.LineThrough
		u -> TextDecoration.Underline
		s -> TextDecoration.LineThrough
		else -> null
	}
}

// ----- Block-level handling -----

private fun isBlockTag(name: String): Boolean = name in blockTags

private val blockTags = setOf(
	"p", "div", "li", "tr", "h1", "h2", "h3", "h4", "h5", "h6", "blockquote", "pre", "section", "article"
)

/**
 * Handle void/self-effect tags that produce literal text (newlines, hr) rather than
 * style scopes. Returns the literal text to append, or null if the tag isn't void.
 */
private fun handleVoid(name: String, @Suppress("UNUSED_PARAMETER") attrs: Map<String, String>): String? {
	return when (name) {
		"br", "wbr" -> "\n"
		"hr" -> "\n"
		else -> null
	}
}

// ----- Tokenizer -----

private sealed class HtmlToken {
	data class Text(val content: String) : HtmlToken()
	data class StartTag(val name: String, val attrs: Map<String, String>, val selfClose: Boolean) : HtmlToken()
	data class EndTag(val name: String) : HtmlToken()
}

private fun tokenizeHtml(html: String): List<HtmlToken> {
	val tokens = mutableListOf<HtmlToken>()
	var i = 0
	val text = StringBuilder()
	while (i < html.length) {
		val c = html[i]
		if (c == '<' && i + 1 < html.length) {
			// Handle comments and CDATA-like sections by skipping them entirely.
			if (html.startsWith("<!--", i)) {
				val end = html.indexOf("-->", i + 4)
				i = if (end < 0) html.length else end + 3
				continue
			}
			if (html.startsWith("<!", i) || html.startsWith("<?", i)) {
				val end = html.indexOf('>', i)
				i = if (end < 0) html.length else end + 1
				continue
			}
			val tagEnd = html.indexOf('>', i + 1)
			if (tagEnd < 0) {
				// Stray '<' — treat as literal text.
				text.append(c); i++; continue
			}
			// Flush pending text.
			if (text.isNotEmpty()) {
				tokens += HtmlToken.Text(text.toString()); text.clear()
			}
			val raw = html.substring(i + 1, tagEnd)
			tokens += parseTag(raw)
			// Special-case <script> and <style>: skip their content entirely.
			val last = tokens.lastOrNull()
			if (last is HtmlToken.StartTag && (last.name == "script" || last.name == "style") && !last.selfClose) {
				val closeIdx = html.indexOf("</${last.name}", tagEnd + 1, ignoreCase = true)
				if (closeIdx < 0) return tokens
				val closeEnd = html.indexOf('>', closeIdx)
				if (closeEnd < 0) return tokens
				tokens.removeLast() // Drop the script/style tag entirely
				i = closeEnd + 1
				continue
			}
			i = tagEnd + 1
		} else {
			text.append(c); i++
		}
	}
	if (text.isNotEmpty()) tokens += HtmlToken.Text(text.toString())
	return tokens
}

private fun parseTag(raw: String): HtmlToken {
	val trimmed = raw.trim()
	if (trimmed.startsWith("/")) {
		val name = trimmed.substring(1).trim().substringBefore(' ').lowercase()
		return HtmlToken.EndTag(name)
	}
	val selfClose = trimmed.endsWith("/")
	val body = if (selfClose) trimmed.dropLast(1).trim() else trimmed
	val firstSpace = body.indexOfFirst { it.isWhitespace() }
	val name = if (firstSpace < 0) body.lowercase() else body.substring(0, firstSpace).lowercase()
	val attrs = if (firstSpace < 0) emptyMap() else parseAttrs(body.substring(firstSpace + 1))
	val effectiveSelfClose = selfClose || name in voidElements
	return HtmlToken.StartTag(name, attrs, effectiveSelfClose)
}

private val voidElements = setOf("br", "hr", "img", "wbr", "meta", "link", "input", "source", "area", "base", "col")

private fun parseAttrs(raw: String): Map<String, String> {
	val map = mutableMapOf<String, String>()
	var i = 0
	while (i < raw.length) {
		while (i < raw.length && raw[i].isWhitespace()) i++
		if (i >= raw.length) break
		val nameStart = i
		while (i < raw.length && !raw[i].isWhitespace() && raw[i] != '=') i++
		val name = raw.substring(nameStart, i).lowercase()
		if (name.isEmpty()) break
		while (i < raw.length && raw[i].isWhitespace()) i++
		if (i < raw.length && raw[i] == '=') {
			i++
			while (i < raw.length && raw[i].isWhitespace()) i++
			val value = if (i < raw.length && (raw[i] == '"' || raw[i] == '\'')) {
				val quote = raw[i]; i++
				val start = i
				while (i < raw.length && raw[i] != quote) i++
				val v = raw.substring(start, i)
				if (i < raw.length) i++
				v
			} else {
				val start = i
				while (i < raw.length && !raw[i].isWhitespace()) i++
				raw.substring(start, i)
			}
			map[name] = decodeEntities(value)
		} else {
			map[name] = ""
		}
	}
	return map
}

// ----- Entities -----

private fun decodeEntities(s: String): String {
	if ('&' !in s) return s
	val sb = StringBuilder(s.length)
	var i = 0
	while (i < s.length) {
		val c = s[i]
		if (c == '&') {
			val end = s.indexOf(';', i + 1)
			if (end > i && end - i <= 10) {
				val entity = s.substring(i + 1, end)
				val resolved = resolveEntity(entity)
				if (resolved != null) {
					sb.append(resolved); i = end + 1; continue
				}
			}
		}
		sb.append(c); i++
	}
	return sb.toString()
}

private fun resolveEntity(name: String): String? {
	if (name.startsWith("#")) {
		val numPart = name.substring(1)
		val code = if (numPart.startsWith("x") || numPart.startsWith("X")) {
			numPart.substring(1).toIntOrNull(16)
		} else numPart.toIntOrNull()
		return code?.let {
			if (it in 0x10000..0x10FFFF) {
				val adjusted = it - 0x10000
				val high = 0xD800 + (adjusted ushr 10)
				val low = 0xDC00 + (adjusted and 0x3FF)
				"${high.toChar()}${low.toChar()}"
			} else if (it in 0..0xFFFF) it.toChar().toString() else null
		}
	}
	return when (name) {
		"amp" -> "&"
		"lt" -> "<"
		"gt" -> ">"
		"quot" -> "\""
		"apos" -> "'"
		"nbsp" -> " "
		"copy" -> "©"
		"reg" -> "®"
		"hellip" -> "…"
		"mdash" -> "—"
		"ndash" -> "–"
		"lsquo" -> "‘"
		"rsquo" -> "’"
		"ldquo" -> "“"
		"rdquo" -> "”"
		else -> null
	}
}
