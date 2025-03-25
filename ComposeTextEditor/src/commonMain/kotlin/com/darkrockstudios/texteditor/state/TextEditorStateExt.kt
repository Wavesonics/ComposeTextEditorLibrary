package com.darkrockstudios.texteditor.state

import androidx.compose.ui.text.AnnotatedString

fun TextEditorState.insertTypedCharacter(char: Char) {
	if (selector.selection != null) {
		selector.deleteSelection()
	}
	insertCharacterAtCursor(char)
	selector.clearSelection()
}

fun TextEditorState.insertTypedString(string: String) {
	if (selector.selection != null) {
		selector.deleteSelection()
	}
	insertStringAtCursor(string)
	selector.clearSelection()
}

fun TextEditorState.insertTypedString(string: AnnotatedString) {
	if (selector.selection != null) {
		selector.deleteSelection()
	}
	insertStringAtCursor(string)
	selector.clearSelection()
}