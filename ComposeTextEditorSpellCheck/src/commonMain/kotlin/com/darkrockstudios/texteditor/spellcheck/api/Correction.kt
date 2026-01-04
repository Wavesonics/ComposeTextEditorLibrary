package com.darkrockstudios.texteditor.spellcheck.api

import com.darkrockstudios.texteditor.TextEditorRange

/**
 * Represents a correction for a portion of text within a sentence.
 *
 * @param range The document-level range where the correction applies.
 *              This is an absolute position in the document, not relative to sentence start.
 * @param originalText The original text that was identified as incorrect.
 * @param suggestions List of suggested replacements, ordered by likelihood.
 */
data class Correction(
	val range: TextEditorRange,
	val originalText: String,
	val suggestions: List<Suggestion>,
)
