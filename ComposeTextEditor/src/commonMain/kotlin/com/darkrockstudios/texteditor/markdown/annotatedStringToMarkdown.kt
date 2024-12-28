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
			if (boundary.marker.openMarker.contains("#")) {
				// Ensure header starts on a new line
				if (!result.endsWith("\n") && result.isNotEmpty()) {
					result.append("\n")
				}
				result.append(boundary.marker.openMarker)
			} else {
				result.append(boundary.marker.openMarker)
			}
		} else {
			result.append(boundary.marker.closeMarker)
			if (boundary.marker.closeMarker == "\n") {
				// Avoid duplicate newlines
				if (currentIndex < text.length && text[currentIndex] == '\n') {
					currentIndex++
				}
			}
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
		style.fontWeight == FontWeight.Bold && style.fontSize != TextUnit.Unspecified -> {
			when (style.fontSize.value) {
				32f -> StyleMarkerPair("# ", "\n") // Header 1
				24f -> StyleMarkerPair("## ", "\n") // Header 2
				18.72f -> StyleMarkerPair("### ", "\n") // Header 3
				16f -> StyleMarkerPair("#### ", "\n") // Header 4
				13.28f -> StyleMarkerPair("##### ", "\n") // Header 5
				12f -> StyleMarkerPair("###### ", "\n") // Header 6
				else -> null
			}
		}
		style.fontWeight == FontWeight.Bold -> StyleMarkerPair("**", "**")
		style.fontStyle == FontStyle.Italic -> StyleMarkerPair("*", "*")
		style.fontFamily == FontFamily.Monospace -> StyleMarkerPair("`", "`")
		style.textDecoration == TextDecoration.Underline -> StyleMarkerPair("[", "]()")
		else -> null
	}
}

private data class StyleMarkerPair(
	val openMarker: String,
	val closeMarker: String
)