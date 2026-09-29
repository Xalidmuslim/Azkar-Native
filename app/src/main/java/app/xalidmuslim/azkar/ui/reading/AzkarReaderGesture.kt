package app.xalidmuslim.azkar.ui.reading

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

internal enum class AzkarSwipeDecision {
    Next,
    Previous,
}

internal class AzkarSwipeSession(
    private val thresholdPx: Float,
    private val dominanceRatio: Float,
) {
    private var resolved = false
    private var verticalOwned = false
    private var cancelled = false

    fun update(dx: Float, dy: Float, pointerCount: Int): AzkarSwipeDecision? {
        if (resolved || verticalOwned || cancelled) return null
        if (pointerCount > 1) {
            cancelled = true
            return null
        }

        val horizontal = abs(dx)
        val vertical = abs(dy)

        if (vertical >= thresholdPx && vertical > horizontal) {
            verticalOwned = true
            return null
        }

        if (horizontal >= thresholdPx && horizontal > vertical * dominanceRatio) {
            resolved = true
            return if (dx < 0f) AzkarSwipeDecision.Next else AzkarSwipeDecision.Previous
        }

        return null
    }

    fun cancel() {
        cancelled = true
    }

    val consumesHorizontalGesture: Boolean
        get() = resolved
}

internal fun Modifier.azkarHorizontalPaging(
    enabled: Boolean,
    threshold: Dp = 55.dp,
    dominanceRatio: Float = 1.15f,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
): Modifier {
    if (!enabled) return this

    return pointerInput(enabled, threshold, dominanceRatio) {
        val thresholdPx = threshold.toPx()

        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val activePointer: PointerId = down.id
            val start: Offset = down.position
            val session = AzkarSwipeSession(thresholdPx, dominanceRatio)

            while (true) {
                val event = awaitPointerEvent()
                val pressedCount = event.changes.count { it.pressed }
                if (pressedCount > 1) {
                    session.cancel()
                }

                val change = event.changes.firstOrNull { it.id == activePointer }
                if (change == null) {
                    session.cancel()
                    break
                }

                val delta = change.position - start
                val decision = session.update(
                    dx = delta.x,
                    dy = delta.y,
                    pointerCount = pressedCount.coerceAtLeast(1),
                )

                if (decision != null) {
                    event.changes.forEach { it.consume() }
                    when (decision) {
                        AzkarSwipeDecision.Next -> onNext()
                        AzkarSwipeDecision.Previous -> onPrevious()
                    }
                } else if (session.consumesHorizontalGesture) {
                    event.changes.forEach { it.consume() }
                }

                if (!change.pressed) break
            }
        }
    }
}
