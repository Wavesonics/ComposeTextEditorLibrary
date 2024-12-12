package com.darkrockstudios.texteditor.cursor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import com.darkrockstudios.texteditor.state.TextEditorState

internal fun DrawScope.drawCursor(
	textMeasurer: TextMeasurer,
	state: TextEditorState,
) {
	val metrics = state.calculateCursorPosition(textMeasurer, size.width)

	// Only draw cursor if it's in the visible area
	if (metrics.position.y + metrics.height >= 0 && metrics.position.y <= size.height) {
		drawLine(
			color = Color.Black,
			start = metrics.position,
			end = metrics.position.copy(y = metrics.position.y + metrics.height),
			strokeWidth = 2f
		)
	}
}