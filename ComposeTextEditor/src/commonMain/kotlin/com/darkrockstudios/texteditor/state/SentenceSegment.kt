package com.darkrockstudios.texteditor.state

import com.darkrockstudios.texteditor.TextEditorRange

/**
 * Represents a sentence within the editor text.
 *
 * @param text The sentence text content
 * @param range The document position of this sentence
 */
data class SentenceSegment(
	val text: String,
	val range: TextEditorRange,
) {
	override fun toString(): String = "$text (${range.start}-${range.end})"
}
