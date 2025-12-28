package com.darkrockstudios.texteditor.spellcheck.api

import com.darkrockstudios.texteditor.TextEditorRange

/** A platform-agnostic spell checker interface used by the editor. */
interface EditorSpellChecker {
	/** Fast correctness check for a single lexical token (word). */
	suspend fun isCorrectWord(word: String): Boolean

	/** Desired evaluation scope for suggestions. */
	enum class Scope { Word, Sentence }

	/**
	 * Suggestions for the given input. If `scope == Word`, `input` is a single token. If `scope == Sentence`,
	 * implementations may return suggestions that include whitespace (e.g., "in the").
	 * Implementations that can't do sentence-level can ignore it or approximate.
	 */
	suspend fun suggestions(
		input: String,
		scope: Scope = Scope.Word,
		closestOnly: Boolean = true,
	): List<Suggestion>

	/**
	 * Check a sentence for spelling errors that may span multiple words.
	 *
	 * Unlike [isCorrectWord], this method analyzes the sentence as a whole,
	 * potentially finding errors that word-by-word checking would miss
	 * (e.g., concatenated words like "inthe" -> "in the").
	 *
	 * @param sentence The complete sentence to check
	 * @param sentenceRange The document position of the sentence. Corrections will have
	 *                      ranges translated to document coordinates based on this.
	 * @return List of corrections, each with a document-level range and suggestions.
	 *         Returns empty list if the sentence has no errors or if sentence-level
	 *         checking is not supported by this implementation.
	 */
	suspend fun checkSentence(
		sentence: String,
		sentenceRange: TextEditorRange,
	): List<Correction> = emptyList()
}

/** Minimal suggestion model to avoid leaking any library types. */
data class Suggestion(
	val term: String,
)
