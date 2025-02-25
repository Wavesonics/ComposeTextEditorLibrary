package com.darkrockstudios.texteditor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.darkrockstudios.texteditor.cursor.CursorMetrics
import com.darkrockstudios.texteditor.state.TextEditorState

internal fun DrawScope.DrawSelectionHandles(
	state: TextEditorState,
	handleColor: Color = Color(0xFF2196F3),
) {
	if (!state.selector.isTouchSelection || state.selector.selection == null) {
		return
	}

	val selection = state.selector.selection ?: return

	val startOffset = state.getPositionForOffset(selection.start)
	drawHandle(startOffset, handleColor)

	val endOffset = state.getPositionForOffset(selection.end)
	drawHandle(endOffset, handleColor)
}

private fun DrawScope.drawHandle(
	positionMetrics: CursorMetrics,
	color: Color
) {
	val (position, height) = positionMetrics

	val lineWidth = 4f

	val handleY = position.y + height + SELECTION_HANDLE_RADIUS

	drawCircle(
		color = color,
		radius = SELECTION_HANDLE_RADIUS,
		center = position.copy(y = handleY)
	)

	drawLine(
		color = color,
		start = position,
		end = Offset(position.x, position.y + height),
		strokeWidth = lineWidth
	)
}

internal const val SELECTION_HANDLE_DIAMETER = 45f
internal const val SELECTION_HANDLE_RADIUS = SELECTION_HANDLE_DIAMETER / 2