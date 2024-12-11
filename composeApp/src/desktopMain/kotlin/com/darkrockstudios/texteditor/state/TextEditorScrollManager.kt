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

	private fun getViewportInfo(): ViewportInfo {
		val scrollOffset = scrollState.value
		val canvasWidth = getCanvasSize().width
		val viewportHeight = getViewportSize().height
		val lines = getLines()

		var currentY = 0f
		var startLine = 0
		var endLine = lines.lastIndex.coerceAtLeast(0)
		var isStartPartial = false
		var isEndPartial = false

		// Find start line
		for ((index, line) in lines.withIndex()) {
			val layout = textMeasurer.measure(
				text = line,
				constraints = Constraints(maxWidth = maxOf(1, canvasWidth.toInt()))
			)
			val lineHeight = layout.size.height

			if (currentY + lineHeight > scrollOffset) {
				startLine = index
				isStartPartial = currentY < scrollOffset
				break
			}
			currentY += lineHeight
		}

		// For finding end line, we need to check against viewport bottom
		val viewportBottom = scrollOffset + viewportHeight

		// Continue measuring lines until we pass the viewport bottom
		for (index in startLine..lines.lastIndex) {
			val line = lines[index]
			val layout = textMeasurer.measure(
				text = line,
				constraints = Constraints(maxWidth = maxOf(1, canvasWidth.toInt()))
			)
			val lineHeight = layout.size.height

			if (currentY >= viewportBottom) {  // Changed from currentY > scrollOffset + viewportHeight
				endLine = index - 1  // Changed to index - 1 since this line is past viewport
				isEndPartial = true
				break
			}
			currentY += lineHeight
		}

		// Ensure bounds are valid
		endLine = endLine.coerceIn(startLine, lines.lastIndex)

		return ViewportInfo(
			startLine = startLine,
			endLine = endLine,
			isStartPartial = isStartPartial,
			isEndPartial = isEndPartial
		)
	}

	private fun isOffsetVisible(offset: TextOffset): Boolean {
		val viewport = getViewportInfo()
		if (offset.line < viewport.startLine || offset.line > viewport.endLine) {
			return false
		}

		// If the line is partially visible, we need to check the specific position
		if ((viewport.isStartPartial && offset.line == viewport.startLine) ||
			(viewport.isEndPartial && offset.line == viewport.endLine)) {

			val scrollOffset = scrollState.value.toFloat()
			val viewportHeight = getViewportSize().height
			val viewportBottom = scrollOffset + viewportHeight
			val offsetY = calculateOffsetYPosition(offset)

			return offsetY in scrollOffset..viewportBottom
		}

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
