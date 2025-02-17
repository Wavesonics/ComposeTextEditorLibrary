package com.darkrockstudios.texteditor.state

import androidx.compose.ui.text.SpanStyle
import com.darkrockstudios.texteditor.CharLineOffset
import com.darkrockstudios.texteditor.TextEditorRange

data class CursorData(
	val position: CharLineOffset,
	val styles: Set<SpanStyle>,
	val selection: TextEditorRange?,
)
