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
		if (offset.line >= getLines().size) return

		val cursorTop = calculateOffsetYPosition(offset).toInt()
		val cursorHeight = calculateLineHeight(offset)
		val viewportTop = scrollState.value
		val maxScroll = maxOf(0, totalContentHeight - viewportHeight)

		val buffer = 10
		val targetScroll = if (cursorTop < viewportTop + buffer) {
			// Scrolling up - align cursor near top
			(cursorTop - buffer).coerceIn(0, maxScroll)
		} else if (cursorTop + cursorHeight > viewportTop + viewportHeight - buffer) {
			// Scrolling down - ensure full cursor height is visible
			(cursorTop + cursorHeight - viewportHeight + buffer).coerceIn(0, maxScroll)
		} else {
			// Cursor already fully visible, maintain current scroll
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
		val cursorTop = calculateOffsetYPosition(offset).toInt()
		// Assuming cursor height is roughly the line height - we can make this more precise
		// by passing in the actual cursor height from text measurement if needed
		val cursorHeight = calculateLineHeight(offset)
		val cursorBottom = cursorTop + cursorHeight

		val viewPortTop = scrollState.value
		val viewPortBottom = viewPortTop + viewportHeight

		// Check if both top and bottom of cursor are within viewport
		val topVisible = cursorTop in viewPortTop..viewPortBottom
		val bottomVisible = cursorBottom in viewPortTop..viewPortBottom
		val cursorSpansViewport = cursorTop <= viewPortTop && cursorBottom >= viewPortBottom

		return (topVisible && bottomVisible) || cursorSpansViewport
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

	private fun calculateLineHeight(offset: TextOffset): Int {
		val lineOffsets = getLineOffsets()
		val currentLineIndex = lineOffsets.indexOfLast { lineWrap ->
			lineWrap.line == offset.line && lineWrap.wrapStartsAtIndex <= offset.char
		}

		// Find next line's Y position to calculate height
		val currentY = if (currentLineIndex >= 0) lineOffsets[currentLineIndex].offset.y else 0f
		val nextY = if (currentLineIndex + 1 < lineOffsets.size) {
			lineOffsets[currentLineIndex + 1].offset.y
		} else {
			currentY + 20f // Default height if we can't determine it
		}

		return (nextY - currentY).toInt().coerceAtLeast(1)
	}
}