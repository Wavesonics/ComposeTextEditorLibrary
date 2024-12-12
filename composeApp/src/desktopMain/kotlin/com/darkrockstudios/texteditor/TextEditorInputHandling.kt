package com.darkrockstudios.texteditor

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Constraints
import com.darkrockstudios.texteditor.state.TextEditorState
import com.darkrockstudios.texteditor.state.moveCursorDown
import com.darkrockstudios.texteditor.state.moveCursorLeft
import com.darkrockstudios.texteditor.state.moveCursorRight
import com.darkrockstudios.texteditor.state.moveCursorToLineEnd
import com.darkrockstudios.texteditor.state.moveCursorToLineStart
import com.darkrockstudios.texteditor.state.moveCursorUp
import kotlin.math.min

fun Modifier.textEditorKeyboardInputHandler(state: TextEditorState): Modifier {
	return this.onPreviewKeyEvent {
		if (it.type == KeyEventType.KeyDown) {
			return@onPreviewKeyEvent when (it.key) {
				Key.Delete -> {
					state.deleteAtCursor()
					true
				}

				Key.Backspace -> {
					state.backspaceAtCursor()
					true
				}

				Key.Enter -> {
					state.insertNewlineAtCursor()
					true
				}

				Key.DirectionLeft -> {
					state.moveCursorLeft()
					true
				}

				Key.DirectionRight -> {
					state.moveCursorRight()
					true
				}

				Key.DirectionUp -> {
					state.moveCursorUp()
					true
				}

				Key.DirectionDown -> {
					state.moveCursorDown()
					true
				}

				Key.MoveHome -> {
					state.moveCursorToLineStart()
					true
				}

				Key.MoveEnd -> {
					state.moveCursorToLineEnd()
					true
				}

				else -> {
					val char = keyEventToUTF8Character(it)
					if (char != null) {
						state.insertCharacterAtCursor(char)
						true
					} else {
						false
					}
				}
			}
		} else {
			false
		}
	}
}

private fun isVisibleCharacter(char: Char): Boolean {
	return char.isLetterOrDigit() || char.isWhitespace() || char in "!\"#\$%&'()*+,-./:;<=>?@[\\]^_`{|}~"
}

private fun keyEventToUTF8Character(keyEvent: KeyEvent): Char? {
	return if (keyEvent.type == KeyEventType.KeyDown) {
		val keyCode = keyEvent.utf16CodePoint
		if (keyCode in Char.MIN_VALUE.code..Char.MAX_VALUE.code) {
			val newChar = keyCode.toChar()
			if (isVisibleCharacter(newChar)) {
				newChar
			} else {
				null
			}
		} else {
			null
		}
	} else {
		null
	}
}

internal fun Modifier.textEditorPointerInputHandling(
	state: TextEditorState,
	textMeasurer: TextMeasurer,
): Modifier {
	return this.pointerInput(Unit) {
		detectTapGestures { tapOffset ->
			state.apply {
				if (lineOffsets.isEmpty()) return@detectTapGestures

				var curRealLine: LineWrap = lineOffsets[0]
				// Calculate the clicked line and character within the wrapped text
				for (lineWrap in lineOffsets) {
					if (lineWrap.line != curRealLine.line) {
						curRealLine = lineWrap
					}

					val textLayoutResult = textMeasurer.measure(
						textLines[lineWrap.line],
						constraints = Constraints(maxWidth = size.width)
					)

					val relativeTapOffset = tapOffset - curRealLine.offset
					if (tapOffset.y in curRealLine.offset.y..(curRealLine.offset.y + textLayoutResult.size.height)) {
						val charPos =
							textLayoutResult.multiParagraph.getOffsetForPosition(relativeTapOffset)
						cursorPosition =
							TextOffset(lineWrap.line, min(charPos, textLines[lineWrap.line].length))
						break
					}
				}
			}
		}
	}
}