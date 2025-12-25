package com.darkrockstudios.texteditor.clipboard

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.text.AnnotatedString
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException

@OptIn(ExperimentalComposeUiApi::class)
actual object ClipboardHelper {
	// Custom DataFlavor for AnnotatedString
	private val annotatedStringFlavor = DataFlavor(AnnotatedString::class.java, "AnnotatedString")

	actual suspend fun getText(clipboard: Clipboard): AnnotatedString? {
		val transferable = clipboard.getClipEntry()?.nativeClipEntry as? Transferable
		return transferable?.let {
			// Try AnnotatedString first, fall back to plain text
			when {
				it.isDataFlavorSupported(annotatedStringFlavor) ->
					it.getTransferData(annotatedStringFlavor) as? AnnotatedString

				it.isDataFlavorSupported(DataFlavor.stringFlavor) ->
					AnnotatedString(it.getTransferData(DataFlavor.stringFlavor) as String)

				else -> null
			}
		}
	}

	actual suspend fun setText(clipboard: Clipboard, text: AnnotatedString) {
		val transferable = AnnotatedStringTransferable(text)
		clipboard.setClipEntry(ClipEntry(transferable))
	}
}

/**
 * A Transferable that supports both AnnotatedString and plain String data flavors.
 * This allows copying styled text within the editor while also supporting
 * pasting as plain text in external applications.
 */
private class AnnotatedStringTransferable(
	private val annotatedString: AnnotatedString
) : Transferable {

	private val annotatedStringFlavor = DataFlavor(AnnotatedString::class.java, "AnnotatedString")

	override fun getTransferDataFlavors(): Array<DataFlavor> {
		return arrayOf(annotatedStringFlavor, DataFlavor.stringFlavor)
	}

	override fun isDataFlavorSupported(flavor: DataFlavor): Boolean {
		return flavor == annotatedStringFlavor || flavor == DataFlavor.stringFlavor
	}

	override fun getTransferData(flavor: DataFlavor): Any {
		return when {
			flavor == annotatedStringFlavor -> annotatedString
			flavor == DataFlavor.stringFlavor -> annotatedString.text
			else -> throw UnsupportedFlavorException(flavor)
		}
	}
}
