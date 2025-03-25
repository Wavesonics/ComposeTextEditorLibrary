package com.darkrockstudios.texteditor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
internal fun InputServiceEffect(
	onStart: () -> Unit,
	onDispose: () -> Unit,
	key: Any? = null,
	lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
) {
	val lifecycleObserver = remember {
		LifecycleEventObserver { _, event ->
			if (event == Lifecycle.Event.ON_START) {
				onStart()
			}
		}
	}

	DisposableEffect(lifecycleOwner, key) {
		lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

		onDispose {
			lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
			onDispose()
		}
	}
}