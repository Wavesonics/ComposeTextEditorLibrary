package com.darkrockstudios.texteditor.markdown

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser

// Style Constants
private object MarkdownStyles {
	val BOLD = SpanStyle(fontWeight = FontWeight.Bold)
	val ITALICS = SpanStyle(fontStyle = FontStyle.Italic)
	val CODE = SpanStyle(
		fontFamily = FontFamily.Monospace,
		background = Color(0xFFE0E0E0)
	)
	val LINK = SpanStyle(
		color = Color.Blue,
		textDecoration = TextDecoration.Underline
	)
	val BLOCKQUOTE = SpanStyle(
		color = Color.Gray,
		fontStyle = FontStyle.Italic
	)

	fun header(level: Int) = SpanStyle(
		fontSize = when (level) {
			1 -> 32f
			2 -> 24f
			3 -> 18.72f
			4 -> 16f
			5 -> 13.28f
			else -> 12f
		}.sp,
		fontWeight = FontWeight.Bold
	)
}

fun String.toAnnotatedStringFromMarkdown(): AnnotatedString {
	val flavour = CommonMarkFlavourDescriptor()
	val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(this)
	return buildAnnotatedString {
		appendMarkdownChildren(this@toAnnotatedStringFromMarkdown, parsedTree, 0)
	}
}

private fun AnnotatedString.Builder.appendMarkdownChildren(
	original: String,
	node: ASTNode,
	startOffset: Int
) {
	var childOffset = startOffset
	node.children.forEach { child ->
		appendMarkdownNode(original, child, childOffset)
		childOffset += child.getTextInNode(original).length
	}
}

private fun AnnotatedString.Builder.appendMarkdownNode(
	original: String,
	node: ASTNode,
	startOffset: Int
) {
	val nodeText = node.getTextInNode(original).toString()

	when (node.type) {
		MarkdownElementTypes.PARAGRAPH -> {
			appendMarkdownChildren(original, node, startOffset)
			append("\n")
		}

		MarkdownTokenTypes.WHITE_SPACE -> {
			// Only keep newlines and spaces between words
			if (nodeText.contains("\n") || startOffset > 0) {
				append(nodeText)
			}
		}

		MarkdownElementTypes.EMPH -> {
			pushStyle(MarkdownStyles.ITALICS)
			// Get the content without the markdown characters
			val content = node.children.find { it.type == MarkdownTokenTypes.TEXT }
			if (content != null) {
				append(content.getTextInNode(original))
			} else {
				appendMarkdownChildren(original, node, startOffset)
			}
			pop()
		}

		MarkdownElementTypes.STRONG -> {
			pushStyle(MarkdownStyles.BOLD)
			// Get the content without the markdown characters
			val content = node.children.find { it.type == MarkdownTokenTypes.TEXT }
			if (content != null) {
				append(content.getTextInNode(original))
			} else {
				appendMarkdownChildren(original, node, startOffset)
			}
			pop()
		}

		MarkdownElementTypes.CODE_SPAN -> {
			pushStyle(MarkdownStyles.CODE)
			// Remove the backticks from code spans
			val codeText = nodeText.removeSurrounding("`")
			append(codeText)
			pop()
		}

		MarkdownElementTypes.CODE_FENCE -> {
			pushStyle(MarkdownStyles.CODE)

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

		MarkdownElementTypes.ATX_1 -> handleHeader(original, node, startOffset, 1)
		MarkdownElementTypes.ATX_2 -> handleHeader(original, node, startOffset, 2)
		MarkdownElementTypes.ATX_3 -> handleHeader(original, node, startOffset, 3)
		MarkdownElementTypes.ATX_4 -> handleHeader(original, node, startOffset, 4)
		MarkdownElementTypes.ATX_5 -> handleHeader(original, node, startOffset, 5)
		MarkdownElementTypes.ATX_6 -> handleHeader(original, node, startOffset, 6)

		MarkdownElementTypes.INLINE_LINK -> {
			pushStyle(MarkdownStyles.LINK)
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
			appendMarkdownChildren(original, node, startOffset)
			append("\n")
		}

		MarkdownElementTypes.LIST_ITEM -> {
			append("• ") // Using bullet for both ordered and unordered for simplicity
			appendMarkdownChildren(original, node, startOffset)
			append("\n")
		}

		MarkdownElementTypes.BLOCK_QUOTE -> {
			pushStyle(MarkdownStyles.BLOCKQUOTE)
			append("> ")
			appendMarkdownChildren(original, node, startOffset)
			pop()
			append("\n")
		}

		MarkdownTokenTypes.TEXT,
		MarkdownTokenTypes.EOL -> {
			append(nodeText)
		}

		MarkdownElementTypes.MARKDOWN_FILE -> {
			appendMarkdownChildren(original, node, startOffset)
		}

		else -> {
			// For any unhandled node types, just append the text
			if (nodeText.isNotEmpty()) {
				append(nodeText)
			} else {
				appendMarkdownChildren(original, node, startOffset)
			}
		}
	}
}

private fun AnnotatedString.Builder.handleHeader(
	original: String,
	node: ASTNode,
	startOffset: Int,
	level: Int
) {
	pushStyle(MarkdownStyles.header(level))

	// Get the header text content, skipping the # markers and whitespace
	node.children.forEach { child ->
		when (child.type) {
			MarkdownTokenTypes.ATX_CONTENT -> {
				append(child.getTextInNode(original).toString().trim())
			}
			// Skip other header-related tokens
			MarkdownTokenTypes.ATX_HEADER,
			MarkdownTokenTypes.WHITE_SPACE -> {
			}

			else -> appendMarkdownNode(original, child, startOffset)
		}
	}
	pop()
	append("\n")
}