package texteditor

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.darkrockstudios.texteditor.cursor.calculateCursorPosition
import com.darkrockstudios.texteditor.richstyle.BulletList
import com.darkrockstudios.texteditor.richstyle.applyLineBlock
import com.darkrockstudios.texteditor.state.TextEditorState
import kotlinx.coroutines.test.TestScope
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Verifies that the cursor on an empty line sits at the effective first-line
 * indent position rather than at `x=0`. Without this, the cursor jumps from 0
 * to the indent the moment the user types the first character — visible
 * regression as "the cursor snaps right when I start typing".
 */
class CursorIndentTest {
	private val testScope = TestScope()
	private val density = Density(1f, 1f)

	private fun realTextMeasurer(): TextMeasurer = TextMeasurer(
		defaultFontFamilyResolver = createFontFamilyResolver(),
		defaultDensity = density,
		defaultLayoutDirection = LayoutDirection.Ltr,
	)

	private fun freshState(): TextEditorState = TextEditorState(
		scope = testScope,
		measurer = realTextMeasurer(),
	).also {
		it.density = density
		it.onViewportSizeChange(Size(500f, 200f))
	}

	@Test
	fun `cursor on empty line with editor-wide firstLine indent sits at indent`() {
		val state = freshState()
		state.textStyle = TextStyle(textIndent = TextIndent(firstLine = 40.sp))

		val metrics = state.calculateCursorPosition()

		// With Density(1f, 1f), 40.sp == 40px.
		assertEquals(40f, metrics.position.x, 0.5f)
	}

	@Test
	fun `cursor on empty line without firstLine indent sits at x=0`() {
		val state = freshState()
		// No textIndent applied.

		val metrics = state.calculateCursorPosition()

		assertEquals(0f, metrics.position.x, 0.5f)
	}

	@Test
	fun `cursor on empty bullet line sits at bullet indent`() {
		val state = freshState()
		state.applyLineBlock(0, BulletList)

		val metrics = state.calculateCursorPosition()

		// BULLET_LIST_PARAGRAPH_STYLE.textIndent.firstLine == 16.sp.
		assertEquals(16f, metrics.position.x, 0.5f)
	}

	@Test
	fun `block-line indent takes precedence over editor-wide indent`() {
		val state = freshState()
		state.textStyle = TextStyle(textIndent = TextIndent(firstLine = 40.sp))
		state.applyLineBlock(0, BulletList)

		val metrics = state.calculateCursorPosition()

		// Block lines carry their own paragraph style; the editor-wide indent
		// is not layered on top.
		assertEquals(16f, metrics.position.x, 0.5f)
	}
}
