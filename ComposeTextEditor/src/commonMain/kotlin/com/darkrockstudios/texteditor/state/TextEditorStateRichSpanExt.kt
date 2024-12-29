package com.darkrockstudios.texteditor.state

import com.darkrockstudios.texteditor.CharLineOffset
import com.darkrockstudios.texteditor.TextEditorRange
import com.darkrockstudios.texteditor.richstyle.RichSpan

/**
 * Extension functions for TextEditorState to inspect spans at a given position
 */

fun TextEditorState.getRichSpansAtPosition(position: CharLineOffset): Set<RichSpan> {
	// Get the line wrap that contains our position
	val lineWrap = lineOffsets.firstOrNull { wrap ->
		wrap.line == position.line && position.char >= wrap.wrapStartsAtIndex
	} ?: return emptySet()

	// Filter spans that contain the position
	return lineWrap.richSpans.filter { span ->
		span.containsPosition(position)
	}.toSet()
}

fun TextEditorState.getRichSpansInRange(range: TextEditorRange): Set<RichSpan> {
	// Validate range
	if (range.start.line < 0 || range.start.line >= textLines.size ||
		range.end.line < 0 || range.end.line >= textLines.size
	) {
		return emptySet()
	}

	val richSpans = mutableSetOf<RichSpan>()

	when {
		// Single line case
		range.isSingleLine() -> {
			val lineWraps = lineOffsets.filter { it.line == range.start.line }
			lineWraps.forEach { lineWrap ->
				richSpans.addAll(
					lineWrap.richSpans.filter { span ->
						(span.range.start.char < range.end.char && span.range.end.char > range.start.char)
					}
				)
			}
		}
		// Multi-line case
		else -> {
			// First line - from start position to end of line
			val firstLineWraps = lineOffsets.filter { it.line == range.start.line }
			firstLineWraps.forEach { lineWrap ->
				richSpans.addAll(
					lineWrap.richSpans.filter { span -> span.range.end.char > range.start.char }
				)
			}

			// Middle lines - all spans
			for (lineIndex in (range.start.line + 1) until range.end.line) {
				val middleLineWraps = lineOffsets.filter { it.line == lineIndex }
				middleLineWraps.forEach { lineWrap ->
					richSpans.addAll(lineWrap.richSpans)
				}
			}

			// Last line - from start of line to end position
			if (range.end.line > range.start.line) {
				val lastLineWraps = lineOffsets.filter { it.line == range.end.line }
				lastLineWraps.forEach { lineWrap ->
					richSpans.addAll(
						lineWrap.richSpans.filter { span -> span.range.start.char < range.end.char }
					)
				}
			}
		}
	}

	return richSpans
}
