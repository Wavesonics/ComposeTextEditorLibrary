package com.darkrockstudios.texteditor.state

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import com.darkrockstudios.texteditor.LineWrap
import com.darkrockstudios.texteditor.TextOffset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class TextEditorScrollManager(
	private val scope: CoroutineScope,
	private val getLines: () -> List<String>,
	private val getLineOffsets: () -> List<LineWrap>,
	private val getViewportSize: () -> Size,
	private val getCursorPosition: () -> TextOffset,
	val scrollState: ScrollState
) {
	var totalContentHeight by mutableStateOf(0)
		private set

	val viewportHeight: Int
		get() = getViewportSize().height.toInt()

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
			scrollState.animateScrollTo(totalContentHeight - viewportHeight)
		}
	}

	fun scrollToPosition(position: Int, animated: Boolean = true) {
		scope.launch {
			val scrollToY = position.coerceIn(0, totalContentHeight)
			if (animated) {
				scrollState.animateScrollTo(scrollToY)
			} else {
				scrollState.scrollTo(scrollToY)
			}
		}
	}

	fun scrollToPosition(offset: TextOffset) {
		if (scrollState.isScrollInProgress) return
		if (offset.line >= getLines().size) return

		val offsetY = calculateOffsetYPosition(offset).toInt()
		val viewportTop = scrollState.value
		val maxScroll = maxOf(0, totalContentHeight - viewportHeight)

		val buffer = 10
		val targetScroll = if (offsetY < viewportTop) {
			// Scrolling up - align cursor near top
			(offsetY - buffer).coerceIn(0, maxScroll)
		} else if (offsetY > viewportTop + viewportHeight) {
			// Scrolling down - align cursor near bottom
			(offsetY - viewportHeight + buffer).coerceIn(0, maxScroll)
		} else {
			// Cursor already visible, maintain current scroll
			viewportTop
		}

		if(targetScroll != viewportTop) {
			scope.launch {
				scrollState.animateScrollTo(targetScroll)
			}
		}
	}

	fun scrollToCursor() {
		scrollToPosition(getCursorPosition())
	}

	fun ensureCursorVisible() {
		val cursorPos = getCursorPosition()
		if (!isOffsetVisible(cursorPos)) {
			scrollToCursor()
		}
	}

	fun isOffsetVisible(offset: TextOffset): Boolean {
		val cursorY = calculateOffsetYPosition(offset).toInt()
		val viewPortTop = scrollState.value
		val viewPortBottom = viewPortTop + getViewportSize().height.toInt()

		val isVisible = cursorY in viewPortTop..viewPortBottom

		return isVisible
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