package com.darkrockstudios.texteditor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.darkrockstudios.texteditor.contextmenu.ContextMenuActions
import com.darkrockstudios.texteditor.contextmenu.ContextMenuStrings
import com.darkrockstudios.texteditor.contextmenu.TextEditorContextMenuProvider
import com.darkrockstudios.texteditor.contextmenu.TextEditorContextMenuState
import com.darkrockstudios.texteditor.cursor.DrawCursor
import com.darkrockstudios.texteditor.input.CaptureViewForIme
import com.darkrockstudios.texteditor.input.TextEditorInputModifierElement
import com.darkrockstudios.texteditor.richstyle.BlockSpanStyle
import com.darkrockstudios.texteditor.richstyle.RichSpan
import com.darkrockstudios.texteditor.scrollbar.TextEditorScrollbar
import com.darkrockstudios.texteditor.state.SpanClickType
import com.darkrockstudios.texteditor.state.TextEditorState
import com.darkrockstudios.texteditor.state.rememberTextEditorState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.time.Duration.Companion.milliseconds

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
	contextMenuState: TextEditorContextMenuState? = null,
	onRichSpanClick: RichSpanClickListener? = null,
	decorateLine: LineDecorator? = null,
) {
	// Capture platform view for IME cursor synchronization (Android only)
	CaptureViewForIme(state)

	val focusRequester = remember { FocusRequester() }
	val interactionSource = remember { MutableInteractionSource() }
	val clipboard = LocalClipboard.current
	val density = LocalDensity.current
	val layoutDirection = LocalLayoutDirection.current

	val inputModifierElement = remember(state, clipboard, enabled) {
		TextEditorInputModifierElement(state, clipboard, enabled)
	}

	val horizontalPadding = remember(contentPadding, layoutDirection) {
		PaddingValues(
			start = contentPadding.calculateStartPadding(layoutDirection),
			end = contentPadding.calculateEndPadding(layoutDirection),
		)
	}

	LaunchedEffect(contentPadding, density) {
		with(density) {
			state.scrollManager.topContentPaddingPx = contentPadding.calculateTopPadding().roundToPx()
			state.scrollManager.bottomContentPaddingPx = contentPadding.calculateBottomPadding().roundToPx()
		}
	}

	LaunchedEffect(density) {
		state.density = density
	}

	// Use provided context menu state or create internal one
	val internalContextMenuState = remember { TextEditorContextMenuState() }
	val effectiveContextMenuState = contextMenuState ?: internalContextMenuState

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
				delay(CURSOR_BLINK_SPEED_MS.milliseconds)
				state.cursor.toggleVisibility()
			}
		}
	}

	LaunchedEffect(style.textStyle) {
		state.textStyle = style.textStyle
	}

	LaunchedEffect(
		style.bulletColor,
		style.blockquoteBarColor,
		style.blockquoteBackgroundColor,
		style.orderedListMarkerColor,
		style.codeFenceBackgroundColor,
		style.codeFenceBorderColor,
	) {
		state.bulletColor = style.bulletColor
		state.blockquoteBarColor = style.blockquoteBarColor
		state.blockquoteBackgroundColor = style.blockquoteBackgroundColor
		state.orderedListMarkerColor = style.orderedListMarkerColor
		state.codeFenceBackgroundColor = style.codeFenceBackgroundColor
		state.codeFenceBorderColor = style.codeFenceBorderColor
	}

	// Re-run layout when an asynchronous block-state change (e.g. an image
	// finishing its load) changes a [BlockSpanStyle]'s reported height.
	// `updateBookKeeping` reads block heights but isn't itself snapshot-tracked,
	// so without this effect a freshly loaded image leaves stale Y offsets —
	// subsequent paragraphs render at the placeholder height position and
	// overlap the image until the user scrolls or resizes.
	LaunchedEffect(state) {
		snapshotFlow {
			val d = state.density ?: return@snapshotFlow emptyList<Float>()
			val viewportWidth = state.viewportSize.width
			state.richSpanManager.getAllRichSpans().mapNotNull { span ->
				(span.style as? BlockSpanStyle)?.blockHeight(d, viewportWidth)
			}
		}
			.distinctUntilChanged()
			.collect { state.updateBookKeeping() }
	}

	TextEditorContextMenuProvider(
		menuState = effectiveContextMenuState,
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
					.padding(horizontalPadding)
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
							onContextMenuRequest = { offset -> effectiveContextMenuState.showMenu(offset) }
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

internal fun Modifier.requestFocusOnPress(focusRequester: FocusRequester) = pointerInput(Unit) {
	awaitEachGesture {
		awaitFirstDown(requireUnconsumed = false)
		focusRequester.requestFocus()
	}
}

typealias RichSpanClickListener = ((RichSpan, SpanClickType, Offset) -> Boolean)
