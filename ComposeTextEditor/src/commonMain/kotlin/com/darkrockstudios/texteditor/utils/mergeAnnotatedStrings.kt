package com.darkrockstudios.texteditor.utils

import androidx.annotation.VisibleForTesting
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import com.darkrockstudios.texteditor.state.SpanManager

@VisibleForTesting
internal fun SpanManager.combineAnnotatedStrings(
	prefix: AnnotatedString,
	center: AnnotatedString,
	suffix: AnnotatedString,
): AnnotatedString {
	val temp = appendAnnotatedStrings(prefix, center)
	return appendAnnotatedStrings(temp, suffix)
}

@VisibleForTesting
internal fun SpanManager.appendAnnotatedStrings(
	original: AnnotatedString,
	newText: AnnotatedString
): AnnotatedString = mergeAnnotatedStrings(
	original = original,
	start = original.length,
	end = original.length,
	newText = newText
)

@VisibleForTesting
internal fun SpanManager.prependAnnotatedStrings(
	original: AnnotatedString,
	newText: AnnotatedString
): AnnotatedString = mergeAnnotatedStrings(
	original = original,
	start = 0,
	end = 0,
	newText = newText
)

@VisibleForTesting
internal fun SpanManager.mergeAnnotatedStrings(
	original: AnnotatedString,
	start: Int,
	end: Int = start, // For insertions, start == end
	newText: AnnotatedString? = null
): AnnotatedString = buildAnnotatedString {
	// Add text outside the affected range
	append(original.text.substring(0, start))
	if (newText != null) append(newText.text)
	append(original.text.substring(end))

	// Process spans with SpanManager
	val processedSpans = processSpans(
		originalText = original,
		insertionPoint = if (newText != null) start else -1,
		insertedText = newText,
		deletionStart = if (end > start) start else -1,
		deletionEnd = if (end > start) end else -1
	)

	// Add processed spans to the result
	processedSpans.forEach { span ->
		addStyle(span.item, span.start, span.end)
	}
}