package com.darkrockstudios.texteditor.markdown

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import com.darkrockstudios.texteditor.state.TextEditorState
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UpdateMarkdownStylesTest {

	private val testScope = TestScope()

	@Test
	fun `test updating bold style`() {
		// Create two different markdown configurations
		val oldConfig = MarkdownConfiguration(
			boldStyle = SpanStyle(fontWeight = FontWeight.Bold)
		)
		val newConfig = MarkdownConfiguration(
			boldStyle = SpanStyle(fontWeight = FontWeight.Bold, color = Color.Red)
		)

		// Create editor state with bold text
		val state = createEditorStateWithBoldText()

		// Update the styles
		updateMarkdownStyles(state, oldConfig, newConfig)

		// Verify the bold style now includes red color
		val boldSpan = state.textLines[0].spanStyles.first()
		assertEquals(newConfig.boldStyle, boldSpan.item)
		assertEquals(Color.Red, (boldSpan.item as SpanStyle).color)
	}

	@Test
	fun `test updating italic style specifically`() {
		// Create two different markdown configurations
		val oldConfig = MarkdownConfiguration(
			italicStyle = SpanStyle(fontStyle = FontStyle.Italic)
		)
		val newConfig = MarkdownConfiguration(
			italicStyle = SpanStyle(fontStyle = FontStyle.Italic, color = Color.Blue)
		)

		// Create state with just italic text
		val state = TextEditorState(
			scope = testScope.backgroundScope,
			measurer = mockk(relaxed = true)
		)

		val italicText = buildAnnotatedString {
			withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
				append("This is italic text")
			}
		}

		state.setText(italicText)

		// Print initial style
		println("Initial italic style: ${state.textLines[0].spanStyles.firstOrNull()?.item}")

		// Update the styles
		updateMarkdownStyles(state, oldConfig, newConfig)

		// Verify the italic style now includes blue color
		val italicSpan = state.textLines[0].spanStyles.firstOrNull()
		println("Updated italic style: $italicSpan")

		assertTrue(italicSpan != null, "Italic span should exist after update")
		assertEquals(FontStyle.Italic, italicSpan.item.fontStyle)
		assertEquals(Color.Blue, italicSpan.item.color)
	}

	@Test
	fun `test empty text gets default style`() {
		val oldConfig = MarkdownConfiguration.DEFAULT
		val newConfig = MarkdownConfiguration(
			boldStyle = SpanStyle(fontWeight = FontWeight.Bold, color = Color.Red),
		)

		val state = TextEditorState(
			scope = testScope.backgroundScope,
			measurer = mockk(relaxed = true)
		)

		// Update should apply the default base style even to empty text
		updateMarkdownStyles(state, oldConfig, newConfig)

		assertTrue(state.textLines.size == 1)
		assertTrue(state.textLines[0].text.isEmpty())

		// Now we expect exactly one span style - the BASE_TEXT style
		assertEquals(1, state.textLines[0].spanStyles.size)
		val baseSpan = state.textLines[0].spanStyles.first()
		assertEquals(newConfig.defaultTextStyle, baseSpan.item)
	}

	@Test
	fun `test updating fontSize of styles`() {
		// Create two different markdown configurations with different font sizes
		val oldConfig = MarkdownConfiguration(
			boldStyle = SpanStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp),
			italicStyle = SpanStyle(fontStyle = FontStyle.Italic, fontSize = 14.sp),
			codeStyle = SpanStyle(
				fontFamily = FontFamily.Monospace,
				background = Color(0xFFE0E0E0),
				fontSize = 14.sp
			)
		)

		val newConfig = MarkdownConfiguration(
			boldStyle = SpanStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp),
			italicStyle = SpanStyle(fontStyle = FontStyle.Italic, fontSize = 18.sp),
			codeStyle = SpanStyle(
				fontFamily = FontFamily.Monospace,
				background = Color(0xFFE0E0E0),
				fontSize = 18.sp
			)
		)

		// Create editor state with mixed styles
		val state = createEditorStateWithMixedStyles()

		// Print initial styles for debugging
		println("Initial styles:")
		state.textLines[0].spanStyles.forEach { span ->
			println("Span from ${span.start} to ${span.end}: ${span.item}")
		}

		// Update the styles
		updateMarkdownStyles(state, oldConfig, newConfig)

		// Print updated styles for debugging
		println("Updated styles:")
		state.textLines[0].spanStyles.forEach { span ->
			println("Span from ${span.start} to ${span.end}: ${span.item}")
		}

		// Verify all styles are updated with the new font size
		val spans = state.textLines[0].spanStyles

		// Find bold span and verify it has new font size
		val boldSpan = spans.find { span ->
			(span.item as? SpanStyle)?.fontWeight == FontWeight.Bold
		}
		assertTrue(boldSpan != null, "Bold span should exist")
		assertEquals(
			18.sp,
			boldSpan.item.fontSize,
			"Bold span should have updated font size"
		)

		// Find italic span and verify it has new font size
		val italicSpan = spans.find { span ->
			(span.item as? SpanStyle)?.fontStyle == FontStyle.Italic
		}
		assertTrue(italicSpan != null, "Italic span should exist")
		assertEquals(
			18.sp,
			italicSpan.item.fontSize,
			"Italic span should have updated font size"
		)

		// Find code span and verify it has new font size
		val codeSpan = spans.find { span ->
			(span.item as? SpanStyle)?.fontFamily == FontFamily.Monospace
		}
		assertTrue(codeSpan != null, "Code span should exist")
		assertEquals(
			18.sp,
			codeSpan.item.fontSize,
			"Code span should have updated font size"
		)
	}

	private fun createEditorStateWithBoldText(): TextEditorState {
		val state = TextEditorState(
			scope = testScope.backgroundScope,
			measurer = mockk(relaxed = true)
		)

		val boldText = buildAnnotatedString {
			withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
				append("Bold text")
			}
		}

		state.setText(boldText)
		return state
	}

	private fun createEditorStateWithMixedStyles(): TextEditorState {
		val state = TextEditorState(
			scope = testScope.backgroundScope,
			measurer = mockk(relaxed = true)
		)

		val mixedText = buildAnnotatedString {
			append("Normal ")
			withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
				append("bold ")
			}
			withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
				append("italic ")
			}
			withStyle(SpanStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)) {
				append("code")
			}
		}

		state.setText(mixedText)
		return state
	}
}