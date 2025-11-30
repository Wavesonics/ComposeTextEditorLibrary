package com.darkrockstudios.texteditor.spellcheck.adapters

import com.darkrockstudios.libs.platformspellchecker.MisspelledWord
import com.darkrockstudios.libs.platformspellchecker.PlatformSpellChecker
import com.darkrockstudios.libs.platformspellchecker.SpellingCorrection
import com.darkrockstudios.texteditor.spellcheck.api.EditorSpellChecker
import com.darkrockstudios.texteditor.spellcheck.api.Suggestion
import kotlinx.coroutines.runBlocking

class PlatformEditorSpellChecker(
	private val checker: PlatformSpellChecker
) : EditorSpellChecker {
	override fun isCorrectWord(word: String): Boolean {
		return runBlocking { checker.isWordCorrect(word) }
	}

	override fun suggestions(
		input: String,
		scope: EditorSpellChecker.Scope,
		closestOnly: Boolean
	): List<Suggestion> {
		return runBlocking {
			if (scope == EditorSpellChecker.Scope.Word) {
				val result = checker.checkWord(input)
				if (result is MisspelledWord) {
					if (closestOnly) {
						listOfNotNull(result.suggestions.firstOrNull())
					} else {
						result.suggestions
					}
				} else {
					emptyList()
				}.map { s -> Suggestion(term = s) }
			} else {
				val results: List<SpellingCorrection> = checker.checkMultiword(input)
				val terms: List<String> = if (closestOnly) {
					results.mapNotNull { it.suggestions.firstOrNull() }
				} else {
					results.flatMap { it.suggestions }
				}
				terms.map { s -> Suggestion(term = s) }
			}
		}
	}
}
