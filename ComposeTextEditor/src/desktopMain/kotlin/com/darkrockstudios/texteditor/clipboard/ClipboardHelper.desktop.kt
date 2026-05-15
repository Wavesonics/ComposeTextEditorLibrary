package com.darkrockstudios.texteditor.clipboard

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.text.AnnotatedString
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.StringReader

@OptIn(ExperimentalComposeUiApi::class)
actual object ClipboardHelper {
	actual suspend fun getText(clipboard: Clipboard): AnnotatedString? {
		val transferable = clipboard.getClipEntry()?.nativeClipEntry as? Transferable ?: return null

		// 1. Same-JVM lossless fast path: if our own AnnotatedStringTransferable is
		// still on the clipboard, the original spans are intact in memory.
		if (transferable.isDataFlavorSupported(annotatedStringFlavor)) {
			(transferable.getTransferData(annotatedStringFlavor) as? AnnotatedString)?.let { return it }
		}

		// 2. HTML — survives cross-process transfers because it's just a String.
		readHtmlFlavor(transferable)?.let { return it.htmlToAnnotatedString() }

		// 3. Plain text fallback.
		if (transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
			val plain = transferable.getTransferData(DataFlavor.stringFlavor) as? String
			return plain?.let { AnnotatedString(it) }
		}
		return null
	}

	actual suspend fun setText(clipboard: Clipboard, text: AnnotatedString) {
		clipboard.setClipEntry(ClipEntry(AnnotatedStringTransferable(text)))
	}
}

// AWT's HTML flavors come in three variants for "selection", "fragment", and "all".
// Different source apps publish different ones; reads check all three.
private val htmlReadFlavors: Array<DataFlavor> = run {
	val list = mutableListOf<DataFlavor>()
	runCatching { list += DataFlavor.selectionHtmlFlavor }
	runCatching { list += DataFlavor.fragmentHtmlFlavor }
	runCatching { list += DataFlavor.allHtmlFlavor }
	list.toTypedArray()
}

private fun readHtmlFlavor(t: Transferable): String? {
	for (flavor in htmlReadFlavors) {
		if (t.isDataFlavorSupported(flavor)) {
			val data = runCatching { t.getTransferData(flavor) }.getOrNull() ?: continue
			when (data) {
				is String -> return data
				is java.io.Reader -> return runCatching { data.use { it.readText() } }.getOrNull()
				is java.io.InputStream -> return runCatching {
					data.use { it.readBytes().toString(Charsets.UTF_8) }
				}.getOrNull()
			}
		}
	}
	return null
}

// Custom flavor for the same-JVM lossless path. Mirrors the (internal)
// DataFlavor used by androidx.compose.foundation in 1.9+ so BasicTextField
// selection interops with us when both ends are in the same process.
private val annotatedStringFlavor = DataFlavor(AnnotatedString::class.java, "AnnotatedString")

private class AnnotatedStringTransferable(
	private val annotatedString: AnnotatedString,
) : Transferable {
	private val html: String by lazy { annotatedString.toHtml() }

	private val supportedFlavors: Array<DataFlavor> = run {
		val list = mutableListOf(annotatedStringFlavor, DataFlavor.stringFlavor)
		// Publish HTML on every flavor variant the OS / target app might query.
		runCatching { list += DataFlavor.selectionHtmlFlavor }
		runCatching { list += DataFlavor.fragmentHtmlFlavor }
		runCatching { list += DataFlavor.allHtmlFlavor }
		list.toTypedArray()
	}

	override fun getTransferDataFlavors(): Array<DataFlavor> = supportedFlavors

	override fun isDataFlavorSupported(flavor: DataFlavor): Boolean =
		supportedFlavors.any { it == flavor }

	override fun getTransferData(flavor: DataFlavor): Any {
		if (flavor == annotatedStringFlavor) return annotatedString
		if (flavor == DataFlavor.stringFlavor) return annotatedString.text
		// HTML flavors: AWT's html flavors expect a Reader/InputStream depending on
		// the representation class; provide whichever the flavor declares.
		if (isHtmlFlavor(flavor)) {
			return when (flavor.representationClass) {
				String::class.java -> html
				java.io.Reader::class.java -> StringReader(html)
				java.io.InputStream::class.java -> html.byteInputStream(Charsets.UTF_8)
				else -> html
			}
		}
		throw UnsupportedFlavorException(flavor)
	}

	private fun isHtmlFlavor(flavor: DataFlavor): Boolean {
		val mime = flavor.mimeType ?: return false
		return mime.startsWith("text/html")
	}
}
