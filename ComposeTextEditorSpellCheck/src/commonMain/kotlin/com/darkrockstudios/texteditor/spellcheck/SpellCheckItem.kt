package com.darkrockstudios.texteditor.spellcheck

import com.darkrockstudios.texteditor.spellcheck.api.Correction
import com.darkrockstudios.texteditor.state.WordSegment

/**
 * Represents a spell check item that can be either a word-level misspelling or a sentence-level correction.
 */
sealed class SpellCheckItem {
	data class MisspelledWord(val segment: WordSegment) : SpellCheckItem()
	data class SentenceIssue(val correction: Correction) : SpellCheckItem()
}