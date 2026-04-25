package texteditor

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.darkrockstudios.texteditor.state.TextEditorState
import kotlinx.coroutines.test.TestScope
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Regression test for TextIndent-induced wrap jitter.
 *
 * Unlike [TextEditorWrapTest] which mocks the measurer (and therefore cannot exhibit
 * any font/width-driven wrap behavior), this test uses a real [TextMeasurer] so the
 * layout actually wraps across virtual lines.
 *
 * These tests assert invariants rather than exact wrap indices, because absolute
 * indices depend on the default system font's metrics, which vary across JVMs.
 */
class TextIndentWrapTest {
	private val testScope = TestScope()

	private fun realTextMeasurer(): TextMeasurer = TextMeasurer(
		defaultFontFamilyResolver = createFontFamilyResolver(),
		defaultDensity = Density(1f, 1f),
		defaultLayoutDirection = LayoutDirection.Ltr,
	)

	private fun freshState(): TextEditorState = TextEditorState(
		scope = testScope,
		measurer = realTextMeasurer(),
	)

	/** Long enough to wrap several times at viewport width 200px with the default font. */
	private val longText =
		"The quick brown fox jumps over the lazy dog near the riverbank where the old mill stood for many years before the storm finally brought it down in autumn."

	@Test
	fun `wrapStartsAtIndex matches textLayoutResult getLineStart`() {
		val state = freshState()
		state.onViewportSizeChange(Size(200f, 500f))
		state.setText(AnnotatedString(longText))

		assertTrue(
			state.lineOffsets.size > 1,
			"Expected the test text to wrap across multiple virtual lines"
		)

		state.lineOffsets.forEach { lw ->
			val expected = lw.textLayoutResult.getLineStart(lw.virtualLineIndex)
			assertEquals(
				expected,
				lw.wrapStartsAtIndex,
				"wrapStartsAtIndex for virtual line ${lw.virtualLineIndex} of line ${lw.line} " +
						"should match getLineStart"
			)
		}
	}

	@Test
	fun `wrapStartsAtIndex is strictly monotonically increasing within a physical line`() {
		val state = freshState()
		state.onViewportSizeChange(Size(200f, 500f))
		state.setText(AnnotatedString(longText))

		val byLine = state.lineOffsets.groupBy { it.line }
		byLine.forEach { (line, wraps) ->
			val indices = wraps.sortedBy { it.virtualLineIndex }.map { it.wrapStartsAtIndex }
			indices.zipWithNext().forEach { (a, b) ->
				assertTrue(
					b > a,
					"wrapStartsAtIndex should strictly increase across virtual lines of line $line " +
							"but saw $indices"
				)
			}
		}
	}

	@Test
	fun `virtualLength matches getLineEnd minus getLineStart`() {
		val state = freshState()
		state.onViewportSizeChange(Size(200f, 500f))
		state.setText(AnnotatedString(longText))

		state.lineOffsets.forEach { lw ->
			val expected = lw.textLayoutResult.getLineEnd(lw.virtualLineIndex) -
					lw.textLayoutResult.getLineStart(lw.virtualLineIndex)
			assertEquals(
				expected,
				lw.virtualLength,
				"virtualLength mismatch for virtual line ${lw.virtualLineIndex} of line ${lw.line}"
			)
		}
	}

	@Test
	fun `TextIndent does not reduce virtual line count versus no indent`() {
		val noIndent = freshState().also {
			it.onViewportSizeChange(Size(200f, 500f))
			it.setText(AnnotatedString(longText))
		}
		val withIndent = freshState().also {
			it.textStyle = TextStyle(
				fontFamily = FontFamily.Default,
				textIndent = TextIndent(firstLine = 40.sp),
			)
			it.onViewportSizeChange(Size(200f, 500f))
			it.setText(AnnotatedString(longText))
		}

		val noIndentCount = noIndent.lineOffsets.size
		val withIndentCount = withIndent.lineOffsets.size

		assertTrue(
			withIndentCount >= noIndentCount,
			"Applying a first-line indent should not reduce the number of wrapped lines " +
					"(no indent: $noIndentCount, with indent: $withIndentCount)"
		)
	}

	/**
	 * Repro of the sample-app bug: a header-sized line that fits on one line without
	 * TextIndent must still fit with a small first-line indent. Before this was fixed,
	 * Compose's paragraph layout shrank the effective width to the natural content
	 * width, and TextIndent then consumed part of that *shrunken* width, forcing a wrap
	 * that shouldn't happen. The fix is in [com.darkrockstudios.texteditor.state.TextEditorState.updateBookKeeping]
	 * which uses a tight (minWidth == maxWidth) width constraint so the paragraph lays
	 * out at the full viewport width.
	 */
	@Test
	fun `header-sized line that fits without indent still fits with small indent`() {
		val headerSpan = SpanStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold)
		val headerText = buildAnnotatedString {
			pushStyle(headerSpan)
			append("Understanding RichSpan Management")
			pop()
		}

		// Wide enough that the header easily fits on one line at 32sp.
		val viewport = Size(2000f, 500f)

		val noIndent = freshState().also {
			it.onViewportSizeChange(viewport)
			it.setText(headerText)
		}
		val withSmallIndent = freshState().also {
			it.textStyle = TextStyle(textIndent = TextIndent(firstLine = 24.sp))
			it.onViewportSizeChange(viewport)
			it.setText(headerText)
		}

		assertEquals(
			1,
			noIndent.lineOffsets.size,
			"Baseline: header should fit on one line with no indent"
		)
		assertEquals(
			1,
			withSmallIndent.lineOffsets.size,
			"Header should still fit on one line with a 24sp first-line indent " +
					"(viewport is ${viewport.width}px, indent is tiny). Got " +
					"${withSmallIndent.lineOffsets.size} virtual lines: " +
					withSmallIndent.lineOffsets.map {
						"vL${it.virtualLineIndex}@${it.wrapStartsAtIndex}"
					}
		)
	}

	@Test
	fun `wrapStartsAtIndex is still getLineStart when TextIndent is applied`() {
		val state = freshState()
		state.textStyle = TextStyle(
			fontFamily = FontFamily.Default,
			textIndent = TextIndent(firstLine = 40.sp),
		)
		state.onViewportSizeChange(Size(200f, 500f))
		state.setText(AnnotatedString(longText))

		assertTrue(
			state.lineOffsets.size > 1,
			"Expected text to wrap when a TextIndent is applied"
		)

		state.lineOffsets.forEach { lw ->
			assertEquals(
				lw.textLayoutResult.getLineStart(lw.virtualLineIndex),
				lw.wrapStartsAtIndex,
				"With TextIndent, wrapStartsAtIndex should still equal getLineStart for " +
						"virtual line ${lw.virtualLineIndex} of line ${lw.line}"
			)
		}
	}
}
