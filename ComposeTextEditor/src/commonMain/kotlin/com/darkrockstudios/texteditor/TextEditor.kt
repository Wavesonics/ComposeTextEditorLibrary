package com.darkrockstudios.texteditor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.darkrockstudios.texteditor.cursor.DrawCursor
import com.darkrockstudios.texteditor.richstyle.RichSpan
import com.darkrockstudios.texteditor.scrollbar.TextEditorScrollbar
import com.darkrockstudios.texteditor.state.SpanClickType
import com.darkrockstudios.texteditor.state.TextEditorState
import com.darkrockstudios.texteditor.state.rememberTextEditorState
import kotlinx.coroutines.delay

private const val CURSOR_BLINK_SPEED_MS = 500L

@Composable
fun TextEditor(
	state: TextEditorState = rememberTextEditorState(),
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
	style: TextEditorStyle = rememberTextEditorStyle(),
	onRichSpanClick: RichSpanClickListener? = null,
) {
	val focusRequester = remember { FocusRequester() }
	val interactionSource = remember { MutableInteractionSource() }
	val clipboardManager = LocalClipboardManager.current

	LaunchedEffect(Unit) {
		if (enabled) {
			focusRequester.requestFocus()
		}
	}

	LaunchedEffect(state.isFocused, state.cursorPosition, enabled) {
		if (!enabled) {
			state.updateFocus(false)
			return@LaunchedEffect
		}

		state.cursor.setVisible()
		while (state.isFocused) {
			delay(CURSOR_BLINK_SPEED_MS)
			state.cursor.toggleVisibility()
		}
	}

	Surface(modifier = modifier.focusBorder(state.isFocused && enabled, style)) {
		TextEditorScrollbar(
			modifier = Modifier.padding(start = 8.dp),
			scrollState = state.scrollState,
		) { editorModifier ->
			Box(
				modifier = editorModifier
					.then(
						if (enabled) {
							Modifier
								.focusRequester(focusRequester)
								.onFocusChanged { focusState ->
									state.updateFocus(focusState.isFocused)
								}
								.requestFocusOnPress(focusRequester)
								.focusable(enabled = true, interactionSource = interactionSource)
								.textEditorKeyboardInputHandler(state, clipboardManager)
						} else {
							Modifier
						}
					)
					.onSizeChanged { size ->
						state.onViewportSizeChange(
							size.toSize()
						)
					}
					.fillMaxSize()
					.scrollable(
						orientation = Orientation.Vertical,
						reverseDirection = false,
						state = state.scrollState,
					)
			) {
				Canvas(
					modifier = Modifier
						.size(
							width = state.viewportSize.width.dp,
							height = state.viewportSize.height.dp
						)
						.graphicsLayer {
							clip = true
						}
						.then(
							if (enabled) {
								Modifier.textEditorPointerInputHandling(state, onRichSpanClick)
							} else {
								Modifier
							}
						)
				) {
					if (state.isEmpty() && style.placeholderText.isNotEmpty()) {
						DrawPlaceholderText(state, style)
					}

					try {
						DrawEditorText(state, style)
					} catch (e: IllegalArgumentException) {
						// Handle resize exception gracefully
					}

					DrawSelection(state, style.selectionColor)

					DrawSelectionHandles(state)

					if (enabled && state.isFocused && state.cursor.isVisible) {
						DrawCursor(state, style.cursorColor)
					}
				}
			}
		}
	}
}

private fun Modifier.focusBorder(isFocused: Boolean, style: TextEditorStyle): Modifier {
	return this.border(
		width = 1.dp,
		color = if (isFocused) style.focusedBorderColor else style.unfocusedBorderColor
	)
}

private fun Modifier.requestFocusOnPress(focusRequester: FocusRequester) = pointerInput(Unit) {
	awaitEachGesture {
		awaitFirstDown(requireUnconsumed = false)
		focusRequester.requestFocus()
	}
}

typealias RichSpanClickListener = ((RichSpan, SpanClickType, Offset) -> Boolean)