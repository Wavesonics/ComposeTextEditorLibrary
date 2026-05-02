package com.darkrockstudios.texteditor.markdown

import com.darkrockstudios.texteditor.CharLineOffset
import com.darkrockstudios.texteditor.richstyle.HR_PLACEHOLDER
import com.darkrockstudios.texteditor.richstyle.HorizontalRuleSpanStyle
import com.darkrockstudios.texteditor.state.TextEditorState

private val HR_LINE_TOKENS = setOf("---", "***", "___")

/**
 * An extension to TextEditorState that provides markdown functionality.
 * This separates markdown concerns from the core text editor functionality.
 */
class MarkdownExtension(
	val editorState: TextEditorState,
	initialConfiguration: MarkdownConfiguration = MarkdownConfiguration.DEFAULT
) {
	var markdownConfiguration: MarkdownConfiguration = initialConfiguration
		set(value) {
			field = value
			markdownStyles = MarkdownStyles(markdownConfiguration)
		}

	var markdownStyles: MarkdownStyles = MarkdownStyles(markdownConfiguration)
		private set

	fun exportAsMarkdown(): String {
		val hrLines = editorState.richSpanManager.getAllRichSpans()
			.asSequence()
			.filter { it.style === HorizontalRuleSpanStyle }
			.map { it.range.start.line }
			.toHashSet()

		val annotated = editorState.getAllText()
		val text = annotated.text
		if (text.isEmpty() && hrLines.isEmpty()) return ""

		val sb = StringBuilder()
		var lineIndex = 0
		var cursor = 0
		while (true) {
			val nextNewline = text.indexOf('\n', cursor)
			val end = if (nextNewline == -1) text.length else nextNewline
			val lineMarkdown = if (lineIndex in hrLines) {
				"---"
			} else {
				annotated.subSequence(cursor, end).toMarkdown(markdownConfiguration)
			}
			sb.append(lineMarkdown)
			if (nextNewline == -1) break
			// Header export already ends with \n; don't double it.
			if (!lineMarkdown.endsWith('\n')) sb.append('\n')
			cursor = nextNewline + 1
			lineIndex++
		}
		return sb.toString()
	}

	fun importMarkdown(markdownText: String) {
		val hrLineIndices = mutableListOf<Int>()
		val processedLines = markdownText.lines().mapIndexed { index, line ->
			if (line.trim() in HR_LINE_TOKENS) {
				hrLineIndices += index
				HR_PLACEHOLDER
			} else {
				line
			}
		}
		val processedMarkdown = processedLines.joinToString("\n")
		val annotatedString = processedMarkdown.toAnnotatedStringFromMarkdown(markdownConfiguration)
		editorState.setText(annotatedString)
		hrLineIndices.forEach { lineIdx ->
			editorState.addRichSpan(
				start = CharLineOffset(lineIdx, 0),
				end = CharLineOffset(lineIdx, HR_PLACEHOLDER.length),
				style = HorizontalRuleSpanStyle,
			)
		}
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other == null || this::class != other::class) return false

		other as MarkdownExtension

		if (editorState != other.editorState) return false
		if (markdownConfiguration != other.markdownConfiguration) return false

		return true
	}

	override fun hashCode(): Int {
		var result = editorState.hashCode()
		result = 31 * result + markdownConfiguration.hashCode()
		return result
	}
}

fun TextEditorState.withMarkdown(
	initialConfiguration: MarkdownConfiguration = MarkdownConfiguration.DEFAULT
): MarkdownExtension {
	return MarkdownExtension(this, initialConfiguration)
}