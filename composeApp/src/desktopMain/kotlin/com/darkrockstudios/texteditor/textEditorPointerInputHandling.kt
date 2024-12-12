package com.darkrockstudios.texteditor

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Constraints
import com.darkrockstudios.texteditor.state.TextEditorState
import kotlin.math.min

internal fun Modifier.textEditorPointerInputHandling(
	state: TextEditorState,
): Modifier {
	return this.pointerInput(Unit) {
		detectTapGestures { tapOffset ->
			state.apply {
				if (lineOffsets.isEmpty()) return@detectTapGestures

				var curRealLine: LineWrap = lineOffsets[0]
				// Calculate the clicked line and character within the wrapped text
				for (lineWrap in lineOffsets) {
					if (lineWrap.line != curRealLine.line) {
						curRealLine = lineWrap
					}

					val textLayoutResult = textMeasurer.measure(
						textLines[lineWrap.line],
						constraints = Constraints(maxWidth = size.width)
					)

					val relativeTapOffset = tapOffset - curRealLine.offset
					if (tapOffset.y in curRealLine.offset.y..(curRealLine.offset.y + textLayoutResult.size.height)) {
						val charPos =
							textLayoutResult.multiParagraph.getOffsetForPosition(relativeTapOffset)
						cursorPosition =
							TextOffset(lineWrap.line, min(charPos, textLines[lineWrap.line].length))

						state.selector.clearSelection()
						break
					}
				}
			}
		}
	}.pointerInput(Unit) {
		var dragStartPosition: TextOffset? = null
		detectDragGestures(
			onDragStart = { offset ->
				// Convert the initial click position to a TextOffset
				dragStartPosition = state.getOffsetAtPosition(offset)
				// Update cursor to drag start position
				dragStartPosition?.let { pos ->
					state.updateCursorPosition(pos)
				}
			},
			onDragEnd = {
				dragStartPosition = null
			},
			onDrag = { change, _ ->
				// Convert current drag position to TextOffset
				val currentPosition = state.getOffsetAtPosition(change.position)
				// Update selection between drag start and current position
				dragStartPosition?.let { startPos ->
					state.selector.updateSelection(startPos, currentPosition)
				}
				// Update cursor position to follow drag
				state.updateCursorPosition(currentPosition)
			}
		)
	}
}