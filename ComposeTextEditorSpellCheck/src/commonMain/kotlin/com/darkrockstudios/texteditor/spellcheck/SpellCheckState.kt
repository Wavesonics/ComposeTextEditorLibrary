package com.darkrockstudios.texteditor.spellcheck

import androidx.compose.ui.util.fastForEach
import com.darkrockstudios.symspellkt.api.SpellChecker
import com.darkrockstudios.symspellkt.common.SuggestionItem
import com.darkrockstudios.symspellkt.common.Verbosity
import com.darkrockstudios.symspellkt.common.isSpelledCorrectly
import com.darkrockstudios.symspellkt.common.spellingIsCorrect
import com.darkrockstudios.texteditor.TextEditorRange
import com.darkrockstudios.texteditor.richstyle.RichSpan
import com.darkrockstudios.texteditor.richstyle.SpellCheckStyle
import com.darkrockstudios.texteditor.state.TextEditOperation
import com.darkrockstudios.texteditor.state.TextEditorState
import com.darkrockstudios.texteditor.state.WordSegment
import com.darkrockstudios.texteditor.state.getRichSpansInRange
import com.darkrockstudios.texteditor.state.wordSegments
import com.mohamedrejeb.richeditor.compose.spellcheck.utils.applyCapitalizationStrategy

class SpellCheckState(
	val textState: TextEditorState,
	var spellChecker: SpellChecker?
) {
	private var lastTextHash = -1
	private val misspelledWords = mutableListOf<WordSegment>()

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
		textState.replace(segment.range, correction, true)
	}

	/**
	 * This is a very naive algorithm that just removes all spell check spans and
	 * reruns the entire spell check again.
	 */
	fun runFullSpellCheck() {
		val sp = spellChecker ?: return

		textState.apply {
			// Remove all existing spell checks
			richSpanManager.getAllRichSpans()
				.filter { it.style is SpellCheckStyle }
				.forEach { span ->
					removeRichSpan(span)
				}

			misspelledWords.clear()
			wordSegments().mapNotNullTo(misspelledWords) { segment ->
				val suggestions = sp.lookup(segment.text)
				if (suggestions.spellingIsCorrect(segment.text)) {
					null
				} else {
					segment
				}
			}

			misspelledWords.fastForEach { wordSegment ->
				addRichSpan(wordSegment.range, SpellCheckStyle)
			}
		}
	}

	fun onTextChange(operation: TextEditOperation) {
		val newTextHash = textState.computeTextHash()
		if (lastTextHash != newTextHash) {
			runFullSpellCheck()
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

	fun getSuggestions(word: String): List<SuggestionItem> {
		val sp = spellChecker ?: return emptyList()

		val suggestions = sp.lookup(word, verbosity = Verbosity.Closest)
		val proposedSuggestions = if (word.isSpelledCorrectly(suggestions).not()) {
			// If things are misspelled, see if it just needs to be broken up
			val composition = sp.wordBreakSegmentation(word)
			val segmentedWord = composition.segmentedString
			if (segmentedWord != null
				&& segmentedWord.equals(word, ignoreCase = true).not()
				&& suggestions.find { it.term.equals(segmentedWord, ignoreCase = true) } == null
			) {
				// Add the segmented suggest as first item if it didn't already exist
				listOf(SuggestionItem(segmentedWord, 1.0, 0.1)) + suggestions
			} else {
				suggestions
			}
		} else {
			emptyList()
		}

		return proposedSuggestions.map { suggestionItem ->
			suggestionItem.copy(
				term = applyCapitalizationStrategy(
					source = word,
					target = suggestionItem.term
				)
			)
		}
	}
}