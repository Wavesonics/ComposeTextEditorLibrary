package com.darkrockstudios.texteditor.markdown

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

/**
 * Configurable markdown style settings to customize how markdown elements
 * are rendered and parsed in the text editor.
 */
data class MarkdownConfiguration(
	val defaultTextStyle: SpanStyle = SpanStyle(fontSize = 16.sp),
	val boldStyle: SpanStyle = SpanStyle(fontWeight = FontWeight.Bold),
	val italicStyle: SpanStyle = SpanStyle(fontStyle = FontStyle.Italic),
	val codeStyle: SpanStyle = SpanStyle(
		fontFamily = FontFamily.Monospace,
		background = Color(0xFFE0E0E0)
	),
	val linkStyle: SpanStyle = SpanStyle(
		color = Color.Blue,
		textDecoration = TextDecoration.Underline
	),
	val blockquoteStyle: SpanStyle = SpanStyle(
		color = Color.Gray,
		fontStyle = FontStyle.Italic
	),
	// Header styles with configurable font sizes
	val header1Style: SpanStyle = SpanStyle(fontSize = 32f.sp, fontWeight = FontWeight.Bold),
	val header2Style: SpanStyle = SpanStyle(fontSize = 24f.sp, fontWeight = FontWeight.Bold),
	val header3Style: SpanStyle = SpanStyle(fontSize = 18.72f.sp, fontWeight = FontWeight.Bold),
	val header4Style: SpanStyle = SpanStyle(fontSize = 16f.sp, fontWeight = FontWeight.Bold),
	val header5Style: SpanStyle = SpanStyle(fontSize = 13.28f.sp, fontWeight = FontWeight.Bold),
	val header6Style: SpanStyle = SpanStyle(fontSize = 12f.sp, fontWeight = FontWeight.Bold),
) {
	companion object {
		val DEFAULT = MarkdownConfiguration()
		val DEFAULT_DARK = DEFAULT.copy(
			//linkStyle = DEFAULT.linkStyle.copy(color = Color.Blue),
			codeStyle = DEFAULT.codeStyle.copy(background = Color.DarkGray),
			//blockquoteStyle = DEFAULT.blockquoteStyle.copy(color = Color.DarkGray)
		)
	}

	/**
	 * Get the appropriate header style for the specified level
	 */
	fun getHeaderStyle(level: Int): SpanStyle {
		return when (level) {
			1 -> header1Style
			2 -> header2Style
			3 -> header3Style
			4 -> header4Style
			5 -> header5Style
			else -> header6Style
		}
	}
}