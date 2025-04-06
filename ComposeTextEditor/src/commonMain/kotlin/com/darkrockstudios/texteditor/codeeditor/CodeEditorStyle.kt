package com.darkrockstudios.texteditor.codeeditor

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.darkrockstudios.texteditor.TextEditorStyle

data class CodeEditorStyle(
	val baseStyle: TextEditorStyle,
	val gutterBackgroundColor: Color = Color.DarkGray,
	val gutterTextColor: Color = Color.White,
)

@Composable
fun rememberCodeEditorStyle(
	textColor: Color = MaterialTheme.colorScheme.onSurface,
	placeholderText: String = "",
	placeholderColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
	cursorColor: Color = MaterialTheme.colorScheme.onSurface,
	selectionColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
	focusedBorderColor: Color = MaterialTheme.colorScheme.outline,
	unfocusedBorderColor: Color = MaterialTheme.colorScheme.outlineVariant,
	gutterBackgroundColor: Color = Color.DarkGray,
	gutterTextColor: Color = Color.White,
): CodeEditorStyle = remember(
	textColor, placeholderText, placeholderColor,
	cursorColor, selectionColor, focusedBorderColor, unfocusedBorderColor
) {
	CodeEditorStyle(
		baseStyle = TextEditorStyle(
			textColor = textColor,
			placeholderText = placeholderText,
			placeholderColor = placeholderColor,
			cursorColor = cursorColor,
			selectionColor = selectionColor,
			focusedBorderColor = focusedBorderColor,
			unfocusedBorderColor = unfocusedBorderColor,
		),
		gutterBackgroundColor = gutterBackgroundColor,
		gutterTextColor = gutterTextColor,
	)
}
