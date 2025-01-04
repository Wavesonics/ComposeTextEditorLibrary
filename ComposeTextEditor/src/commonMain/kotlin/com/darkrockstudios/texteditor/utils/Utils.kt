package com.darkrockstudios.texteditor.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.time.TimeSource
import kotlin.time.measureTimedValue

@OptIn(ExperimentalContracts::class)
internal inline fun <T> measureAndReport(message: String, block: () -> T): T {
	contract {
		callsInPlace(block, InvocationKind.EXACTLY_ONCE)
	}
	val value = TimeSource.Monotonic.measureTimedValue(block)
	println("$message: ${value.duration}")

	return value.value
}

internal fun buildAnnotatedStringWithSpans(
	block: AnnotatedString.Builder.(addSpan: (SpanStyle, Int, Int) -> Unit) -> Unit
): AnnotatedString {
	return buildAnnotatedString {
		// Track spans by style to detect overlaps
		val spansByStyle = mutableMapOf<Any, MutableSet<IntRange>>()

		fun addSpanIfNew(item: SpanStyle, start: Int, end: Int) {
			val ranges = spansByStyle.getOrPut(item) { mutableSetOf() }

			// Check for overlaps
			val overlapping = ranges.filter { range ->
				start <= range.last + 1 && end >= range.first - 1
			}

			if (overlapping.isNotEmpty()) {
				// Remove overlapping ranges
				ranges.removeAll(overlapping.toSet())

				// Create one merged range
				val newStart = minOf(start, overlapping.minOf { it.first })
				val newEnd = maxOf(end, overlapping.maxOf { it.last })

				ranges.add(newStart..newEnd)
				addStyle(item, newStart, newEnd)
			} else {
				// No overlap - add new range
				ranges.add(start..end)
				addStyle(item, start, end)
			}
		}

		block(::addSpanIfNew)
	}
}