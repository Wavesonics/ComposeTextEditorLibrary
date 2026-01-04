package com.darkrockstudios.texteditor.find

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.darkrockstudios.texteditor.state.TextEditorState

/**
 * Creates and remembers a FindState for the given TextEditorState.
 *
 * @param textState The TextEditorState to search within
 * @return A FindState that manages find functionality
 */
@Composable
fun rememberFindState(textState: TextEditorState): FindState {
	val scope = rememberCoroutineScope()
	val findState = remember(textState) {
		FindState(textState, scope)
	}

	DisposableEffect(findState) {
		onDispose {
			findState.dispose()
		}
	}

	return findState
}
