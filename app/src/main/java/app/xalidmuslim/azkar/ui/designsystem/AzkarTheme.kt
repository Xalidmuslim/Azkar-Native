package app.xalidmuslim.azkar.ui.designsystem

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color as AndroidColor
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class AzkarThemeMode { Light, Dark, System }

internal fun resolveAzkarDarkTheme(mode: AzkarThemeMode, systemIsDark: Boolean): Boolean = when (mode) {
    AzkarThemeMode.Light -> false
    AzkarThemeMode.Dark -> true
    AzkarThemeMode.System -> systemIsDark
}

private val LocalAzkarColors = staticCompositionLocalOf { AzkarColors.Light }
private val LocalAzkarTypography = staticCompositionLocalOf { AzkarTypography.create() }
private val LocalAzkarElevation = staticCompositionLocalOf { AzkarElevation.Light }

object AzkarThemeValues {
    val colors: AzkarColorScheme
        @Composable @ReadOnlyComposable get() = LocalAzkarColors.current
    val typography: AzkarTypographySet
        @Composable @ReadOnlyComposable get() = LocalAzkarTypography.current
    val elevation: AzkarElevationScheme
        @Composable @ReadOnlyComposable get() = LocalAzkarElevation.current
}

@Composable
fun AzkarTheme(
    themeMode: AzkarThemeMode = AzkarThemeMode.System,
    russianFontFamily: RussianFontFamily = RussianFontFamily.LITERATA,
    arabicFontFamily: ArabicFontFamily = ArabicFontFamily.NOTO_NASKH_ARABIC,
    arabicSizeSp: Float = AzkarDimensions.defaultArabicSizeSp,
    russianSizeSp: Float = AzkarDimensions.defaultRussianSizeSp,
    readerLineHeight: Float = AzkarDimensions.defaultReaderLineHeight,
    prepareEdgeToEdge: Boolean = true,
    content: @Composable () -> Unit,
) {
    val dark = resolveAzkarDarkTheme(themeMode, isSystemInDarkTheme())
    val darkFraction = animateFloatAsState(
        targetValue = if (dark) 1f else 0f,
        animationSpec = tween(
            durationMillis = AzkarMotion.themeTransitionDurationMillis,
            easing = AzkarMotion.themeEasing,
        ),
        label = "azkar-theme-transition",
    ).value
    val colors = lerpAzkarColorScheme(AzkarColors.Light, AzkarColors.Dark, darkFraction)
    val typography = AzkarTypography.create(
        russianFont = russianFontFamily,
        arabicFont = arabicFontFamily,
        arabicSizeSp = arabicSizeSp,
        russianSizeSp = russianSizeSp,
        readerLineHeight = readerLineHeight,
    )
    val elevation = if (dark) AzkarElevation.Dark else AzkarElevation.Light

    if (prepareEdgeToEdge) AzkarEdgeToEdgeEffect(dark)

    CompositionLocalProvider(
        LocalAzkarColors provides colors,
        LocalAzkarTypography provides typography,
        LocalAzkarElevation provides elevation,
        content = content,
    )
}

private fun lerpAzkarColorScheme(
    light: AzkarColorScheme,
    dark: AzkarColorScheme,
    fraction: Float,
): AzkarColorScheme = AzkarColorScheme(
    background = lerp(light.background, dark.background, fraction),
    foreground = lerp(light.foreground, dark.foreground, fraction),
    card = lerp(light.card, dark.card, fraction),
    surface = lerp(light.surface, dark.surface, fraction),
    muted = lerp(light.muted, dark.muted, fraction),
    border = lerp(light.border, dark.border, fraction),
    primary = lerp(light.primary, dark.primary, fraction),
    accent = lerp(light.accent, dark.accent, fraction),
    warning = lerp(light.warning, dark.warning, fraction),
    success = lerp(light.success, dark.success, fraction),
    primaryActionText = lerp(light.primaryActionText, dark.primaryActionText, fraction),
    countButtonText = lerp(light.countButtonText, dark.countButtonText, fraction),
    overlay = lerp(light.overlay, dark.overlay, fraction),
    doneCardBorder = lerp(light.doneCardBorder, dark.doneCardBorder, fraction),
    doneMarkerBorder = lerp(light.doneMarkerBorder, dark.doneMarkerBorder, fraction),
    doneMarkerBackground = lerp(light.doneMarkerBackground, dark.doneMarkerBackground, fraction),
    warningBadgeBorder = lerp(light.warningBadgeBorder, dark.warningBadgeBorder, fraction),
    noteBorder = lerp(light.noteBorder, dark.noteBorder, fraction),
    noteBackground = lerp(light.noteBackground, dark.noteBackground, fraction),
    stickyToolbarBackground = lerp(light.stickyToolbarBackground, dark.stickyToolbarBackground, fraction),
    activeItemBackground = lerp(light.activeItemBackground, dark.activeItemBackground, fraction),
    sheetHandle = lerp(light.sheetHandle, dark.sheetHandle, fraction),
)

@Composable
private fun AzkarEdgeToEdgeEffect(dark: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    SideEffect {
        val activity = view.context.findActivity() ?: return@SideEffect
        val window = activity.window
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = AndroidColor.TRANSPARENT
        window.navigationBarColor = AndroidColor.TRANSPARENT
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
