package com.darkrockstudios.texteditor.spellcheck.api

/** A platform-agnostic spell checker interface used by the editor. */
interface EditorSpellChecker {
	/** Fast correctness check for a single lexical token (word). */
	fun isCorrectWord(word: String): Boolean

	/** Desired evaluation scope for suggestions. */
	enum class Scope { Word, Sentence }

	/**
	 * Suggestions for the given input. If `scope == Word`, `input` is a single token. If `scope == Sentence`,
	 * implementations may return suggestions that include whitespace (e.g., "in the").
	 * Implementations that can't do sentence-level can ignore it or approximate.
	 */
	fun suggestions(
		input: String,
		scope: Scope = Scope.Word,
		closestOnly: Boolean = true,
	): List<Suggestion>
}

/** Minimal suggestion model to avoid leaking any library types. */
data class Suggestion(
	val term: String,
)
