package wordsegmentation

import com.darkrockstudios.texteditor.state.isWordChar
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WordCharTest {
	@Test
	fun `test basic word characters`() {
		assertTrue(isWordChar("word", 0)) // 'w'
		assertTrue(isWordChar("word", 1)) // 'o'
		assertTrue(isWordChar("word", 2)) // 'r'
		assertTrue(isWordChar("word", 3)) // 'd'
		assertTrue(isWordChar("word123", 4)) // '1'
		assertTrue(isWordChar("under_score", 5)) // '_'
	}

	@Test
	fun `test apostrophes`() {
		assertTrue(isWordChar("don't", 3))  // Valid contraction
		assertTrue(isWordChar("Mary's", 4)) // Valid possessive
		assertFalse(isWordChar("'word", 0)) // Invalid at start
		assertFalse(isWordChar("word'", 4)) // Invalid at end
		assertFalse(isWordChar("wo''rd", 3)) // Invalid double apostrophe
	}

	@Test
	fun `test hyphens`() {
		// Changed: hyphens are no longer part of words
		assertFalse(isWordChar("self-aware", 4)) // No longer valid - hyphens are separators
		assertFalse(isWordChar("-word", 0)) // Invalid at start
		assertFalse(isWordChar("word-", 4)) // Invalid at end
		assertFalse(isWordChar("self--aware", 5)) // Invalid double hyphen
	}

	@Test
	fun `test periods`() {
		assertTrue(isWordChar("U.S.A", 1)) // Valid abbreviation period
		assertFalse(isWordChar(".word", 0)) // Invalid at start
		assertFalse(isWordChar("word.", 4)) // Invalid at end
		assertFalse(isWordChar("U..S", 2)) // Invalid double period
	}

	@Test
	fun `test non-word characters`() {
		assertFalse(isWordChar("word!", 4)) // Punctuation
		assertFalse(isWordChar("word ", 4)) // Space
		assertFalse(isWordChar("word\n", 4)) // Newline
		assertFalse(isWordChar("word\t", 4)) // Tab
		assertFalse(isWordChar("pre-warm", 3)) // Hyphen
	}

	@Test
	fun `test index boundary cases`() {
		assertFalse(isWordChar("word", -1)) // Before start
		assertFalse(isWordChar("word", 4)) // At length
		assertFalse(isWordChar("word", 5)) // After end
		assertFalse(isWordChar("", 0)) // Empty string
	}

	@Test
	fun `test boundary cases with special characters`() {
		// Test apostrophe at boundaries
		assertFalse(isWordChar("'word", 0)) // Start of text
		assertFalse(isWordChar("word'", 4)) // End of text

		// Test period at boundaries
		assertFalse(isWordChar(".word", 0)) // Start of text
		assertFalse(isWordChar("word.", 4)) // End of text

		// Test hyphen at boundaries
		assertFalse(isWordChar("-word", 0)) // Start of text
		assertFalse(isWordChar("word-", 4)) // End of text

		// Edge cases for special character context checks
		assertFalse(isWordChar("a'", 1)) // Apostrophe at end
		assertFalse(isWordChar("a.", 1)) // Period at end
		assertFalse(isWordChar("a-", 1)) // Hyphen at end
		assertFalse(isWordChar("'b", 0)) // Apostrophe at start
		assertFalse(isWordChar(".b", 0)) // Period at start
		assertFalse(isWordChar("-b", 0)) // Hyphen at start
	}

	@Test
	fun `test hyphenated words split properly`() {
		// New test to explicitly verify hyphenated words are treated as separate words
		// "pre-warm" should be two separate words: "pre" and "warm"
		assertTrue(isWordChar("pre-warm", 0)) // 'p' in "pre"
		assertTrue(isWordChar("pre-warm", 1)) // 'r' in "pre"
		assertTrue(isWordChar("pre-warm", 2)) // 'e' in "pre"
		assertFalse(isWordChar("pre-warm", 3)) // '-' is a separator
		assertTrue(isWordChar("pre-warm", 4)) // 'w' in "warm"
		assertTrue(isWordChar("pre-warm", 5)) // 'a' in "warm"
		assertTrue(isWordChar("pre-warm", 6)) // 'r' in "warm"
		assertTrue(isWordChar("pre-warm", 7)) // 'm' in "warm"
	}
}
