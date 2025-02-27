package com.darkrockstudios.texteditor.markdown

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.TextUnit
import com.darkrockstudios.texteditor.state.TextEditorState

/**
 * Updates all text in the editor to use the new markdown configuration styles.
 * This preserves the semantic meaning of styles while updating their visual appearance.
 *
 * @param state The TextEditorState to update
 * @param oldConfig The previous configuration that was used
 * @param newConfig The new configuration to apply
 */
internal fun updateMarkdownStyles(
	state: TextEditorState,
	oldConfig: MarkdownConfiguration,
	newConfig: MarkdownConfiguration
) {
	if (state.textLines.isEmpty()) return

	val styleMapping = mapOf(
		oldConfig.boldStyle to newConfig.boldStyle,
		oldConfig.italicStyle to newConfig.italicStyle,
		oldConfig.codeStyle to newConfig.codeStyle,
		oldConfig.linkStyle to newConfig.linkStyle,
		oldConfig.blockquoteStyle to newConfig.blockquoteStyle,
		oldConfig.header1Style to newConfig.header1Style,
		oldConfig.header2Style to newConfig.header2Style,
		oldConfig.header3Style to newConfig.header3Style,
		oldConfig.header4Style to newConfig.header4Style,
		oldConfig.header5Style to newConfig.header5Style,
		oldConfig.header6Style to newConfig.header6Style
	)

	for (i in state.textLines.indices) {
		val line = state.textLines[i]

		val updatedLine = buildAnnotatedString {
			append(line.text)
			if (line.spanStyles.isEmpty()) {
				addStyle(newConfig.defaultTextStyle, 0, line.length)
			} else {
				line.spanStyles.forEach { span ->
					val newStyle = findMatchingStyle(span.item, styleMapping)
					if (newStyle == newConfig.linkStyle) {
						println("Found link")
					}
					if (newStyle != null) {
						addStyle(newStyle, span.start, span.end)
					} else {
						// Keep the original style if it's not a markdown style
						addStyle(span.item, span.start, span.end)
					}
				}
			}
		}

		state.setLine(i, updatedLine)
	}
	state.updateBookKeeping()

	state.notifyContentChanged()
}

/**
 * Find a matching style in the style mapping.
 * This compares the key style properties to determine if a style is a match.
 */
private fun findMatchingStyle(
	style: SpanStyle,
	styleMapping: Map<SpanStyle, SpanStyle>
): SpanStyle? {
	for ((oldStyle, newStyle) in styleMapping) {
		if (stylesMatch(oldStyle, style)) {
			return newStyle
		}
	}
	return null
}

/**
 * Determine if two styles match semantically based on the type of markdown style represented by style2.
 * This focuses on the properties that define the markdown style type that style2 represents.
 */
private fun stylesMatch(needle: SpanStyle, haystack: SpanStyle): Boolean {
	// Determine what type of markdown style style2 represents
	return when {
		// Check if both are default text styles (no special styling properties)
		isDefaultTextStyle(needle) && isDefaultTextStyle(haystack) -> true

		// If style2 has fontWeight set, it represents bold
		needle.fontWeight != null ->
			haystack.fontWeight != null && haystack.fontWeight == needle.fontWeight

		// If style2 has fontStyle set, it represents italic
		needle.fontStyle != null ->
			haystack.fontStyle != null && haystack.fontStyle == needle.fontStyle

		// If style2 has fontFamily set, it represents code
		needle.fontFamily != null ->
			haystack.fontFamily != null && haystack.fontFamily == needle.fontFamily

		// If style2 has textDecoration set, it represents link
		needle.textDecoration != null ->
			haystack.textDecoration != null && haystack.textDecoration == needle.textDecoration

		// If style2 has fontSize set, it might represent a header
		needle.fontSize != TextUnit.Unspecified ->
			haystack.fontSize != TextUnit.Unspecified && haystack.fontSize == needle.fontSize

		// Default case - no match
		else -> false
	}
}

/**
 * Determines if the given style is a default text style without special formatting
 * properties. This helps identify plain text styles.
 */
private fun isDefaultTextStyle(style: SpanStyle): Boolean {
	return style.fontWeight == null &&
			style.fontStyle == null &&
			style.fontFamily == null &&
			style.textDecoration == null &&
			style.fontSize != TextUnit.Unspecified
}