package com.darkrockstudios.texteditor

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import com.darkrockstudios.texteditor.richstyle.RichSpan
import com.darkrockstudios.texteditor.state.SpanClickType
import com.darkrockstudios.texteditor.state.TextEditorState
import kotlinx.datetime.Clock

internal fun Modifier.textEditorPointerInputHandling(
	state: TextEditorState,
	onSpanClick: ((RichSpan, SpanClickType) -> Unit)? = null,
): Modifier {
	return this.pointerInput(Unit) {
		awaitEachGesture {
			var didHandlePress = false

			while (true) {
				val event = awaitPointerEvent()

				when (event.type) {
					PointerEventType.Press -> {
						val eventChange = event.changes.first()
						val position = eventChange.position

						when (eventChange.type) {
							PointerType.Touch -> {
								handleSpanInteraction(
									state,
									position,
									SpanClickType.TAP,
									onSpanClick
								)
								didHandlePress = true
							}

							PointerType.Mouse -> {
								if (event.buttons.isPrimaryPressed) {
									handleSpanInteraction(
										state,
										position,
										SpanClickType.PRIMARY_CLICK,
										onSpanClick
									)
									didHandlePress = true
								} else if (event.buttons.isSecondaryPressed) {
									handleSpanInteraction(
										state,
										position,
										SpanClickType.SECONDARY_CLICK,
										onSpanClick
									)
									didHandlePress = true
								}
							}

							else -> { /* Ignore other pointer types */
							}
						}
					}

					PointerEventType.Release -> {
						if (didHandlePress) {
							break
						}
					}
				}
			}
		}
	}.pointerInput(Unit) {
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
		var dragStartPosition: CharLineOffset? = null
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

private fun TextEditorState.selectLineAt(position: CharLineOffset) {
	val lineStart = CharLineOffset(position.line, 0)
	val lineEnd = CharLineOffset(position.line, textLines[position.line].length)
	updateCursorPosition(lineEnd)
	selector.updateSelection(lineStart, lineEnd)
}

private fun TextEditorState.selectWordAt(position: CharLineOffset) {
	val (wordStart, wordEnd) = findWordBoundary(position)
	updateCursorPosition(wordEnd)
	selector.updateSelection(wordStart, wordEnd)
}

private fun TextEditorState.findWordBoundary(position: CharLineOffset): Pair<CharLineOffset, CharLineOffset> {
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
		CharLineOffset(position.line, startChar),
		CharLineOffset(position.line, endChar)
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
			val downTime = Clock.System.now().toEpochMilliseconds()
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

private fun handleSpanInteraction(
	state: TextEditorState,
	offset: Offset,
	clickType: SpanClickType,
	onSpanClick: ((RichSpan, SpanClickType) -> Unit)?
) {
	val position = state.getOffsetAtPosition(offset)
	val clickedSpan = state.findSpanAtPosition(position)

	if (clickedSpan != null) {
		onSpanClick?.invoke(clickedSpan, clickType)
	} else {
		// Only update cursor on primary clicks or taps
		if (clickType == SpanClickType.PRIMARY_CLICK || clickType == SpanClickType.TAP) {
			state.updateCursorPosition(position)
			state.selector.clearSelection()
		}
	}
}