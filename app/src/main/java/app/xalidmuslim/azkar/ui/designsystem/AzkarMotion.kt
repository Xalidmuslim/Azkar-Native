package app.xalidmuslim.azkar.ui.designsystem

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.unit.dp

object AzkarMotion {
    // Match the restrained motion language used in Al-Fatiha:
    // screen content fades in from 7dp over ~155ms; small reveals use ~170ms.
    const val dhikrPageDurationMillis = 155
    const val sheetDurationMillis = 170
    const val progressDurationMillis = 155
    const val toggleDurationMillis = 170
    const val stateTransitionDurationMillis = 155
    const val themeTransitionDurationMillis = 155

    val dhikrPageEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
    val sheetEasing = dhikrPageEasing
    val progressEasing = dhikrPageEasing
    val themeEasing = dhikrPageEasing

    val pageStartOffsetY = 7.dp
    val sheetStartOffsetY = 5.dp
    const val dhikrStartOpacity = 0f
    const val dhikrStartScale = 1f
    const val sheetStartOpacity = 0f
}
