package com.darkrockstudios.texteditor.richstyle

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange

class SpellCheckStyle(
	private val color: Color = Color.Red,
	private val waveLength: Float = 8f,
	private val amplitude: Float = 1.5f
) : RichSpanStyle {
	override fun DrawScope.drawCustomStyle(
		layoutResult: TextLayoutResult,
		lineIndex: Int,
		textRange: TextRange
	) {
		val startX = layoutResult.getHorizontalPosition(textRange.start, usePrimaryDirection = true)
		val endX = layoutResult.getHorizontalPosition(textRange.end, usePrimaryDirection = true)

		// Position the wave at the bottom of the text
		val lineIndex = layoutResult.getLineForOffset(textRange.start)
		val lineHeight = layoutResult.multiParagraph.getLineHeight(lineIndex)
		val baselineY = lineHeight - 2f // Slightly above the bottom

		val path = Path().apply {
			moveTo(startX, baselineY)

			// Create a wavy line by repeatedly adding sine wave segments
			var x = startX
			var up = true
			while (x < endX) {
				val halfWave = waveLength / 2
				val targetX = (x + halfWave).coerceAtMost(endX)
				val targetY = baselineY + (if (up) -amplitude else amplitude)

				quadraticTo(
					x1 = x + (targetX - x) / 2,
					y1 = targetY,
					x2 = targetX,
					y2 = baselineY
				)

				x = targetX
				up = !up
			}
		}

		drawPath(
			path = path,
			color = color,
			style = Stroke(width = 1f)
		)
	}
}