package com.darkrockstudios.texteditor.annotatedstring

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString

internal fun String.toAnnotatedString() = AnnotatedString(this)

internal fun AnnotatedString.subSequence(startIndex: Int = 0, endIndex: Int = length) =
	subSequence(startIndex = startIndex, endIndex = endIndex)

internal fun AnnotatedString.splitToAnnotatedString(): List<AnnotatedString> {
	if (this.isEmpty()) return listOf(AnnotatedString(""))

	val result = mutableListOf<AnnotatedString>()
	var currentIndex = 0

	this.text.split('\n').forEach { lineText ->
		val endIndex = currentIndex + lineText.length

		// Create a new AnnotatedString for this line
		val lineAnnotatedString = buildAnnotatedString {
			// Append the line text
			append(lineText)

			// Copy over all relevant spans for this line segment
			this@splitToAnnotatedString.spanStyles.forEach { span ->
				val spanStart = span.start
				val spanEnd = span.end

				// Check if this span overlaps with our current line
				if (spanStart < endIndex && spanEnd > currentIndex) {
					// Calculate the overlapping region
					val overlapStart = maxOf(spanStart - currentIndex, 0)
					val overlapEnd = minOf(spanEnd - currentIndex, lineText.length)

					if (overlapStart < overlapEnd) {
						addStyle(span.item, overlapStart, overlapEnd)
					}
				}
			}

			// Copy over all relevant paragraph styles
			this@splitToAnnotatedString.paragraphStyles.forEach { paragraph ->
				val paragraphStart = paragraph.start
				val paragraphEnd = paragraph.end

				// Check if this paragraph style overlaps with our current line
				if (paragraphStart < endIndex && paragraphEnd > currentIndex) {
					// Calculate the overlapping region
					val overlapStart = maxOf(paragraphStart - currentIndex, 0)
					val overlapEnd = minOf(paragraphEnd - currentIndex, lineText.length)

					if (overlapStart < overlapEnd) {
						addStyle(paragraph.item, overlapStart, overlapEnd)
					}
				}
			}
		}

		result.add(lineAnnotatedString)
		currentIndex = endIndex + 1 // +1 for the newline character
	}

	return result
}
