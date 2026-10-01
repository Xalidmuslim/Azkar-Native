package app.xalidmuslim.azkar.ui.designsystem

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.unit.dp

object AzkarMotion {
    const val dhikrPageDurationMillis = 520
    const val sheetDurationMillis = 500
    const val progressDurationMillis = 650
    const val toggleDurationMillis = 320
    const val stateTransitionDurationMillis = 380
    const val themeTransitionDurationMillis = 900
    val dhikrPageEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
    val sheetEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
    val progressEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
    val themeEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
    val nextStartOffsetX = 24.dp
    val previousStartOffsetX = (-24).dp
    val sheetStartOffsetY = 18.dp
    const val dhikrStartOpacity = 0.62f
    const val dhikrStartScale = 0.994f
    const val sheetStartOpacity = 0.70f
}
