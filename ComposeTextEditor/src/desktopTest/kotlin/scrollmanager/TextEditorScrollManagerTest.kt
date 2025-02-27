package scrollmanager

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.IntSize
import com.darkrockstudios.texteditor.CharLineOffset
import com.darkrockstudios.texteditor.LineWrap
import com.darkrockstudios.texteditor.state.TextEditorScrollManager
import com.darkrockstudios.texteditor.state.TextEditorScrollState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TextEditorScrollManagerTest {
	private val testScope = TestScope()

	private fun createMockScrollState(): TextEditorScrollState {
		return mockk(relaxed = true) {
			every { value } returns 0
			every { maxValue } returns 1000
		}
	}

	private fun createMockTextLayoutResult(): TextLayoutResult {
		return mockk {
			every { size } returns IntSize(100, 20)
		}
	}

	private fun createTestLineWraps(count: Int): List<LineWrap> {
		val result = mutableListOf<LineWrap>()
		var yOffset = 0f

		repeat(count) { index ->
			result.add(
				LineWrap(
					line = index,
					wrapStartsAtIndex = 0,
					virtualLength = 10,
					virtualLineIndex = 0,
					offset = Offset(0f, yOffset),
					textLayoutResult = createMockTextLayoutResult(),
					richSpans = emptyList()
				)
			)
			yOffset += 20f // Each line is 20 units tall
		}
		return result
	}

	@Test
	fun `test scroll to cursor keeps cursor visible when in viewport`() = testScope.runTest {
		val scrollState = createMockScrollState()
		val cursorPosition = CharLineOffset(2, 5)
		val lineWraps = createTestLineWraps(5)

		val manager = TextEditorScrollManager(
			scope = testScope,
			scrollState = scrollState,
			getLines = { List(5) { AnnotatedString("Line $it") } },
			getViewportSize = { Size(100f, 60f) }, // Shows 3 lines
			getCursorPosition = { cursorPosition },
			getLineOffsets = { lineWraps }
		)
		manager.updateContentHeight(100)

		// Position cursor in middle of viewport (line 2, y=40)
		every { scrollState.value } returns 20

		manager.scrollToCursor()
		testScope.advanceUntilIdle()

		// Verify no scroll animation was triggered
		coVerify(exactly = 0) { scrollState.animateScrollTo(any()) }
	}

	@Test
	fun `test scroll to cursor scrolls up when cursor above viewport`() = testScope.runTest {
		val scrollState = createMockScrollState()
		val cursorPosition = CharLineOffset(1, 5)
		val lineWraps = createTestLineWraps(5)

		val manager = TextEditorScrollManager(
			scope = testScope,
			scrollState = scrollState,
			getLines = { List(5) { AnnotatedString("Line $it") } },
			getViewportSize = { Size(100f, 60f) }, // Shows 3 lines
			getCursorPosition = { cursorPosition },
			getLineOffsets = { lineWraps }
		)
		manager.updateContentHeight(100)

		// Position viewport below cursor
		every { scrollState.value } returns 40

		val scrollSlot = slot<Int>()
		coEvery { scrollState.animateScrollTo(capture(scrollSlot)) } returns Unit

		manager.scrollToCursor()
		testScope.advanceUntilIdle()

		// Verify scroll was triggered to proper position
		assertTrue(scrollSlot.isCaptured)
		assertEquals(10, scrollSlot.captured) // Line 1 starts at y=20, minus 10 buffer
	}

	@Test
	fun `test scroll to cursor scrolls down when cursor below viewport`() = testScope.runTest {
		val scrollState = createMockScrollState()
		val cursorPosition = CharLineOffset(4, 5)
		val lineWraps = createTestLineWraps(5)

		val manager = TextEditorScrollManager(
			scope = testScope,
			scrollState = scrollState,
			getLines = { List(5) { AnnotatedString("Line $it") } },
			getViewportSize = { Size(100f, 60f) }, // Shows 3 lines
			getCursorPosition = { cursorPosition },
			getLineOffsets = { lineWraps }
		)
		manager.updateContentHeight(100)

		// Position viewport above cursor
		coEvery { scrollState.value } returns 0

		val scrollSlot = slot<Int>()
		coEvery { scrollState.animateScrollTo(capture(scrollSlot)) } returns Unit

		manager.scrollToCursor()
		testScope.advanceUntilIdle()

		println("Cursor position line: ${cursorPosition.line}")
		println("Lines size: ${lineWraps.size}")
		println("Last line y: ${lineWraps.last().offset.y}")
		println("Cursor top: ${manager.calculateOffsetYPosition(cursorPosition)}")
		println("Cursor height: ${manager.calculateLineHeight(cursorPosition)}")
		println("Viewport height: ${manager.viewportHeight}")
		println("Total content height: ${manager.totalContentHeight}")

		// Verify scroll was triggered to proper position
		assertTrue(scrollSlot.isCaptured)
		assertEquals(
			40,
			scrollSlot.captured
		) // Line 4 ends at y=100, minus viewport height (60), plus buffer (10)
	}

	@Test
	fun `test scroll to cursor scrolls down when cursor below viewport and adjusts for overscroll`() =
		testScope.runTest {
			val scrollState = createMockScrollState()
			val cursorPosition = CharLineOffset(4, 5)
			val lineWraps = createTestLineWraps(5)

			val manager = TextEditorScrollManager(
				scope = testScope,
				scrollState = scrollState,
				getLines = { List(5) { AnnotatedString("Line $it") } },
				getViewportSize = { Size(100f, 60f) }, // Shows 3 lines
				getCursorPosition = { cursorPosition },
				getLineOffsets = { lineWraps }
			)
			manager.updateContentHeight(100)

			// Position viewport at top
			coEvery { scrollState.value } returns 0

			val scrollSlot = slot<Int>()
			coEvery { scrollState.animateScrollTo(capture(scrollSlot)) } returns Unit

			manager.scrollToCursor()
			testScope.advanceUntilIdle()

			// The raw desired scroll position would be:
			// cursorTop(80) + cursorHeight(20) - viewportHeight(60) + buffer(10) = 50
			// But this is coerced to maxScroll = totalContentHeight(100) - viewportHeight(60) = 40
			assertTrue(scrollSlot.isCaptured)
			assertEquals(40, scrollSlot.captured)
		}

	@Test
	fun `test isOffsetVisible correctly identifies visible cursor`() = testScope.runTest {
		val scrollState = createMockScrollState()
		val lineWraps = createTestLineWraps(5)

		val manager = TextEditorScrollManager(
			scope = testScope,
			scrollState = scrollState,
			getLines = { List(5) { AnnotatedString("Line $it") } },
			getViewportSize = { Size(100f, 60f) }, // Shows 3 lines
			getCursorPosition = { CharLineOffset(2, 5) },
			getLineOffsets = { lineWraps }
		)
		manager.updateContentHeight(100)

		// Test cursor in middle of viewport
		every { scrollState.value } returns 20
		assertTrue(manager.isOffsetVisible(CharLineOffset(2, 5)))

		// Test cursor above viewport
		every { scrollState.value } returns 60
		assertFalse(manager.isOffsetVisible(CharLineOffset(2, 5)))

		// Test cursor below viewport
		every { scrollState.value } returns 0
		assertFalse(manager.isOffsetVisible(CharLineOffset(4, 5)))
	}

	@Test
	fun `test scroll to bottom`() = testScope.runTest {
		val scrollState = createMockScrollState()
		val lineWraps = createTestLineWraps(5)

		val manager = TextEditorScrollManager(
			scope = testScope,
			scrollState = scrollState,
			getLines = { List(5) { AnnotatedString("Line $it") } },
			getViewportSize = { Size(100f, 60f) },
			getCursorPosition = { CharLineOffset(0, 0) },
			getLineOffsets = { lineWraps }
		)
		manager.updateContentHeight(100)

		val scrollSlot = slot<Int>()
		coEvery { scrollState.animateScrollTo(capture(scrollSlot)) } returns Unit

		manager.scrollToBottom()
		testScope.advanceUntilIdle()

		assertTrue(scrollSlot.isCaptured)
		assertEquals(40, scrollSlot.captured) // Total height (100) minus viewport height (60)
	}

	@Test
	fun `test scroll to top`() = testScope.runTest {
		val scrollState = createMockScrollState()
		val lineWraps = createTestLineWraps(5)

		val manager = TextEditorScrollManager(
			scope = testScope,
			scrollState = scrollState,
			getLines = { List(5) { AnnotatedString("Line $it") } },
			getViewportSize = { Size(100f, 60f) },
			getCursorPosition = { CharLineOffset(0, 0) },
			getLineOffsets = { lineWraps }
		)
		manager.updateContentHeight(100)

		val scrollSlot = slot<Int>()
		coEvery { scrollState.animateScrollTo(capture(scrollSlot)) } returns Unit

		manager.scrollToTop()
		testScope.advanceUntilIdle()

		assertTrue(scrollSlot.isCaptured)
		assertEquals(0, scrollSlot.captured)
	}

	@Test
	fun `test calculate offset y position`() = testScope.runTest {
		val scrollState = createMockScrollState()
		val lineWraps = createTestLineWraps(5)

		val manager = TextEditorScrollManager(
			scope = testScope,
			scrollState = scrollState,
			getLines = { List(5) { AnnotatedString("Line $it") } },
			getViewportSize = { Size(100f, 60f) },
			getCursorPosition = { CharLineOffset(0, 0) },
			getLineOffsets = { lineWraps }
		)

		assertEquals(0f, manager.calculateOffsetYPosition(CharLineOffset(0, 5)))
		assertEquals(20f, manager.calculateOffsetYPosition(CharLineOffset(1, 5)))
		assertEquals(40f, manager.calculateOffsetYPosition(CharLineOffset(2, 5)))
	}

	@Test
	fun `test calculate line height`() = testScope.runTest {
		val scrollState = createMockScrollState()
		val lineWraps = createTestLineWraps(5)

		val manager = TextEditorScrollManager(
			scope = testScope,
			scrollState = scrollState,
			getLines = { List(5) { AnnotatedString("Line $it") } },
			getViewportSize = { Size(100f, 60f) },
			getCursorPosition = { CharLineOffset(0, 0) },
			getLineOffsets = { lineWraps }
		)

		assertEquals(20, manager.calculateLineHeight(CharLineOffset(0, 5)))
		assertEquals(20, manager.calculateLineHeight(CharLineOffset(1, 5)))
		assertEquals(20, manager.calculateLineHeight(CharLineOffset(2, 5)))
	}
}