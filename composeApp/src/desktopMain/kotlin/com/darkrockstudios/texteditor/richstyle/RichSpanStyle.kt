package com.darkrockstudios.texteditor.richstyle

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import com.darkrockstudios.texteditor.CharLineOffset
import com.darkrockstudios.texteditor.LineWrap

interface RichSpanStyle {
	fun DrawScope.drawCustomStyle(
		layoutResult: TextLayoutResult,
		textRange: TextRange
	)
}

data class RichSpan(
	val start: CharLineOffset,
	val end: CharLineOffset,
	val style: RichSpanStyle
) {
	fun intersectsWith(lineWrap: LineWrap): Boolean {
		// If not on the right line, no intersection
		if (lineWrap.line < start.line || lineWrap.line > end.line) {
			return false
		}

		// Get the start and end of this virtual line segment
		val lineStart = lineWrap.wrapStartsAtIndex
		val lineEnd = if (lineWrap.textLayoutResult.lineCount > 0) {
			lineWrap.textLayoutResult.getLineEnd(0)
		} else {
			lineStart
		}

		// For spans on same line, check character overlap
		if (start.line == end.line && start.line == lineWrap.line) {
			return !(end.char <= lineStart || start.char >= lineEnd)
		}

		// For multi-line spans:
		return when (lineWrap.line) {
			start.line -> start.char < lineEnd
			end.line -> end.char > lineStart
			else -> true // Middle lines are fully covered
		}
	}

	fun containsPosition(position: CharLineOffset): Boolean {
		return when {
			position.line < start.line || position.line > end.line -> false
			position.line == start.line && position.line == end.line ->
				position.char >= start.char && position.char < end.char

			position.line == start.line -> position.char >= start.char
			position.line == end.line -> position.char < end.char
			else -> true
		}
	}
}