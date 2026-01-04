package com.darkrockstudios.texteditor.cursor

import androidx.compose.ui.geometry.Offset

data class CursorMetrics(
	val position: Offset,
	val height: Float,
	/** Top of the line in view coordinates */
	val lineTop: Float = position.y,
	/** Text baseline in view coordinates */
	val lineBaseline: Float = position.y + height * 0.8f,
	/** Bottom of the line in view coordinates */
	val lineBottom: Float = position.y + height
)