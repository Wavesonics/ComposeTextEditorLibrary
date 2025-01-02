package com.darkrockstudios.texteditor.spellcheck

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.MultiParagraph
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import com.darkrockstudios.symspellkt.api.SpellChecker
import com.darkrockstudios.symspellkt.common.Composition
import com.darkrockstudios.symspellkt.common.SpellCheckSettings
import com.darkrockstudios.symspellkt.common.SuggestionItem
import com.darkrockstudios.symspellkt.common.Verbosity
import com.darkrockstudios.texteditor.CharLineOffset
import com.darkrockstudios.texteditor.TextEditorRange
import com.darkrockstudios.texteditor.richstyle.SpellCheckStyle
import com.darkrockstudios.texteditor.state.TextEditorState
import com.darkrockstudios.texteditor.state.WordSegment
import com.darkrockstudios.texteditor.state.getRichSpansInRange
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SpellCheckStateTest {
	private lateinit var textState: TextEditorState
	private lateinit var spellCheckState: SpellCheckState
	private lateinit var spellChecker: MockSpellChecker
	private lateinit var textMeasurer: TextMeasurer

	@Before
	fun setup() {
		textMeasurer = mockk()

		every {
			textMeasurer.measure(
				text = any<AnnotatedString>(),
				constraints = any()
			)
		} answers {
			mockk<TextLayoutResult>().apply {
				every { getLineStart(any()) } returns 0
				every { getLineEnd(any()) } returns 5
				every { lineCount } returns 1

				every { multiParagraph } answers {
					mockk<MultiParagraph>().apply {
						every { lineCount } returns 1
						every { getLineHeight(any()) } returns 10f
					}
				}
			}
		}

		textState = TextEditorState(
			scope = TestScope(),
			measurer = textMeasurer,
			initialText = null
		)
		spellChecker = MockSpellChecker()
		spellCheckState = SpellCheckState(textState, spellChecker)
	}

	@Test
	fun `test checkWordSegment with correct word`() {
		// Setup
		val word = "hello"
		textState.setText(word)
		val segment = WordSegment(
			text = word,
			range = TextEditorRange(
				start = CharLineOffset(0, 0),
				end = CharLineOffset(0, 5)
			)
		)
		spellChecker.response = listOf(SuggestionItem(word, 1.0, 1.0))

		// Act
		val result = spellCheckState.checkWordSegment(segment)

		// Assert
		assertFalse(result)
		assertTrue(textState.getRichSpansInRange(segment.range).isEmpty())
	}

	@Test
	fun `test checkWordSegment with incorrect word`() {
		// Setup
		val word = "helllo"
		textState.setText(word)

		val segment = WordSegment(
			text = word,
			range = TextEditorRange(
				start = CharLineOffset(0, 0),
				end = CharLineOffset(0, 6)
			)
		)

		spellChecker.response = listOf(SuggestionItem("hello", 1.0, 1.0))

		// Act
		val result = spellCheckState.checkWordSegment(segment)

		// Assert
		assertTrue(result)
		val spans = textState.getRichSpansInRange(segment.range)
		assertEquals(1, spans.size)
		assertTrue(spans.first().style is SpellCheckStyle)
	}

	@Test
	fun `test checkWordSegment removes existing spell check spans`() {
		// Setup
		val word = "helllo"
		textState.setText(word)
		val segment = WordSegment(
			text = word,
			range = TextEditorRange(
				start = CharLineOffset(0, 0),
				end = CharLineOffset(0, 6)
			)
		)

		// Add initial spell check span
		textState.addRichSpan(segment.range, SpellCheckStyle)

		// Make the word correct for the second check
		spellChecker.response = listOf(SuggestionItem(word, 1.0, 1.0))

		// Act
		val result = spellCheckState.checkWordSegment(segment)

		// Assert
		assertFalse(result)
		assertTrue(textState.getRichSpansInRange(segment.range).isEmpty())
	}
}

private class MockSpellChecker(
	var response: List<SuggestionItem> = emptyList(),
	var wordBreakResponse: Composition = Composition(),
) : SpellChecker(
	dictionary = mockk(),
	stringDistance = mockk(),
	spellCheckSettings = SpellCheckSettings(),
) {
	override fun createDictionaryEntry(word: String, frequency: Int): Boolean = true
	override fun createDictionaryEntry(word: String, frequency: Double): Boolean = true

	override fun lookup(
		word: String,
		verbosity: Verbosity,
		editDistance: Double
	): List<SuggestionItem> = response

	override fun lookupCompound(
		word: String,
		editDistance: Double,
		tokenizeOnWhiteSpace: Boolean
	): List<SuggestionItem> = response

	override fun wordBreakSegmentation(
		phrase: String,
		maxSegmentationWordLength: Int,
		maxEditDistance: Double
	): Composition = wordBreakResponse
}