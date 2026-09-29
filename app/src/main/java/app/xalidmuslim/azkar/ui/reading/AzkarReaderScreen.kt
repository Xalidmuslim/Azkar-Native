package app.xalidmuslim.azkar.ui.reading

import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import app.xalidmuslim.azkar.ui.designsystem.AzkarMotion
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

data class AzkarReaderEntry(
    val item: AzkarReadingItem,
    val currentCount: Int = 0,
)

@Composable
fun AzkarReaderScreen(
    entries: List<AzkarReaderEntry>,
    period: AzkarPeriod,
    modifier: Modifier = Modifier,
    initialIndex: Int = 0,
) {
    val controller = remember(entries.size, initialIndex) {
        AzkarReaderNavigationController(entries.size, initialIndex)
    }
    AzkarReaderScreen(
        entries = entries,
        period = period,
        controller = controller,
        modifier = modifier,
    )
}

@Composable
fun AzkarReaderScreen(
    entries: List<AzkarReaderEntry>,
    period: AzkarPeriod,
    controller: AzkarReaderNavigationController,
    modifier: Modifier = Modifier,
) {
    require(entries.isNotEmpty()) { "Reader requires at least one entry" }

    val navigation = controller.state
    val activeIndex = navigation.activeIndex.coerceIn(entries.indices)
    val active = entries[activeIndex]
    val shellScrollState = rememberScrollState()
    val readingScrollState = remember(navigation.generation) { ScrollState(0) }

    LaunchedEffect(readingScrollState) {
        snapshotFlow { readingScrollState.value }.collect(controller::recordScrollY)
    }

    BackHandler(enabled = navigation.history.isNotEmpty()) {
        controller.back()
    }

    val context = LocalContext.current
    val animationsEnabled = remember {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) > 0f
    }
    val density = LocalDensity.current
    val startOffsetPx = with(density) {
        when (navigation.direction) {
            AzkarNavigationDirection.Next -> AzkarMotion.nextStartOffsetX.toPx()
            AzkarNavigationDirection.Previous -> AzkarMotion.previousStartOffsetX.toPx()
            AzkarNavigationDirection.None -> 0f
        }
    }
    val initialOffset = if (navigation.generation > 0L && animationsEnabled) startOffsetPx else 0f
    val initialAlpha = if (navigation.generation > 0L && animationsEnabled) {
        AzkarMotion.dhikrStartOpacity
    } else {
        1f
    }
    val transitionOffset = remember(navigation.generation) { Animatable(initialOffset) }
    val transitionAlpha = remember(navigation.generation) { Animatable(initialAlpha) }

    LaunchedEffect(navigation.generation, animationsEnabled) {
        if (!animationsEnabled || navigation.generation == 0L) {
            transitionOffset.snapTo(0f)
            transitionAlpha.snapTo(1f)
        } else {
            coroutineScope {
                launch {
                    transitionOffset.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(
                            durationMillis = AzkarMotion.dhikrPageDurationMillis,
                            easing = AzkarMotion.dhikrPageEasing,
                        ),
                    )
                }
                launch {
                    transitionAlpha.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = AzkarMotion.dhikrPageDurationMillis,
                            easing = AzkarMotion.dhikrPageEasing,
                        ),
                    )
                }
            }
        }
    }

    val completedItems = entries.count { it.currentCount >= it.item.count }
    val uiState = AzkarGoldenReadingUiState(
        item = active.item,
        period = period,
        position = activeIndex + 1,
        total = entries.size,
        completedItems = completedItems,
        currentCount = active.currentCount,
    )

    val previous: () -> Unit = {
        controller.previous()
        Unit
    }
    val next: () -> Unit = {
        controller.next()
        Unit
    }
    val gestureModifier = Modifier
        .azkarHorizontalPaging(
            enabled = entries.size > 1,
            onPrevious = previous,
            onNext = next,
        )
        .graphicsLayer {
            translationX = transitionOffset.value
            alpha = transitionAlpha.value
        }

    AzkarGoldenReadingScreen(
        state = uiState,
        modifier = modifier,
        onPrevious = previous,
        onNext = next,
        shellScrollState = shellScrollState,
        readingScrollState = readingScrollState,
        readingAreaModifier = gestureModifier,
    )
}
