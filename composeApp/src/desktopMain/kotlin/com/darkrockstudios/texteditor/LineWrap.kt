package com.darkrockstudios.texteditor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextLayoutResult

data class LineWrap(
	val line: Int,
	// The character index of the first character on this line
	val wrapStartsAtIndex: Int,
	val offset: Offset,
	val textLayoutResult: TextLayoutResult,
)