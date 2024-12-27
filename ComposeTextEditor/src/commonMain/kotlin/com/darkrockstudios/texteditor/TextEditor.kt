package com.darkrockstudios.texteditor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.fastForEach
import com.darkrockstudios.texteditor.cursor.drawCursor
import com.darkrockstudios.texteditor.richstyle.RichSpan
import com.darkrockstudios.texteditor.scrollbar.TextEditorScrollbar
import com.darkrockstudios.texteditor.state.SpanClickType
import com.darkrockstudios.texteditor.state.TextEditorState
import com.darkrockstudios.texteditor.state.rememberTextEditorState
import kotlinx.coroutines.delay

@Composable
fun TextEditor(
	state: TextEditorState = rememberTextEditorState(),
	modifier: Modifier = Modifier,
	onSpanClick: ((RichSpan, SpanClickType) -> Unit)? = null,
) {
	val focusRequester = remember { FocusRequester() }
	val interactionSource = remember { MutableInteractionSource() }
	val clipboardManager = LocalClipboardManager.current

	LaunchedEffect(Unit) {
		focusRequester.requestFocus()
	}

	LaunchedEffect(state.isFocused, state.cursorPosition) {
		state.setCursorVisible()
		while (state.isFocused) {
			delay(500)
			state.toggleCursor()
		}
	}

	TextEditorScrollbar(
		modifier = modifier,
		scrollState = state.scrollState,
	) {
		BoxWithConstraints(
			modifier = Modifier
				.focusBorder(state.isFocused)
				.padding(8.dp)
				.focusRequester(focusRequester)
				.onFocusChanged { focusState ->
					state.updateFocus(focusState.isFocused)
				}
				.requestFocusOnPress(focusRequester)
				.focusable(enabled = true, interactionSource = interactionSource)
				.textEditorKeyboardInputHandler(state, clipboardManager)
				.onSizeChanged { size ->
					state.onViewportSizeChange(size.toSize())
				}
				.verticalScroll(state.scrollState)
		) {

			// We need a fixed height Box to enable scrolling
			Box(
				modifier = Modifier
					.fillMaxWidth()
					.height(state.totalContentHeight.dp)
			) {
				Canvas(
					modifier = Modifier
						.fillMaxWidth()
						.height(state.totalContentHeight.dp)
						.textEditorPointerInputHandling(state, onSpanClick)
				) {
					try {
						var lastLine = -1
						state.lineOffsets.fastForEach { virtualLine ->
							if (lastLine != virtualLine.line && state.textLines.size > virtualLine.line) {
								val line = state.textLines[virtualLine.line]

								drawText(
									state.textMeasurer,
									line,
									topLeft = virtualLine.offset
								)

								lastLine = virtualLine.line
							}

							drawRichSpans(virtualLine, state)
						}

						drawSelection(state)

						if (state.isFocused && state.isCursorVisible) {
							drawCursor(state)
						}
					} catch (e: IllegalArgumentException) {
						// TODO obviously have to fix this at some point.
						// but drawText is throwing an exception when you resize the view
						// If you catch it then we just move on happily
					}
				}
			}
		}
	}
}

fun Modifier.focusBorder(isFocused: Boolean): Modifier {
	return this.border(
		width = 1.dp,
		color = if (isFocused) Color(0xFFcfd6dc) else Color(0xFFDDDDDD)
	)
}

private fun Modifier.requestFocusOnPress(focusRequester: FocusRequester) = pointerInput(Unit) {
	awaitEachGesture {
		// Wait for at least one pointer to press down
		awaitFirstDown(requireUnconsumed = false)
		focusRequester.requestFocus()
	}
}
