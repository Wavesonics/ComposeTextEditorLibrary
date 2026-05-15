package com.darkrockstudios.texteditor.clipboard

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType

/**
 * Serializes an [AnnotatedString] to an HTML fragment for the system clipboard.
 *
 * Covers the inline `SpanStyle` properties the editor produces (bold, italic,
 * underline, strikethrough, color, background, font size, monospace family).
 * Newlines become `<br/>`. Block-level [com.darkrockstudios.texteditor.richstyle.RichSpanStyle]
 * decorations are intentionally not serialized — the clipboard is for inline
 * selection text, not whole-document blocks.
 */
fun AnnotatedString.toHtml(): String {
	if (text.isEmpty()) return ""

	// Collect every position where the active style set could change.
	val boundariesSet = mutableSetOf(0, text.length)
	spanStyles.forEach {
		if (it.start in 0..text.length) boundariesSet.add(it.start)
		if (it.end in 0..text.length) boundariesSet.add(it.end)
	}

	val sb = StringBuilder()
	val positions = boundariesSet.sorted().toIntArray()
	var openTags: List<HtmlTag> = emptyList()
	for (i in 0 until positions.size - 1) {
		val start = positions[i]
		val end = positions[i + 1]
		if (start >= end) continue

		val merged = mergeStyles(spanStyles, start, end)
		val newTags = stylesToTags(merged)

		// Hold tags open across adjacent segments that share them — we only
		// close down to the longest common prefix and then open the rest.
		// `stylesToTags` returns tags in a deterministic order, so prefixes match.
		var commonLen = 0
		while (commonLen < openTags.size && commonLen < newTags.size &&
			openTags[commonLen] == newTags[commonLen]
		) commonLen++

		for (j in openTags.size - 1 downTo commonLen) sb.append(openTags[j].closeTag)
		for (j in commonLen until newTags.size) sb.append(newTags[j].openTag)
		appendEscaped(sb, text, start, end)
		openTags = newTags
	}
	for (j in openTags.size - 1 downTo 0) sb.append(openTags[j].closeTag)
	return sb.toString()
}

private fun mergeStyles(
	spans: List<AnnotatedString.Range<SpanStyle>>,
	start: Int,
	end: Int,
): SpanStyle {
	var merged = SpanStyle()
	spans.forEach { range ->
		// A span covers this segment iff it strictly contains [start, end).
		if (range.start <= start && range.end >= end) {
			merged = merged.merge(range.item)
		}
	}
	return merged
}

private data class HtmlTag(val openTag: String, val closeTag: String)

private fun stylesToTags(style: SpanStyle): List<HtmlTag> {
	val tags = mutableListOf<HtmlTag>()

	// Order matters for nesting symmetry but not for rendering.
	if (style.fontWeight != null && style.fontWeight!!.weight >= FontWeight.Bold.weight) {
		tags += HtmlTag("<strong>", "</strong>")
	}
	if (style.fontStyle == FontStyle.Italic) {
		tags += HtmlTag("<em>", "</em>")
	}
	style.textDecoration?.let { dec ->
		if (dec.contains(TextDecoration.Underline)) tags += HtmlTag("<u>", "</u>")
		if (dec.contains(TextDecoration.LineThrough)) tags += HtmlTag("<s>", "</s>")
	}
	if (style.fontFamily == FontFamily.Monospace) {
		tags += HtmlTag("<code>", "</code>")
	}

	val styleAttrs = mutableListOf<String>()
	if (style.color.isSpecified) {
		styleAttrs += "color:${style.color.toCssHex()}"
	}
	if (style.background.isSpecified) {
		styleAttrs += "background-color:${style.background.toCssHex()}"
	}
	style.fontSize.takeIf { it != TextUnit.Unspecified }?.let { size ->
		val unit = when (size.type) {
			TextUnitType.Sp -> "px"   // Round-trip as px on the clipboard; sp is Android-only.
			TextUnitType.Em -> "em"
			else -> null
		}
		if (unit != null) {
			styleAttrs += "font-size:${size.value.formatNumber()}$unit"
		}
	}
	if (styleAttrs.isNotEmpty()) {
		tags += HtmlTag("<span style=\"${styleAttrs.joinToString(";")}\">", "</span>")
	}

	return tags
}

private fun Color.toCssHex(): String {
	val r = (red * 255f + 0.5f).toInt().coerceIn(0, 255)
	val g = (green * 255f + 0.5f).toInt().coerceIn(0, 255)
	val b = (blue * 255f + 0.5f).toInt().coerceIn(0, 255)
	return "#" + r.toHex2() + g.toHex2() + b.toHex2()
}

private fun Int.toHex2(): String {
	val s = toString(16)
	return if (s.length < 2) "0$s" else s
}

private fun Float.formatNumber(): String {
	val rounded = (this * 100f).toInt() / 100f
	return if (rounded == rounded.toInt().toFloat()) rounded.toInt().toString() else rounded.toString()
}

private fun appendEscaped(sb: StringBuilder, text: CharSequence, start: Int, end: Int) {
	for (i in start until end) {
		when (val c = text[i]) {
			'&' -> sb.append("&amp;")
			'<' -> sb.append("&lt;")
			'>' -> sb.append("&gt;")
			'"' -> sb.append("&quot;")
			'\'' -> sb.append("&#39;")
			'\n' -> sb.append("<br/>")
			'\r' -> {} // Drop bare CR; CRLF becomes <br/> via the LF.
			else -> sb.append(c)
		}
	}
}
