package com.darkrockstudios.texteditor.scrollbar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.darkrockstudios.texteditor.state.TextEditorScrollState
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
actual fun TextEditorScrollbar(
	modifier: Modifier,
	scrollState: TextEditorScrollState,
	content: @Composable (modifier: Modifier) -> Unit
) {
	Row(modifier = modifier) {
		val style = remember { ScrollbarStyle() }
		content(Modifier.weight(1f))

		InternalVerticalScrollbar(
			modifier = Modifier.fillMaxHeight(),
			state = scrollState,
			style = style,
		)
	}
}

@Composable
private fun InternalVerticalScrollbar(
	modifier: Modifier = Modifier,
	state: TextEditorScrollState,
	style: ScrollbarStyle = ScrollbarStyle()
) {
	val scope = rememberCoroutineScope()
	var isHovered by remember { mutableStateOf(false) }

	var componentSize by remember { mutableStateOf(Size(0f, 0f)) }
	var isDragging by remember { mutableStateOf(false) }
	var dragStartY by remember { mutableStateOf(0f) }
	var dragStartScrollPosition by remember { mutableStateOf(0) }

	// Calculate thumb size
	val thumbHeight =
		(componentSize.height * componentSize.height / (state.maxValue + componentSize.height))
			.coerceAtLeast(style.minThumbSize.value)

	val dragState = rememberDraggableState { delta ->
		if (isDragging) {
			// Calculate scroll in terms of the content, not the thumb position
			val scrollRatio = state.maxValue.toFloat() / (componentSize.height - thumbHeight)
			val scrollDelta = (delta * scrollRatio).roundToInt()
			scope.launch {
				state.scrollTo((state.value + scrollDelta).coerceIn(0, state.maxValue))
			}
		}
	}

	Box(
		modifier = modifier
			.width(style.width)
			.fillMaxHeight()
			.pointerInput(Unit) {
				awaitPointerEventScope {
					while (true) {
						val event = awaitPointerEvent()
						when (event.type) {
							PointerEventType.Enter -> isHovered = true
							PointerEventType.Exit -> isHovered = false
							else -> { /* Ignore other events */
							}
						}
					}
				}
			}
			.draggable(
				orientation = Orientation.Vertical,
				state = dragState,
				onDragStarted = { offset ->
					val scrollableHeight = componentSize.height - thumbHeight
					val thumbPosition = (state.value.toFloat() / state.maxValue) * scrollableHeight
					// Only start drag if we click on the thumb
					if (offset.y in thumbPosition..(thumbPosition + thumbHeight)) {
						isDragging = true
						dragStartY = offset.y
						dragStartScrollPosition = state.value
					}
				},
				onDragStopped = {
					isDragging = false
				}
			)
			.pointerInput(Unit) {
				awaitPointerEventScope {
					while (true) {
						val event = awaitPointerEvent()
						when (event.type) {
							PointerEventType.Press -> {
								// Handle clicks on track
								val position = event.changes.first().position
								val scrollableHeight = componentSize.height - thumbHeight
								val thumbPosition =
									(state.value.toFloat() / state.maxValue) * scrollableHeight

								if (position.y < thumbPosition) {
									// Click above thumb - scroll up by page
									scope.launch {
										state.animateScrollTo(
											(state.value - componentSize.height).coerceAtLeast(0f)
												.toInt()
										)
									}
								} else if (position.y > thumbPosition + thumbHeight) {
									// Click below thumb - scroll down by page
									scope.launch {
										state.animateScrollTo(
											(state.value + componentSize.height).coerceAtMost(state.maxValue.toFloat())
												.toInt()
										)
									}
								}
							}

							else -> { /* Ignore other events */
							}
						}
					}
				}
			}
	) {
		Canvas(
			modifier = Modifier
				.fillMaxSize()
				.onSizeChanged { size ->
					componentSize = Size(size.width.toFloat(), size.height.toFloat())
				}
		) {
			val cornerRadius = style.cornerRadius.toPx()
			val padding = style.padding.toPx()

			// Draw track with rounded corners
			drawRoundRect(
				color = if (isHovered) style.trackColorHovered else style.trackColor,
				size = Size(size.width - padding * 2, size.height - padding * 2),
				topLeft = Offset(padding, padding),
				cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius)
			)

			// Calculate and draw thumb with rounded corners
			val scrollableHeight = size.height - padding * 2 - thumbHeight
			val thumbY = (state.value.toFloat() / state.maxValue) * scrollableHeight + padding

			drawRoundRect(
				color = if (isHovered) style.thumbColorHovered else style.thumbColor,
				topLeft = Offset(padding, thumbY),
				size = Size(size.width - padding * 2, thumbHeight),
				cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius)
			)
		}
	}
}

private data class ScrollbarStyle(
	val width: Dp = 16.dp,
	val minThumbSize: Dp = 48.dp,
	val thumbColor: Color = Color.Gray.copy(alpha = 0.6f),
	val thumbColorHovered: Color = Color.Gray.copy(alpha = 0.8f),
	val trackColor: Color = Color.Gray.copy(alpha = 0.2f),
	val trackColorHovered: Color = Color.Gray.copy(alpha = 0.3f),
	val cornerRadius: Dp = 4.dp,
	val padding: Dp = 2.dp,
	val margin: Dp = 2.dp,
)