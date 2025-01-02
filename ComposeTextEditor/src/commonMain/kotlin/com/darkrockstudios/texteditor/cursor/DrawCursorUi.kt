package com.darkrockstudios.texteditor.cursor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.darkrockstudios.texteditor.state.TextEditorState

internal fun DrawScope.DrawCursor(
	state: TextEditorState,
	cursorColor: Color,
) {
	val metrics = state.calculateCursorPosition()

	// Only draw cursor if it's in the visible area
	if (metrics.position.y + metrics.height >= 0 && metrics.position.y <= size.height) {
		drawLine(
			color = cursorColor,
			start = metrics.position,
			end = metrics.position.copy(y = metrics.position.y + metrics.height),
			strokeWidth = 2f
		)
	}
}