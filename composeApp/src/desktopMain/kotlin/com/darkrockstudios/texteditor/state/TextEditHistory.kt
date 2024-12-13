package com.darkrockstudios.texteditor.state

// History manager for undo/redo support
class TextEditHistory {
	private val undoStack = mutableListOf<TextEditOperation>()
	private val redoStack = mutableListOf<TextEditOperation>()

	fun recordEdit(operation: TextEditOperation) {
		undoStack.add(operation)
		redoStack.clear() // Clear redo stack when new edit is made
	}

	fun undo(): TextEditOperation? {
		return undoStack.removeLastOrNull()?.also { redoStack.add(it) }
	}

	fun redo(): TextEditOperation? {
		return redoStack.removeLastOrNull()?.also { undoStack.add(it) }
	}

	fun clear() {
		undoStack.clear()
		redoStack.clear()
	}
}