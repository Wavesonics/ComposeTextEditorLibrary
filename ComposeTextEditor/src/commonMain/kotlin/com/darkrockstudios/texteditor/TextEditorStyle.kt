package com.darkrockstudios.texteditor

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

data class TextEditorStyle(
	val fontFamily: FontFamily = FontFamily.Default,
	val textColor: Color = Color.Unspecified,
	val backgroundColor: Color = Color.Unspecified,
	val placeholderText: String = "",
	val placeholderColor: Color = Color.Unspecified,
	val cursorColor: Color = Color.Unspecified,
	val selectionColor: Color = Color.Unspecified,
	val focusedBorderColor: Color = Color.Unspecified,
	val unfocusedBorderColor: Color = Color.Unspecified
)

@Composable
fun rememberTextEditorStyle(
	fontFamily: FontFamily = FontFamily.Default,
	textColor: Color = MaterialTheme.colorScheme.onBackground,
	backgroundColor: Color = MaterialTheme.colorScheme.background,
	placeholderText: String = "",
	placeholderColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
	cursorColor: Color = MaterialTheme.colorScheme.onSurface,
	selectionColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
	focusedBorderColor: Color = MaterialTheme.colorScheme.outline,
	unfocusedBorderColor: Color = MaterialTheme.colorScheme.outlineVariant
): TextEditorStyle = remember(
	textColor, placeholderText, placeholderColor,
	cursorColor, selectionColor, focusedBorderColor, unfocusedBorderColor
) {
	TextEditorStyle(
		fontFamily = fontFamily,
		textColor = textColor,
		backgroundColor = backgroundColor,
		placeholderText = placeholderText,
		placeholderColor = placeholderColor,
		cursorColor = cursorColor,
		selectionColor = selectionColor,
		focusedBorderColor = focusedBorderColor,
		unfocusedBorderColor = unfocusedBorderColor,
	)
}
