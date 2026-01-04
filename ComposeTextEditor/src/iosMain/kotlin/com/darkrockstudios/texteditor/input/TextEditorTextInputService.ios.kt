package com.darkrockstudios.texteditor.input

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.platform.PlatformTextInputSession
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TextEditingScope
import androidx.compose.ui.text.input.CommitTextCommand
import androidx.compose.ui.text.input.DeleteSurroundingTextCommand
import androidx.compose.ui.text.input.SetSelectionCommand
import androidx.compose.ui.text.input.BackspaceCommand
import androidx.compose.ui.text.TextLayoutResult
import com.darkrockstudios.texteditor.TextEditorRange
import com.darkrockstudios.texteditor.state.TextEditorState

/**
 * iOS implementation of TextEditorTextInputService.
 * This creates a PlatformTextInputMethodRequest that bridges iOS's UITextInput
 * to the TextEditorState.
 */
actual class TextEditorTextInputService actual constructor(
	private val state: TextEditorState
) {
	actual suspend fun startInput(session: PlatformTextInputSession): Nothing {
		val request = TextEditorIOSInputMethodRequest(state)
		session.startInputMethod(request)
	}
}

/**
 * Bridge implementation of Compose framework's TextEditorState interface
 * that delegates to our custom TextEditorState.
 */
@OptIn(ExperimentalComposeUiApi::class)
private class TextEditorStateBridge(
	private val editorState: com.darkrockstudios.texteditor.state.TextEditorState
) : androidx.compose.ui.text.input.TextEditorState {

	// CharSequence implementation - delegate to our editor state's text
	override val length: Int
		get() = editorState.getTextLength()

	override fun get(index: Int): Char {
		val text = editorState.getAllText()
		return text[index]
	}

	override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
		return editorState.getAllText().subSequence(startIndex, endIndex)
	}

	override fun toString(): String {
		return editorState.getAllText().toString()
	}

	// Selection range - map from our custom state
	override val selection: TextRange
		get() {
			val selection = editorState.selector.selection
			return if (selection != null) {
				val start = editorState.getCharacterIndex(selection.start)
				val end = editorState.getCharacterIndex(selection.end)
				TextRange(start, end)
			} else {
				val cursorPos = editorState.getCharacterIndex(editorState.cursorPosition)
				TextRange(cursorPos, cursorPos)
			}
		}

	// Composition range - map from our custom state's composing range
	override val composition: TextRange?
		get() {
			val composing = editorState.composingRange
			return if (composing != null) {
				val start = editorState.getCharacterIndex(composing.start)
				val end = editorState.getCharacterIndex(composing.end)
				TextRange(start, end)
			} else {
				null
			}
		}
}

/**
 * iOS-specific implementation of PlatformTextInputMethodRequest.
 * Bridges UITextInput to the custom TextEditorState.
 */
