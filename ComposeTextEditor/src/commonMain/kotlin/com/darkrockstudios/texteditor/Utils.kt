package com.darkrockstudios.texteditor

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.time.TimeSource
import kotlin.time.measureTimedValue

@OptIn(ExperimentalContracts::class)
public inline fun <T> measureAndReport(message: String, block: () -> T): T {
	contract {
		callsInPlace(block, InvocationKind.EXACTLY_ONCE)
	}
	val value = TimeSource.Monotonic.measureTimedValue(block)
	println("$message: ${value.duration}")

	return value.value
}