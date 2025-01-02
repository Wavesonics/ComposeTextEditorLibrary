package com.darkrockstudios.texteditor.spellcheck.utils

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration

internal fun <T> Flow<T>.debounceUntilQuiescent(duration: Duration): Flow<T> = channelFlow {
	var job: Job? = null
	collect { value ->
		job?.cancel()
		job = launch {
			delay(duration)
			send(value)
			job = null
		}
	}
}

internal fun <T> Flow<T>.debounceUntilQuiescentWithBatch(
	duration: Duration
): Flow<List<T>> = channelFlow {
	val operations = mutableListOf<T>()
	var job: Job? = null

	collect { value ->
		operations.add(value)
		job?.cancel()
		job = launch {
			delay(duration)
			send(operations.toList())
			operations.clear()
			job = null
		}
	}
}