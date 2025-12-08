package com.darkrockstudios.texteditor.spellcheck

import androidx.compose.ui.util.fastForEach
import com.darkrockstudios.texteditor.TextEditorRange
import com.darkrockstudios.texteditor.richstyle.RichSpan
import com.darkrockstudios.texteditor.richstyle.SpellCheckStyle
import com.darkrockstudios.texteditor.spellcheck.api.EditorSpellChecker
import com.darkrockstudios.texteditor.spellcheck.api.EditorSpellChecker.Scope
import com.darkrockstudios.texteditor.spellcheck.api.Suggestion
import com.darkrockstudios.texteditor.spellcheck.utils.applyCapitalizationStrategy
import com.darkrockstudios.texteditor.state.*

class SpellCheckState(
	val textState: TextEditorState,
	var spellChecker: EditorSpellChecker?,
	enableSpellChecking: Boolean = true,
) {
	var spellCheckingEnabled: Boolean = enableSpellChecking
		private set

	suspend fun setSpellCheckingEnabled(value: Boolean) {
		spellCheckingEnabled = value

		val runSpellcheck = (spellCheckingEnabled != value && value == true)
		spellCheckingEnabled = value
		if (runSpellcheck) {
			runFullSpellCheck()
		} else {
			clearSpellCheck()
		}
	}

	private var lastTextHash = -1
	private val misspelledWords = mutableListOf<WordSegment>()

	private fun removeMissSpellingsInRange(range: TextEditorRange) {
		misspelledWords.removeAll { it.range.intersects(range) }
	}

	fun handleSpanClick(span: RichSpan): WordSegment? {
		return if (span.style is SpellCheckStyle) {
			findWordSegmentContainingRange(
				misspelledWords,
				span.range
			)
		} else {
			null
		}
	}

	fun correctSpelling(segment: WordSegment, correction: String) {
		textState.getRichSpansInRange(segment.range)
			.filter { it.style == SpellCheckStyle }
			.forEach { span ->
				textState.removeRichSpan(span)
			}
		misspelledWords.remove(segment)
		println("Correcting spelling for $segment, correcting to: $correction")
		textState.replace(segment.range, correction, true)
	}

	private fun clearSpellCheck() {
		textState.apply {
			richSpanManager.getAllRichSpans()
				.filter { it.style is SpellCheckStyle }
				.forEach { span ->
					removeRichSpan(span)
				}
		}

		misspelledWords.clear()
	}

	/**
	 * This is a very naive algorithm that just removes all spell check spans and
	 * reruns the entire spell check again.
	 */
	suspend fun runFullSpellCheck() {
		val sp = spellChecker ?: return
		if (spellCheckingEnabled.not()) return

		println("Running full Spell Check")
		textState.apply {
			// Remove all existing spell checks
			richSpanManager.getAllRichSpans()
				.filter { it.style is SpellCheckStyle }
				.forEach { span ->
					removeRichSpan(span)
				}

			misspelledWords.clear()

			wordSegments()
				.filter(::shouldSpellCheck)
				.mapNotNullTo(misspelledWords) { segment ->
					if (sp.isCorrectWord(segment.text)) {
						null
					} else {
						addRichSpan(segment.range, SpellCheckStyle)
						segment
					}
				}
		}
	}

	suspend fun runPartialSpellCheck(range: TextEditorRange) {
		val sp = spellChecker ?: return
		if (spellCheckingEnabled.not()) return

		// Remove existing spell check spans in the range
		textState.richSpanManager.getSpansInRange(range)
			.filter { it.style is SpellCheckStyle }
			.forEach { span -> textState.removeRichSpan(span) }

		if (spellCheckingEnabled.not()) return

		// Check spelling in the range
		val misspelledSegments = mutableListOf<WordSegment>()

		removeMissSpellingsInRange(range)
		textState.wordSegmentsInRange(range)
			.filter(::shouldSpellCheck)
			.mapNotNullTo(misspelledSegments) { segment ->
				println("Checking Segment: $segment")
				if (sp.isCorrectWord(segment.text)) {
					null
				} else {
					textState.addRichSpan(segment.range, SpellCheckStyle)
					segment
				}
			}

		misspelledWords.addAll(misspelledSegments)
	}

	/**
	 * Run spell check on a specific word segment.
	 * This will remove any existing spell check spans for the word and add a new one if misspelled.
	 *
	 * @param segment The word segment to check
	 * @return true if the word is misspelled and a new span was added, false otherwise
	 */
	suspend fun checkWordSegment(segment: WordSegment): Boolean {
		val sp = spellChecker ?: return false

		removeMissSpellingsInRange(segment.range)

		textState.apply {
			// First remove any existing spell check spans in this range
			getRichSpansInRange(segment.range)
				.filter { it.style is SpellCheckStyle }
				.forEach { span ->
					removeRichSpan(span)
				}

			// Check if the word is misspelled
			val isSpelledCorrectly = sp.isCorrectWord(segment.text)

			if (!isSpelledCorrectly) {
				// Add a new spell check span
				addRichSpan(segment.range, SpellCheckStyle)

				// Update our misspelled words cache
				misspelledWords.removeAll { it.range == segment.range }
				misspelledWords.add(segment)

				return true
			}
		}

		// Word is spelled correctly
		return false
	}

	private fun shouldSpellCheck(segment: WordSegment): Boolean {
		// Skip segments that are purely numeric
		return !segment.text.all { it.isDigit() }
	}

	fun invalidateSpellCheckSpans(operation: TextEditOperation) {
		val newTextHash = textState.computeTextHash()
		if (lastTextHash != newTextHash) {
			val range: TextEditorRange? = when (operation) {
				is TextEditOperation.Delete -> operation.range
				is TextEditOperation.Insert -> TextEditorRange(
					operation.position,
					operation.position
				)

				is TextEditOperation.Replace -> operation.range
				is TextEditOperation.StyleSpan -> null
			}

			range?.let {
				removeMissSpellingsInRange(range)
			}

			range?.affectedLineWraps(textState)?.forEach { vLine ->
				val lineWrap = textState.getWrappedLine(vLine)
				lineWrap.richSpans
					.filter { it.style is SpellCheckStyle }
					.fastForEach { span ->
						if (range.intersects(span.range)) {
							textState.removeRichSpan(span)
						}
					}
			}

			lastTextHash = newTextHash
		}
	}

	private fun findWordSegmentContainingRange(
		segments: List<WordSegment>,
		range: TextEditorRange,
	): WordSegment? {
		return segments.find { wordSegment ->
			val segmentRange = wordSegment.range
			range.start >= segmentRange.start && range.end <= segmentRange.end
		}
	}

	suspend fun getSuggestions(word: String): List<Suggestion> {
		val sp = spellChecker ?: return emptyList()

		val wordLevel = sp.suggestions(word, scope = Scope.Word, closestOnly = true)
		val sentenceLevel = if (!sp.isCorrectWord(word)) {
			sp.suggestions(word, scope = Scope.Sentence, closestOnly = false)
		} else emptyList()

		val combined = (wordLevel + sentenceLevel)
			.distinctBy { it.term.lowercase() }
			.map { suggestion ->
				suggestion.copy(
					term = applyCapitalizationStrategy(
						source = word,
						target = suggestion.term
					)
				)
			}

		return combined
	}
}