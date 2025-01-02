package com.darkrockstudios.texteditor

import com.darkrockstudios.texteditor.state.TextEditorState

data class TextEditorRange(
	val start: CharLineOffset,
	val end: CharLineOffset
) {
	fun isSingleLine(): Boolean = start.line == end.line
	fun validate(): Boolean = end isAfter start
	fun containsLine(lineIndex: Int): Boolean {
		return lineIndex >= start.line && lineIndex <= end.line
	}

	/**
	 * Returns an IntRange representing the line numbers that this TextEditorRange spans.
	 */
	fun affectedLines(): IntRange {
		return minOf(start.line, end.line)..maxOf(start.line, end.line)
	}

	/**
	 * Returns an IntRange representing the line wrap numbers that this TextEditorRange spans.
	 */
	fun affectedLineWraps(state: TextEditorState): IntRange {
		val startIndex = state.getWrappedLineIndex(start)
		val endIndex = state.getWrappedLineIndex(end)

		return minOf(startIndex, endIndex).coerceAtLeast(0)..maxOf(startIndex, endIndex)
	}

	/**
	 * Returns an IntRange representing the line numbers that this TextEditorRange spans,
	 * with an optional buffer of additional lines on either side.
	 *
	 * @param buffer Number of additional lines to include before and after the range
	 * @param maxLines The maximum line number allowed (inclusive)
	 */
	fun affectedLines(buffer: Int, maxLines: Int): IntRange {
		val minLine = minOf(start.line, end.line)
		val maxLine = maxOf(start.line, end.line)

		val startLine = (minLine - buffer).coerceAtLeast(0)
		val endLine = (maxLine + buffer).coerceAtMost(maxLines)
		return startLine..endLine
	}

	fun intersects(other: TextEditorRange): Boolean {
		return !(
				// This range ends before other starts
				end isBeforeOrEqual other.start ||
						// This range starts after other ends
						start isAfterOrEqual other.end
				)
	}

	fun merge(other: TextEditorRange): TextEditorRange {
		val newStart = if (this.start isBefore other.start) this.start else other.start
		val newEnd = if (this.end isAfter other.end) this.end else other.end
		return TextEditorRange(newStart, newEnd)
	}

	companion object {
		fun fromOffsets(offset1: CharLineOffset, offset2: CharLineOffset): TextEditorRange {
			IntRange
			return if (offset1 isBefore offset2) {
				TextEditorRange(offset1, offset2)
			} else {
				TextEditorRange(offset2, offset1)
			}
		}
	}
}


fun CharLineOffset.toRange(end: CharLineOffset): TextEditorRange {
	return TextEditorRange.fromOffsets(this, end)
}