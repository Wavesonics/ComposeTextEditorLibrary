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

		override fun toString(): String = "Span($start-$end: $style)"
	}

	fun processSpans(
		originalText: AnnotatedString,
		insertionPoint: Int = -1,
		insertedText: AnnotatedString? = null,
		deletionStart: Int = -1,
		deletionEnd: Int = -1
	): List<AnnotatedString.Range<SpanStyle>> {
		println("\nSpanManager processing:")
		println("Initial spans: ${originalText.spanStyles.size}")
		if (originalText.spanStyles.isNotEmpty()) {
			println("Sample span: ${originalText.spanStyles.first()}")
		}

		// First, deduplicate identical spans
		val uniqueSpans = originalText.spanStyles.distinctBy { span ->
			Triple(span.start, span.end, span.item)
		}
		println("After deduplication: ${uniqueSpans.size} spans")

		// Convert spans to mutable form
		val spans = uniqueSpans.map { span ->
			SpanInfo(span.item, span.start, span.end)
		}.toMutableList()

		// Add spans from inserted text if applicable
		if (insertedText != null && insertionPoint >= 0) {
			println("Processing insertion at $insertionPoint with ${insertedText.spanStyles.size} spans")
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
			println("After adding insertion spans: ${spans.size} spans")
		}

		// Handle deletion if applicable
		if (deletionStart >= 0 && deletionEnd >= 0) {
			println("Processing deletion from $deletionStart to $deletionEnd")
			val beforeCount = spans.size
			handleDeletion(spans, deletionStart, deletionEnd)
			println("After deletion: ${spans.size} spans (removed ${beforeCount - spans.size})")
		}

		// Handle insertion if applicable
		if (insertedText != null && insertionPoint >= 0) {
			println("Adjusting spans for insertion length: ${insertedText.length}")
			handleInsertion(spans, insertionPoint, insertedText.length)
		}

		// Merge overlapping and adjacent spans
		val beforeMerge = spans.size
		val mergedSpans = mergeSpans(spans)
		println("After merging: ${mergedSpans.size} spans (merged ${beforeMerge - mergedSpans.size})")

		if (mergedSpans.isNotEmpty()) {
			println("Sample merged spans (first 3):")
			mergedSpans.take(3).forEach { span ->
				println("  $span")
			}
		}

		// Convert back to AnnotatedString.Range format
		return mergedSpans.map { span ->
			AnnotatedString.Range(span.style, span.start, span.end)
		}.also {
			println("Final span count: ${it.size}")
			println("SpanManager processing complete\n")
		}
	}

	private fun handleDeletion(spans: MutableList<SpanInfo>, start: Int, end: Int) {
		val deletionLength = end - start
		val iterator = spans.iterator()
		var removed = 0
		var modified = 0

		while (iterator.hasNext()) {
			val span = iterator.next()
			when {
				// Span ends before deletion - no change needed
				span.end < start -> continue

				// Span starts after deletion - shift left
				span.start > end -> {
					span.start -= deletionLength
					span.end -= deletionLength
					modified++
				}

				// Span completely within deletion - remove it
				span.start >= start && span.end <= end -> {
					iterator.remove()
					removed++
				}

				// Deletion removes middle of span - shrink span
				span.start < start && span.end > end -> {
					span.end -= deletionLength
					modified++
				}

				// Deletion removes end of span - truncate end
				span.start < start && span.end <= end -> {
					span.end = start
					modified++
				}

				// Deletion removes start of span - adjust start
				span.start >= start && span.end > end -> {
					span.start = start
					span.end -= deletionLength
					modified++
				}
			}
		}
		println("Deletion details: removed $removed spans, modified $modified spans")
	}

	private fun handleInsertion(spans: MutableList<SpanInfo>, point: Int, length: Int) {
		var modified = 0
		spans.forEach { span ->
			when {
				// Span ends before insertion - no change needed
				span.end < point -> {}

				// Span starts after insertion - shift right
				span.start >= point -> {
					span.start += length
					span.end += length
					modified++
				}

				// Insertion is in middle of span - expand span
				span.start < point && span.end >= point -> {
					span.end += length
					modified++
				}
			}
		}
		println("Insertion details: modified $modified spans")
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

		println("Merge details: performed $mergeCount merges")
		return result
	}
}