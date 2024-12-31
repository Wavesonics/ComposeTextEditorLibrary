package com.darkrockstudios.texteditor.state

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.buildAnnotatedString
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TextEditManagerSpanMergeTest {
	private lateinit var state: TextEditorState
	private lateinit var manager: TextEditManager

	@Before
	fun setup() {
		state = createTestEditorState()
		manager = TextEditManager(state)
	}

	@Test
	fun `test insert text with no spans into unstyled text`() {
		// Arrange
		val original = AnnotatedString("Hello World")
		val newText = AnnotatedString("new ")
		val insertionIndex = 6

		// Act
		val result = manager.mergeSpanStyles(original, insertionIndex, newText)

		// Assert
		assertEquals("Hello new World", result.text)
		assertTrue(result.spanStyles.isEmpty())
	}

	@Test
	fun `test insert unstyled text into styled text`() {
		// Arrange
		val original = buildAnnotatedString {
			append("Hello World")
			addStyle(SpanStyle(color = Color.Red), 0, 11)
		}
		val newText = AnnotatedString("new ")
		val insertionIndex = 6

		// Act
		val result = manager.mergeSpanStyles(original, insertionIndex, newText)

		// Assert
		assertEquals("Hello new World", result.text)
		assertEquals(1, result.spanStyles.size)

		val span = result.spanStyles[0]
		assertEquals(0, span.start)
		assertEquals(15, span.end)
		assertEquals(Color.Red, (span.item as SpanStyle).color)
	}

	@Test
	fun `test insert styled text into unstyled text`() {
		// Arrange
		val original = AnnotatedString("Hello World")
		val newText = buildAnnotatedString {
			append("new ")
			addStyle(SpanStyle(color = Color.Blue), 0, 4)
		}
		val insertionIndex = 6

		// Act
		val result = manager.mergeSpanStyles(original, insertionIndex, newText)

		// Assert
		assertEquals("Hello new World", result.text)
		assertEquals(1, result.spanStyles.size)

		val span = result.spanStyles[0]
		assertEquals(6, span.start)
		assertEquals(10, span.end)
		assertEquals(Color.Blue, (span.item as SpanStyle).color)
	}

	@Test
	fun `test insert styled text into differently styled text`() {
		// Arrange
		val original = buildAnnotatedString {
			append("Hello World")
			addStyle(SpanStyle(color = Color.Red), 0, 11)
		}
		val newText = buildAnnotatedString {
			append("new ")
			addStyle(SpanStyle(color = Color.Blue), 0, 4)
		}
		val insertionIndex = 6

		// Act
		val result = manager.mergeSpanStyles(original, insertionIndex, newText)

		// Assert
		assertEquals("Hello new World", result.text)
		assertEquals(2, result.spanStyles.size) // Updated to 2 because spans overlap

		// Check styles are preserved and in correct positions
		with(result.spanStyles) {
			val redSpan = first { it.start == 0 && it.end == 15 }
			assertEquals(Color.Red, (redSpan.item as SpanStyle).color)

			val blueSpan = first { it.start == 6 && it.end == 10 }
			assertEquals(Color.Blue, (blueSpan.item as SpanStyle).color)
		}
	}

	@Test
	fun `test insert text at boundary of styled region`() {
		// Arrange
		val original = buildAnnotatedString {
			append("Hello World")
			addStyle(SpanStyle(color = Color.Red), 0, 5)
		}
		val newText = AnnotatedString("nice ")
		val insertionIndex = 5

		// Act
		val result = manager.mergeSpanStyles(original, insertionIndex, newText)

		// Assert
		assertEquals("Hellonice  World", result.text)
		assertEquals(1, result.spanStyles.size)

		val span = result.spanStyles[0]
		assertEquals(0, span.start)
		assertEquals(5, span.end)
		assertEquals(Color.Red, (span.item as SpanStyle).color)
	}

	@Test
	fun `test merge adjacent identical styles`() {
		// Arrange
		val original = buildAnnotatedString {
			append("Hello World")
			addStyle(SpanStyle(color = Color.Red), 0, 5)
		}
		val newText = buildAnnotatedString {
			append(" new")
			addStyle(SpanStyle(color = Color.Red), 0, 4)
		}
		val insertionIndex = 5

		// Act
		val result = manager.mergeSpanStyles(original, insertionIndex, newText)

		// Assert
		assertEquals("Hello new World", result.text)
		assertEquals(1, result.spanStyles.size)

		val span = result.spanStyles[0]
		assertEquals(0, span.start)
		assertEquals(9, span.end)
		assertEquals(Color.Red, (span.item as SpanStyle).color)
	}

	@Test
	fun `test insert at start of text`() {
		// Arrange
		val original = buildAnnotatedString {
			append("Hello")
			addStyle(SpanStyle(color = Color.Red), 0, 5)
		}
		val newText = buildAnnotatedString {
			append("Hey ")
			addStyle(SpanStyle(color = Color.Blue), 0, 4)
		}
		val insertionIndex = 0

		// Act
		val result = manager.mergeSpanStyles(original, insertionIndex, newText)

		// Assert
		assertEquals("Hey Hello", result.text)
		assertEquals(2, result.spanStyles.size)

		with(result.spanStyles) {
			val blueSpan = first { it.start == 0 }
			assertEquals(0, blueSpan.start)
			assertEquals(4, blueSpan.end)
			assertEquals(Color.Blue, (blueSpan.item as SpanStyle).color)

			val redSpan = first { it.start == 4 }
			assertEquals(4, redSpan.start)
			assertEquals(9, redSpan.end)
			assertEquals(Color.Red, (redSpan.item as SpanStyle).color)
		}
	}

	@Test
	fun `test insert at end of text`() {
		// Arrange
		val original = buildAnnotatedString {
			append("Hello")
			addStyle(SpanStyle(color = Color.Red), 0, 5)
		}
		val newText = buildAnnotatedString {
			append(" World")
			addStyle(SpanStyle(color = Color.Blue), 0, 6)
		}
		val insertionIndex = 5

		// Act
		val result = manager.mergeSpanStyles(original, insertionIndex, newText)

		// Assert
		assertEquals("Hello World", result.text)
		assertEquals(2, result.spanStyles.size)

		with(result.spanStyles) {
			val redSpan = first { it.start == 0 }
			assertEquals(0, redSpan.start)
			assertEquals(5, redSpan.end)
			assertEquals(Color.Red, (redSpan.item as SpanStyle).color)

			val blueSpan = first { it.start == 5 }
			assertEquals(5, blueSpan.start)
			assertEquals(11, blueSpan.end)
			assertEquals(Color.Blue, (blueSpan.item as SpanStyle).color)
		}
	}

	private fun createTestEditorState(): TextEditorState {
		// Create a mock TextMeasurer or use a test double
		return TextEditorState(
			scope = TestScope(),
			measurer = createTestTextMeasurer(),
			initialText = null
		)
	}

	private fun createTestTextMeasurer(): TextMeasurer = mockk(relaxed = true)
}