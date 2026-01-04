package com.darkrockstudios.texteditor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.darkrockstudios.texteditor.contextmenu.ContextMenuActions
import com.darkrockstudios.texteditor.contextmenu.ContextMenuStrings
import com.darkrockstudios.texteditor.contextmenu.TextEditorContextMenuProvider
import com.darkrockstudios.texteditor.contextmenu.TextEditorContextMenuState
import com.darkrockstudios.texteditor.cursor.DrawCursor
import com.darkrockstudios.texteditor.input.CaptureViewForIme
import com.darkrockstudios.texteditor.input.TextEditorInputModifierElement
import com.darkrockstudios.texteditor.richstyle.RichSpan
import com.darkrockstudios.texteditor.scrollbar.TextEditorScrollbar
import com.darkrockstudios.texteditor.state.SpanClickType
import com.darkrockstudios.texteditor.state.TextEditorState
import com.darkrockstudios.texteditor.state.rememberTextEditorState
import kotlinx.coroutines.delay

private const val CURSOR_BLINK_SPEED_MS = 500L

@Composable
fun BasicTextEditor(
	state: TextEditorState = rememberTextEditorState(),
	modifier: Modifier = Modifier,
	contentPadding: PaddingValues = PaddingValues(0.dp),
	enabled: Boolean = true,
	autoFocus: Boolean = false,
	style: TextEditorStyle = rememberTextEditorStyle(),
	contextMenuStrings: ContextMenuStrings = ContextMenuStrings.Default,
	onRichSpanClick: RichSpanClickListener? = null,
	decorateLine: LineDecorator? = null,
) {
	// Capture platform view for IME cursor synchronization (Android only)
	CaptureViewForIme(state)

	val focusRequester = remember { FocusRequester() }
	val interactionSource = remember { MutableInteractionSource() }
	val clipboard = LocalClipboard.current

	val inputModifierElement = remember(state, clipboard, enabled) {
		TextEditorInputModifierElement(state, clipboard, enabled)
	}

	val contextMenuState = remember { TextEditorContextMenuState() }
	val contextMenuActions = remember(state, clipboard) {
		ContextMenuActions(state, clipboard, state.scope)
	}

	LaunchedEffect(Unit) {
		if (enabled && autoFocus) {
			focusRequester.requestFocus()
		}
	}

	LaunchedEffect(state.isFocused, state.cursorPosition, enabled) {
		if (enabled && state.isFocused) {
			state.cursor.setVisible()
			while (state.isFocused) {
				delay(CURSOR_BLINK_SPEED_MS)
				state.cursor.toggleVisibility()
			}
		}
	}

	TextEditorContextMenuProvider(
		menuState = contextMenuState,
		actions = contextMenuActions,
		strings = contextMenuStrings,
		enabled = enabled,
	) {
		TextEditorScrollbar(
			modifier = modifier,
			scrollState = state.scrollState,
		) { editorModifier ->
			Box(
				modifier = editorModifier
					.padding(contentPadding)
					.focusRequester(focusRequester)
					.requestFocusOnPress(focusRequester)
					.then(inputModifierElement)
					.focusable(enabled = true, interactionSource = interactionSource)
					.background(style.backgroundColor)
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
						.textEditorPointerInputHandling(
							state = state,
							onSpanClick = onRichSpanClick,
							onContextMenuRequest = { offset ->
								contextMenuState.showMenu(offset)
							}
						)
						.size(
							width = state.viewportSize.width.dp,
							height = state.viewportSize.height.dp
						)
						.graphicsLayer {
							clip = false
						}
				) {
					if (state.isEmpty() && style.placeholderText.isNotEmpty()) {
						DrawPlaceholderText(state, style)
					}

					try {
						DrawEditorText(state, style, decorateLine)
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

private fun Modifier.requestFocusOnPress(focusRequester: FocusRequester) = pointerInput(Unit) {
	awaitEachGesture {
		awaitFirstDown(requireUnconsumed = false)
		waitForUpOrCancellation()?.let {
			focusRequester.requestFocus()
		}
	}
}

typealias RichSpanClickListener = ((RichSpan, SpanClickType, Offset) -> Boolean)
