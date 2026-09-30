package app.xalidmuslim.azkar.ui.reading

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
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
    private val horizontalLockRatio: Float,
    private val verticalLockRatio: Float,
    private val directionLockPx: Float = thresholdPx * 0.26f,
) {
    private enum class Axis { Undecided, Horizontal, Vertical }

    private var axis = Axis.Undecided
    private var resolved = false
    private var cancelled = false

    fun update(dx: Float, dy: Float, pointerCount: Int): AzkarSwipeDecision? {
        if (resolved || cancelled) return null
        if (pointerCount > 1) {
            cancelled = true
            return null
        }

        val horizontal = abs(dx)
        val vertical = abs(dy)

        if (axis == Axis.Undecided) {
            val moved = maxOf(horizontal, vertical)
            if (moved < directionLockPx) return null

            axis = when {
                horizontal >= vertical * horizontalLockRatio -> Axis.Horizontal
                vertical >= directionLockPx * 1.35f &&
                    vertical > horizontal * verticalLockRatio -> Axis.Vertical
                else -> Axis.Undecided
            }
        }

        if (axis != Axis.Horizontal) return null

        if (horizontal >= thresholdPx) {
            resolved = true
            return if (dx < 0f) AzkarSwipeDecision.Next else AzkarSwipeDecision.Previous
        }

        return null
    }

    fun cancel() {
        cancelled = true
    }

    val consumesHorizontalGesture: Boolean
        get() = axis == Axis.Horizontal && !cancelled
}

internal fun Modifier.azkarHorizontalPaging(
    enabled: Boolean,
    threshold: Dp = 30.dp,
    horizontalLockRatio: Float = 0.90f,
    verticalLockRatio: Float = 1.35f,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
): Modifier {
    if (!enabled) return this

    return pointerInput(enabled, threshold, horizontalLockRatio, verticalLockRatio) {
        val thresholdPx = threshold.toPx()

        awaitEachGesture {
            val down = awaitFirstDown(
                requireUnconsumed = false,
                pass = PointerEventPass.Initial,
            )
            val activePointer: PointerId = down.id
            val start: Offset = down.position
            val session = AzkarSwipeSession(
                thresholdPx = thresholdPx,
                horizontalLockRatio = horizontalLockRatio,
                verticalLockRatio = verticalLockRatio,
            )

            while (true) {
                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                val pressedCount = event.changes.count { it.pressed }

                if (pressedCount > 1) session.cancel()

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

                if (session.consumesHorizontalGesture) {
                    event.changes.forEach { it.consume() }
                }

                when (decision) {
                    AzkarSwipeDecision.Next -> onNext()
                    AzkarSwipeDecision.Previous -> onPrevious()
                    null -> Unit
                }

                if (!change.pressed) break
            }
        }
    }
}
