package app.xalidmuslim.azkar.ui.reading

import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import app.xalidmuslim.azkar.ui.designsystem.AzkarDimensions
import app.xalidmuslim.azkar.ui.designsystem.AzkarMotion
import app.xalidmuslim.azkar.persistence.AzkarDateProvider
import app.xalidmuslim.azkar.persistence.AzkarPreferencesRepository
import app.xalidmuslim.azkar.persistence.SystemAzkarDateProvider
import app.xalidmuslim.azkar.ui.designsystem.AzkarTheme
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
    onPeriodChange: (AzkarPeriod) -> Unit = {},
) {
    val navigationController = remember(entries.size, initialIndex) {
        AzkarReaderNavigationController(entries.size, initialIndex)
    }
    val uiController = remember { AzkarReaderUiController() }
    AzkarReaderScreen(
        entries = entries,
        period = period,
        controller = navigationController,
        uiController = uiController,
        modifier = modifier,
        onPeriodChange = onPeriodChange,
    )
}

@Composable
fun AzkarReaderScreen(
    entries: List<AzkarReaderEntry>,
    period: AzkarPeriod,
    preferencesRepository: AzkarPreferencesRepository,
    dateProvider: AzkarDateProvider = SystemAzkarDateProvider,
    modifier: Modifier = Modifier,
    initialIndex: Int = 0,
    onPeriodChange: (AzkarPeriod) -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val visibleItemIds = remember(entries) { entries.map { it.item.id }.toSet() }
    val navigationController = remember(entries.size, initialIndex) {
        AzkarReaderNavigationController(entries.size, initialIndex)
    }
    val uiController = remember(preferencesRepository, dateProvider, visibleItemIds) {
        AzkarReaderUiController(
            repository = preferencesRepository,
            dateProvider = dateProvider,
            persistenceScope = scope,
            visibleItemIds = visibleItemIds,
        )
    }
    AzkarReaderScreen(
        entries = entries,
        period = period,
        controller = navigationController,
        uiController = uiController,
        modifier = modifier,
        onPeriodChange = onPeriodChange,
    )
}

