import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import com.darkrockstudios.texteditor.LineWrap
import com.darkrockstudios.texteditor.richstyle.RichSpanStyle

// Carries the URL through edit operations; visuals are supplied by the LINK SpanStyle.
data class LinkSpanStyle(val url: String) : RichSpanStyle {
	override fun DrawScope.drawCustomStyle(
		layoutResult: TextLayoutResult,
		lineWrap: LineWrap,
		textRange: TextRange,
	) = Unit
}

data object HorizontalRuleSpanStyle : RichSpanStyle {
	override fun DrawScope.drawCustomStyle(
		layoutResult: TextLayoutResult,
		lineWrap: LineWrap,
		textRange: TextRange,
	) {
		val lineHeight = layoutResult.multiParagraph.getLineHeight(lineWrap.virtualLineIndex)
		val left = layoutResult.getLineLeft(lineWrap.virtualLineIndex)
		val right = layoutResult.getLineRight(lineWrap.virtualLineIndex)
		// If the line is empty, getLineRight may equal getLineLeft; fall back to canvas width.
		val end = if (right > left) right else size.width
		val midY = lineHeight / 2f
		drawLine(
			color = Color.Gray,
			start = Offset(x = left, y = midY),
			end = Offset(x = end, y = midY),
			strokeWidth = 1.5f,
			cap = Stroke.DefaultCap,
		)
	}
}
