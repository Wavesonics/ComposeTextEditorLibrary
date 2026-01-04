package com.darkrockstudios.texteditor.spellcheck.adapters

import com.darkrockstudios.libs.platformspellchecker.MisspelledWord
import com.darkrockstudios.libs.platformspellchecker.PlatformSpellChecker
import com.darkrockstudios.libs.platformspellchecker.SpellingCorrection
import com.darkrockstudios.texteditor.CharLineOffset
import com.darkrockstudios.texteditor.TextEditorRange
import com.darkrockstudios.texteditor.spellcheck.api.Correction
import com.darkrockstudios.texteditor.spellcheck.api.EditorSpellChecker
import com.darkrockstudios.texteditor.spellcheck.api.Suggestion

class PlatformEditorSpellChecker(
	private val checker: PlatformSpellChecker
) : EditorSpellChecker {
	override suspend fun isCorrectWord(word: String): Boolean {
		return checker.isWordCorrect(word)
	}

	override suspend fun suggestions(
		input: String,
		scope: EditorSpellChecker.Scope,
		closestOnly: Boolean
	): List<Suggestion> {
		return if (scope == EditorSpellChecker.Scope.Word) {
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

	override suspend fun checkSentence(
		sentence: String,
		sentenceRange: TextEditorRange,
	): List<Correction> {
		val results: List<SpellingCorrection> = checker.checkMultiword(sentence)

		return results.mapNotNull { correction ->
			if (correction.suggestions.isEmpty()) {
				return@mapNotNull null
			}

			// SpellingCorrection has: misspelledWord, startIndex, length, suggestions
			val localStart = correction.startIndex
			val localEnd = correction.startIndex + correction.length

			// Translate to document coordinates
			val docRange = translateToDocumentRange(
				sentenceRange = sentenceRange,
				localStart = localStart,
				localEnd = localEnd,
				sentence = sentence
			)

			Correction(
				range = docRange,
				originalText = correction.misspelledWord,
				suggestions = correction.suggestions.map { Suggestion(it) }
			)
		}
	}

	/**
	 * Translate a sentence-relative character range to document coordinates.
	 */
	private fun translateToDocumentRange(
		sentenceRange: TextEditorRange,
		localStart: Int,
		localEnd: Int,
		sentence: String,
	): TextEditorRange {
		// For single-line sentences, this is straightforward
		if (sentenceRange.start.line == sentenceRange.end.line) {
			return TextEditorRange(
				start = CharLineOffset(
					line = sentenceRange.start.line,
					char = sentenceRange.start.char + localStart
				),
				end = CharLineOffset(
					line = sentenceRange.start.line,
					char = sentenceRange.start.char + localEnd
				)
			)
		}

		// For multi-line sentences, track line breaks
		var currentLine = sentenceRange.start.line
		var lineStartOffset = 0

		var startLine = sentenceRange.start.line
		var startChar = sentenceRange.start.char + localStart

		var endLine = sentenceRange.start.line
		var endChar = sentenceRange.start.char + localEnd

		for (i in sentence.indices) {
			if (i == localStart) {
				startLine = currentLine
				startChar = if (currentLine == sentenceRange.start.line) {
					sentenceRange.start.char + (i - lineStartOffset)
				} else {
					i - lineStartOffset
				}
			}
			if (i == localEnd) {
				endLine = currentLine
				endChar = if (currentLine == sentenceRange.start.line) {
					sentenceRange.start.char + (i - lineStartOffset)
				} else {
					i - lineStartOffset
				}
				break
			}
			if (sentence[i] == '\n') {
				currentLine++
				lineStartOffset = i + 1
			}
		}

		return TextEditorRange(
			start = CharLineOffset(startLine, startChar),
			end = CharLineOffset(endLine, endChar)
		)
	}
}
