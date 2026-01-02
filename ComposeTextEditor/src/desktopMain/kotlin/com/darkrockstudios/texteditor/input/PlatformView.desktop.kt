package com.darkrockstudios.texteditor.input

import androidx.compose.ui.Modifier

/**
 * Desktop implementation - returns null as there's no Android View on desktop.
 */
actual fun Modifier.Node.getPlatformView(): Any? = null
