package com.darkrockstudios.texteditor.scrollbar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.darkrockstudios.texteditor.state.TextEditorScrollState

@Composable
actual fun TextEditorScrollbar(
	modifier: Modifier,
	scrollState: TextEditorScrollState,
	content: @Composable (modifier: Modifier) -> Unit
) {
	Box(
		modifier = modifier
	) {
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

			val onSurface = MaterialTheme.colorScheme.onSurface
			val trackColor = remember(onSurface) {
				onSurface.copy(alpha = 0.12f)
			}
			val thumbColor = remember(onSurface) {
				onSurface.copy(alpha = 0.38f)
			}

			Canvas(
				modifier = Modifier
					.align(Alignment.CenterEnd)
					.fillMaxHeight()
					.padding(2.dp)
					.width(4.dp)
			) {
				// Draw track background
				drawRoundRect(
					color = trackColor,
					cornerRadius = CornerRadius(2.dp.toPx()),
					size = Size(size.width, size.height)
				)

				// Draw thumb
				drawRoundRect(
					color = thumbColor,
					cornerRadius = CornerRadius(2.dp.toPx()),
					topLeft = Offset(0f, thumbOffset * size.height),
					size = Size(size.width, thumbHeight * size.height)
				)
			}
		}
	}
}