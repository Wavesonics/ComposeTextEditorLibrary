package com.darkrockstudios.texteditor.markdown

import com.darkrockstudios.texteditor.CharLineOffset
import com.darkrockstudios.texteditor.richstyle.*
import com.darkrockstudios.texteditor.state.TextEditorState

private val HR_LINE_TOKENS = setOf("---", "***", "___")

/** Markdown line prefix used to emit blockquote / bullet items on export. */
private const val BLOCKQUOTE_PREFIX = "> "
private const val BULLET_PREFIX = "- "

/**
 * Matches a line whose entire content is a single markdown image, optionally
 * surrounded by whitespace. Captures alt text (group 1) and URL (group 2).
 * URL must not contain whitespace or close-paren; alt text must not contain
 * close-bracket.
 */
private val STANDALONE_IMAGE_REGEX =
	Regex("""^\s*!\[([^\]]*)\]\(([^)\s]+)\)\s*$""")

/**
 * Matches a markdown blockquote line. Captures the inner content (group 1) so
 * the `> ` prefix can be stripped before passing the body to the markdown
 * parser. Single-level only — nested `> > ` collapses one level per pass.
 */
private val BLOCKQUOTE_LINE_REGEX = Regex("""^>\s?(.*)$""")

/**
 * Matches a markdown bullet-list line: `-`, `*`, or `+` followed by at least one
 * space. Captures the body (group 1) so the marker can be stripped before parsing.
 * Single-level only — nested (indented) bullets are not yet supported.
 */
private val BULLET_LINE_REGEX = Regex("""^[-*+]\s+(.*)$""")

/**
 * An extension to TextEditorState that provides markdown functionality.
 * This separates markdown concerns from the core text editor functionality.
 */
