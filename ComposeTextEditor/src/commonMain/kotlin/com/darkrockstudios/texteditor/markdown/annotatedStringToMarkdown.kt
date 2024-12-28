package com.darkrockstudios.texteditor.markdown

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit

/**
 * Converts an AnnotatedString to a markdown string, handling supported markdown styles.
 * Only converts styles that match our supported markdown styles, dropping any unsupported styles.
 */
fun AnnotatedString.toMarkdown(): String {
	if (text.isEmpty()) return ""

	// Create a list of style boundaries (start and end points)
	data class StyleBoundary(
		val index: Int,
		val isStart: Boolean,
		val marker: StyleMarkerPair,
		val originalSpan: AnnotatedString.Range<SpanStyle>
	)

	val boundaries = mutableListOf<StyleBoundary>()

	// Process each span and create boundaries
	spanStyles.forEach { span ->
		val marker = getStyleMarker(span.item) ?: return@forEach
		boundaries.add(StyleBoundary(span.start, true, marker, span))
		boundaries.add(StyleBoundary(span.end, false, marker, span))
	}

	// Sort boundaries:
	// 1. By index
	// 2. For same index, close markers come before open markers
	// 3. For same index and type, sort by span length (longer spans close first)
	boundaries.sortWith(compareBy<StyleBoundary> { it.index }
		.thenBy { it.isStart }
		.thenByDescending { it.originalSpan.end - it.originalSpan.start })

	val result = StringBuilder()
	var currentIndex = 0

	boundaries.forEach { boundary ->
		// Add any text between the last position and this boundary
		while (currentIndex < boundary.index) {
			result.append(text[currentIndex])
			currentIndex++
		}

		// Add the appropriate marker
		if (boundary.isStart) {
			result.append(boundary.marker.openMarker)
		} else {
			result.append(boundary.marker.closeMarker)
		}
	}

	// Add any remaining text
	while (currentIndex < text.length) {
		result.append(text[currentIndex])
		currentIndex++
	}

	return result.toString()
}

private data class StyleMarker(
	val openMarker: String,
	val closeMarker: String,
	val startIndex: Int,
	val endIndex: Int
)

private fun getStyleMarker(style: SpanStyle): StyleMarkerPair? {
	return when {
		style.fontWeight == FontWeight.Bold -> StyleMarkerPair("**", "**")
		style.fontStyle == FontStyle.Italic -> StyleMarkerPair("*", "*")
		style.fontFamily == FontFamily.Monospace -> StyleMarkerPair("`", "`")
		style.textDecoration == TextDecoration.Underline -> StyleMarkerPair("[", "]()")
		// Headers are a special case as they must be at the start of a line
		style.fontSize != TextUnit.Unspecified -> {
			when (style.fontSize.value) {
				32f -> StyleMarkerPair("# ", "\n")
				24f -> StyleMarkerPair("## ", "\n")
				18.72f -> StyleMarkerPair("### ", "\n")
				16f -> StyleMarkerPair("#### ", "\n")
				13.28f -> StyleMarkerPair("##### ", "\n")
				12f -> StyleMarkerPair("###### ", "\n")
				else -> null
			}
		}

		(style.fontStyle == FontStyle.Italic && style.color?.let { it.red == it.green && it.green == it.blue } == true) ->
			StyleMarkerPair("> ", "\n")

		else -> null
	}
}

private data class StyleMarkerPair(
	val openMarker: String,
	val closeMarker: String
)

/**
 * Utility extension to help determine if a position is at the start of a line in the text
 */
private fun String.isStartOfLine(position: Int): Boolean {
	if (position == 0) return true
	return position > 0 && this[position - 1] == '\n'
}