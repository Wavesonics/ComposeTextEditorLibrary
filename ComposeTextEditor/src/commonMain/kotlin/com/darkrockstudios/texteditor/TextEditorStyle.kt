package com.darkrockstudios.texteditor

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

data class TextEditorStyle(
	val textColor: Color = Color.Unspecified,
	val backgroundColor: Color = Color.Unspecified,
	val placeholderText: String = "",
	val placeholderColor: Color = Color.Unspecified,
	val cursorColor: Color = Color.Unspecified,
	val selectionColor: Color = Color.Unspecified,
	val focusedBorderColor: Color = Color.Unspecified,
	val unfocusedBorderColor: Color = Color.Unspecified,
	val textStyle: TextStyle = TextStyle.Default,
	/**
	 * Color of the bullet-list dot drawn in the gutter. `Color.Unspecified` falls
	 * back to `Color.DarkGray` at draw time — a sensible light-mode default that
	 * users running dark themes will want to override.
	 */
	val bulletColor: Color = Color.Unspecified,
	/**
	 * Color of the blockquote bar drawn in the gutter. `Color.Unspecified` falls
	 * back to `Color.Gray.copy(alpha = 0.6f)` at draw time.
	 */
	val blockquoteBarColor: Color = Color.Unspecified,
	/**
	 * Color of the ordered-list numeral drawn in the gutter. `Color.Unspecified`
	 * inherits the editor's text color (via `drawText`'s color override).
	 */
	val orderedListMarkerColor: Color = Color.Unspecified,
)

@Composable
fun rememberTextEditorStyle(
	textColor: Color = MaterialTheme.colorScheme.onBackground,
	backgroundColor: Color = MaterialTheme.colorScheme.background,
	placeholderText: String = "",
	placeholderColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
	cursorColor: Color = MaterialTheme.colorScheme.onSurface,
	selectionColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
	focusedBorderColor: Color = MaterialTheme.colorScheme.outline,
	unfocusedBorderColor: Color = MaterialTheme.colorScheme.outlineVariant,
	textStyle: TextStyle = TextStyle.Default,
	// Markers default to dimmed variants of the text color so they work in both
	// light and dark themes without per-app configuration.
	bulletColor: Color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
	blockquoteBarColor: Color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
	orderedListMarkerColor: Color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
): TextEditorStyle = remember(
	textColor, placeholderText, placeholderColor,
	cursorColor, selectionColor, focusedBorderColor, unfocusedBorderColor, textStyle,
	bulletColor, blockquoteBarColor, orderedListMarkerColor,
) {
	TextEditorStyle(
		textColor = textColor,
		backgroundColor = backgroundColor,
		placeholderText = placeholderText,
		placeholderColor = placeholderColor,
		cursorColor = cursorColor,
		selectionColor = selectionColor,
		focusedBorderColor = focusedBorderColor,
		unfocusedBorderColor = unfocusedBorderColor,
		textStyle = textStyle,
		bulletColor = bulletColor,
		blockquoteBarColor = blockquoteBarColor,
		orderedListMarkerColor = orderedListMarkerColor,
	)
}
