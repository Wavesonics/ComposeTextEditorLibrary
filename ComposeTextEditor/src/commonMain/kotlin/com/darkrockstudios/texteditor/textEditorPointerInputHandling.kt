package com.darkrockstudios.texteditor

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import com.darkrockstudios.texteditor.state.SpanClickType
import com.darkrockstudios.texteditor.state.TextEditorState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

data class SelectionHandle(
	val position: CharLineOffset,
	val isStart: Boolean,
	val bounds: Offset
)

internal fun Modifier.textEditorPointerInputHandling(
	state: TextEditorState,
	onSpanClick: RichSpanClickListener? = null,
): Modifier {
	return this
		.handleDragInput(state)
		.handleTextInteractions(state, onSpanClick)
		.detectMouseClicksImperatively(
			onClick = { offset: Offset ->
				val position = state.getOffsetAtPosition(offset)
				state.cursor.updatePosition(position)
				state.selector.clearSelection()
			},
			onDoubleClick = { offset: Offset ->
				val position = state.getOffsetAtPosition(offset)
				state.selector.startSelection(position, isTouch = false)
				state.selector.selectWordAt(position)
			},
			onTripleClick = { offset: Offset ->
				val position = state.getOffsetAtPosition(offset)
				state.selector.startSelection(position, isTouch = false)
				state.selector.selectLineAt(position)
			}
		)
}

private fun Modifier.handleDragInput(state: TextEditorState): Modifier {
	return pointerInput(Unit) {
		awaitEachGesture {
			val down = awaitFirstDown()
			val isTouch = down.type == PointerType.Touch
			val isMouse = down.type == PointerType.Mouse

			val initialPosition = down.position

			var mouseSelectionAnchor: CharLineOffset? = null

			if (isTouch) {
				val handle = findHandleAtPosition(initialPosition, state)
				if (handle != null) {
					state.selector.setDraggingHandle(handle.isStart)
				}
			} else if (isMouse) {
				mouseSelectionAnchor = state.getOffsetAtPosition(initialPosition)
				state.selector.startSelection(position = mouseSelectionAnchor, isTouch = false)
			}

			val pointerId = down.id
			var currentPosition: Offset

			// Continue reading pointer events until release
			while (true) {
				val event = awaitPointerEvent()
				val dragEvent = event.changes.firstOrNull { it.id == pointerId } ?: break

				if (!dragEvent.pressed) {
					state.selector.clearDraggingHandle()
					break
				}

				currentPosition = dragEvent.position

				if (state.selector.isDraggingHandle()) {
					val newPosition = state.getOffsetAtPosition(currentPosition)
					val selection = state.selector.selection
					if (selection != null) {
						if (state.selector.isDraggingStartHandle()) {
							state.selector.updateSelection(newPosition, selection.end)
						} else {
							state.selector.updateSelection(selection.start, newPosition)
						}
					}
					dragEvent.consume()
				} else {
					if (isMouse && mouseSelectionAnchor != null) {
						val currentOffset = state.getOffsetAtPosition(currentPosition)
						state.selector.updateSelection(mouseSelectionAnchor, currentOffset)
					}
				}
			}
		}
	}
}

private fun findHandleAtPosition(
	position: Offset,
	state: TextEditorState,
): SelectionHandle? {
	val selection = state.selector.selection ?: return null

	val startMetrics = state.getPositionForOffset(selection.start)
	val startHandleY = startMetrics.position.y + startMetrics.height + SELECTION_HANDLE_RADIUS
	val startHandlePos = startMetrics.position.copy(y = startHandleY)

	val endMetrics = state.getPositionForOffset(selection.end)
	val endHandleY = endMetrics.position.y + startMetrics.height + SELECTION_HANDLE_RADIUS
	val endHandlePos = endMetrics.position.copy(y = endHandleY)

	val handleHitArea = 64f

	return if ((position - startHandlePos).getDistance() < handleHitArea) {
		SelectionHandle(selection.start, true, startHandlePos)
	} else if ((position - endHandlePos).getDistance() < handleHitArea) {
		SelectionHandle(selection.end, false, endHandlePos)
	} else {
		null
	}
}

