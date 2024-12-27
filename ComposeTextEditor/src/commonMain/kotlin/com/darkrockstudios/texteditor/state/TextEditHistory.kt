package com.darkrockstudios.texteditor.state

import androidx.compose.ui.text.AnnotatedString
import com.darkrockstudios.texteditor.richstyle.RichSpan
import com.darkrockstudios.texteditor.richstyle.RichSpanStyle

// History manager for undo/redo support
class TextEditHistory {
	private val undoStack = mutableListOf<HistoryEntry>()
	private val redoStack = mutableListOf<HistoryEntry>()

	fun hasUndoLevels(): Boolean = undoStack.isNotEmpty()
	fun hasRedoLevels(): Boolean = redoStack.isNotEmpty()

	fun recordEdit(operation: TextEditOperation, metadata: OperationMetadata) {
		undoStack.add(HistoryEntry(operation, metadata))
		redoStack.clear() // Clear redo stack when new edit is made
	}

	fun undo(): HistoryEntry? {
		return undoStack.removeLastOrNull()?.also { redoStack.add(it) }
	}

	fun redo(): HistoryEntry? {
		return redoStack.removeLastOrNull()?.also { undoStack.add(it) }
	}

	fun clear() {
		undoStack.clear()
		redoStack.clear()
	}
}

data class RelativePosition(
	val lineDiff: Int,
	val char: Int
)

data class PreservedRichSpan(
	val relativeStart: RelativePosition,
	val relativeEnd: RelativePosition,
	val style: RichSpanStyle
)

data class OperationMetadata(
	val deletedText: AnnotatedString? = null,
	val deletedSpans: List<RichSpan> = emptyList(),
	val preservedRichSpans: List<PreservedRichSpan> = emptyList(),
)

data class HistoryEntry(
	val operation: TextEditOperation,
	val metadata: OperationMetadata
)