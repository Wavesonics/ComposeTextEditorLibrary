package com.darkrockstudios.texteditor.markdown

/**
 * Characters that have special meaning in markdown and need to be escaped
 * with a backslash when they appear as literal text.
 */
internal val MARKDOWN_SPECIAL_CHARS: Set<Char> = setOf(
	'*', '_', '`', '#', '+', '-', '!', '[', ']', '(', ')', '{', '}', '<', '>', '|', '\\'
)

private val UNESCAPE_REGEX: Regex = buildUnescapeRegex(MARKDOWN_SPECIAL_CHARS)

private fun buildUnescapeRegex(chars: Set<Char>): Regex {
	val escaped = chars.joinToString("") { char ->
		when (char) {
			'\\' -> "\\\\"
			'[', ']' -> "\\$char"
			'-' -> "\\-"
			else -> char.toString()
		}
	}
	return """\\([$escaped])""".toRegex()
}

internal fun escapeMarkdownChar(char: Char): String {
	return if (char in MARKDOWN_SPECIAL_CHARS) "\\$char" else char.toString()
}

internal fun CharSequence.removeMarkdownEscapes(): String {
	return replace(UNESCAPE_REGEX, "$1")
}
