package com.darkrockstudios.texteditor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
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
	val textMeasurer = rememberTextMeasurer()
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

	LaunchedEffect(Unit) {
		val text = "test ssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssss\nxxxxxxxxxxxxxxxxxx\nHello cat\n".repeat(5)
		state.setInitialText(text)
	}

	Box(
		modifier = modifier
			.border(width = 2.dp, color = if (state.isFocused) Color.Green else Color.Blue)
			.padding(8.dp)
			.focusRequester(focusRequester)
			.onFocusChanged { focusState ->
				state.updateFocus(focusState.isFocused)
			}
			.focusable(enabled = true, interactionSource = interactionSource)
			.textEditorKeyboardInputHandler(state)
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
					.textEditorPointerInputHandling(state, state.scrollState, textMeasurer)
			) {
				// Calculate content and draw text
				val offsets = mutableListOf<LineWrap>()
				var currentY = 0f

				state.textLines.forEachIndexed { lineIndex, line ->
					val textLayoutResult = textMeasurer.measure(
						line,
						constraints = Constraints(
							maxWidth = maxOf(1, size.width.toInt()),
							minHeight = 0,
							maxHeight = Constraints.Infinity
						)
					)

					// Adjust drawing position for scroll offset
					val adjustedY = currentY - state.scrollState.value

					// Only draw if the line is visible in the viewport
					if (adjustedY + textLayoutResult.size.height >= 0 && adjustedY <= size.height) {
						drawText(
							textMeasurer,
							line,
							topLeft = Offset(0f, adjustedY)
						)
					}

					for (virtualLineIndex in 0 until textLayoutResult.multiParagraph.lineCount) {
						val lineTop = currentY
						val lineOffset = Offset(0f, lineTop)

						val lineWrapsAt = if (virtualLineIndex == 0) {
							0
						} else {
							textLayoutResult.getLineEnd(virtualLineIndex - 1, visibleEnd = true) + 1
						}

						offsets.add(
							LineWrap(
								line = lineIndex,
								wrapStartsAtIndex = lineWrapsAt,
								offset = lineOffset,
							)
						)
						currentY += textLayoutResult.multiParagraph.getLineHeight(virtualLineIndex)
					}
				}

				// Update total content height
				state.updateContentHeight(currentY.toInt())

				state.updateLineOffsets(offsets)

				if (state.isFocused && state.isCursorVisible) {
					drawCursor(textMeasurer, state)
				}
			}
		}
	}
}