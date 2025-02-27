package com.darkrockstudios.texteditor.markdown

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser

fun String.toAnnotatedStringFromMarkdown(
	configuration: MarkdownConfiguration = MarkdownConfiguration.DEFAULT
): AnnotatedString {
	val styles = MarkdownStyles(configuration)

	val flavour = CommonMarkFlavourDescriptor()
	val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(this)
	return buildAnnotatedString {
		appendMarkdownChildren(this@toAnnotatedStringFromMarkdown, parsedTree, 0, styles)
	}
}

internal fun AnnotatedString.Builder.appendMarkdownChildren(
	original: String,
	node: ASTNode,
	startOffset: Int,
	styles: MarkdownStyles
) {
	var childOffset = startOffset
	node.children.forEach { child ->
		appendMarkdownNode(original, child, childOffset, styles)
		childOffset += child.getTextInNode(original).length
	}
}

private fun AnnotatedString.Builder.appendMarkdownNode(
	original: String,
	node: ASTNode,
	startOffset: Int,
	styles: MarkdownStyles
) {
	val nodeText = node.getTextInNode(original).toString()

	when (node.type) {
		MarkdownElementTypes.PARAGRAPH -> {
			pushStyle(styles.BASE_TEXT)
			appendMarkdownChildren(original, node, startOffset, styles)
			pop()
		}

		MarkdownTokenTypes.WHITE_SPACE -> {
			// Only keep newlines and spaces between words
			if (nodeText.contains("\n") || startOffset > 0) {
				append(nodeText)
			}
		}

		MarkdownElementTypes.EMPH -> {
			pushStyle(styles.ITALICS)
			// Get the content without the markdown characters
			val content = node.children.find { it.type == MarkdownTokenTypes.TEXT }
			if (content != null) {
				append(content.getTextInNode(original))
			} else {
				appendMarkdownChildren(original, node, startOffset, styles)
			}
			pop()
		}

		MarkdownElementTypes.STRONG -> {
			pushStyle(styles.BOLD)
			// Get the content without the markdown characters
			val content = node.children.find { it.type == MarkdownTokenTypes.TEXT }
			if (content != null) {
				append(content.getTextInNode(original))
			} else {
				appendMarkdownChildren(original, node, startOffset, styles)
			}
			pop()
		}

		MarkdownElementTypes.CODE_SPAN -> {
			pushStyle(styles.CODE)
			// Remove the backticks from code spans
			val codeText = nodeText.removeSurrounding("`")
			append(codeText)
			pop()
		}

		MarkdownElementTypes.CODE_FENCE -> {
			pushStyle(styles.CODE)

			// Get the lines and strip fence markers
			val lines = nodeText.lines()
				.dropWhile { it.trim().startsWith("```") } // Drop opening fence
				.dropLastWhile { it.trim().startsWith("```") } // Drop closing fence
				.filter { it.isNotEmpty() } // Remove empty lines

			if (lines.isNotEmpty()) {
				// Calculate minimum indentation from non-empty lines
				val minIndent = lines
					.filter { it.isNotBlank() }
					.map { it.indexOfFirst { char -> !char.isWhitespace() } }
					.filter { it != -1 }
					.minOrNull() ?: 0

				// Process and append each line with proper indentation
				lines.joinToString("\n") { line ->
					if (line.length >= minIndent) {
						line.substring(minIndent)
					} else {
						line
					}
				}.let { processedContent ->
					append(processedContent.trim())
					append('\n')
				}
			}
			pop()
		}

		MarkdownElementTypes.ATX_1 -> handleHeader(original, node, startOffset, 1, styles)
		MarkdownElementTypes.ATX_2 -> handleHeader(original, node, startOffset, 2, styles)
		MarkdownElementTypes.ATX_3 -> handleHeader(original, node, startOffset, 3, styles)
		MarkdownElementTypes.ATX_4 -> handleHeader(original, node, startOffset, 4, styles)
		MarkdownElementTypes.ATX_5 -> handleHeader(original, node, startOffset, 5, styles)
		MarkdownElementTypes.ATX_6 -> handleHeader(original, node, startOffset, 6, styles)

		MarkdownElementTypes.INLINE_LINK -> {
			pushStyle(styles.LINK)
			node.children.forEach { child ->
				if (child.type.toString() == "Markdown:LINK_TEXT") {
					val text = child.getTextInNode(original).toString()
						.removeSurrounding("[", "]")
					append(text)
				}
			}
			pop()
		}

		MarkdownElementTypes.ORDERED_LIST,
		MarkdownElementTypes.UNORDERED_LIST -> {
			appendMarkdownChildren(original, node, startOffset, styles)
			append("\n")
		}

		MarkdownElementTypes.LIST_ITEM -> {
			append("• ") // Using bullet for both ordered and unordered for simplicity
			appendMarkdownChildren(original, node, startOffset, styles)
			append("\n")
		}

		MarkdownElementTypes.BLOCK_QUOTE -> {
			pushStyle(styles.BLOCKQUOTE)
			append("> ")
			appendMarkdownChildren(original, node, startOffset, styles)
			pop()
			append("\n")
		}

		MarkdownTokenTypes.TEXT,
		MarkdownTokenTypes.EOL -> {
			append(nodeText)
		}

		MarkdownElementTypes.MARKDOWN_FILE -> {
			appendMarkdownChildren(original, node, startOffset, styles)
		}

		else -> {
			// For any unhandled node types, just append the text
			if (nodeText.isNotEmpty()) {
				append(nodeText)
			} else {
				appendMarkdownChildren(original, node, startOffset, styles)
			}
		}
	}
}

private fun AnnotatedString.Builder.handleHeader(
	original: String,
	node: ASTNode,
	startOffset: Int,
	level: Int,
	styles: MarkdownStyles
) {
	// Apply the header style
	pushStyle(styles.header(level))

	// Process the child nodes, ignoring `#` markers but supporting nested spans
	node.children.forEach { child ->
		when (child.type) {
			MarkdownTokenTypes.ATX_HEADER -> {
				// Skip processing the actual `#` markers
			}
			MarkdownTokenTypes.WHITE_SPACE -> {
				// Skip leading whitespace (if it's part of the `#` header)
				if (startOffset == 0) return@forEach
				append(child.getTextInNode(original))
			}
			MarkdownTokenTypes.ATX_CONTENT -> {
				// Recursively process the header's content for nested styles
				appendMarkdownChildren(original, child, startOffset, styles)
			}
			else -> {
				// Process any other nested styles or text
				appendMarkdownNode(original, child, startOffset, styles)
			}
		}
	}

	// Pop the header style
	pop()

	// Add a newline for formatting
	append("\n")
}
