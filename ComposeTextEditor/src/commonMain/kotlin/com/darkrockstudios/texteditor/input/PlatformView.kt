package com.darkrockstudios.texteditor.input

import androidx.compose.ui.Modifier

/**
 * Platform-specific helper to get the underlying view from a modifier node.
 * On Android, returns the Android View.
 * On other platforms, returns null.
 */
expect fun Modifier.Node.getPlatformView(): Any?
