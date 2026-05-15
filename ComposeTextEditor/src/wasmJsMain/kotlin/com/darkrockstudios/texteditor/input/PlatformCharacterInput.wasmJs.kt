package com.darkrockstudios.texteditor.input

import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type

internal actual fun KeyEvent.isCharacterInputCandidate(): Boolean =
	type == KeyEventType.Unknown
