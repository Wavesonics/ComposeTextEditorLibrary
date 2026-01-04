package com.darkrockstudios.texteditor.scrollbar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.darkrockstudios.texteditor.state.TextEditorScrollState

/**
 * iOS implementation of TextEditorScrollbar.
 * Provides a simple visual scrollbar indicator similar to native iOS scroll views.
 * The scrollbar is display-only (non-interactive) following iOS conventions
 * where users scroll by touching the content area, not the scrollbar.
 */
@Composable
actual fun TextEditorScrollbar(
	modifier: Modifier,
	scrollState: TextEditorScrollState,
	content: @Composable (modifier: Modifier) -> Unit
) {
	Box(modifier = modifier) {
		content(Modifier)

		// Only show scrollbar if content is scrollable
		val showScrollbar by remember {
			derivedStateOf { scrollState.maxValue > 0 }
		}

		if (showScrollbar) {
			val thumbHeight by remember {
				derivedStateOf {
					val viewportRatio = scrollState.maxValue.toFloat() / (scrollState.maxValue * 2)
					0.15f.coerceAtLeast(viewportRatio)
				}
			}

			val thumbOffset by remember {
				derivedStateOf {
					val maxOffset = 1f - thumbHeight
					(scrollState.value.toFloat() / scrollState.maxValue.toFloat() * maxOffset)
				}
			}

			// iOS-style scrollbar: thin, subtle gray indicator (no track background)
			val thumbColor = remember {
				Color.Gray.copy(alpha = 0.5f)
			}

			Canvas(
				modifier = Modifier
					.align(Alignment.CenterEnd)
					.fillMaxHeight()
					.padding(2.dp)
					.width(3.dp) // Thinner than Android, matching iOS style
			) {
				// Draw thumb only (no track background, matching iOS style)
				drawRoundRect(
					color = thumbColor,
					cornerRadius = CornerRadius(1.5.dp.toPx()),
					topLeft = Offset(0f, thumbOffset * size.height),
					size = Size(size.width, thumbHeight * size.height)
				)
			}
		}
	}
}