private fun handleSpanInteraction(
	state: TextEditorState,
	offset: Offset,
	clickType: SpanClickType,
	onSpanClick: RichSpanClickListener?
): Boolean {
	if (findHandleAtPosition(offset, state) != null) {
		return true
	}

	val position = state.getOffsetAtPosition(offset)
	val clickedSpan = state.findSpanAtPosition(position)

	if (clickType == SpanClickType.PRIMARY_CLICK || clickType == SpanClickType.TAP) {
		state.cursor.updatePosition(position)
		state.selector.clearSelection()
	}

	return if (clickedSpan != null && onSpanClick != null) {
		onSpanClick.invoke(clickedSpan, clickType, offset)
	} else {
		true
	}
}

private fun Modifier.handleTextInteractions(
	state: TextEditorState,
	onSpanClick: RichSpanClickListener?
): Modifier {
	return pointerInput(Unit) {
		awaitEachGesture {
			var didHandlePress = false
			var longPressJob: Job? = null
			var didLongPress = false

			while (true) {
				val event = awaitPointerEvent()
				val eventChange = event.changes.first()

				when (event.type) {
					PointerEventType.Press -> {
						val position = eventChange.position

						val handle = findHandleAtPosition(position, state)
						if (handle != null) {
							didHandlePress = true
							break
						}

						when (eventChange.type) {
							PointerType.Touch -> {
								didLongPress = false
								longPressJob = state.scope.launch {
									delay(500)
									val wordPosition = state.getOffsetAtPosition(position)
									state.selector.startSelection(wordPosition, isTouch = true)
									state.selector.selectWordAt(wordPosition)
									didLongPress = true
									didHandlePress = true
								}
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

							else -> {}
						}
					}

					PointerEventType.Release -> {
						if (eventChange.type == PointerType.Touch && !didLongPress) {
							val position = eventChange.position
							handleSpanInteraction(
								state,
								position,
								SpanClickType.TAP,
								onSpanClick
							)
						}

						longPressJob?.cancel()
						longPressJob = null

						if (didHandlePress) {
							break
						}
					}

					PointerEventType.Move -> {
						val movement = event.changes.first()
						if (movement.positionChanged()) {
							longPressJob?.cancel()
							longPressJob = null
						}
					}
				}
			}
		}
	}
}

private fun Modifier.detectMouseClicksImperatively(
	onClick: (Offset) -> Unit,
	onDoubleClick: (Offset) -> Unit,
	onTripleClick: (Offset) -> Unit,
): Modifier {
	return pointerInput(Unit) {
		awaitPointerEventScope {
			var lastTapTime = 0L
			var secondLastTapTime = 0L
			var lastTapPosition: Offset? = null
			var secondLastTapPosition: Offset? = null

			while (true) {
				val down = awaitFirstDown()

				// Skip if not a mouse event
				if (down.type != PointerType.Mouse) {
					// Consume events until release
					do {
						val event = awaitPointerEvent()
					} while (event.changes.any { it.pressed })
					continue
				}

				val downTime = Clock.System.now().toEpochMilliseconds()
				val downPosition = down.position

				onClick(downPosition)

				do {
					val event = awaitPointerEvent()
				} while (event.changes.any { it.pressed })

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

				val isDoubleTap = if (!isTripleTap) {
					lastTapPosition?.let { lastPos ->
						val timeDiff = downTime - lastTapTime
						val posDiff = (downPosition - lastPos).getDistance()
						timeDiff < 300L && posDiff < 20f
					} ?: false
				} else false

				when {
					isTripleTap -> {
						onTripleClick(downPosition)
						lastTapTime = 0L
						secondLastTapTime = 0L
						lastTapPosition = null
						secondLastTapPosition = null
					}

					isDoubleTap -> {
						onDoubleClick(downPosition)
						secondLastTapTime = lastTapTime
						secondLastTapPosition = lastTapPosition
						lastTapTime = downTime
						lastTapPosition = downPosition
					}

					else -> {
						secondLastTapTime = lastTapTime
						secondLastTapPosition = lastTapPosition
						lastTapTime = downTime
						lastTapPosition = downPosition
					}
				}
			}
		}
	}
}
