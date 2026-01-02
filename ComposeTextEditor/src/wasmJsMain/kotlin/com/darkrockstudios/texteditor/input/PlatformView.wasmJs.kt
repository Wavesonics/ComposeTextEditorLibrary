package com.darkrockstudios.texteditor.input

import androidx.compose.ui.Modifier

/**
 * WASM implementation - returns null as there's no Android View on web.
 */
actual fun Modifier.Node.getPlatformView(): Any? = null
