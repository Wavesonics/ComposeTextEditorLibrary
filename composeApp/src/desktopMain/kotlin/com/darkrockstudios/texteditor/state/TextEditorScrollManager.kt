package com.darkrockstudios.texteditor.state

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Constraints
import com.darkrockstudios.texteditor.LineWrap
import com.darkrockstudios.texteditor.TextOffset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class TextEditorScrollManager(
	private val scope: CoroutineScope,
	private val textMeasurer: TextMeasurer,
	private val getLines: () -> List<String>,
	private val getLineOffsets: () -> List<LineWrap>,
	private val getCanvasSize: () -> Size,
	private val getViewportSize: () -> Size,
	private val getCursorPosition: () -> TextOffset,
	val scrollState: ScrollState
) {
	var totalContentHeight by mutableStateOf(0)
		private set

	data class ViewportInfo(
		val startLine: Int,
		val endLine: Int,
		val isStartPartial: Boolean,
		val isEndPartial: Boolean
	)

	fun updateContentHeight(height: Int) {
		totalContentHeight = height
	}

	fun scrollToTop() {
		scope.launch {
			scrollState.animateScrollTo(0)
		}
	}

	fun scrollToBottom() {
		scope.launch {
			scrollState.animateScrollTo(totalContentHeight)
		}
	}

	fun scrollToPosition(position: Int, animated: Boolean = true) {
		scope.launch {
			val scrollToY = position.coerceIn(0, totalContentHeight)
			if(animated) {
				scrollState.animateScrollTo(scrollToY)
			} else {
				scrollState.scrollTo(scrollToY)
			}
		}
	}

	fun scrollToPosition(offset: TextOffset) {
		if(scrollState.isScrollInProgress) return
		if (offset.line >= getLines().size) return

		val yPosition = calculateOffsetYPosition(offset)
		val viewportHeight = getViewportSize().height
		val maxScroll = maxOf(0f, totalContentHeight - viewportHeight)

		// Calculate target scroll position to show cursor near bottom of viewport
		val targetScroll = (yPosition - viewportHeight + 50f).coerceIn(0f, maxScroll)

		scope.launch {
			scrollState.animateScrollTo(targetScroll.toInt())
		}
	}

	fun scrollToCursor() {
		scrollToPosition(getCursorPosition())
	}

	fun ensureCursorVisible() {
		val cursorPos = getCursorPosition()
		if (!isOffsetVisible(cursorPos)) {
			println("cursor IS NOT visible!!!!!!!!")
			scrollToCursor()
		}
	}


	fun getViewportInfo(): ViewportInfo {
		val scrollOffset = scrollState.value.toFloat()
		val canvasWidth = getCanvasSize().width
		val viewportHeight = getViewportSize().height
		val lines = getLines()

		println("====== getViewportInfo ======")
		println("scrollOffset: $scrollOffset")
		println("viewportHeight: $viewportHeight")
		println("viewport bottom should be: ${scrollOffset + viewportHeight}")

		var currentY = 0f
		var startLine = 0
		var endLine = lines.lastIndex.coerceAtLeast(0)
		var isStartPartial = false
		var isEndPartial = false

		// Find the first visible line
		for ((index, line) in lines.withIndex()) {
			val layout = textMeasurer.measure(
				text = line,
				constraints = Constraints(maxWidth = maxOf(1, canvasWidth.toInt()))
			)
			val lineHeight = layout.size.height
			val nextY = currentY + lineHeight

			// If this line extends past the scroll offset, it's our start line
			if (nextY > scrollOffset) {
				startLine = index
				// It's partial if it starts before the viewport
				isStartPartial = currentY < scrollOffset
				println("Found start line: $index at Y: $currentY (partial=$isStartPartial)")
				break
			}
			currentY = nextY
		}

		// Reset currentY to the start line's Y position for end line calculation
		currentY = 0f
		for (index in 0..<startLine) {
			val layout = textMeasurer.measure(
				text = lines[index],
				constraints = Constraints(maxWidth = maxOf(1, canvasWidth.toInt()))
			)
			currentY += layout.size.height
		}

		// Find the last visible line
		for (index in startLine..lines.lastIndex) {
			val layout = textMeasurer.measure(
				text = lines[index],
				constraints = Constraints(maxWidth = maxOf(1, canvasWidth.toInt()))
			)
			val lineHeight = layout.size.height
			val nextY = currentY + lineHeight

			// Changed condition: if the line starts at or after viewport bottom, use previous line
			if (currentY >= scrollOffset + viewportHeight) {
				endLine = index - 1
				isEndPartial = false
				break
			}

			if (index == lines.lastIndex) {
				endLine = index
				isEndPartial = nextY > scrollOffset + viewportHeight
			}

			currentY = nextY
		}

		println("Final viewport: $startLine..$endLine (partial start=$isStartPartial, partial end=$isEndPartial)")
		return ViewportInfo(
			startLine = startLine,
			endLine = endLine,
			isStartPartial = isStartPartial,
			isEndPartial = isEndPartial
		)
	}

	private fun isOffsetVisible(offset: TextOffset): Boolean {
		val viewport = getViewportInfo()
		println("======= isOffsetVisible =======")
		println("Offset line: ${offset.line}")
		println("Viewport: start=${viewport.startLine}, end=${viewport.endLine}")
		println("isStartPartial=${viewport.isStartPartial}, isEndPartial=${viewport.isEndPartial}")

		if (offset.line < viewport.startLine || offset.line > viewport.endLine) {
			println("Not visible: Outside line bounds")
			return false
		}

		// Changed logic to always check Y position for end line
		if (offset.line == viewport.endLine) {
			val scrollOffset = scrollState.value.toFloat()
			val viewportHeight = getViewportSize().height
			val viewportBottom = scrollOffset + viewportHeight
			val offsetY = calculateOffsetYPosition(offset)

			println("End line Y check:")
			println("- offsetY: $offsetY")
			println("- scroll range: $scrollOffset..$viewportBottom")

			if (offsetY !in scrollOffset..viewportBottom) {
				println("Not visible: Y position outside viewport")
				return false
			}
		}

		println("Visible!")
		return true
	}

	private fun calculateOffsetYPosition(offset: TextOffset): Float {
		val lineOffsets = getLineOffsets()
		val wrappedLineIndex = lineOffsets.indexOfLast { lineWrap ->
			lineWrap.line == offset.line && lineWrap.wrapStartsAtIndex <= offset.char
		}

		if (wrappedLineIndex == -1) return 0f

		val wrappedLine = lineOffsets[wrappedLineIndex]
		return wrappedLine.offset.y
	}
}