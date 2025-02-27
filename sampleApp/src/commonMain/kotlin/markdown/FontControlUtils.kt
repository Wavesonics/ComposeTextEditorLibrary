package markdown

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.sp
import com.darkrockstudios.texteditor.markdown.MarkdownConfiguration
import com.darkrockstudios.texteditor.markdown.MarkdownExtension

private val FONT_SIZES = listOf(
	12f, 14f, 16f, 18f, 20f, 24f, 32f
)

/**
 * Calculate the next font size (up or down) based on the current size
 */
private fun getNextFontSize(currentSize: Float?, increase: Boolean): Float {
	val defaultIndex = 2

	if (currentSize == null) {
		return if (increase) FONT_SIZES[defaultIndex + 1] else FONT_SIZES[defaultIndex - 1]
	}

	val currentIndex = FONT_SIZES.indexOfFirst { it >= currentSize }
	val safeCurrentIndex = if (currentIndex == -1) defaultIndex else currentIndex

	val newIndex = if (increase) {
		(safeCurrentIndex + 1).coerceAtMost(FONT_SIZES.size - 1)
	} else {
		(safeCurrentIndex - 1).coerceAtLeast(0)
	}

	return FONT_SIZES[newIndex]
}

/**
 * Change the font size in the editor by updating the MarkdownConfiguration
 * This affects all styling in the document that uses markdown styles
 */
private fun changeFontSize(
	markdownExtension: MarkdownExtension,
	increase: Boolean
) {
	val currentConfig = markdownExtension.markdownConfiguration

	// Determine the baseline font size to adjust from
	val baselineFontSize = currentConfig.defaultTextStyle.fontSize.value
	val newBaseFontSize = getNextFontSize(baselineFontSize, increase)
	println("Font size OLD: $baselineFontSize NEW: $newBaseFontSize")

	val scaleFactor = newBaseFontSize / baselineFontSize

	// Create a new configuration with scaled font sizes
	// The key is to handle each property individually without relying on the copy method's handling of colors
	val newConfig = MarkdownConfiguration(
		defaultTextStyle = currentConfig.defaultTextStyle.copy(
			fontSize = newBaseFontSize.sp
		),
		boldStyle = SpanStyle(
			fontSize = newBaseFontSize.sp,
			fontWeight = currentConfig.boldStyle.fontWeight,
			color = currentConfig.boldStyle.color,
			fontFamily = currentConfig.boldStyle.fontFamily,
			fontStyle = currentConfig.boldStyle.fontStyle,
			background = currentConfig.boldStyle.background
		),
		italicStyle = SpanStyle(
			fontSize = newBaseFontSize.sp,
			fontWeight = currentConfig.italicStyle.fontWeight,
			color = currentConfig.italicStyle.color,
			fontFamily = currentConfig.italicStyle.fontFamily,
			fontStyle = currentConfig.italicStyle.fontStyle,
			background = currentConfig.italicStyle.background
		),
		codeStyle = SpanStyle(
			fontSize = newBaseFontSize.sp,
			fontWeight = currentConfig.codeStyle.fontWeight,
			color = currentConfig.codeStyle.color,
			fontFamily = currentConfig.codeStyle.fontFamily,
			fontStyle = currentConfig.codeStyle.fontStyle,
			background = currentConfig.codeStyle.background
		),
		linkStyle = SpanStyle(
			fontSize = newBaseFontSize.sp,
			fontWeight = currentConfig.linkStyle.fontWeight,
			color = currentConfig.linkStyle.color,
			fontFamily = currentConfig.linkStyle.fontFamily,
			fontStyle = currentConfig.linkStyle.fontStyle,
			background = currentConfig.linkStyle.background
		),
		blockquoteStyle = SpanStyle(
			fontSize = newBaseFontSize.sp,
			fontWeight = currentConfig.blockquoteStyle.fontWeight,
			color = currentConfig.blockquoteStyle.color,
			fontFamily = currentConfig.blockquoteStyle.fontFamily,
			fontStyle = currentConfig.blockquoteStyle.fontStyle,
			background = currentConfig.blockquoteStyle.background
		),
		header1Style = SpanStyle(
			fontSize = (currentConfig.header1Style.fontSize.value * scaleFactor).sp,
			fontWeight = currentConfig.header1Style.fontWeight,
			color = currentConfig.header1Style.color,
			fontFamily = currentConfig.header1Style.fontFamily,
			fontStyle = currentConfig.header1Style.fontStyle,
			background = currentConfig.header1Style.background
		),
		header2Style = SpanStyle(
			fontSize = (currentConfig.header2Style.fontSize.value * scaleFactor).sp,
			fontWeight = currentConfig.header2Style.fontWeight,
			color = currentConfig.header2Style.color,
			fontFamily = currentConfig.header2Style.fontFamily,
			fontStyle = currentConfig.header2Style.fontStyle,
			background = currentConfig.header2Style.background
		),
		header3Style = SpanStyle(
			fontSize = (currentConfig.header3Style.fontSize.value * scaleFactor).sp,
			fontWeight = currentConfig.header3Style.fontWeight,
			color = currentConfig.header3Style.color,
			fontFamily = currentConfig.header3Style.fontFamily,
			fontStyle = currentConfig.header3Style.fontStyle,
			background = currentConfig.header3Style.background
		),
		header4Style = SpanStyle(
			fontSize = (currentConfig.header4Style.fontSize.value * scaleFactor).sp,
			fontWeight = currentConfig.header4Style.fontWeight,
			color = currentConfig.header4Style.color,
			fontFamily = currentConfig.header4Style.fontFamily,
			fontStyle = currentConfig.header4Style.fontStyle,
			background = currentConfig.header4Style.background
		),
		header5Style = SpanStyle(
			fontSize = (currentConfig.header5Style.fontSize.value * scaleFactor).sp,
			fontWeight = currentConfig.header5Style.fontWeight,
			color = currentConfig.header5Style.color,
			fontFamily = currentConfig.header5Style.fontFamily,
			fontStyle = currentConfig.header5Style.fontStyle,
			background = currentConfig.header5Style.background
		),
		header6Style = SpanStyle(
			fontSize = (currentConfig.header6Style.fontSize.value * scaleFactor).sp,
			fontWeight = currentConfig.header6Style.fontWeight,
			color = currentConfig.header6Style.color,
			fontFamily = currentConfig.header6Style.fontFamily,
			fontStyle = currentConfig.header6Style.fontStyle,
			background = currentConfig.header6Style.background
		)
	)

	markdownExtension.updateMarkdownConfiguration(newConfig)
}

fun increaseFontSize(markdownExtension: MarkdownExtension) = changeFontSize(markdownExtension, true)
fun decreaseFontSize(markdownExtension: MarkdownExtension) =
	changeFontSize(markdownExtension, false)