class MarkdownExtension(
	val editorState: TextEditorState,
	initialConfiguration: MarkdownConfiguration = MarkdownConfiguration.DEFAULT,
	var imageProvider: ImageProvider? = null,
) {
	var markdownConfiguration: MarkdownConfiguration = initialConfiguration
		set(value) {
			field = value
			markdownStyles = MarkdownStyles(markdownConfiguration)
		}

	var markdownStyles: MarkdownStyles = MarkdownStyles(markdownConfiguration)
		private set

	fun exportAsMarkdown(): String {
		val allSpans = editorState.richSpanManager.getAllRichSpans()
		val hrLines = allSpans
			.asSequence()
			.filter { it.style === HorizontalRuleSpanStyle }
			.map { it.range.start.line }
			.toHashSet()
		val imageLines: Map<Int, ImageBlockSpanStyle> = allSpans
			.asSequence()
			.mapNotNull { span ->
				val style = span.style as? ImageBlockSpanStyle ?: return@mapNotNull null
				span.range.start.line to style
			}
			.toMap()
		val blockquoteLines = allSpans
			.asSequence()
			.filter { it.style === BlockquoteSpanStyle }
			.map { it.range.start.line }
			.toHashSet()
		val bulletLines = allSpans
			.asSequence()
			.filter { it.style === BulletListSpanStyle }
			.map { it.range.start.line }
			.toHashSet()

		val annotated = editorState.getAllText()
		val text = annotated.text
		if (text.isEmpty() && hrLines.isEmpty() && imageLines.isEmpty()) return ""

		val sb = StringBuilder()
		var lineIndex = 0
		var cursor = 0
		while (true) {
			val nextNewline = text.indexOf('\n', cursor)
			val end = if (nextNewline == -1) text.length else nextNewline
			val lineMarkdown = when {
				lineIndex in hrLines -> "---"
				imageLines.containsKey(lineIndex) -> {
					val style = imageLines.getValue(lineIndex)
					"![${style.alt}](${style.source})"
				}

				else -> annotated.subSequence(cursor, end).toMarkdown(markdownConfiguration)
			}
			if (lineIndex in blockquoteLines) sb.append(BLOCKQUOTE_PREFIX)
			if (lineIndex in bulletLines) sb.append(BULLET_PREFIX)
			sb.append(lineMarkdown)
			if (nextNewline == -1) break
			// Header export already ends with \n; don't double it.
			if (!lineMarkdown.endsWith('\n')) sb.append('\n')
			cursor = nextNewline + 1
			lineIndex++
		}
		return sb.toString()
	}

	fun importMarkdown(markdownText: String) {
		val hrLineIndices = mutableListOf<Int>()
		val imageLines = mutableListOf<Pair<Int, ImageBlockSpanStyle>>()
		val blockquoteLineIndices = mutableListOf<Int>()
		val bulletLineIndices = mutableListOf<Int>()
		val provider = imageProvider
		val processedLines = markdownText.lines().mapIndexed { index, line ->
			val imageMatch = STANDALONE_IMAGE_REGEX.matchEntire(line)
			val blockquoteMatch = BLOCKQUOTE_LINE_REGEX.matchEntire(line)
			val bulletMatch = BULLET_LINE_REGEX.matchEntire(line)
			when {
				line.trim() in HR_LINE_TOKENS -> {
					hrLineIndices += index
					HR_PLACEHOLDER
				}

				imageMatch != null && provider != null -> {
					val alt = imageMatch.groupValues[1]
					val url = imageMatch.groupValues[2]
					imageLines += index to ImageBlockSpanStyle(
						source = url,
						alt = alt,
						provider = provider,
					)
					IMAGE_PLACEHOLDER
				}

				blockquoteMatch != null -> {
					blockquoteLineIndices += index
					blockquoteMatch.groupValues[1]
				}

				bulletMatch != null -> {
					bulletLineIndices += index
					bulletMatch.groupValues[1]
				}

				else -> line
			}
		}
		val processedMarkdown = processedLines.joinToString("\n")
		val annotatedString = processedMarkdown.toAnnotatedStringFromMarkdown(markdownConfiguration)
		editorState.setText(annotatedString)
		hrLineIndices.forEach { lineIdx ->
			editorState.addRichSpan(
				start = CharLineOffset(lineIdx, 0),
				end = CharLineOffset(lineIdx, HR_PLACEHOLDER.length),
				style = HorizontalRuleSpanStyle,
			)
		}
		imageLines.forEach { (lineIdx, style) ->
			editorState.addRichSpan(
				start = CharLineOffset(lineIdx, 0),
				end = CharLineOffset(lineIdx, IMAGE_PLACEHOLDER.length),
				style = style,
			)
		}
		blockquoteLineIndices.forEach { editorState.applyBlockquote(it) }
		bulletLineIndices.forEach { editorState.applyBulletList(it) }
	}

	/** Returns whether [line] is currently rendered as a blockquote. */
	fun isBlockquote(line: Int): Boolean = editorState.hasBlockquote(line)

	/** Returns whether [line] is currently rendered as a bullet-list item. */
	fun isBulletList(line: Int): Boolean = editorState.hasBulletList(line)

	/**
	 * Adds blockquote rendering (left bar + indented text) to each line in
	 * [lines] that doesn't already have it; removes it from lines that do.
	 * Mixed selections enable on every line for predictable toolbar behavior.
	 */
	fun toggleBlockquote(lines: IntRange) =
		toggleLineBlock(lines, ::isBlockquote, editorState::applyBlockquote, editorState::demoteBlockquote)

	/**
	 * Adds bullet-list rendering (gutter dot + hanging indent) to each line in
	 * [lines] that doesn't already have it; removes it from lines that do.
	 * Mixed selections enable on every line for predictable toolbar behavior.
	 */
	fun toggleBulletList(lines: IntRange) =
		toggleLineBlock(lines, ::isBulletList, editorState::applyBulletList, editorState::demoteBulletList)

	private inline fun toggleLineBlock(
		lines: IntRange,
		has: (Int) -> Boolean,
		apply: (Int) -> Unit,
		demote: (Int) -> Unit,
	) {
		val anyOff = lines.any { !has(it) }
		for (lineIdx in lines) {
			if (anyOff) {
				if (!has(lineIdx)) apply(lineIdx)
			} else {
				demote(lineIdx)
			}
		}
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other == null || this::class != other::class) return false

		other as MarkdownExtension

		if (editorState != other.editorState) return false
		if (markdownConfiguration != other.markdownConfiguration) return false

		return true
	}

	override fun hashCode(): Int {
		var result = editorState.hashCode()
		result = 31 * result + markdownConfiguration.hashCode()
		return result
	}
}

fun TextEditorState.withMarkdown(
	initialConfiguration: MarkdownConfiguration = MarkdownConfiguration.DEFAULT,
	imageProvider: ImageProvider? = null,
): MarkdownExtension {
	return MarkdownExtension(this, initialConfiguration, imageProvider)
}