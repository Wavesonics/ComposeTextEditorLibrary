package com.darkrockstudios.texteditor.cursor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Constraints
import com.darkrockstudios.texteditor.LineWrap
import com.darkrockstudios.texteditor.state.TextEditorState
import com.darkrockstudios.texteditor.TextOffset

fun TextEditorState.calculateCursorPosition(
	textMeasurer: TextMeasurer,
	canvasWidth: Float,
	scrollOffset: Int
): CursorMetrics {
	val (line, charIndex) = cursorPosition
	val layout = textMeasurer.measure(
		textLines[line],
		constraints = Constraints(
			maxWidth = maxOf(1, canvasWidth.toInt()),
			minHeight = 0,
			maxHeight = Constraints.Infinity
		)
	)

	val currentWrappedLineIndex = lineOffsets.getWrappedLineIndex(cursorPosition)
	val currentWrappedLine = lineOffsets[currentWrappedLineIndex]
	val startOfLineOffset = lineOffsets.first { it.line == currentWrappedLine.line }.offset

	val currentLine = layout.multiParagraph.getLineForOffset(charIndex)
	val cursorX = layout.multiParagraph.getHorizontalPosition(charIndex, true)
	val cursorY = startOfLineOffset.y - scrollOffset + layout.multiParagraph.getLineTop(currentLine)
	val lineHeight = layout.multiParagraph.getLineHeight(currentLine)

	return CursorMetrics(
		position = Offset(cursorX, cursorY),
		height = lineHeight
	)
}

private fun List<LineWrap>.getWrappedLineIndex(position: TextOffset): Int {
	return indexOfLast { lineOffset ->
		lineOffset.line == position.line && lineOffset.wrapStartsAtIndex <= position.char
	}
}

private fun List<LineWrap>.getWrappedLine(position: TextOffset): LineWrap {
	return last { lineOffset ->
		lineOffset.line == position.line && lineOffset.wrapStartsAtIndex <= position.char
	}
}