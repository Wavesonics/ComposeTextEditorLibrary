package fontcontrol

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.sp
import com.darkrockstudios.texteditor.markdown.MarkdownConfiguration
import com.darkrockstudios.texteditor.markdown.decreaseFontSize
import com.darkrockstudios.texteditor.markdown.increaseFontSize
import com.darkrockstudios.texteditor.state.TextEditorState
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FontControlUtilsTest {
	private lateinit var mockTextEditorState: TextEditorState
	private val configSlot = slot<MarkdownConfiguration>()

	@BeforeTest
	fun setup() {
		// Create a relaxed mock for TextEditorState
		mockTextEditorState = mockk<TextEditorState>(relaxed = true)

		// Mock the textLines property to return an empty list
		val emptyTextLines = listOf<AnnotatedString>()
		every { mockTextEditorState.textLines } returns emptyTextLines

		// Mock the MarkdownConfiguration.DEFAULT value
		mockkObject(MarkdownConfiguration.Companion)
		val defaultConfig = MarkdownConfiguration()
		every { MarkdownConfiguration.DEFAULT } returns defaultConfig

		mockkStatic(TextEditorState::updateMarkdownConfiguration)
		every {
			mockTextEditorState.updateMarkdownConfiguration(capture(configSlot))
		} just Runs
	}

	@Test
	fun `test increaseFontSize increases font size`() {
		// Call the function to test
		increaseFontSize(mockTextEditorState)

		// Verify updateMarkdownConfiguration was called
		verify { mockTextEditorState.updateMarkdownConfiguration(any()) }

		// Get the captured configuration
		val newConfig = configSlot.captured

		// Default font size (16f) should be increased to 18f
		assertEquals(18f, newConfig.boldStyle.fontSize.value)
		assertEquals(18f, newConfig.italicStyle.fontSize.value)
		assertEquals(18f, newConfig.codeStyle.fontSize.value)

		// Header sizes should be scaled proportionally (18/16 = 1.125)
		val defaultConfig = MarkdownConfiguration.DEFAULT
		val expectedScale = 18f / defaultConfig.boldStyle.fontSize.value
		assertEquals(
			defaultConfig.header1Style.fontSize.value * expectedScale,
			newConfig.header1Style.fontSize.value
		)
	}

	@Test
	fun `test decreaseFontSize decreases font size`() {
		// Call the function to test
		decreaseFontSize(mockTextEditorState)

		// Verify updateMarkdownConfiguration was called
		verify { mockTextEditorState.updateMarkdownConfiguration(any()) }

		// Get the captured configuration
		val newConfig = configSlot.captured

		// Default font size (16f) should be decreased to 14f
		assertEquals(14f, newConfig.boldStyle.fontSize.value)
		assertEquals(14f, newConfig.italicStyle.fontSize.value)
		assertEquals(14f, newConfig.codeStyle.fontSize.value)

		// Header sizes should be scaled proportionally (14/16 = 0.875)
		val defaultConfig = MarkdownConfiguration.DEFAULT
		val expectedScale = 14f / defaultConfig.boldStyle.fontSize.value
		assertEquals(
			defaultConfig.header1Style.fontSize.value * expectedScale,
			newConfig.header1Style.fontSize.value
		)
	}

	@Test
	fun `test changeFontSize at minimum size does not decrease further`() {
		// Start with minimum size
		val baseSize = 12f

		// Call decrease - should remain at minimum
		decreaseFontSize(mockTextEditorState)

		// Clear the slot for our next capture
		configSlot.clear()

		// Force mockk to use the minimum size for the next call
		val minConfig = MarkdownConfiguration(
			boldStyle = MarkdownConfiguration.DEFAULT.boldStyle.copy(fontSize = baseSize.sp),
			italicStyle = MarkdownConfiguration.DEFAULT.italicStyle.copy(fontSize = baseSize.sp),
			codeStyle = MarkdownConfiguration.DEFAULT.codeStyle.copy(fontSize = baseSize.sp),
			linkStyle = MarkdownConfiguration.DEFAULT.linkStyle.copy(fontSize = baseSize.sp),
			blockquoteStyle = MarkdownConfiguration.DEFAULT.blockquoteStyle.copy(fontSize = baseSize.sp),
			header1Style = MarkdownConfiguration.DEFAULT.header1Style,
			header2Style = MarkdownConfiguration.DEFAULT.header2Style,
			header3Style = MarkdownConfiguration.DEFAULT.header3Style,
			header4Style = MarkdownConfiguration.DEFAULT.header4Style,
			header5Style = MarkdownConfiguration.DEFAULT.header5Style,
			header6Style = MarkdownConfiguration.DEFAULT.header6Style
		)
		every { MarkdownConfiguration.DEFAULT } returns minConfig

		// Try to decrease again
		decreaseFontSize(mockTextEditorState)

		// Should still be 12f (minimum)
		assertEquals(12f, configSlot.captured.boldStyle.fontSize.value)
	}

	@Test
	fun `test changeFontSize at maximum size does not increase further`() {
		// Start with maximum size
		val baseSize = 32f

		// Force mockk to use the maximum size
		val maxConfig = MarkdownConfiguration(
			boldStyle = MarkdownConfiguration.DEFAULT.boldStyle.copy(fontSize = baseSize.sp),
			italicStyle = MarkdownConfiguration.DEFAULT.italicStyle.copy(fontSize = baseSize.sp),
			codeStyle = MarkdownConfiguration.DEFAULT.codeStyle.copy(fontSize = baseSize.sp),
			linkStyle = MarkdownConfiguration.DEFAULT.linkStyle.copy(fontSize = baseSize.sp),
			blockquoteStyle = MarkdownConfiguration.DEFAULT.blockquoteStyle.copy(fontSize = baseSize.sp),
			header1Style = MarkdownConfiguration.DEFAULT.header1Style,
			header2Style = MarkdownConfiguration.DEFAULT.header2Style,
			header3Style = MarkdownConfiguration.DEFAULT.header3Style,
			header4Style = MarkdownConfiguration.DEFAULT.header4Style,
			header5Style = MarkdownConfiguration.DEFAULT.header5Style,
			header6Style = MarkdownConfiguration.DEFAULT.header6Style
		)
		every { MarkdownConfiguration.DEFAULT } returns maxConfig

		// Try to increase
		increaseFontSize(mockTextEditorState)

		// Should still be 32f (maximum)
		assertEquals(32f, configSlot.captured.boldStyle.fontSize.value)
	}

	@Test
	fun `test all style components are updated correctly`() {
		// Call the function to test
		increaseFontSize(mockTextEditorState)

		// Get the captured configuration
		val newConfig = configSlot.captured

		// Base styles should all have the same font size
		val expectedBaseSize = 18f
		assertEquals(expectedBaseSize, newConfig.boldStyle.fontSize.value)
		assertEquals(expectedBaseSize, newConfig.italicStyle.fontSize.value)
		assertEquals(expectedBaseSize, newConfig.codeStyle.fontSize.value)
		assertEquals(expectedBaseSize, newConfig.linkStyle.fontSize.value)
		assertEquals(expectedBaseSize, newConfig.blockquoteStyle.fontSize.value)

		// Header styles should maintain relative proportions
		val defaultConfig = MarkdownConfiguration.DEFAULT
		val scaleFactor = expectedBaseSize / defaultConfig.boldStyle.fontSize.value
		assertEquals(
			defaultConfig.header1Style.fontSize.value * scaleFactor,
			newConfig.header1Style.fontSize.value
		)
		assertEquals(
			defaultConfig.header2Style.fontSize.value * scaleFactor,
			newConfig.header2Style.fontSize.value
		)
		assertEquals(
			defaultConfig.header3Style.fontSize.value * scaleFactor,
			newConfig.header3Style.fontSize.value
		)
		assertEquals(
			defaultConfig.header4Style.fontSize.value * scaleFactor,
			newConfig.header4Style.fontSize.value
		)
		assertEquals(
			defaultConfig.header5Style.fontSize.value * scaleFactor,
			newConfig.header5Style.fontSize.value
		)
		assertEquals(
			defaultConfig.header6Style.fontSize.value * scaleFactor,
			newConfig.header6Style.fontSize.value
		)
	}

	@Test
	fun `test font size selection from intermediate value`() {
		// For 13f, the function behavior:
		// 1. Finds 14f as the first value >= 13f (at index 1)
		// 2. When increasing, it returns the value at index 1+1=2, which is 16f
		val customBaseSize = 13f
		val customConfig = MarkdownConfiguration(
			boldStyle = MarkdownConfiguration.DEFAULT.boldStyle.copy(fontSize = customBaseSize.sp),
			italicStyle = MarkdownConfiguration.DEFAULT.italicStyle.copy(fontSize = customBaseSize.sp),
			codeStyle = MarkdownConfiguration.DEFAULT.codeStyle.copy(fontSize = customBaseSize.sp),
			linkStyle = MarkdownConfiguration.DEFAULT.linkStyle.copy(fontSize = customBaseSize.sp),
			blockquoteStyle = MarkdownConfiguration.DEFAULT.blockquoteStyle.copy(fontSize = customBaseSize.sp),
			header1Style = MarkdownConfiguration.DEFAULT.header1Style,
			header2Style = MarkdownConfiguration.DEFAULT.header2Style,
			header3Style = MarkdownConfiguration.DEFAULT.header3Style,
			header4Style = MarkdownConfiguration.DEFAULT.header4Style,
			header5Style = MarkdownConfiguration.DEFAULT.header5Style,
			header6Style = MarkdownConfiguration.DEFAULT.header6Style
		)

		every { MarkdownConfiguration.DEFAULT } returns customConfig
		increaseFontSize(mockTextEditorState)
		assertEquals(16f, configSlot.captured.boldStyle.fontSize.value)

		// Similarly, for 17f:
		// 1. Finds 18f as the first value >= 17f (at index 3)
		// 2. When increasing, it returns the value at index 3+1=4, which is 20f
		configSlot.clear()
		val customConfig2 = MarkdownConfiguration(
			boldStyle = MarkdownConfiguration.DEFAULT.boldStyle.copy(fontSize = 17f.sp),
			italicStyle = MarkdownConfiguration.DEFAULT.italicStyle.copy(fontSize = 17f.sp),
			codeStyle = MarkdownConfiguration.DEFAULT.codeStyle.copy(fontSize = 17f.sp),
			linkStyle = MarkdownConfiguration.DEFAULT.linkStyle.copy(fontSize = 17f.sp),
			blockquoteStyle = MarkdownConfiguration.DEFAULT.blockquoteStyle.copy(fontSize = 17f.sp),
			header1Style = MarkdownConfiguration.DEFAULT.header1Style,
			header2Style = MarkdownConfiguration.DEFAULT.header2Style,
			header3Style = MarkdownConfiguration.DEFAULT.header3Style,
			header4Style = MarkdownConfiguration.DEFAULT.header4Style,
			header5Style = MarkdownConfiguration.DEFAULT.header5Style,
			header6Style = MarkdownConfiguration.DEFAULT.header6Style
		)

		every { MarkdownConfiguration.DEFAULT } returns customConfig2
		increaseFontSize(mockTextEditorState)
		assertEquals(20f, configSlot.captured.boldStyle.fontSize.value)
	}
}