@OptIn(ExperimentalComposeUiApi::class)
private class TextEditorIOSInputMethodRequest(
	private val editorState: com.darkrockstudios.texteditor.state.TextEditorState
) : PlatformTextInputMethodRequest {

	// Bridge to Compose framework's TextEditorState
	override val state: androidx.compose.ui.text.input.TextEditorState =
		TextEditorStateBridge(editorState)

	override val imeOptions: ImeOptions = ImeOptions(
		keyboardType = KeyboardType.Text,
		imeAction = ImeAction.Default,
		singleLine = false
	)

	// Provide current text field value to iOS
	override val value: () -> TextFieldValue = {
		val text = editorState.getAllText()
		val cursorPos = editorState.getCharacterIndex(editorState.cursorPosition)
		val selection = editorState.selector.selection

		if (selection != null) {
			val start = editorState.getCharacterIndex(selection.start)
			val end = editorState.getCharacterIndex(selection.end)
			TextFieldValue(text, TextRange(start, end))
		} else {
			TextFieldValue(text, TextRange(cursorPos, cursorPos))
		}
	}

	// Handle edit commands from iOS IME
	override val onEditCommand: (List<EditCommand>) -> Unit = { commands ->
		for (command in commands) {
			when (command) {
				is CommitTextCommand -> {
					// Delete selection if present
					if (editorState.selector.hasSelection()) {
						editorState.selector.deleteSelection()
					}
					// Insert text at cursor
					editorState.insertStringAtCursor(command.text)
				}

				is DeleteSurroundingTextCommand -> {
					val cursorIndex = editorState.getCharacterIndex(editorState.cursorPosition)
					val deleteStart = maxOf(0, cursorIndex - command.lengthBeforeCursor)
					val deleteEnd = minOf(editorState.getTextLength(), cursorIndex + command.lengthAfterCursor)

					if (deleteStart < deleteEnd) {
						val startOffset = editorState.getOffsetAtCharacter(deleteStart)
						val endOffset = editorState.getOffsetAtCharacter(deleteEnd)
						val range = TextEditorRange(startOffset, endOffset)
						editorState.delete(range)
					}
				}

				is SetSelectionCommand -> {
					val start = command.start.coerceIn(0, editorState.getTextLength())
					val end = command.end.coerceIn(0, editorState.getTextLength())

					if (start == end) {
						// Just cursor
						val offset = editorState.getOffsetAtCharacter(start)
						editorState.cursor.updatePosition(offset)
						editorState.selector.clearSelection()
					} else {
						// Selection
						val startOffset = editorState.getOffsetAtCharacter(start)
						val endOffset = editorState.getOffsetAtCharacter(end)
						editorState.selector.updateSelection(startOffset, endOffset)
						editorState.cursor.updatePosition(endOffset)
					}
				}

				is BackspaceCommand -> {
					if (editorState.selector.hasSelection()) {
						editorState.selector.deleteSelection()
					} else {
						editorState.backspaceAtCursor()
					}
				}

				// Handle other edit commands as needed
				else -> {
					// Log or handle unknown commands
				}
			}
		}
	}

	override val onImeAction: ((ImeAction) -> Unit)? = null

	// No text layout available for now
	override val textLayoutResult: () -> TextLayoutResult? = { null }

	// Placeholder rects - iOS may not use these for custom text editors
	override val focusedRectInRoot: () -> Rect? = { null }
	override val textFieldRectInRoot: () -> Rect? = { null }
	override val textClippingRectInRoot: () -> Rect? = { null }

	// Edit text scope - allows iOS to apply edits
	override val editText: (TextEditingScope.() -> Unit) -> Unit = { block ->
		val currentValue = value()
		val scope = object : TextEditingScope {
			var value: TextFieldValue = currentValue

			override fun deleteSurroundingTextInCodePoints(lengthBeforeCursor: Int, lengthAfterCursor: Int) {
				// Not implemented for now
			}

			override fun commitText(text: CharSequence, newCursorPosition: Int) {
				// Insert text at cursor
				val currentVal = value
				val newText = currentVal.text.replaceRange(
					currentVal.selection.start,
					currentVal.selection.end,
					text.toString()
				)
				val newCursor = currentVal.selection.start + text.length
				value = TextFieldValue(newText, TextRange(newCursor, newCursor))
			}

			override fun setComposingText(text: CharSequence, newCursorPosition: Int) {
				// For now, treat like commit
				commitText(text, newCursorPosition)
			}

			override fun finishComposingText() {
				// No-op for now
			}
		}
		scope.block()
		// Apply the changes from scope back to editorState if needed
		val newValue = scope.value
		if (newValue.text != currentValue.text || newValue.selection != currentValue.selection) {
			applyTextFieldValue(newValue)
		}
	}

	private fun applyTextFieldValue(newValue: TextFieldValue) {
		val currentText = editorState.getAllText()
		val newText = newValue.text

		// Compare as CharSequence since both are CharSequence types
		if (!currentText.contentEquals(newText)) {
			// Replace all text (simple approach for now)
			val allRange = TextEditorRange(
				editorState.getOffsetAtCharacter(0),
				editorState.getOffsetAtCharacter(currentText.length)
			)
			// Pass AnnotatedString directly - replace() has an overload for it
			editorState.replace(allRange, newText)
		}

		// Update cursor/selection
		val selStart = newValue.selection.start
		val selEnd = newValue.selection.end

		if (selStart == selEnd) {
			val offset = editorState.getOffsetAtCharacter(selStart.coerceIn(0, newText.length))
			editorState.cursor.updatePosition(offset)
			editorState.selector.clearSelection()
		} else {
			val startOffset = editorState.getOffsetAtCharacter(selStart.coerceIn(0, newText.length))
			val endOffset = editorState.getOffsetAtCharacter(selEnd.coerceIn(0, newText.length))
			editorState.selector.updateSelection(startOffset, endOffset)
			editorState.cursor.updatePosition(endOffset)
		}
	}
}
