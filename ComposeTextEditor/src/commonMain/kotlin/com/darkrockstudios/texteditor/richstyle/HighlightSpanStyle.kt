package com.darkrockstudios.texteditor.richstyle


import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import com.darkrockstudios.texteditor.LineWrap

class HighlightSpanStyle(
	private val color: Color
) : RichSpanStyle {
	override fun DrawScope.drawCustomStyle(
		layoutResult: TextLayoutResult,
		lineWrap: LineWrap,
		textRange: TextRange
	) {
		val lineHeight = layoutResult.multiParagraph.getLineHeight(lineWrap.virtualLineIndex)

		val lineStartOffset = layoutResult.getLineStart(lineWrap.virtualLineIndex) + 1
		val startX = if (textRange.start <= lineStartOffset) {
			layoutResult.getLineLeft(lineWrap.virtualLineIndex)
		} else {
			try {
				layoutResult.getHorizontalPosition(textRange.start, usePrimaryDirection = true)
			} catch (e: Exception) {
				error(e)
			}
		}

		val lineEndOffset = layoutResult.getLineEnd(lineWrap.virtualLineIndex, false)
		val endX = if (textRange.end >= lineEndOffset) {
			layoutResult.getLineRight(lineWrap.virtualLineIndex)
		} else {
			layoutResult.getHorizontalPosition(textRange.end, usePrimaryDirection = true)
		}

//		println("Line Count: ${layoutResult.multiParagraph.lineCount}")
//		println("textRange: $textRange")
//		println("lineIndex: $lineIndex")
//		println("lineEndOffset: $lineEndOffset")
//		println("lineTop: $lineTop")
//		println("lineStartOffset: $lineStartOffset")
//		println("StartX $startX for: ${textRange.start}")
//		println("endX $endX for: ${textRange.end}")
//		println("width ${endX - startX}")
//		println("lineHeight: $lineHeight")

		drawRect(
			color = color,
			topLeft = Offset(x = startX, y = 0f),
			size = Size(width = endX - startX, height = lineHeight)
		)
	}
}

fun printTextLayoutResult(textLayoutResult: TextLayoutResult) {
	val lineCount = textLayoutResult.lineCount
	println("TextLayoutResult:")
	println("Total Lines: $lineCount")
	println("Total Text Length: ${textLayoutResult.layoutInput.text.length}")
	println("========================================")

	for (lineIndex in 0 until lineCount) {
		val lineStart = textLayoutResult.getLineStart(lineIndex)
		val lineEnd = textLayoutResult.getLineEnd(lineIndex)
		val lineBaseline = textLayoutResult.getLineBaseline(lineIndex)
		val lineLeft = textLayoutResult.getLineLeft(lineIndex)
		val lineRight = textLayoutResult.getLineRight(lineIndex)

		println("Line $lineIndex:")
		println("  Start Offset: $lineStart")
		println("  End Offset: $lineEnd")
		println("  Baseline: $lineBaseline")
		println("  Left Edge: $lineLeft")
		println("  Right Edge: $lineRight")
		println(
			"  Text: \"${
				textLayoutResult.layoutInput.text.text.substring(
					lineStart,
					lineEnd
				)
			}\""
		)
		println("----------------------------------------")
	}

	println("========================================")
}