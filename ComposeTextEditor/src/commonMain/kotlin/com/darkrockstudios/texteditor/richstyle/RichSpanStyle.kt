package com.darkrockstudios.texteditor.richstyle

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.Density
import com.darkrockstudios.texteditor.CharLineOffset
import com.darkrockstudios.texteditor.LineWrap
import com.darkrockstudios.texteditor.TextEditorRange

interface RichSpanStyle {
	fun DrawScope.drawCustomStyle(
		layoutResult: TextLayoutResult,
		lineWrap: LineWrap,
		textRange: TextRange
	)
}

/**
 * A [RichSpanStyle] that occupies a full line as a block — its [blockHeight]
 * overrides the line's text-derived height, so the line takes up the requested
 * vertical space (image height, code-block padding, etc.).
 *
 * The line should contain only a placeholder character; the block's
 * [drawCustomStyle] is responsible for the entire visual.
 */
interface BlockSpanStyle : RichSpanStyle {
	/**
	 * Returns the desired height of the block in pixels. Called during layout
	 * book-keeping; should be cheap and deterministic for a given input.
	 */
	fun blockHeight(density: Density, viewportWidth: Float): Float

	/**
	 * If true, the editor skips drawing the underlying placeholder text on
	 * this line — the block paints the full line itself.
	 */
	fun replacesText(): Boolean = true
}

data class RichSpan(
	val range: TextEditorRange,
	val style: RichSpanStyle
) {
	fun intersectsWith(lineWrap: LineWrap): Boolean {
		// If not on the right line, no intersection
		if (lineWrap.line < range.start.line || lineWrap.line > range.end.line) {
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
		if (range.start.line == range.end.line && range.start.line == lineWrap.line) {
			// Check if any part of the span overlaps with this wrapped segment
			return (range.start.char < lineEnd && range.end.char > lineStart)
		}

		// For multi-line spans or wrapped lines:
		return when (lineWrap.line) {
			range.start.line -> range.start.char < lineEnd     // First line: span starts before line segment ends
			range.end.line -> range.end.char > lineStart       // Last line: span ends after line segment starts
			else -> true                           // Middle lines are fully covered
		}
	}

	fun containsPosition(position: CharLineOffset): Boolean {
		// Validate position
		if (position.line < 0 || position.char < 0) {
			return false
		}

		return when {
			position.line < range.start.line || position.line > range.end.line -> false
			position.line == range.start.line && position.line == range.end.line ->
				position.char >= range.start.char && position.char < range.end.char

			position.line == range.start.line -> position.char >= range.start.char
			position.line == range.end.line -> position.char < range.end.char
			else -> true
		}
	}
}