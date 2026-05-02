package com.darkrockstudios.texteditor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.darkrockstudios.texteditor.state.TextEditorState

/**
 * Read-only view of a [TextEditorState]'s content using the editor's full rendering pipeline
 * (text, span styles, rich spans). No surface, scrollbar, focus border, IME, gestures, or
 * cursor — height wraps to content. Useful for showing rich content in detail screens or list
 * cards without the editor chrome.
 *
 * The caller owns the [TextEditorState] and is responsible for seeding it with content
 * (e.g. via `rememberTextEditorState(initialText)` or `withMarkdown(...)`).
 */
@Composable
fun RichTextView(
	state: TextEditorState,
	modifier: Modifier = Modifier,
	contentPadding: PaddingValues = PaddingValues(0.dp),
	style: TextEditorStyle = rememberTextEditorStyle(),
) {
	LaunchedEffect(style.textStyle) {
		state.textStyle = style.textStyle
	}

	val density = LocalDensity.current
	val layoutDirection = LocalLayoutDirection.current

	BoxWithConstraints(modifier = modifier) {
		val totalWidthPx = with(density) { maxWidth.toPx() }
		val startPx = with(density) { contentPadding.calculateStartPadding(layoutDirection).toPx() }
		val endPx = with(density) { contentPadding.calculateEndPadding(layoutDirection).toPx() }
		val contentWidthPx = (totalWidthPx - startPx - endPx).coerceAtLeast(0f)

		// Drive line layout from the available content width. Height is set arbitrarily large
		// — RichTextView never scrolls, so the viewport height only affects DrawEditorText's
		// visible-range culling, which we want to be a no-op.
		LaunchedEffect(contentWidthPx) {
			if (contentWidthPx > 0f) {
				state.onViewportSizeChange(Size(contentWidthPx, Float.MAX_VALUE / 2))
			}
		}

		val contentHeightPx = state.lineOffsets.lastOrNull()?.let { last ->
			last.offset.y + last.textLayoutResult.size.height
		} ?: 0f

		Box(modifier = Modifier.padding(contentPadding)) {
			Canvas(
				modifier = Modifier
					.fillMaxWidth()
					.height(with(density) { contentHeightPx.toDp() })
					.graphicsLayer { clip = false }
			) {
				try {
					DrawEditorText(state, style, decorateLine = null)
				} catch (_: IllegalArgumentException) {
					// Mid-resize layout race; the next frame will recover, mirrors BasicTextEditor.
				}
			}
		}
	}
}
