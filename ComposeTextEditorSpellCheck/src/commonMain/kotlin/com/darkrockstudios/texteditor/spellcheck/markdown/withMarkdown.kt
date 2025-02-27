package com.darkrockstudios.texteditor.spellcheck.markdown

import com.darkrockstudios.texteditor.markdown.MarkdownConfiguration
import com.darkrockstudios.texteditor.markdown.MarkdownExtension
import com.darkrockstudios.texteditor.spellcheck.SpellCheckState

fun SpellCheckState.withMarkdown(
	initialConfiguration: MarkdownConfiguration = MarkdownConfiguration.DEFAULT
): MarkdownExtension {
	return MarkdownExtension(textState, initialConfiguration)
}