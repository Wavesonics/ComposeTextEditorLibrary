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
		assertTrue(isWordChar("self-aware", 4)) // Valid hyphenation
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
}