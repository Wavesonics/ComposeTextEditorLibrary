package com.darkrockstudios.texteditor.cursor

import androidx.compose.ui.geometry.Offset
import com.darkrockstudios.texteditor.CharLineOffset
import com.darkrockstudios.texteditor.LineWrap
import com.darkrockstudios.texteditor.state.TextEditorState

fun TextEditorState.calculateCursorPosition(): CursorMetrics {
	val (_, charIndex) = cursorPosition

	val currentWrappedLineIndex = lineOffsets.getWrappedLineIndex(cursorPosition)
	val currentWrappedLine = lineOffsets[currentWrappedLineIndex]
	val startOfLineOffset = lineOffsets.first { it.line == currentWrappedLine.line }.offset

	val layout = currentWrappedLine.textLayoutResult

	val lineEndOffset = layout.getLineEnd(currentWrappedLine.virtualLineIndex, false)
	val cursorX = if (charIndex == lineEndOffset) {
		layout.getLineRight(currentWrappedLine.virtualLineIndex)
	} else {
		layout.getHorizontalPosition(charIndex, usePrimaryDirection = true)
	}

	val cursorY =
		startOfLineOffset.y + layout.multiParagraph.getLineTop(currentWrappedLine.virtualLineIndex)
	val lineHeight = layout.multiParagraph.getLineHeight(currentWrappedLine.virtualLineIndex)

	return CursorMetrics(
		position = Offset(cursorX, cursorY),
		height = lineHeight
	)
}

private fun List<LineWrap>.getWrappedLineIndex(position: CharLineOffset): Int {
	return indexOfLast { lineOffset ->
		lineOffset.line == position.line && lineOffset.wrapStartsAtIndex <= position.char
	}
}

private fun List<LineWrap>.getWrappedLine(position: CharLineOffset): LineWrap {
	return last { lineOffset ->
		lineOffset.line == position.line && lineOffset.wrapStartsAtIndex <= position.char
	}
}