package com.darkrockstudios.texteditor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Constraints
import com.darkrockstudios.texteditor.state.TextEditorState

internal fun DrawScope.drawSelection(
	textMeasurer: TextMeasurer,
	state: TextEditorState,
) {
	val scrollOffset = state.scrollState.value
	state.selector.selection?.let { selection ->
		for (lineIndex in state.textLines.indices) {
			val line = state.textLines[lineIndex]
			val textLayoutResult = textMeasurer.measure(
				line,
				constraints = Constraints(
					maxWidth = maxOf(1, size.width.toInt()),
					minHeight = 0,
					maxHeight = Constraints.Infinity
				)
			)

			// Find the wrapped lines for this actual line
			val lineWraps = state.lineOffsets.filter { it.line == lineIndex }

			for (wrap in lineWraps) {
				val adjustedY = wrap.offset.y - scrollOffset

				// Only process if this wrapped line is visible
				if (adjustedY + textLayoutResult.size.height >= 0 && adjustedY <= size.height) {
					// Calculate selection bounds for this wrapped line
					val nextWrapStartIndex = lineWraps
						.firstOrNull { it.wrapStartsAtIndex > wrap.wrapStartsAtIndex }
						?.wrapStartsAtIndex
						?: line.length

					val selectionStart = when {
						lineIndex < selection.start.line -> null
						lineIndex > selection.end.line -> null
						lineIndex == selection.start.line &&
								selection.start.char > nextWrapStartIndex -> null
						lineIndex == selection.start.line ->
							maxOf(selection.start.char, wrap.wrapStartsAtIndex)
						else -> wrap.wrapStartsAtIndex
					}

					val selectionEnd = when {
						lineIndex < selection.start.line -> null
						lineIndex > selection.end.line -> null
						lineIndex == selection.end.line &&
								selection.end.char <= wrap.wrapStartsAtIndex -> null
						lineIndex == selection.end.line ->
							minOf(selection.end.char, nextWrapStartIndex)
						else -> nextWrapStartIndex
					}

					// Draw selection highlight if we have valid bounds
					if (selectionStart != null && selectionEnd != null && selectionEnd > selectionStart) {
						val startX = textLayoutResult.multiParagraph.getHorizontalPosition(selectionStart, true)
						val endX = textLayoutResult.multiParagraph.getHorizontalPosition(selectionEnd, true)
						val lineHeight = textLayoutResult.multiParagraph.getLineHeight(0)

						drawRect(
							color = Color(0x400000FF),
							topLeft = Offset(startX, adjustedY),
							size = Size(
								width = endX - startX,
								height = lineHeight
							)
						)
					}
				}
			}
		}
	}
}
