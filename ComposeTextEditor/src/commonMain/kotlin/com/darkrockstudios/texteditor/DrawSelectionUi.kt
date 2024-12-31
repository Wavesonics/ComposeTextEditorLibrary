package com.darkrockstudios.texteditor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.darkrockstudios.texteditor.state.TextEditorState

internal fun DrawScope.drawSelection(
	state: TextEditorState,
	selectionColor: Color,
) {
	state.selector.selection?.let { selection ->
		// Get only the wrapped lines within the selection range
		val relevantLines = state.lineOffsets.filter { wrap ->
			wrap.line in selection.start.line..selection.end.line
		}

		relevantLines.forEach { wrap ->
			val multiParagraph = wrap.textLayoutResult.multiParagraph

			// Get the actual start and end positions for this virtual line
			val lineStart = multiParagraph.getLineStart(wrap.virtualLineIndex)
			val lineEnd = multiParagraph.getLineEnd(wrap.virtualLineIndex, visibleEnd = true)

			val (selectionStart, selectionEnd) = when (wrap.line) {
				selection.start.line -> {
					if (selection.start.char > lineEnd) {
						null to null
					} else {
						val start = maxOf(lineStart, selection.start.char)
						val end = minOf(
							lineEnd,
							if (wrap.line == selection.end.line) selection.end.char else lineEnd
						)
						start to end
					}
				}

				selection.end.line -> {
					if (selection.end.char <= lineStart) {
						null to null
					} else {
						val start = maxOf(lineStart, wrap.wrapStartsAtIndex)
						val end = minOf(lineEnd, selection.end.char)
						start to end
					}
				}

				else -> {
					// For lines in between start and end, select the entire virtual line
					lineStart to lineEnd
				}
			}

			// Draw the selection if we have valid bounds
			if (selectionStart != null && selectionEnd != null && selectionEnd > selectionStart) {
				val startX = multiParagraph.getHorizontalPosition(selectionStart, true)

				val lineEndOffset = wrap.textLayoutResult.getLineEnd(wrap.virtualLineIndex, false)
				val endX = if (selectionEnd >= lineEndOffset) {
					wrap.textLayoutResult.getLineRight(wrap.virtualLineIndex)
				} else {
					wrap.textLayoutResult.getHorizontalPosition(
						selectionEnd,
						usePrimaryDirection = true
					)
				}

				val lineHeight = multiParagraph.getLineHeight(wrap.virtualLineIndex)

				drawRect(
					color = selectionColor,
					topLeft = Offset(startX, wrap.offset.y),
					size = Size(
						width = endX - startX,
						height = lineHeight
					)
				)
			}
		}
	}
}