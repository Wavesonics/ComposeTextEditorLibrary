package com.darkrockstudios.texteditor.markdown

/**
 * Characters that have special meaning in markdown and need to be escaped
 * with a backslash when they appear as literal text.
 */
internal val MARKDOWN_SPECIAL_CHARS: Set<Char> = setOf(
	'*', '_', '`', '#', '+', '-', '!', '[', ']', '(', ')', '{', '}', '<', '>', '|', '\\'
)

/**
 * Characters that can appear as backslash escapes in markdown input.
 * This is a superset of MARKDOWN_SPECIAL_CHARS — includes '.' which is only
 * escaped contextually (e.g. "1\.") but must always be unescaped on parse.
 */
private val UNESCAPE_CHARS: Set<Char> = MARKDOWN_SPECIAL_CHARS + '.'

private val UNESCAPE_REGEX: Regex = buildUnescapeRegex(UNESCAPE_CHARS)

private fun buildUnescapeRegex(chars: Set<Char>): Regex {
	val escaped = chars.joinToString("") { char ->
		when (char) {
			'\\' -> "\\\\"
			'[', ']' -> "\\$char"
			'-' -> "\\-"
			'.' -> "\\."
			else -> char.toString()
		}
	}
	return """\\([$escaped])""".toRegex()
}

/**
 * Escapes "1." / "2." etc. at line starts to prevent ordered list parsing.
 */
private val ORDERED_LIST_REGEX = Regex("(?m)^(\\d+)\\.")

internal fun escapeOrderedListMarkers(markdown: String): String {
	return markdown.replace(ORDERED_LIST_REGEX, "$1\\\\.")
}

internal fun escapeMarkdownChar(char: Char): String {
	return if (char in MARKDOWN_SPECIAL_CHARS) "\\$char" else char.toString()
}

internal fun CharSequence.removeMarkdownEscapes(): String {
	return replace(UNESCAPE_REGEX, "$1")
}
