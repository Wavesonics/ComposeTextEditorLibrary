package com.darkrockstudios.texteditor.state

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle

/**
 * Manages text spans efficiently by handling merging, shrinking, and expansion of spans
 * while maintaining the minimal necessary number of spans.
 */
class SpanManager {
	private data class SpanInfo(
		val style: SpanStyle,
		var start: Int,
		var end: Int
	) {
		fun overlaps(other: SpanInfo): Boolean =
			(start <= other.end && end >= other.start) && style == other.style

		fun isAdjacent(other: SpanInfo): Boolean =
			(end + 1 == other.start || other.end + 1 == start) && style == other.style

		fun merge(other: SpanInfo): SpanInfo =
			SpanInfo(style, minOf(start, other.start), maxOf(end, other.end))

		fun coerceIn(range: IntRange): SpanInfo? {
			val newStart = start.coerceIn(range)
			val newEnd = end.coerceIn(range)
			return if (newStart < newEnd) {
				SpanInfo(style, newStart, newEnd)
			} else null
		}

		override fun toString(): String = "Span($start-$end: $style)"
	}

	fun processSpans(
		originalText: AnnotatedString,
		insertionPoint: Int = -1,
		insertedText: AnnotatedString? = null,
		deletionStart: Int = -1,
		deletionEnd: Int = -1
	): List<AnnotatedString.Range<SpanStyle>> {
		// Calculate the final text length after operations
		val finalLength = when {
			insertedText != null && insertionPoint >= 0 ->
				originalText.length + insertedText.length

			deletionStart >= 0 && deletionEnd >= 0 ->
				originalText.length - (deletionEnd - deletionStart)

			else -> originalText.length
		}

		// First, deduplicate identical spans
		val uniqueSpans = originalText.spanStyles.distinctBy { span ->
			Triple(span.start, span.end, span.item)
		}

		// Convert spans to mutable form
		val spans = uniqueSpans.map { span ->
			SpanInfo(span.item, span.start, span.end)
		}.toMutableList()

		// Add spans from inserted text if applicable
		if (insertedText != null && insertionPoint >= 0) {
			val uniqueInsertedSpans = insertedText.spanStyles.distinctBy { span ->
				Triple(
					span.start + insertionPoint,
					span.end + insertionPoint,
					span.item
				)
			}
			spans.addAll(uniqueInsertedSpans.map { span ->
				SpanInfo(
					span.item,
					span.start + insertionPoint,
					span.end + insertionPoint
				)
			})
		}

		// Handle deletion if applicable
		if (deletionStart >= 0 && deletionEnd >= 0) {
			handleDeletion(spans, deletionStart, deletionEnd)
		}

		// Handle insertion if applicable
		if (insertedText != null && insertionPoint >= 0) {
			handleInsertion(spans, insertionPoint, insertedText.length)
		}

		// Merge overlapping and adjacent spans
		val mergedSpans = mergeSpans(spans)

		// Ensure all spans are within bounds and convert back to AnnotatedString.Range format
		return mergedSpans
			.mapNotNull { span -> span.coerceIn(0..finalLength) }
			.map { span ->
				AnnotatedString.Range(span.style, span.start, span.end)
			}
	}

	private fun handleDeletion(spans: MutableList<SpanInfo>, start: Int, end: Int) {
		val deletionLength = end - start
		val iterator = spans.iterator()

		while (iterator.hasNext()) {
			val span = iterator.next()
			when {
				// Span ends before deletion - no change needed
				span.end <= start -> {}

				// Span starts after deletion - shift left
				span.start >= end -> {
					span.start = (span.start - deletionLength).coerceAtLeast(0)
					span.end = (span.end - deletionLength).coerceAtLeast(0)
					if (span.start >= span.end) {
						iterator.remove()
					}
				}

				// Deletion is entirely within span - contract span
				span.start < start && span.end > end -> {
					span.end = span.end - deletionLength
				}

				// Deletion overlaps start of span - adjust start
				span.start >= start && span.start < end -> {
					if (span.end > end) {
						span.start = start
						span.end -= deletionLength
					} else {
						// Span is completely within deletion - remove it
						iterator.remove()
					}
				}

				// Deletion overlaps end of span - adjust end
				span.start < start && span.end <= end -> {
					span.end = start
					if (span.start >= span.end) {
						iterator.remove()
					}
				}

				// Any other case means the span is invalid or empty - remove it
				else -> {
					iterator.remove()
				}
			}
		}
	}

	private fun handleInsertion(spans: MutableList<SpanInfo>, point: Int, length: Int) {
		spans.forEach { span ->
			when {
				// Span ends before insertion - no change needed
				span.end < point -> {}

				// Span starts after insertion - shift right
				span.start >= point -> {
					span.start += length
					span.end += length
				}

				// Insertion is at the end of span - don't expand span
				span.end == point -> {}

				// Insertion is in middle of span - expand span
				span.start < point && span.end > point -> {
					span.end += length
				}
			}
		}
	}

	private fun mergeSpans(spans: List<SpanInfo>): List<SpanInfo> {
		if (spans.isEmpty()) return emptyList()

		val result = mutableListOf<SpanInfo>()
		val sorted = spans.sortedBy { it.start }

		var current = sorted.first()
		var mergeCount = 0

		for (i in 1 until sorted.size) {
			val next = sorted[i]
			if (current.overlaps(next) || current.isAdjacent(next)) {
				current = current.merge(next)
				mergeCount++
			} else {
				result.add(current)
				current = next
			}
		}
		result.add(current)

		return result
	}
}