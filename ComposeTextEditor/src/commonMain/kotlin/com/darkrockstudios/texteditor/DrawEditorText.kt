package com.darkrockstudios.texteditor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.drawText
import androidx.compose.ui.util.fastForEach
import com.darkrockstudios.texteditor.state.TextEditorState
import com.darkrockstudios.texteditor.utils.getBoundingBoxes

internal fun DrawScope.DrawEditorText(
	state: TextEditorState,
	style: TextEditorStyle,
	decorateLine: LineDecorator?,
) {
	// Get current scroll position and viewport height
	val scrollY = state.scrollState.value
	val viewportHeight = size.height

	// Calculate visible range with some padding to ensure smooth scrolling
	val minY = (scrollY - viewportHeight * 0.1f).coerceAtLeast(0f)
	val maxY = scrollY + viewportHeight

	var lastLine = -1
	state.lineOffsets.fastForEach { virtualLine ->
		// Check if this line could be visible
		val lineTop = virtualLine.offset.y
		val lineHeight = virtualLine.textLayoutResult.size.height
		val lineBottom = lineTop + lineHeight

		if (lineBottom >= minY && lineTop <= maxY) {
			if (lastLine != virtualLine.line && state.textLines.size > virtualLine.line) {
				val line = state.textLines[virtualLine.line]

				val offset = virtualLine.offset.copy(y = virtualLine.offset.y - scrollY)
				decorateLine?.let {
					decorateLine(virtualLine.line, offset, state, style)
				}

				drawText(
					textMeasurer = state.textMeasurer,
					text = line,
					topLeft = offset,
					style = state.textStyle.copy(
						color = style.textColor,
					)
				)

				lastLine = virtualLine.line
			}

			drawRichSpans(virtualLine, state)

			// Draw composing underline if this line intersects the composing region
			state.composingRange?.let { composingRange ->
				drawComposingUnderline(virtualLine, state, composingRange, style)
			}
		}
	}
}

/**
 * Draws an underline for the IME composing region (autocomplete preview).
 */
private fun DrawScope.drawComposingUnderline(
	lineWrap: LineWrap,
	state: TextEditorState,
	composingRange: TextEditorRange,
	style: TextEditorStyle
) {
	val textLayoutResult = lineWrap.textLayoutResult

	// Get the range of text visible in this wrap
	val wrapVisibleStart = textLayoutResult.getLineStart(lineWrap.virtualLineIndex)
	val wrapVisibleEnd = textLayoutResult.getLineEnd(lineWrap.virtualLineIndex, visibleEnd = true)

	// Calculate where in the original line this wrapped segment starts and ends
	val lineStart = CharLineOffset(line = lineWrap.line, char = lineWrap.wrapStartsAtIndex)
	val lineEnd = CharLineOffset(
		line = lineWrap.line,
		char = lineWrap.wrapStartsAtIndex + (wrapVisibleEnd - wrapVisibleStart)
	)

	// Convert to absolute character indices
	val composingStartAbsChar = composingRange.start.toCharacterIndex(state)
	val composingEndAbsChar = composingRange.end.toCharacterIndex(state)
	val lineStartAbsChar = lineStart.toCharacterIndex(state)
	val lineEndAbsChar = lineEnd.toCharacterIndex(state)

	// Check if this wrapped segment intersects with the composing region
	if (composingStartAbsChar <= lineEndAbsChar && composingEndAbsChar >= lineStartAbsChar) {
		// Calculate the local range within this wrapped segment
		val localStart = if (composingStartAbsChar <= lineStartAbsChar) {
			wrapVisibleStart
		} else {
			(composingStartAbsChar - lineStartAbsChar) + wrapVisibleStart
		}

		val localEnd = if (composingEndAbsChar >= lineEndAbsChar) {
			wrapVisibleEnd
		} else {
			((composingEndAbsChar - lineStartAbsChar) + wrapVisibleStart)
				.coerceAtMost(wrapVisibleEnd)
		}

		if (localStart < localEnd) {
			// Get bounding boxes for the composing text
			val boxes = textLayoutResult.getBoundingBoxes(localStart, localEnd)

			// Draw underline for each box
			val scrollY = state.scrollState.value
			val underlineColor = style.textColor.copy(alpha = 0.6f)
			val strokeWidth = 2f

			boxes.forEach { box ->
				val y = lineWrap.offset.y - scrollY + box.bottom - strokeWidth
				drawLine(
					color = underlineColor,
					start = Offset(box.left, y),
					end = Offset(box.right, y),
					strokeWidth = strokeWidth
				)
			}
		}
	}
}