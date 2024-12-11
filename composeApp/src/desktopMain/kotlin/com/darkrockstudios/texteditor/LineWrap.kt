package com.darkrockstudios.texteditor

import androidx.compose.ui.geometry.Offset

data class LineWrap(
	val line: Int,
	// The character index of the first character on this line
	val wrapStartsAtIndex: Int,
	val offset: Offset,
)