@Composable
fun AzkarReaderScreen(
    entries: List<AzkarReaderEntry>,
    period: AzkarPeriod,
    controller: AzkarReaderNavigationController,
    modifier: Modifier = Modifier,
    uiController: AzkarReaderUiController? = null,
    onPeriodChange: (AzkarPeriod) -> Unit = {},
) {
    require(entries.isNotEmpty()) { "Reader requires at least one entry" }

    val resolvedUiController = uiController ?: remember { AzkarReaderUiController() }
    val navigation = controller.state
    val readerUi = resolvedUiController.state
    if (!readerUi.isHydrated) {
        Box(modifier = modifier.fillMaxSize())
        return
    }
    val settings = readerUi.settings
    val resolvedEntries = entries.map { entry ->
        entry.copy(
            currentCount = resolvedUiController
                .currentCount(entry.item.id, entry.currentCount)
                .coerceIn(0, entry.item.count),
        )
    }
    val activeIndex = navigation.activeIndex.coerceIn(resolvedEntries.indices)
    val active = resolvedEntries[activeIndex]

    var restoredLastItem by remember(period, controller) { mutableStateOf(false) }
    LaunchedEffect(readerUi.isHydrated, period, readerUi.lastItemByPeriod) {
        if (readerUi.isHydrated && !restoredLastItem) {
            val savedId = readerUi.lastItemByPeriod[period]
            val savedIndex = resolvedEntries.indexOfFirst { it.item.id == savedId }
            if (savedIndex >= 0 && savedIndex != controller.state.activeIndex) {
                controller.selectAnchor(savedIndex)
            }
            restoredLastItem = true
        }
    }
    LaunchedEffect(restoredLastItem, period, navigation.activeIndex) {
        if (restoredLastItem) {
            resolvedEntries.getOrNull(navigation.activeIndex)?.let { entry ->
                resolvedUiController.saveLastItem(period, entry.item.id)
            }
        }
    }

    val previousTarget = if (settings.hideCompleted) {
        (activeIndex - 1 downTo 0).firstOrNull { index ->
            resolvedEntries[index].currentCount < resolvedEntries[index].item.count
        }
    } else {
        (activeIndex - 1).takeIf { it >= 0 }
    }
    val nextTarget = if (settings.hideCompleted) {
        (activeIndex + 1 until resolvedEntries.size).firstOrNull { index ->
            resolvedEntries[index].currentCount < resolvedEntries[index].item.count
        }
    } else {
        (activeIndex + 1).takeIf { it < resolvedEntries.size }
    }

    LaunchedEffect(settings.hideCompleted, active.item.id, active.currentCount) {
        if (settings.hideCompleted && active.currentCount >= active.item.count) {
            (nextTarget ?: previousTarget)?.let(controller::navigateTo)
        }
    }

    val listVisibleIndices = resolvedEntries.indices.filter { index ->
        !settings.hideCompleted ||
            resolvedEntries[index].currentCount < resolvedEntries[index].item.count ||
            index == activeIndex
    }
    val activeListIndex = listVisibleIndices.indexOf(activeIndex).coerceAtLeast(0)

    val shellScrollState = rememberScrollState()
    val readingScrollState = remember(navigation.generation, readerUi.viewMode) { ScrollState(0) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val listScrollMarginPx = with(density) {
        AzkarDimensions.dhikrScrollMarginTop.roundToPx()
    }

    LaunchedEffect(readingScrollState) {
        snapshotFlow { readingScrollState.value }.collect(controller::recordScrollY)
    }

    LaunchedEffect(readerUi.viewMode) {
        if (readerUi.viewMode == AzkarReaderViewMode.List) {
            listState.animateScrollToItem(
                index = AzkarListCardStartIndex + activeListIndex,
                scrollOffset = -listScrollMarginPx,
            )
        }
    }

    BackHandler(enabled = readerUi.activeSheet != AzkarReaderSheet.None) {
        resolvedUiController.closeSheet()
    }
    BackHandler(
        enabled = readerUi.activeSheet == AzkarReaderSheet.None &&
            navigation.history.isNotEmpty(),
    ) {
        if (controller.back() && readerUi.viewMode == AzkarReaderViewMode.List) {
            val destination = controller.state.activeIndex
            scope.launch {
                listState.animateScrollToItem(
                    index = AzkarListCardStartIndex + destination,
                    scrollOffset = -listScrollMarginPx,
                )
            }
        }
    }

    val context = LocalContext.current
    val animationsEnabled = remember {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) > 0f
    }
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

    val completedItems = countCompletedItems(resolvedEntries)
    val uiState = AzkarGoldenReadingUiState(
        item = active.item,
        period = period,
        position = activeIndex + 1,
        total = entries.size,
        completedItems = completedItems,
        currentCount = active.currentCount,
    )

    val previous: () -> Unit = {
        if (
            readerUi.viewMode == AzkarReaderViewMode.Cards &&
            readerUi.activeSheet == AzkarReaderSheet.None
        ) {
            previousTarget?.let(controller::navigateTo)
        }
        Unit
    }
    val next: () -> Unit = {
        if (
            readerUi.viewMode == AzkarReaderViewMode.Cards &&
            readerUi.activeSheet == AzkarReaderSheet.None
        ) {
            nextTarget?.let(controller::navigateTo)
        }
        Unit
    }
    val gestureModifier = Modifier
        .azkarHorizontalPaging(
            enabled = readerUi.allowsHorizontalPaging(resolvedEntries.size),
            onPrevious = previous,
            onNext = next,
        )
        .graphicsLayer {
            translationX = transitionOffset.value
            alpha = transitionAlpha.value
        }

    AzkarTheme(
        themeMode = settings.themeMode,
        russianFontFamily = settings.russianFontFamily,
        arabicFontFamily = settings.arabicFontFamily,
        arabicSizeSp = settings.arabicSizeSp,
        russianSizeSp = settings.russianSizeSp,
        readerLineHeight = settings.lineHeight,
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            when (readerUi.viewMode) {
                AzkarReaderViewMode.Cards -> {
                    AzkarGoldenReadingScreen(
                        state = uiState,
                        modifier = Modifier.fillMaxSize(),
                        onPrevious = previous,
                        onNext = next,
                        shellScrollState = shellScrollState,
                        readingScrollState = readingScrollState,
                        readingAreaModifier = gestureModifier,
                        onOpenSettings = resolvedUiController::openSettings,
                        onOpenContents = resolvedUiController::openContents,
                        onOpenSourceInfo = resolvedUiController::openSourceInfo,
                        onOpenExplanation = resolvedUiController::openExplanation,
                        onOpenActions = resolvedUiController::openActions,
                        onIncrementCount = { itemId, target ->
                            resolvedUiController.incrementProgress(itemId, target)
                        },
                        onResetProgress = {
                            resolvedUiController.resetProgress(resolvedEntries.map { it.item.id })
                        },
                        onPeriodChange = onPeriodChange,
                        compactReader = settings.readerStyle == AzkarReaderStyle.Compact,
                        showTranslation = settings.showTranslation,
                        showSources = settings.showSources,
                        showNotes = settings.showNotes,
                        canPrevious = previousTarget != null,
                        canNext = nextTarget != null,
                    )
                }

                AzkarReaderViewMode.List -> {
                    AzkarListReadingScreen(
                        entries = resolvedEntries,
                        period = period,
                        activeIndex = activeIndex,
                        listState = listState,
                        settings = settings,
                        modifier = Modifier.fillMaxSize(),
                        onOpenSettings = resolvedUiController::openSettings,
                        onOpenContents = resolvedUiController::openContents,
                        onOpenSourceInfo = resolvedUiController::openSourceInfo,
                        onIncrementCount = { itemId, target ->
                            resolvedUiController.incrementProgress(itemId, target)
                        },
                        onResetProgress = {
                            resolvedUiController.resetProgress(resolvedEntries.map { it.item.id })
                        },
                        onPeriodChange = onPeriodChange,
                        hideCompleted = settings.hideCompleted,
                        onOpenActions = { index, itemId ->
                            controller.selectAnchor(index)
                            resolvedUiController.openActions(itemId)
                        },
                        onOpenExplanation = { index, itemId ->
                            controller.selectAnchor(index)
                            resolvedUiController.openExplanation(itemId)
                        },
                    )
                }
            }

            AzkarReaderSheetHost(
                activeSheet = readerUi.activeSheet,
                entries = resolvedEntries,
                activeIndex = activeIndex,
                selectedExplanationId = readerUi.selectedExplanationId,
                selectedActionId = readerUi.selectedActionId,
                settings = settings,
                viewMode = readerUi.viewMode,
                onDismiss = { resolvedUiController.closeSheet() },
                onResetSettings = {
                    resolvedUiController.updateSettings { AzkarReaderSettings() }
                },
                onSelectContents = { index ->
                    when (readerUi.viewMode) {
                        AzkarReaderViewMode.Cards -> {
                            if (index == controller.state.activeIndex) {
                                controller.reopenCurrentAtTop()
                            } else {
                                controller.navigateTo(index)
                            }
                            resolvedUiController.closeSheet()
                        }

                        AzkarReaderViewMode.List -> {
                            controller.selectAnchor(index)
                            resolvedUiController.closeSheet()
                            scope.launch {
                                listState.animateScrollToItem(
                                    index = AzkarListCardStartIndex +
                                        listVisibleIndices.indexOf(index).coerceAtLeast(0),
                                    scrollOffset = -listScrollMarginPx,
                                )
                            }
                        }
                    }
                },
                onViewModeChange = { mode ->
                    resolvedUiController.setViewMode(mode)
                    resolvedUiController.closeSheet()
                },
                onUpdateSettings = resolvedUiController::updateSettings,
            )
        }
    }
}
