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

internal fun Modifier.textEditorPointerInputHandling(
	state: TextEditorState,
	onSpanClick: RichSpanClickListener? = null,
): Modifier {
	return this.pointerInput(Unit) {
		awaitEachGesture {
			var didHandlePress = false
			var longPressJob: Job? = null

			while (true) {
				val event = awaitPointerEvent()

				when (event.type) {
					PointerEventType.Press -> {
						val eventChange = event.changes.first()
						val position = eventChange.position

						when (eventChange.type) {
							PointerType.Touch -> {
								longPressJob = state.scope.launch {
									delay(500)
									val wordPosition = state.getOffsetAtPosition(position)
									state.selector.selectWordAt(wordPosition)
									didHandlePress = true
								}

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

							else -> {}
						}
					}

					PointerEventType.Release -> {
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
	}.pointerInput(Unit) {
		detectTapsImperatively(
			onTap = { offset: Offset ->
				val position = state.getOffsetAtPosition(offset)
				state.cursor.updatePosition(position)
				state.selector.clearSelection()
			},
			onDoubleTap = { offset: Offset ->
				val position = state.getOffsetAtPosition(offset)
				state.selector.selectWordAt(position)
			},
			onTripleTap = { offset: Offset ->
				val position = state.getOffsetAtPosition(offset)
				state.selector.selectLineAt(position)
			}
		)
	}.pointerInput(Unit) {
		var dragStartPosition: CharLineOffset? = null
		detectDragGestures(
			onDragStart = { offset ->
				dragStartPosition = state.getOffsetAtPosition(offset)
				dragStartPosition?.let { pos ->
					state.cursor.updatePosition(pos)
				}
			},
			onDragEnd = {
				dragStartPosition = null
			},
			onDrag = { change, _ ->
				val currentPosition = state.getOffsetAtPosition(change.position)
				dragStartPosition?.let { startPos ->
					state.selector.updateSelection(startPos, currentPosition)
				}
				state.cursor.updatePosition(currentPosition)
			}
		)
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

private fun handleSpanInteraction(
	state: TextEditorState,
	offset: Offset,
	clickType: SpanClickType,
	onSpanClick: RichSpanClickListener?
): Boolean {
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
