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
		.pointerInput(Unit) {
			detectDragGestures(
				onDragStart = { offset ->
					println("Starting drag handle handling")
					val handle = findHandleAtPosition(offset, state)
					if (handle != null) {
						println("Found drag handle!")
						state.selector.setDraggingHandle(handle.isStart)
						println("onDrag: start ${state.selector.draggingStartHandle} end ${state.selector.draggingEndHandle}")
					}
				},
				onDrag = { change, _ ->
					println("onDrag: start ${state.selector.draggingStartHandle} end ${state.selector.draggingEndHandle}")
					if (state.selector.isDraggingHandle()) {
						println("Dragging handle!")
						val newPosition = state.getOffsetAtPosition(change.position)
						val selection = state.selector.selection
						if (selection != null) {
							if (state.selector.isDraggingStartHandle()) {
								state.selector.updateSelection(newPosition, selection.end)
							} else {
								state.selector.updateSelection(selection.start, newPosition)
							}
						}
					}
				},
				onDragEnd = {
					state.selector.clearDraggingHandle()
				}
			)
		}
		.pointerInput(Unit) {
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
		.pointerInput(Unit) {
			detectTapsImperatively(
				onTap = { offset: Offset ->
					val position = state.getOffsetAtPosition(offset)
					state.cursor.updatePosition(position)
					state.selector.clearSelection()
				},
				onDoubleTap = { offset: Offset ->
					val position = state.getOffsetAtPosition(offset)
					state.selector.startSelection(position, isTouch = true)
					state.selector.selectWordAt(position)
				},
				onTripleTap = { offset: Offset ->
					val position = state.getOffsetAtPosition(offset)
					state.selector.startSelection(position, isTouch = true)
					state.selector.selectLineAt(position)
				}
			)
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

	return if (clickedSpan != null && onSpanClick != null) {
		onSpanClick.invoke(clickedSpan, clickType, offset)
	} else {
		if (clickType == SpanClickType.PRIMARY_CLICK || clickType == SpanClickType.TAP) {
			state.cursor.updatePosition(position)
			state.selector.clearSelection()
		}
		true
	}
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

			onTap(downPosition)

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
					onTripleTap(downPosition)
					lastTapTime = 0L
					secondLastTapTime = 0L
					lastTapPosition = null
					secondLastTapPosition = null
				}

				isDoubleTap -> {
					onDoubleTap(downPosition)
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
