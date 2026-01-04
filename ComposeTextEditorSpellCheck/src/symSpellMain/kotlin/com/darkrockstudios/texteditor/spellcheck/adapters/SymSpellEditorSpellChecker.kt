package com.darkrockstudios.texteditor.spellcheck.adapters

import com.darkrockstudios.symspellkt.api.SpellChecker
import com.darkrockstudios.symspellkt.common.Verbosity
import com.darkrockstudios.symspellkt.common.isSpelledCorrectly
import com.darkrockstudios.texteditor.CharLineOffset
import com.darkrockstudios.texteditor.TextEditorRange
import com.darkrockstudios.texteditor.spellcheck.api.Correction
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

	override suspend fun checkSentence(
		sentence: String,
		sentenceRange: TextEditorRange,
	): List<Correction> {
		// Use wordBreakSegmentation to find concatenated words
		val segmentationResult = impl.wordBreakSegmentation(sentence)
		val segmentedString = segmentationResult.segmentedString ?: return emptyList()

		if (segmentedString.equals(sentence, ignoreCase = true)) {
			return emptyList() // No corrections needed
		}

		// Find word-level differences between original and corrected
		return findWordCorrections(sentence, segmentedString, sentenceRange)
	}

	/**
	 * Compare words in original and corrected sentences to find specific corrections.
	 * This handles cases where words are split (e.g., "inthe" -> "in the").
	 */
	private fun findWordCorrections(
		original: String,
		corrected: String,
		sentenceRange: TextEditorRange,
	): List<Correction> {
		val corrections = mutableListOf<Correction>()

		// Split into words while tracking positions
		val originalWords = tokenizeWithPositions(original)
		val correctedWords = corrected.split(Regex("\\s+")).filter { it.isNotEmpty() }

		var correctedIdx = 0
		var originalIdx = 0

		while (originalIdx < originalWords.size && correctedIdx < correctedWords.size) {
			val (origWord, origStart, origEnd) = originalWords[originalIdx]
			val corrWord = correctedWords[correctedIdx]

			if (origWord.equals(corrWord, ignoreCase = true)) {
				// Words match, move both pointers
				originalIdx++
				correctedIdx++
			} else {
				// Check if original word should be split into multiple corrected words
				val possibleSplitEnd = findSplitMatch(
					originalWords, originalIdx,
					correctedWords, correctedIdx
				)

				if (possibleSplitEnd != null) {
					// Found a split match: original word(s) map to multiple corrected words
					val (origEndIdx, corrEndIdx) = possibleSplitEnd
					val origStartPos = originalWords[originalIdx].second
					val origEndPos = originalWords[origEndIdx].third
					val correctedText = correctedWords.subList(correctedIdx, corrEndIdx + 1).joinToString(" ")

					// Translate to document coordinates
					val docRange = translateToDocumentRange(
						sentenceRange, origStartPos, origEndPos, original
					)

					corrections.add(
						Correction(
							range = docRange,
							originalText = original.substring(origStartPos, origEndPos),
							suggestions = listOf(Suggestion(correctedText))
						)
					)

					originalIdx = origEndIdx + 1
					correctedIdx = corrEndIdx + 1
				} else {
					// Single word correction
					val wordSuggestions = impl.lookup(origWord, verbosity = Verbosity.Closest)
						.map { Suggestion(it.term) }

					if (wordSuggestions.isNotEmpty() || !origWord.equals(corrWord, ignoreCase = true)) {
						val docRange = translateToDocumentRange(
							sentenceRange, origStart, origEnd, original
						)

						val suggestionList = if (wordSuggestions.isNotEmpty()) {
							wordSuggestions
						} else {
							listOf(Suggestion(corrWord))
						}

						corrections.add(
							Correction(
								range = docRange,
								originalText = origWord,
								suggestions = suggestionList
							)
						)
					}

					originalIdx++
					correctedIdx++
				}
			}
		}

		return corrections
	}

	/**
	 * Tokenize a string into words with their start and end positions.
	 */
	private fun tokenizeWithPositions(text: String): List<Triple<String, Int, Int>> {
		val result = mutableListOf<Triple<String, Int, Int>>()
		var i = 0

		while (i < text.length) {
			// Skip whitespace
			while (i < text.length && text[i].isWhitespace()) {
				i++
			}

			if (i >= text.length) break

			// Find word end
			val start = i
			while (i < text.length && !text[i].isWhitespace()) {
				i++
			}

			result.add(Triple(text.substring(start, i), start, i))
		}

		return result
	}

	/**
	 * Try to find a match where one or more original words correspond to
	 * multiple corrected words (word splitting case).
	 *
	 * Returns pair of (original end index, corrected end index) if match found.
	 */
	private fun findSplitMatch(
		originalWords: List<Triple<String, Int, Int>>,
		origStartIdx: Int,
		correctedWords: List<String>,
		corrStartIdx: Int
	): Pair<Int, Int>? {
		// Try matching concatenation of corrected words to original word(s)
		for (origEndIdx in origStartIdx until minOf(origStartIdx + 3, originalWords.size)) {
			val origConcat = originalWords
				.subList(origStartIdx, origEndIdx + 1)
				.joinToString("") { it.first }
				.lowercase()

			for (corrEndIdx in corrStartIdx until minOf(corrStartIdx + 4, correctedWords.size)) {
				val corrConcat = correctedWords
					.subList(corrStartIdx, corrEndIdx + 1)
					.joinToString("")
					.lowercase()

				if (origConcat == corrConcat && (origEndIdx > origStartIdx || corrEndIdx > corrStartIdx)) {
					return Pair(origEndIdx, corrEndIdx)
				}
			}
		}

		return null
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
