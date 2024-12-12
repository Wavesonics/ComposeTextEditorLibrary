package com.darkrockstudios.texteditor

import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import com.darkrockstudios.texteditor.state.TextEditorState

internal fun Modifier.textEditorPointerInputHandling(
	state: TextEditorState,
): Modifier {
	return this.pointerInput(Unit) {
		detectTapsImperatively(
			onTap = { offset: Offset ->
				val position = state.getOffsetAtPosition(offset)
				state.updateCursorPosition(position)
				state.selector.clearSelection()
			},
			onDoubleTap = { offset: Offset ->
				val position = state.getOffsetAtPosition(offset)
				state.selectWordAt(position)
			},
			onTripleTap = { offset: Offset ->
				val position = state.getOffsetAtPosition(offset)
				state.selectLineAt(position)
			}
		)
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

private fun TextEditorState.selectLineAt(position: TextOffset) {
	val lineStart = TextOffset(position.line, 0)
	val lineEnd = TextOffset(position.line, textLines[position.line].length)
	updateCursorPosition(lineEnd)
	selector.updateSelection(lineStart, lineEnd)
}

private fun TextEditorState.selectWordAt(position: TextOffset) {
	val (wordStart, wordEnd) = findWordBoundary(position)
	updateCursorPosition(wordEnd)
	selector.updateSelection(wordStart, wordEnd)
}

private fun TextEditorState.findWordBoundary(position: TextOffset): Pair<TextOffset, TextOffset> {
	val line = textLines[position.line]
	var startChar = position.char
	var endChar = position.char

	// If we're at a word boundary or whitespace, try to find the nearest word
	if (startChar >= line.length || !isWordChar(line[startChar])) {
		// Look backward for the start of a word
		startChar = (startChar - 1).coerceAtLeast(0)
		while (startChar > 0 && !isWordChar(line[startChar])) {
			startChar--
		}
	}

	// Find start of word
	while (startChar > 0 && isWordChar(line[startChar - 1])) {
		startChar--
	}

	// Find end of word
	while (endChar < line.length && isWordChar(line[endChar])) {
		endChar++
	}

	return Pair(
		TextOffset(position.line, startChar),
		TextOffset(position.line, endChar)
	)
}

private fun isWordChar(char: Char): Boolean {
	return char.isLetterOrDigit() || char == '_'
}

suspend fun PointerInputScope.detectTapsImperatively(
	onTap: (Offset) -> Unit,
	onDoubleTap: (Offset) -> Unit,
	onTripleTap: (Offset) -> Unit,
) {
	awaitPointerEventScope {
		var lastTapTime = 0L
		var secondLastTapTime = 0L
		var lastTapPosition: Offset? = null
		var secondLastTapPosition: Offset? = null

		while (true) {
			// Wait for tap down
			val down = awaitFirstDown()
			val downTime = System.currentTimeMillis()
			val downPosition = down.position

			// Handle single tap immediately
			onTap(downPosition)

			// Wait for tap up
			do {
				val event = awaitPointerEvent()
			} while (event.changes.any { it.pressed })

			// Check for triple tap first
			val isTripleTap = lastTapPosition?.let { lastPos ->
				secondLastTapPosition?.let { secondLastPos ->
					val firstToSecondTimeDiff = lastTapTime - secondLastTapTime
					val secondToThirdTimeDiff = downTime - lastTapTime
					val firstToSecondPosDiff = (lastPos - secondLastPos).getDistance()
					val secondToThirdPosDiff = (downPosition - lastPos).getDistance()

					firstToSecondTimeDiff < 300L &&
							secondToThirdTimeDiff < 300L &&
							firstToSecondPosDiff < 20f &&
							secondToThirdPosDiff < 20f
				}
			} ?: false

			// Check for double tap
			val isDoubleTap = if (!isTripleTap) {
				lastTapPosition?.let { lastPos ->
					val timeDiff = downTime - lastTapTime
					val posDiff = (downPosition - lastPos).getDistance()
					timeDiff < 300L && posDiff < 20f
				} ?: false
			} else false

			when {
				isTripleTap -> {
					onTripleTap(downPosition)
					// Reset all tap tracking
					lastTapTime = 0L
					secondLastTapTime = 0L
					lastTapPosition = null
					secondLastTapPosition = null
				}

				isDoubleTap -> {
					onDoubleTap(downPosition)
					// Save second last tap info before resetting
					secondLastTapTime = lastTapTime
					secondLastTapPosition = lastTapPosition
					// Update last tap info
					lastTapTime = downTime
					lastTapPosition = downPosition
				}

				else -> {
					// Update tap tracking
					secondLastTapTime = lastTapTime
					secondLastTapPosition = lastTapPosition
					lastTapTime = downTime
					lastTapPosition = downPosition
				}
			}
		}
	}
}
