import androidx.compose.foundation.ScrollState
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import com.darkrockstudios.texteditor.LineWrap
import com.darkrockstudios.texteditor.TextOffset
import com.darkrockstudios.texteditor.state.TextEditorScrollManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestScope
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TextEditorScrollManagerTest {
	private lateinit var scope: CoroutineScope
	private lateinit var textMeasurer: TextMeasurer
	private lateinit var scrollState: ScrollState
	private lateinit var scrollManager: TextEditorScrollManager

	private var getCanvasSize: Size = Size.Zero
	private var getViewportSize: Size = Size.Zero
	private lateinit var getCursorPosition: TextOffset

	// Test data
	private val lines = listOf(
		"Line 1",  // height: 20
		"Line 2",  // height: 20
		"Line 3",  // height: 20
		"Line 4",  // height: 20
		"Line 5"   // height: 20
	)
	private var lineOffsets = mutableListOf<LineWrap>()

	@Before
	fun setup() {
		scope = TestScope()
		textMeasurer = mockk(relaxed = true)
		scrollState = spyk(ScrollState(0))

		val mockLayoutResult = mockk<TextLayoutResult>()
		every { mockLayoutResult.size } returns IntSize(100, 20)

		every {
			textMeasurer.measure(
				text = any<String>(),
				constraints = Constraints(maxWidth = 500)
			)
		} returns mockk {
			every { size } returns IntSize(100, 20)
		}

		getCanvasSize = Size(500f, 500f)
		getViewportSize = Size(500f, 60f) // Shows 3 lines at a time
		getCursorPosition = TextOffset(0, 0)

		scrollManager = TextEditorScrollManager(
			scope = scope,
			textMeasurer = textMeasurer,
			getLines = { lines },
			getLineOffsets = { emptyList() },
			getCanvasSize = { getCanvasSize },
			getViewportSize = { getViewportSize },
			getCursorPosition = { getCursorPosition },
			scrollState = scrollState
		)
	}

	@Test
	fun `isOffsetVisible - fully visible within viewport`() {
		every { scrollState.value } returns 0
		getCursorPosition = TextOffset(1, 0)

		val isVisible = scrollManager.isOffsetVisible(TextOffset(1, 0))

		assertTrue(isVisible, "Expected the cursor to be fully visible within the viewport.")
	}

	@Test
	fun `isOffsetVisible - partially visible at top`() {
		every { scrollState.value } returns 10 // Partially scrolled into line 1

		val isVisible = scrollManager.isOffsetVisible(TextOffset(0, 0))

		assertTrue(isVisible, "Expected the cursor to be partially visible at the top.")
	}

	@Test
	fun `isOffsetVisible - partially visible at bottom`() {
		every { scrollState.value } returns 40 // End of line 2 is partially visible
		getCursorPosition = TextOffset(2, 0)

		val isVisible = scrollManager.isOffsetVisible(TextOffset(2, 0))

		assertTrue(isVisible, "Expected the cursor to be partially visible at the bottom.")
	}

	@Test
	fun `isOffsetVisible - below viewport`() {
		every { scrollState.value } returns 0 // Showing lines 0-2
		getCursorPosition = TextOffset(4, 0)

		val isVisible = scrollManager.isOffsetVisible(TextOffset(4, 0))

		assertFalse(isVisible, "Expected the cursor to be outside the viewport.")
	}

	@Test
	fun `isOffsetVisible - above viewport`() {
		every { scrollState.value } returns 20 // Showing lines 1-3
		getCursorPosition = TextOffset(3, 0)

		val isVisible = scrollManager.isOffsetVisible(TextOffset(0, 0))

		assertFalse(isVisible, "Expected the cursor to be outside the viewport.")
	}

	@Test
	fun `isOffsetVisible - edge case at first line`() {
		every { scrollState.value } returns 0

		val isVisible = scrollManager.isOffsetVisible(TextOffset(0, 0))

		assertTrue(isVisible, "Expected the cursor to be visible at the very first line.")
	}

	@Test
	fun `isOffsetVisible - edge case at last visible line`() {
		every { scrollState.value } returns 0
		getCursorPosition = TextOffset(2, 0)

		val isVisible = scrollManager.isOffsetVisible(TextOffset(2, 0))

		assertTrue(isVisible, "Expected the cursor to be visible at the last visible line.")
	}

	@Test
	fun `isOffsetVisible - line wrapping`() {
		every {
			textMeasurer.measure(
				text = any<String>(),
				constraints = any()
			)
		} answers {
			val lineText = firstArg<String>()
			mockk {
				every { size } answers {
					when (lineText) {
						lines[0] -> IntSize(500, 60)
						lines[1] -> IntSize(100, 20)
						lines[2] -> IntSize(100, 20)
						lines[3] -> IntSize(100, 20)
						lines[4] -> IntSize(100, 20)
						else -> error("Unhandled line text")
					}
				}
			}
		}

		every { scrollState.value } returns 0
		getCursorPosition = TextOffset(3, 0)

		val isVisible = scrollManager.isOffsetVisible(TextOffset(2, 0))

		assertFalse(
			isVisible,
			"Expected the cursor to not be visible due to line wrapping above it."
		)
	}

//	@Test
//	fun `isOffsetVisible - large test`() {
//		every { scrollState.value } returns 60
//		getViewportSize = Size(width = 192f, height = 425f)
//		getCanvasSize = Size(192f, 6054f)
//		getCursorPosition = TextOffset(12, 7)
//
//		val isVisible = scrollManager.isOffsetVisible(getCursorPosition)
//
//		assertTrue(isVisible, "Expected the cursor to be visible at the last visible line.")
//	}
}