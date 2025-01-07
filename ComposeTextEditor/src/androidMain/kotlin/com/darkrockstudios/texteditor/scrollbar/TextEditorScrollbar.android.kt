package com.darkrockstudios.texteditor.scrollbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
			val scrollbarHeight by remember {
				derivedStateOf {
					val scrollableHeight = scrollState.maxValue.toFloat()
					val viewportRatio = 1f / (1f + (scrollableHeight / scrollState.maxValue))
					viewportRatio.coerceIn(0.1f, 1f)
				}
			}

			val scrollbarPosition by remember {
				derivedStateOf {
					val scrollableHeight = scrollState.maxValue.toFloat()
					val position = scrollState.value.toFloat() / scrollableHeight
					position.coerceIn(0f, 1f)
				}
			}

			Box(
				modifier = Modifier
					.align(Alignment.CenterEnd)
					.fillMaxHeight()
					.padding(2.dp)
					.width(4.dp)
					.clip(RoundedCornerShape(2.dp))
					.background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
			) {
				Box(
					modifier = Modifier
						.align(Alignment.TopCenter)
						.fillMaxHeight(scrollbarHeight)
						.width(4.dp)
						.clip(RoundedCornerShape(2.dp))
						.background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
				)
			}
		}
	}
}