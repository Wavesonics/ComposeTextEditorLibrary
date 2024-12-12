package com.darkrockstudios.texteditor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.fastForEach
import com.darkrockstudios.texteditor.cursor.drawCursor
import com.darkrockstudios.texteditor.state.TextEditorState
import com.darkrockstudios.texteditor.state.rememberTextEditorState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TextEditor(
	state: TextEditorState = rememberTextEditorState(),
	modifier: Modifier = Modifier
) {
	val focusRequester = remember { FocusRequester() }
	val interactionSource = remember { MutableInteractionSource() }
	val scope = rememberCoroutineScope()

	LaunchedEffect(Unit) {
		focusRequester.requestFocus()
	}

	LaunchedEffect(state.isFocused) {
		scope.launch {
			while (state.isFocused) {
				state.toggleCursor()
				delay(750)
			}
		}
	}

	BoxWithConstraints(
		modifier = modifier
			.border(width = 2.dp, color = if (state.isFocused) Color.Green else Color.Blue)
			.padding(8.dp)
			.focusRequester(focusRequester)
			.onFocusChanged { focusState ->
				state.updateFocus(focusState.isFocused)
			}
			.focusable(enabled = true, interactionSource = interactionSource)
			.textEditorKeyboardInputHandler(state)
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
					.textEditorPointerInputHandling(state)
			) {
				var lastLine = -1
				state.lineOffsets.fastForEach { virtualLine ->
					if (lastLine != virtualLine.line) {
						val line = state.textLines[virtualLine.line]
						try {
							drawText(
								state.textMeasurer,
								line,
								topLeft = virtualLine.offset
							)
						} catch (e: IllegalArgumentException) {
							// TODO obviously have to fix this at some point.
							// but drawText is throwing an exception when you resize the view
							// If you catch it then we just move on happily
						}

						lastLine = virtualLine.line
					}
				}

				drawSelection(state.textMeasurer, state)

				if (state.isFocused && state.isCursorVisible) {
					drawCursor(state.textMeasurer, state)
				}
			}
		}
	}
}