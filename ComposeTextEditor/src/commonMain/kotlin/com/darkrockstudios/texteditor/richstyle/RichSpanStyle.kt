package com.darkrockstudios.texteditor.richstyle

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import com.darkrockstudios.texteditor.CharLineOffset
import com.darkrockstudios.texteditor.LineWrap

interface RichSpanStyle {
	fun DrawScope.drawCustomStyle(
		layoutResult: TextLayoutResult,
		lineIndex: Int,
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

		// Get the effective range for this virtual line segment
		val lineStart = lineWrap.textLayoutResult.getLineStart(lineWrap.virtualLineIndex)
		val lineEnd = if (lineWrap.textLayoutResult.lineCount > 0) {
			lineWrap.textLayoutResult.getLineEnd(lineWrap.virtualLineIndex)
		} else {
			lineStart
		}

		// For single-line spans on the same line
		if (start.line == end.line && start.line == lineWrap.line) {
			// Check if any part of the span overlaps with this wrapped segment
			return (start.char < lineEnd && end.char > lineStart)
		}

		// For multi-line spans or wrapped lines:
		return when (lineWrap.line) {
			start.line -> start.char < lineEnd     // First line: span starts before line segment ends
			end.line -> end.char > lineStart       // Last line: span ends after line segment starts
			else -> true                           // Middle lines are fully covered
		}
	}

	fun containsPosition(position: CharLineOffset): Boolean {
		// Validate position
		if (position.line < 0 || position.char < 0) {
			return false
		}

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