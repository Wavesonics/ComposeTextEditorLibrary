package com.darkrockstudios.texteditor.state

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.roundToInt

class TextEditorScrollState(
	initial: Int = 0
) : ScrollableState {
	private val SCROLL_CONTENT_BUFFER = 32

	private var _value by mutableStateOf(initial)
	private var _maxValue by mutableStateOf(0)
	private var _isScrollInProgress by mutableStateOf(false)

	private val scrollScope: ScrollScope = object : ScrollScope {
		override fun scrollBy(pixels: Float): Float {
			return dispatchRawDelta(pixels)
		}
	}

	val value: Int get() = _value
	var maxValue: Int
		get() = _maxValue
		set(value) {
			_maxValue = value.coerceAtLeast(0) + SCROLL_CONTENT_BUFFER
			_value = _value.coerceIn(0, _maxValue)
		}

	override val isScrollInProgress: Boolean
		get() = _isScrollInProgress

	override suspend fun scroll(
		scrollPriority: MutatePriority,
		block: suspend ScrollScope.() -> Unit
	) {
		_isScrollInProgress = true
		try {
			scrollScope.run {
				block()
			}

		} finally {
			_isScrollInProgress = false
		}
	}

	override fun dispatchRawDelta(delta: Float): Float {
		if (delta.isNaN()) return 0f

		val oldValue = _value
		val newValue = (oldValue - delta).roundToInt()
		val coercedValue = newValue.coerceIn(0, maxValue)

		val consumed = (oldValue - coercedValue).toFloat()
		_value = coercedValue

		return consumed
	}

	fun scrollTo(value: Int) {
		_value = value.coerceIn(0, maxValue)
	}

	suspend fun animateScrollTo(
		value: Int,
		animationSpec: AnimationSpec<Float> = spring(
			stiffness = Spring.StiffnessMediumLow,
			visibilityThreshold = 1f
		)
	) {
		val targetValue = value.coerceIn(0, maxValue).toFloat()
		_isScrollInProgress = true
		try {
			animate(
				initialValue = _value.toFloat(),
				targetValue = targetValue,
				animationSpec = animationSpec
			) { value, _ ->
				_value = value.roundToInt()
			}
		} finally {
			_isScrollInProgress = false
		}
	}

	fun scrollBy(delta: Float): Float {
		val oldValue = _value
		val newValue = (oldValue + delta).roundToInt().coerceIn(0, maxValue)
		val consumed = (newValue - oldValue).toFloat()
		_value = newValue
		return consumed
	}
}