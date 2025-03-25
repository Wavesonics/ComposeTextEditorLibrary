package com.darkrockstudios.texteditor.markdown

import com.darkrockstudios.texteditor.state.TextEditorState

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
		return editorState.getAllText().toMarkdown(markdownConfiguration)
	}

	fun importMarkdown(markdownText: String) {
		val annotatedString = markdownText.toAnnotatedStringFromMarkdown(markdownConfiguration)
		editorState.setText(annotatedString)
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