package com.darkrockstudios.texteditor.spellcheck.adapters

import com.darkrockstudios.symspellkt.api.SpellChecker
import com.darkrockstudios.symspellkt.common.Verbosity
import com.darkrockstudios.symspellkt.common.isSpelledCorrectly
import com.darkrockstudios.texteditor.spellcheck.api.EditorSpellChecker
import com.darkrockstudios.texteditor.spellcheck.api.EditorSpellChecker.Scope
import com.darkrockstudios.texteditor.spellcheck.api.Suggestion

class SymSpellEditorSpellChecker(
	private val impl: SpellChecker
) : EditorSpellChecker {
	override suspend fun isCorrectWord(word: String): Boolean {
		val suggestions = impl.lookup(word, verbosity = Verbosity.Top)
		return word.isSpelledCorrectly(suggestions)
	}

	override suspend fun suggestions(input: String, scope: Scope, closestOnly: Boolean): List<Suggestion> {
		val verbosity = if (closestOnly) Verbosity.Closest else Verbosity.All
		val base = impl.lookup(input, verbosity = verbosity)
			.map { Suggestion(term = it.term) }
			.toMutableList()

		if (scope == Scope.Sentence) {
			val segmented = impl.wordBreakSegmentation(input).segmentedString
			if (segmented != null && !segmented.equals(input, ignoreCase = true)
				&& base.none { it.term.equals(segmented, ignoreCase = true) }
			) {
				base.add(Suggestion(segmented))
			}
		}
		return base
	}
}
