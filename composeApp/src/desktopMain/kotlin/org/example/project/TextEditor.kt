package org.example.project

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min

data class TextOffset(
    val line: Int,
    val char: Int,
)

data class LineWrap(
    val line: Int,
    // The character index of the first character on this line
    val wrapStartsAtIndex: Int,
    val offset: Offset,
)

@Composable
fun TextEditor(modifier: Modifier = Modifier) {
    val focusRequester = remember { FocusRequester() }
    val textMeasurer = rememberTextMeasurer()
    val interactionSource = remember { MutableInteractionSource() }
    var isFocused by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    var textLines by remember { mutableStateOf(mutableListOf<String>("")) }
    var cursorPosition by remember {
        mutableStateOf(
            TextOffset(0, textLines[0].length)
        )
    }
    var isCursorVisible by remember { mutableStateOf(true) }

    var lineOffsets by remember { mutableStateOf(emptyList<LineWrap>()) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(isFocused) {
        scope.launch {
            while (isFocused) {
                isCursorVisible = !isCursorVisible
                delay(750)
            }
        }
    }

    LaunchedEffect(Unit) {
        textLines.clear()
        textLines.addAll(
            "test ssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssss\nxxxxxxxxxxxxxxxxxx"
                .split("\n")
        )
        textLines = textLines
    }

    Box(
        modifier = modifier
            .border(width = 2.dp, color = if (isFocused) Color.Green else Color.Blue)
            .padding(8.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
            }
            .focusable(enabled = true, interactionSource = interactionSource)
            .pointerInput(Unit) {
                detectTapGestures { tapOffset ->

                    if(lineOffsets.isEmpty()) return@detectTapGestures

                    var curRealLine: LineWrap = lineOffsets[0]
                    // Calculate the clicked line and character within the wrapped text
                    for (lineWrap in lineOffsets) {
                        if(lineWrap.line != curRealLine.line) {
                            curRealLine = lineWrap
                        }

                        val textLayoutResult = textMeasurer.measure(
                            textLines[lineWrap.line],
                            constraints = Constraints(maxWidth = size.width)
                        )

                        val relativeTapOffset = tapOffset - curRealLine.offset
                        if (tapOffset.y in curRealLine.offset.y ..(curRealLine.offset.y + textLayoutResult.size.height)) {
                            val charPos = textLayoutResult.multiParagraph.getOffsetForPosition(relativeTapOffset)
                            cursorPosition = TextOffset(lineWrap.line, min(charPos, textLines[lineWrap.line].length))
                            break
                        }
                    }
                }
            }
            .onPreviewKeyEvent {
                if (it.type == KeyEventType.KeyDown) {
                    val (line, charIndex) = cursorPosition
                    return@onPreviewKeyEvent when (it.key) {
                        Key.Backspace -> {
                            if (charIndex > 0) {
                                textLines = textLines.apply {
                                    this[line] =
                                        this[line].substring(
                                            0,
                                            charIndex - 1
                                        ) + this[line].substring(charIndex)
                                }
                                cursorPosition = cursorPosition.copy(char = charIndex - 1)
                            } else if (line > 0) {
                                // Merge with previous line
                                val previousLineLength = textLines[line - 1].length
                                textLines = textLines.apply {
                                    this[line - 1] += this[line]
                                    removeAt(line)
                                }
                                cursorPosition = TextOffset(line - 1, previousLineLength)
                            }
                            true
                        }

                        Key.Delete -> {
                            if (charIndex < textLines[line].length) {
                                textLines = textLines.apply {
                                    this[line] =
                                        this[line].substring(0, charIndex) + this[line].substring(
                                            charIndex + 1
                                        )
                                }
                            } else if (line < textLines.size - 1) {
                                // Merge with next line
                                textLines = textLines.apply {
                                    this[line] += this[line + 1]
                                    removeAt(line + 1)
                                }
                            }
                            true
                        }

                        Key.DirectionLeft -> {
                            if (charIndex > 0) {
                                cursorPosition = cursorPosition.copy(char = charIndex - 1)
                            } else if (line > 0) {
                                cursorPosition = TextOffset(line - 1, textLines[line - 1].length)
                            }
                            true
                        }

                        Key.DirectionRight -> {
                            if (charIndex < textLines[line].length) {
                                cursorPosition = cursorPosition.copy(char = charIndex + 1)
                            } else if (line < textLines.size - 1) {
                                cursorPosition = TextOffset(line + 1, 0)
                            }
                            true
                        }

                        Key.DirectionUp -> {
                            val currentWrappedIndex =
                                lineOffsets.getWrappedLineIndex(cursorPosition)
                            if (currentWrappedIndex > 0) {
                                val curWrappedSegment = lineOffsets[currentWrappedIndex]
                                val previousWrappedSegment = lineOffsets[currentWrappedIndex - 1]
                                val localCharIndex = charIndex - curWrappedSegment.wrapStartsAtIndex
                                val newCharIndex = min(
                                    textLines[previousWrappedSegment.line].length,
                                    previousWrappedSegment.wrapStartsAtIndex + localCharIndex
                                )

                                cursorPosition = cursorPosition.copy(
                                    line = previousWrappedSegment.line,
                                    char = newCharIndex
                                )
                            }
                            true
                        }

                        Key.DirectionDown -> {
                            val currentWrappedIndex =
                                lineOffsets.getWrappedLineIndex(cursorPosition)
                            if (currentWrappedIndex < lineOffsets.size - 1) {
                                val curWrappedSegment = lineOffsets[currentWrappedIndex]
                                val nextWrappedSegment = lineOffsets[currentWrappedIndex + 1]
                                val localCharIndex = charIndex - curWrappedSegment.wrapStartsAtIndex

                                val newCharIndex = min(
                                    textLines[nextWrappedSegment.line].length,
                                    nextWrappedSegment.wrapStartsAtIndex + localCharIndex
                                )

                                cursorPosition = cursorPosition.copy(
                                    line = nextWrappedSegment.line,
                                    char = newCharIndex
                                )
                            }
                            true
                        }

                        Key.MoveEnd -> {
                            val currentWrappedLineIndex =
                                lineOffsets.getWrappedLineIndex(cursorPosition)
                            val currentWrappedLine = lineOffsets[currentWrappedLineIndex]
                            cursorPosition = if (currentWrappedLineIndex < lineOffsets.size - 1) {
                                val nextWrappedLine = lineOffsets[currentWrappedLineIndex + 1]
                                if (nextWrappedLine.line == currentWrappedLine.line) {
                                    // Go to the end of this virtual line
                                    cursorPosition.copy(char = nextWrappedLine.wrapStartsAtIndex - 1)
                                } else {
                                    // Go to the end of this real line
                                    cursorPosition.copy(char = textLines[line].length)
                                }
                            } else {
                                cursorPosition.copy(char = textLines[line].length)
                            }
                            true
                        }

                        Key.MoveHome -> {
                            val currentWrappedLine = lineOffsets.getWrappedLine(cursorPosition)
                            cursorPosition =
                                cursorPosition.copy(char = currentWrappedLine.wrapStartsAtIndex)
                            true
                        }

                        Key.Enter -> {
                            textLines = textLines.apply {
                                val newLine = this[line].substring(charIndex)
                                this[line] = this[line].substring(0, charIndex)
                                add(line + 1, newLine)
                            }
                            cursorPosition = TextOffset(line + 1, 0)
                            true
                        }

                        else -> {
                            val char = keyEventToUTF8Character(it)
                            if (char != null) {
                                textLines.apply {
                                    this[line] =
                                        this[line].substring(
                                            0,
                                            charIndex
                                        ) + char + this[line].substring(charIndex)
                                }
                                cursorPosition = cursorPosition.copy(char = charIndex + 1)
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
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val offsets = mutableListOf<LineWrap>()
            var currentY = 0f
            textLines.forEachIndexed { lineIndex, line ->
                val textLayoutResult = textMeasurer.measure(
                    line,
                    constraints = Constraints(maxWidth = size.width.toInt())
                )

                drawText(
                    textMeasurer,
                    line,
                    topLeft = Offset(0f, currentY)
                )

                for (virtualLineIndex in 0 until textLayoutResult.multiParagraph.lineCount) {
                    val lineTop = currentY
                    val lineOffset = Offset(0f, lineTop)

                    val lineWrapsAt = if (virtualLineIndex == 0) {
                        0
                    } else {
                        textLayoutResult.getLineEnd(virtualLineIndex - 1, visibleEnd = true) + 1
                    }

                    offsets.add(
                        LineWrap(
                            line = lineIndex,
                            wrapStartsAtIndex = lineWrapsAt,
                            offset = lineOffset,
                        )
                    )
                    currentY += textLayoutResult.multiParagraph.getLineHeight(virtualLineIndex)
                }
            }
            lineOffsets = offsets
            if (isFocused && isCursorVisible) {
                drawCursor(textMeasurer, textLines, cursorPosition, lineOffsets)
            }
        }
    }
}

private fun List<LineWrap>.getWrappedLineIndex(position: TextOffset): Int {
    return indexOfLast { lineOffset ->
        lineOffset.line == position.line && lineOffset.wrapStartsAtIndex <= position.char
    }
}

private fun List<LineWrap>.getWrappedLine(position: TextOffset): LineWrap {
    return last { lineOffset ->
        lineOffset.line == position.line && lineOffset.wrapStartsAtIndex <= position.char
    }
}

private fun DrawScope.drawCursor(
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    text: List<String>,
    cursorPosition: TextOffset,
    lineOffsets: List<LineWrap>
) {
    val (line, charIndex) = cursorPosition
    val layout = textMeasurer.measure(
        text[line],
        constraints = Constraints(maxWidth = size.width.toInt())
    )

    // Putting the cursor at the end of a virtual line being line-wrapped, makes the cursor
    // render at the start of the next virtual line... So that's a bug.


    val currentWrappedLineIndex = lineOffsets.getWrappedLineIndex(cursorPosition)
    val currentWrappedLine = lineOffsets[currentWrappedLineIndex]
    val startOfLineOffset = lineOffsets.first { it.line == currentWrappedLine.line }.offset

    val currentLine = layout.multiParagraph.getLineForOffset(charIndex)
    val cursorOffsetX = layout.multiParagraph.getHorizontalPosition(charIndex, true)
    val cursorOffsetY = startOfLineOffset.y + layout.multiParagraph.getLineTop(currentLine)
    val lineHeight = layout.multiParagraph.getLineHeight(currentLine)

    //val cursorRect = layout.multiParagraph.getCursorRect(cursorPosition.char)

    drawLine(
        color = Color.Black,
        start = Offset(cursorOffsetX, cursorOffsetY),
        end = Offset(cursorOffsetX, cursorOffsetY + lineHeight),
        strokeWidth = 2f
    )
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