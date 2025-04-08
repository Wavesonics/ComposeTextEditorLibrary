package texteditoperation

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.darkrockstudios.texteditor.CharLineOffset
import com.darkrockstudios.texteditor.TextEditorRange
import com.darkrockstudios.texteditor.TextEditorStyle
import com.darkrockstudios.texteditor.state.TextEditOperation
import com.darkrockstudios.texteditor.state.TextEditorState
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class TextOperationReplaceTest {
	private lateinit var state: TextEditorState

	@BeforeTest
	fun setup() {
		state = createTestEditorState()
	}

	@Test
	fun `test simple replace in single line`() {
		state.setText("Hello World")

		val operation = TextEditOperation.Replace(
			range = TextEditorRange(
				start = CharLineOffset(0, 6),
				end = CharLineOffset(0, 11)
			),
			newText = AnnotatedString("Universe"),
			oldText = AnnotatedString("World"),
			cursorBefore = CharLineOffset(0, 6),
			cursorAfter = CharLineOffset(0, 14)
		)

		state.editManager.applyOperation(operation)

		assertEquals("Hello Universe", state.textLines[0].text)
	}

	@Test
	fun `test replace spanning multiple lines`() {
		state.setText("Hello\nWorld\nText")

		val operation = TextEditOperation.Replace(
			range = TextEditorRange(
				start = CharLineOffset(0, 5),
				end = CharLineOffset(2, 4)
			),
			newText = AnnotatedString(" Awesome"),
			oldText = AnnotatedString("\nWorld\nText"),
			cursorBefore = CharLineOffset(0, 6),
			cursorAfter = CharLineOffset(0, 11)
		)

		state.editManager.applyOperation(operation)

		assertEquals(1, state.textLines.size)
		assertEquals("Hello Awesome", state.textLines[0].text)
	}

	@Test
	fun `test replace spanning multiple lines with multiple lines`() {
		state.setText("Hello\nWorld\nText")

		val operation = TextEditOperation.Replace(
			range = TextEditorRange(
				start = CharLineOffset(0, 5),
				end = CharLineOffset(2, 4)
			),
			newText = AnnotatedString(" Awesome\nNew Line"),
			oldText = AnnotatedString("\nWorld\nText"),
			cursorBefore = CharLineOffset(0, 6),
			cursorAfter = CharLineOffset(0, 11)
		)

		state.editManager.applyOperation(operation)

		assertEquals(2, state.textLines.size)
		assertEquals("Hello Awesome\nNew Line", state.getAllText().text)
	}

	@Test
	fun `test replace with SpanStyle`() {
		state.setText("Hello World")
		val boldStyle = SpanStyle(fontWeight = FontWeight.Bold)

		val oldText = buildAnnotatedString {
			append("World")
			addStyle(boldStyle, 0, 5)
		}

		val newText = buildAnnotatedString {
			append("Universe")
			addStyle(boldStyle, 0, 8)
		}

		val operation = TextEditOperation.Replace(
			range = TextEditorRange(
				start = CharLineOffset(0, 6),
				end = CharLineOffset(0, 11)
			),
			newText = newText,
			oldText = oldText,
			cursorBefore = CharLineOffset(0, 6),
			cursorAfter = CharLineOffset(0, 14)
		)

		state.editManager.applyOperation(operation)

		assertEquals("Hello Universe", state.textLines[0].text)
		assertEquals(1, state.textLines[0].spanStyles.size)
		val resultSpan = state.textLines[0].spanStyles[0]
		assertEquals(6, resultSpan.start)
		assertEquals(14, resultSpan.end)
		assertEquals(boldStyle, resultSpan.item)
	}

	@Test
	fun `test replace inherits style`() {
		state.setText("Hello World")
		val boldStyle = SpanStyle(fontWeight = FontWeight.Bold)

		state.addStyleSpan(
			TextEditorRange(
				start = CharLineOffset(0, 6),
				end = CharLineOffset(0, 11)
			),
			boldStyle
		)

		val operation = TextEditOperation.Replace(
			range = TextEditorRange(
				start = CharLineOffset(0, 6),
				end = CharLineOffset(0, 11)
			),
			newText = AnnotatedString("Universe"),
			oldText = AnnotatedString("World"),
			cursorBefore = CharLineOffset(0, 6),
			cursorAfter = CharLineOffset(0, 14),
			inheritStyle = true
		)

		state.editManager.applyOperation(operation)

		assertEquals("Hello Universe", state.textLines[0].text)
		assertEquals(1, state.textLines[0].spanStyles.size)
		val resultSpan = state.textLines[0].spanStyles[0]
		assertEquals(6, resultSpan.start)
		assertEquals(14, resultSpan.end)
		assertEquals(boldStyle, resultSpan.item)
	}

	private fun createTestEditorState(): TextEditorState {
		return TextEditorState(
			scope = TestScope(),
			editorStyle = TextEditorStyle(),
			measurer = mockk(relaxed = true),
			initialText = AnnotatedString("")
		)
	}
}
