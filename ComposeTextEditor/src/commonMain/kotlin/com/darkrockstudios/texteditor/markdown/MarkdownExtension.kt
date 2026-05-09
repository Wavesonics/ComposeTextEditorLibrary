package com.darkrockstudios.texteditor.markdown

import com.darkrockstudios.texteditor.CharLineOffset
import com.darkrockstudios.texteditor.richstyle.*
import com.darkrockstudios.texteditor.state.TextEditorState

private val HR_LINE_TOKENS = setOf("---", "***", "___")

/**
 * Matches a line whose entire content is a single markdown image, optionally
 * surrounded by whitespace. Captures alt text (group 1) and URL (group 2).
 * URL must not contain whitespace or close-paren; alt text must not contain
 * close-bracket.
 */
private val STANDALONE_IMAGE_REGEX =
	Regex("""^\s*!\[([^\]]*)\]\(([^)\s]+)\)\s*$""")

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
		val blockLines: Map<LineBlockStyle, Set<Int>> = LINE_BLOCK_STYLES.associateWith { block ->
			allSpans.asSequence()
				.filter { it.style === block.spanStyle }
				.map { it.range.start.line }
				.toHashSet()
		}

		val annotated = editorState.getAllText()
		val text = annotated.text
		if (text.isEmpty() && hrLines.isEmpty() && imageLines.isEmpty()) return ""

		val sb = StringBuilder()
		var lineIndex = 0
		var cursor = 0
		// Tracks position-within-run for each block style so the markdown prefix
		// callback can render position-dependent markers (1., 2., 3. for ordered
		// lists). Resets when a block run ends — a non-block line between two OL
		// runs restarts numbering.
		val runPositions = mutableMapOf<LineBlockStyle, Int>()
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
			LINE_BLOCK_STYLES.forEach { block ->
				if (lineIndex in blockLines.getValue(block)) {
					val pos = runPositions[block] ?: 0
					sb.append(block.markdownPrefix(pos))
					runPositions[block] = pos + 1
				} else {
					runPositions.remove(block)
				}
			}
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
		val blockHits = mutableMapOf<LineBlockStyle, MutableList<Int>>()
		val provider = imageProvider
		val processedLines = markdownText.lines().mapIndexed { index, line ->
			val imageMatch = STANDALONE_IMAGE_REGEX.matchEntire(line)
			val blockMatch = LINE_BLOCK_STYLES.firstNotNullOfOrNull { block ->
				block.markdownPattern.matchEntire(line)?.let { block to it }
			}
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

				blockMatch != null -> {
					val (block, match) = blockMatch
					blockHits.getOrPut(block) { mutableListOf() } += index
					match.groupValues[1]
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
		blockHits.forEach { (block, lineIndices) ->
			lineIndices.forEach { editorState.applyLineBlock(it, block) }
		}
	}

	/** Returns whether [line] is currently rendered as a blockquote. */
	fun isBlockquote(line: Int): Boolean = editorState.hasLineBlock(line, Blockquote)

	/** Returns whether [line] is currently rendered as a bullet-list item. */
	fun isBulletList(line: Int): Boolean = editorState.hasLineBlock(line, BulletList)

	/** Returns whether [line] is currently rendered as an ordered-list item. */
	fun isOrderedList(line: Int): Boolean = editorState.hasLineBlock(line, OrderedList)

	/**
	 * Adds blockquote rendering (left bar + indented text) to each line in
	 * [lines] that doesn't already have it; removes it from lines that do.
	 * Mixed selections enable on every line for predictable toolbar behavior.
	 */
	fun toggleBlockquote(lines: IntRange) = toggleLineBlock(lines, Blockquote)

	/**
	 * Adds bullet-list rendering (gutter dot + hanging indent) to each line in
	 * [lines] that doesn't already have it; removes it from lines that do.
	 * Mixed selections enable on every line for predictable toolbar behavior.
	 */
	fun toggleBulletList(lines: IntRange) = toggleLineBlock(lines, BulletList)

	/**
	 * Adds ordered-list rendering (gutter numeral + hanging indent) to each line
	 * in [lines] that doesn't already have it; removes it from lines that do.
	 * Mixed selections enable on every line for predictable toolbar behavior.
	 * Numbering is recomputed automatically based on contiguous-run position.
	 */
	fun toggleOrderedList(lines: IntRange) = toggleLineBlock(lines, OrderedList)

	private fun toggleLineBlock(lines: IntRange, block: LineBlockStyle) {
		val anyOff = lines.any { !editorState.hasLineBlock(it, block) }
		for (lineIdx in lines) {
			if (anyOff) {
				if (!editorState.hasLineBlock(lineIdx, block)) editorState.applyLineBlock(lineIdx, block)
			} else {
				editorState.demoteLineBlock(lineIdx, block)
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
