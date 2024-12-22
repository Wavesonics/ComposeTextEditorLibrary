package com.darkrockstudios.texteditor

import androidx.compose.ui.text.SpanStyle
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.time.TimeSource
import kotlin.time.measureTimedValue

@OptIn(ExperimentalContracts::class)
public inline fun <T> measureAndReport(message: String, block: () -> T): T {
	contract {
		callsInPlace(block, InvocationKind.EXACTLY_ONCE)
	}
	val value = TimeSource.Monotonic.measureTimedValue(block)
	println("$message: ${value.duration}")

	return value.value
}

fun SpanStyle.merge(other: SpanStyle): SpanStyle {
	return SpanStyle(
		color = other.color ?: this.color,
		fontSize = other.fontSize ?: this.fontSize,
		fontWeight = other.fontWeight ?: this.fontWeight,
		fontStyle = other.fontStyle ?: this.fontStyle,
		fontFamily = other.fontFamily ?: this.fontFamily,
		letterSpacing = other.letterSpacing ?: this.letterSpacing,
		background = other.background ?: this.background,
		textDecoration = other.textDecoration ?: this.textDecoration,
		shadow = other.shadow ?: this.shadow,
		textGeometricTransform = other.textGeometricTransform ?: this.textGeometricTransform,
		localeList = other.localeList ?: this.localeList,
		baselineShift = other.baselineShift ?: this.baselineShift,
	)
}