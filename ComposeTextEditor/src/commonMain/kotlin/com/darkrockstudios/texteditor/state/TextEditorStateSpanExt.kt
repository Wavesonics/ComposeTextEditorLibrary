package com.darkrockstudios.texteditor.state

import androidx.compose.ui.text.SpanStyle
import com.darkrockstudios.texteditor.CharLineOffset
import com.darkrockstudios.texteditor.TextRange

/**
 * Returns all unique SpanStyles that are active at the given position.
 */
fun TextEditorState.getSpanStylesAtPosition(position: CharLineOffset): Set<SpanStyle> {
	// Validate line index is within bounds
	if (position.line < 0 || position.line >= textLines.size) {
		return emptySet()
	}

	val line = textLines[position.line]

	// Validate character position is within bounds
	if (position.char < 0 || position.char > line.length) {
		return emptySet()
	}

	// Find all unique spans that contain this position
	return line.spanStyles
		.filter { span ->
			// The position must be within the span's range (inclusive start, exclusive end)
			position.char >= span.start && position.char < span.end
		}
		.mapTo(mutableSetOf()) { span -> span.item }
}

/**
 * Returns all unique SpanStyles that intersect with the given TextRange.
 * For partially overlapping spans, the entire SpanStyle is included.
 */
fun TextEditorState.getSpanStylesInRange(range: TextRange): Set<SpanStyle> {
	// Validate range
	if (range.start.line < 0 || range.start.line >= textLines.size ||
		range.end.line < 0 || range.end.line >= textLines.size
	) {
		return emptySet()
	}

	val spans = mutableSetOf<SpanStyle>()

	when {
		// Single line case
		range.isSingleLine() -> {
			val line = textLines[range.start.line]
			spans.addAll(
				line.spanStyles
					.filter { span ->
						// Check if the span overlaps with our range
						(span.start < range.end.char && span.end > range.start.char)
					}
					.map { it.item }
			)
		}
		// Multi-line case
		else -> {
			// First line - from start position to end of line
			val firstLine = textLines[range.start.line]
			spans.addAll(
				firstLine.spanStyles
					.filter { span -> span.end > range.start.char }
					.map { it.item }
			)

			// Middle lines - all spans
			for (lineIndex in (range.start.line + 1) until range.end.line) {
				spans.addAll(textLines[lineIndex].spanStyles.map { it.item })
			}

			// Last line - from start of line to end position
			if (range.end.line > range.start.line) {
				val lastLine = textLines[range.end.line]
				spans.addAll(
					lastLine.spanStyles
						.filter { span -> span.start < range.end.char }
						.map { it.item }
				)
			}
		}
	}

	return spans
}

fun TextEditorState.debugRichSpans() {
	println("=== Debug Rich Spans ===")
	lineOffsets.forEach { wrap ->
		if (wrap.richSpans.isNotEmpty()) {
			println("Line ${wrap.line} (Virtual line ${wrap.virtualLineIndex}):")
			wrap.richSpans.forEach { span ->
				println("  ${span.style::class.simpleName} [${span.start.line}:${span.start.char} -> ${span.end.line}:${span.end.char}]")
			}
		}
	}
	println("=================")
}

fun TextEditorState.countOverlappingSpans(): Map<CharLineOffset, Int> {
	val spanCounts = mutableMapOf<CharLineOffset, Int>()

	lineOffsets.forEach { wrap ->
		wrap.richSpans.forEach { span ->
			// Check each character position in the span
			var currentLine = span.start.line
			var currentChar = span.start.char

			while (true) {
				val position = CharLineOffset(currentLine, currentChar)
				val count = getRichSpansAtPosition(position).size
				if (count > 1) {
					spanCounts[position] = count
				}

				// Move to next position
				if (currentLine == span.end.line && currentChar >= span.end.char) {
					break
				}

				if (currentChar >= (textLines.getOrNull(currentLine)?.length ?: 0)) {
					currentLine++
					currentChar = 0
				} else {
					currentChar++
				}
			}
		}
	}

	return spanCounts
}