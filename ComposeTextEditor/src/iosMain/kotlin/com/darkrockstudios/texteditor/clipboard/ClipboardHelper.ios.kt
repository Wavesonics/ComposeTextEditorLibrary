package com.darkrockstudios.texteditor.clipboard

import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.text.AnnotatedString
import platform.UIKit.UIPasteboard

/**
 * iOS implementation of ClipboardHelper using UIPasteboard.
 * Supports plain text only (styled text is not preserved).
 */
actual object ClipboardHelper {
	actual suspend fun getText(clipboard: Clipboard): AnnotatedString? {
		return UIPasteboard.generalPasteboard.string?.let { text ->
			AnnotatedString(text)
		}
	}

	actual suspend fun setText(clipboard: Clipboard, text: AnnotatedString) {
		UIPasteboard.generalPasteboard.string = text.text
	}
}
