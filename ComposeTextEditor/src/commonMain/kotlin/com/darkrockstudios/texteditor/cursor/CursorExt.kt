package com.darkrockstudios.texteditor.cursor

import androidx.compose.ui.geometry.Offset
import com.darkrockstudios.texteditor.CharLineOffset
import com.darkrockstudios.texteditor.LineWrap
import com.darkrockstudios.texteditor.richstyle.detectLineBlock
import com.darkrockstudios.texteditor.state.TextEditorState

fun TextEditorState.calculateCursorPosition(): CursorMetrics {
	val (_, charIndex) = cursorPosition

	val currentWrappedLineIndex = lineOffsets.getWrappedLineIndex(cursorPosition)
	val currentWrappedLine = lineOffsets[currentWrappedLineIndex]

	val layout = currentWrappedLine.textLayoutResult
	val virtualLineIndex = currentWrappedLine.virtualLineIndex

	// Clamp charIndex to valid range to handle race conditions when text changes asynchronously
	val textLength = layout.layoutInput.text.length
	val safeCharIndex = charIndex.coerceIn(0, textLength)

	val cursorX = layout.getHorizontalPosition(safeCharIndex, usePrimaryDirection = true) +
			emptyLineIndent()
	val cursorY = currentWrappedLine.offset.y - scrollState.value
	val lineHeight = layout.multiParagraph.getLineHeight(virtualLineIndex)

	// Calculate line metrics for IME cursor anchor info
	val lineTop = cursorY
	val lineBottom = cursorY + lineHeight
	val lineBaseline = cursorY + layout.multiParagraph.getLineBaseline(virtualLineIndex) -
			layout.multiParagraph.getLineTop(virtualLineIndex)

	return CursorMetrics(
		position = Offset(cursorX, cursorY),
		height = lineHeight,
		lineTop = lineTop,
		lineBaseline = lineBaseline,
		lineBottom = lineBottom
	)
}

/**
 * Compose's `getHorizontalPosition` returns 0 on an empty line because the
 * paragraph style's [androidx.compose.ui.text.style.TextIndent] doesn't apply
 * to a degenerate `[0, 0)` paragraph range. Add the indent manually so the
 * cursor sits at the indented position on an empty indented line — without
 * this it jumps from `x=0` to `x=indent` the moment the user types the first
 * character. Covers both line-block indents (bullet/quote/list/code) and the
 * editor-wide [androidx.compose.ui.text.TextStyle.textIndent]. Returns 0 if
 * the line isn't empty or no first-line indent is in effect.
 */
private fun TextEditorState.emptyLineIndent(): Float {
	if (cursorPosition.char != 0) return 0f
	val line = textLines.getOrNull(cursorPosition.line) ?: return 0f
	if (line.text.isNotEmpty()) return 0f
	// Block lines carry their own paragraph-style indent; plain lines inherit
	// the editor-wide textStyle indent (baked in by updateBookKeeping).
	val block = detectLineBlock(cursorPosition.line)
	val indent = block?.paragraphStyle?.textIndent?.firstLine
		?: textStyle.textIndent?.firstLine
		?: return 0f
	val d = density ?: return 0f
	return with(d) { indent.toPx() }
}

internal fun List<LineWrap>.getWrappedLineIndex(position: CharLineOffset): Int {
	return indexOfLast { lineOffset ->
		lineOffset.line == position.line && lineOffset.wrapStartsAtIndex <= position.char
	}
}

private fun List<LineWrap>.getWrappedLine(position: CharLineOffset): LineWrap {
	return last { lineOffset ->
		lineOffset.line == position.line && lineOffset.wrapStartsAtIndex <= position.char
	}
}