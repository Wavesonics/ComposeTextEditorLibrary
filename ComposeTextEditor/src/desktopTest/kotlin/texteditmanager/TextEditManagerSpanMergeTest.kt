package texteditmanager

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.darkrockstudios.texteditor.state.SpanManager
import com.darkrockstudios.texteditor.utils.mergeAnnotatedStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TextEditManagerSpanMergeTest {
	private lateinit var manager: SpanManager

	@Before
	fun setup() {
		manager = SpanManager()
	}

	@Test
	fun `test insert text with no spans into unstyled text`() {
		// Arrange
		val original = AnnotatedString("Hello World")
		val newText = AnnotatedString("new ")
		val insertionIndex = 6

		// Act
		val result =
			manager.mergeAnnotatedStrings(original, start = insertionIndex, newText = newText)

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
		val result =
			manager.mergeAnnotatedStrings(original, start = insertionIndex, newText = newText)

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
		val result =
			manager.mergeAnnotatedStrings(original, start = insertionIndex, newText = newText)

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
		val result =
			manager.mergeAnnotatedStrings(original, start = insertionIndex, newText = newText)

		// Assert
		assertEquals("Hello new World", result.text)
		assertEquals(2, result.spanStyles.size)

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
		val result =
			manager.mergeAnnotatedStrings(original, start = insertionIndex, newText = newText)

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
		val result =
			manager.mergeAnnotatedStrings(original, start = insertionIndex, newText = newText)

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
		val result =
			manager.mergeAnnotatedStrings(original, start = insertionIndex, newText = newText)

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
		val result =
			manager.mergeAnnotatedStrings(original, start = insertionIndex, newText = newText)

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

	@Test
	fun `test delete text with spans`() {
		// Arrange
		val original = buildAnnotatedString {
			append("Hello World")
			addStyle(SpanStyle(color = Color.Red), 0, 5) // "Hello"
			addStyle(SpanStyle(color = Color.Blue), 6, 11) // "World"
		}
		val startIndex = 3
		val endIndex = 8

		// Act
		val result = manager.mergeAnnotatedStrings(original, start = startIndex, end = endIndex)

		// Assert
		assertEquals("Helrld", result.text)
		assertEquals(2, result.spanStyles.size)

		val redSpan = result.spanStyles.first()
		assertEquals(0, redSpan.start)
		assertEquals(3, redSpan.end)
		assertEquals(Color.Red, redSpan.item.color)

		val blueSpan = result.spanStyles.last()
		assertEquals(3, blueSpan.start)
		assertEquals(6, blueSpan.end)
		assertEquals(Color.Blue, blueSpan.item.color)
	}

	@Test
	fun `test replace text with overlapping spans`() {
		// Arrange
		val original = buildAnnotatedString {
			append("Hello World")
			addStyle(SpanStyle(color = Color.Red), 0, 5) // "Hello"
			addStyle(SpanStyle(color = Color.Blue), 6, 11) // "World"
		}
		val newText = buildAnnotatedString {
			append("Beautiful")
			addStyle(SpanStyle(color = Color.Green), 0, 9) // "Beautiful"
		}
		val startIndex = 3
		val endIndex = 8

		// Act
		val result = manager.mergeAnnotatedStrings(
			original,
			start = startIndex,
			end = endIndex,
			newText = newText
		)

		// Assert
		assertEquals("HelBeautifulrld", result.text)
		assertEquals(3, result.spanStyles.size)

		val redSpan = result.spanStyles.first { it.start == 0 && it.end == 3 }
		assertEquals(Color.Red, redSpan.item.color)

		val greenSpan = result.spanStyles.first { it.start == 3 && it.end == 12 }
		assertEquals(Color.Green, greenSpan.item.color)

		val blueSpan = result.spanStyles.first { it.start == 12 && it.end == 15 }
		assertEquals(Color.Blue, blueSpan.item.color)
	}

	@Test
	fun `test insert character into overlapping styled text - bold and underline`() {
		// Arrange - create text with overlapping bold and underline spans
		val original = buildAnnotatedString {
			append("Welcome")
			addStyle(SpanStyle(fontWeight = FontWeight.Bold), 0, 7) // bold
			addStyle(SpanStyle(textDecoration = TextDecoration.Underline), 0, 7) // underline
		}

		// Simulate inserting "a" at position 3 (between "l" and "c")
		val newText = buildAnnotatedString {
			append("a")
			addStyle(SpanStyle(fontWeight = FontWeight.Bold), 0, 1) // bold
			addStyle(SpanStyle(textDecoration = TextDecoration.Underline), 0, 1) // underline
		}
		val insertionIndex = 3

		// Act
		val result = manager.mergeAnnotatedStrings(original, start = insertionIndex, newText = newText)

		// Assert
		assertEquals("Welacome", result.text)
		// Should have 2 spans: bold(0-8) and underline(0-8)
		assertEquals(2, result.spanStyles.size)

		val boldSpan = result.spanStyles.find { it.item.fontWeight == FontWeight.Bold }
		val underlineSpan = result.spanStyles.find { it.item.textDecoration == TextDecoration.Underline }

		assertNotNull(boldSpan)
		assertNotNull(underlineSpan)

		boldSpan?.let { assertEquals(0, it.start); assertEquals(8, it.end) }
		underlineSpan?.let { assertEquals(0, it.start); assertEquals(8, it.end) }
	}
}
