package com.darkrockstudios.texteditor

import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

internal fun Modifier.focusBorder(isFocused: Boolean, style: TextEditorStyle): Modifier {
	return this.border(
		width = 1.dp,
		color = if (isFocused) style.focusedBorderColor else style.unfocusedBorderColor
	)
}