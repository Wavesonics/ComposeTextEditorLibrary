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
}