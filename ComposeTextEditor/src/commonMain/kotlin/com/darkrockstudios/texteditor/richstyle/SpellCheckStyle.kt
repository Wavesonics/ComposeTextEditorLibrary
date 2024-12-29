package com.darkrockstudios.texteditor.richstyle

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import com.darkrockstudios.texteditor.LineWrap

object SpellCheckStyle : RichSpanStyle {

	private val color: Color = Color.Red
	private val waveLength: Float = 8f
	private val amplitude: Float = 1.5f

	override fun DrawScope.drawCustomStyle(
		layoutResult: TextLayoutResult,
		lineWrap: LineWrap,
		textRange: TextRange
	) {
		val lineHeight = layoutResult.multiParagraph.getLineHeight(lineWrap.virtualLineIndex)
		val baselineY = lineHeight - 2f // Slightly above the bottom

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

		// Only draw if we have a valid width
		if (endX > startX) {
			val path = Path().apply {
				moveTo(startX, baselineY)

				// Calculate how many complete waves we can fit
				val width = endX - startX
				val numWaves = (width / waveLength).toInt()
				val remainingWidth = width % waveLength

				var x = startX
				var up = true

				// Draw complete waves
				repeat(numWaves) {
					drawWaveSegment(x, baselineY, waveLength, up)
					x += waveLength
					up = !up
				}

				// Draw remaining partial wave if needed
				if (remainingWidth > 0) {
					drawWaveSegment(x, baselineY, remainingWidth, up)
				}
			}

			drawPath(
				path = path,
				color = color,
				style = Stroke(width = 1f)
			)
		}
	}

	private fun Path.drawWaveSegment(startX: Float, baselineY: Float, width: Float, up: Boolean) {
		val halfWave = width / 2
		val midY = baselineY + (if (up) -amplitude else amplitude)
		val endX = startX + width

		// First half of wave
		quadraticTo(
			x1 = startX + halfWave / 2,
			y1 = midY,
			x2 = startX + halfWave,
			y2 = baselineY
		)

		// Second half of wave (if there's enough space)
		if (width > halfWave) {
			quadraticTo(
				x1 = startX + halfWave + (width - halfWave) / 2,
				y1 = baselineY + (if (!up) -amplitude else amplitude),
				x2 = endX,
				y2 = baselineY
			)
		}
	}
}