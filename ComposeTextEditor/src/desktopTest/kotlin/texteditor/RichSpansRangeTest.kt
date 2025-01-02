package texteditor

class RichSpansRangeTest {
//	@Test
//	fun `test getRichSpansInRange finds span in simple case`() {
//		val textState = TextEditorState(
//			scope = TestScope(),
//			measurer = mockk(relaxed = true),
//			initialText = AnnotatedString("test text")
//		)
//
//		val range = TextEditorRange(
//			start = CharLineOffset(0, 0),
//			end = CharLineOffset(0, 4)
//		)
//		val testStyle = TestStyle()
//		textState.addRichSpan(range, testStyle)
//
//		// Mock lineOffsets to include our span
//		val lineWrap = LineWrap(
//			line = 0,
//			virtualLength = 5,
//			richSpans = listOf(RichSpan(range, testStyle)),
//			virtualLineIndex = 0,
//			wrapStartsAtIndex = 0,
//			textLayoutResult = mockk(relaxed = true),
//			offset = Offset.Zero
//		)
//		textState.setLineOffsets(listOf(lineWrap))
//
//		// Test exact match
//		val spans = textState.getRichSpansInRange(range)
//		assertEquals(1, spans.size)
//		assertEquals(testStyle, spans.first().style)
//		assertEquals(range, spans.first().range)
//	}

//	@Test
//	fun `test getRichSpansInRange updates after text edit`() {
//		val textState = TextEditorState(
//			scope = TestScope(),
//			measurer = mockk(relaxed = true),
//			initialText = AnnotatedString("test text")
//		)
//
//		val range = TextEditorRange(
//			start = CharLineOffset(0, 0),
//			end = CharLineOffset(0, 4)
//		)
//		val testStyle = TestStyle()
//		textState.addRichSpan(range, testStyle)
//
//		// Verify span exists before edit
//		var spans = textState.getRichSpansInRange(range)
//		assertEquals(1, spans.size, "Span should exist before edit")
//
//		// Insert text at cursor position 4 (end of the span)
//		textState.updateCursorPosition(CharLineOffset(0, 4))
//		textState.insertStringAtCursor(" more")
//
//		// Verify the span still exists
//		spans = textState.getRichSpansInRange(range)
//		assertEquals(1, spans.size, "Span should exist after edit")
//	}
}