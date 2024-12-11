import androidx.compose.foundation.ScrollState
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
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

class TextEditorScrollManagerTest {
	private lateinit var scope: CoroutineScope
	private lateinit var textMeasurer: TextMeasurer
	private lateinit var scrollState: ScrollState
	private lateinit var scrollManager: TextEditorScrollManager

	// Test data
	private val lines = listOf(
		"Line 1",  // height: 20
		"Line 2",  // height: 20
		"Line 3",  // height: 20
		"Line 4",  // height: 20
		"Line 5"   // height: 20
	)

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

		scrollManager = TextEditorScrollManager(
			scope = scope,
			textMeasurer = textMeasurer,
			getLines = { lines },
			getLineOffsets = { emptyList() },
			getCanvasSize = { Size(500f, 500f) },
			getViewportSize = { Size(500f, 60f) }, // Shows 3 lines at a time
			getCursorPosition = { TextOffset(0, 0) },
			scrollState = scrollState
		)
	}

	@Test
	fun `getViewportInfo - when scrolled to top`() {
		// Set scroll position to top
		scrollState = ScrollState(0)

		val viewportInfo = scrollManager.getViewportInfo()

		assertEquals(0, viewportInfo.startLine)
		assertEquals(2, viewportInfo.endLine) // Should show first 3 lines
		assertEquals(false, viewportInfo.isStartPartial)
		assertEquals(false, viewportInfo.isEndPartial)
	}

	@Test
	fun `getViewportInfo - when scrolled to middle`() {
		// Set scroll to middle (40 pixels down, starting at line 3)
		every { scrollState.value } returns 40

		val viewportInfo = scrollManager.getViewportInfo()

		assertEquals(2, viewportInfo.startLine)
		assertEquals(4, viewportInfo.endLine)
		assertEquals(false, viewportInfo.isStartPartial)
		assertEquals(false, viewportInfo.isEndPartial)
	}

	@Test
	fun `getViewportInfo - when scrolled to middle with `() {
		// Set scroll to middle (39 pixels down, starting at line 3)
		every { scrollState.value } returns 39

		val viewportInfo = scrollManager.getViewportInfo()

		assertEquals(1, viewportInfo.startLine)
		assertEquals(4, viewportInfo.endLine)
		assertEquals(true, viewportInfo.isStartPartial)
		assertEquals(true, viewportInfo.isEndPartial)
	}

	@Test
	fun `getViewportInfo - when scrolled to bottom`() {
		// Set scroll to bottom (80 pixels down)
		every { scrollState.value } returns 80

		val viewportInfo = scrollManager.getViewportInfo()

		assertEquals(4, viewportInfo.startLine)
		assertEquals(4, viewportInfo.endLine) // Last line
		assertEquals(false, viewportInfo.isStartPartial)
		assertEquals(false, viewportInfo.isEndPartial)
	}
}