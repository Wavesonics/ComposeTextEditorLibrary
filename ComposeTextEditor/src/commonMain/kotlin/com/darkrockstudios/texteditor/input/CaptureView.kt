package com.darkrockstudios.texteditor.input

import androidx.compose.runtime.Composable
import com.darkrockstudios.texteditor.state.TextEditorState

/**
 * Platform-specific composable that captures the current view for IME synchronization.
 * On Android, this stores the View in the state's platformExtensions for use by IME.
 * On other platforms, this is a no-op.
 */
@Composable
expect fun CaptureViewForIme(state: TextEditorState)
