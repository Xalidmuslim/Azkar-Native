package app.xalidmuslim.azkar.ui.reading

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.xalidmuslim.azkar.ui.designsystem.ArabicFontFamily
import app.xalidmuslim.azkar.ui.designsystem.AzkarBorders
import app.xalidmuslim.azkar.ui.designsystem.AzkarDimensions
import app.xalidmuslim.azkar.ui.designsystem.AzkarElevation
import app.xalidmuslim.azkar.ui.designsystem.AzkarFontFamilies
import app.xalidmuslim.azkar.ui.designsystem.AzkarMotion
import app.xalidmuslim.azkar.ui.designsystem.AzkarRadius
import app.xalidmuslim.azkar.ui.designsystem.AzkarSpacing
import app.xalidmuslim.azkar.ui.designsystem.AzkarThemeMode
import app.xalidmuslim.azkar.ui.designsystem.AzkarThemeValues
import app.xalidmuslim.azkar.ui.designsystem.RussianFontFamily
import app.xalidmuslim.azkar.ui.designsystem.azkarShadow
import kotlin.math.roundToInt

internal object AzkarSheetTestTags {
    const val Overlay = "azkar-sheet-overlay"
    const val Sheet = "azkar-sheet"
    const val Close = "azkar-sheet-close"
    const val Settings = "azkar-settings-sheet"
    const val Contents = "azkar-contents-sheet"
    const val Explanation = "azkar-explanation-sheet"
    const val ContentsItemPrefix = "azkar-contents-item-"
    const val ContentsCardsMode = "azkar-contents-mode-cards"
    const val ContentsListMode = "azkar-contents-mode-list"
    const val SettingTranslation = "azkar-setting-translation"
    const val SettingSources = "azkar-setting-sources"
    const val SettingNotes = "azkar-setting-notes"
    const val ThemeLight = "azkar-theme-light"
    const val ThemeDark = "azkar-theme-dark"
    const val ThemeSystem = "azkar-theme-system"
}

@Composable
internal fun AzkarReaderSheetHost(
    activeSheet: AzkarReaderSheet,
    entries: List<AzkarReaderEntry>,
    activeIndex: Int,
    selectedExplanationId: String?,
    settings: AzkarReaderSettings,
    viewMode: AzkarReaderViewMode,
    onDismiss: () -> Unit,
    onSelectContents: (Int) -> Unit,
    onViewModeChange: (AzkarReaderViewMode) -> Unit,
    onUpdateSettings: ((AzkarReaderSettings) -> AzkarReaderSettings) -> Unit,
) {
    AnimatedVisibility(
        visible = activeSheet != AzkarReaderSheet.None,
        enter = fadeIn(tween(AzkarMotion.sheetDurationMillis)) +
            slideInVertically(
                animationSpec = tween(
                    durationMillis = AzkarMotion.sheetDurationMillis,
                    easing = AzkarMotion.sheetEasing,
                ),
                initialOffsetY = { (it * 0.08f).roundToInt().coerceAtLeast(1) },
            ),
        exit = fadeOut(tween(AzkarMotion.sheetDurationMillis)) +
            slideOutVertically(
                animationSpec = tween(
                    durationMillis = AzkarMotion.sheetDurationMillis,
                    easing = AzkarMotion.sheetEasing,
                ),
                targetOffsetY = { (it * 0.08f).roundToInt().coerceAtLeast(1) },
            ),
    ) {
        when (activeSheet) {
            AzkarReaderSheet.Settings -> AzkarSettingsSheet(
                settings = settings,
                onDismiss = onDismiss,
                onUpdateSettings = onUpdateSettings,
            )
            AzkarReaderSheet.Contents -> AzkarContentsSheet(
                entries = entries,
                activeIndex = activeIndex,
                viewMode = viewMode,
                onDismiss = onDismiss,
                onSelect = onSelectContents,
                onViewModeChange = onViewModeChange,
            )
            AzkarReaderSheet.Explanation -> {
                val item = entries.firstOrNull { it.item.id == selectedExplanationId }?.item
                    ?: entries.getOrNull(activeIndex)?.item
                if (item != null) {
                    AzkarExplanationSheet(item = item, onDismiss = onDismiss)
                }
            }
            AzkarReaderSheet.None -> Unit
        }
    }
}

@Composable
private fun AzkarBottomSheet(
    title: String,
    eyebrow: String,
    subtitle: String?,
    maxHeightFraction: Float,
    specificTestTag: String,
    headerTopPadding: Dp = AzkarSpacing.sheetHeaderTop,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    val colors = AzkarThemeValues.colors
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.overlay)
            .testTag(AzkarSheetTestTags.Overlay)
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        val sheetShape = RoundedCornerShape(
            topStart = AzkarRadius.sheetTop,
            topEnd = AzkarRadius.sheetTop,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = AzkarDimensions.sheetMaxWidth)
                .heightIn(max = maxHeight * maxHeightFraction)
                .azkarShadow(AzkarElevation.BottomSheet, AzkarRadius.sheetTop)
                .clip(sheetShape)
                .background(colors.card)
                .border(AzkarBorders.thin, colors.border, sheetShape)
                .clickable(onClick = {})
                .testTag(specificTestTag),
        ) {
            Box(
                modifier = Modifier
                    .padding(top = AzkarSpacing.sheetHandleTop)
                    .align(Alignment.CenterHorizontally)
                    .width(AzkarDimensions.sheetHandleWidth)
                    .height(AzkarDimensions.sheetHandleHeight)
                    .clip(RoundedCornerShape(AzkarRadius.pill))
                    .background(colors.sheetHandle),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = AzkarSpacing.sheetHeaderHorizontal,
                        end = AzkarSpacing.sheetHeaderHorizontal,
                        top = headerTopPadding,
                        bottom = AzkarSpacing.sheetHeaderBottom,
                    ),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    BasicText(
                        text = eyebrow.uppercase(),
                        style = AzkarThemeValues.typography.sheetEyebrow.copy(color = colors.primary),
                    )
                    BasicText(
                        text = title,
                        style = AzkarThemeValues.typography.sheetTitle.copy(color = colors.foreground),
                    )
                    subtitle?.let {
                        BasicText(
                            text = it,
                            style = AzkarThemeValues.typography.sheetSubtitle.copy(color = colors.muted),
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(AzkarDimensions.sheetCloseButton)
                        .clickable(onClick = onDismiss)
                        .testTag(AzkarSheetTestTags.Close),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = "×",
                        style = AzkarThemeValues.typography.sheetTitle.copy(
                            color = colors.foreground,
                            fontSize = 27.sp,
                            lineHeight = 27.sp,
                            fontWeight = FontWeight.Normal,
                        ),
                    )
                }
            }
            content()
        }
    }
}

@Composable
private fun AzkarContentsSheet(
    entries: List<AzkarReaderEntry>,
    activeIndex: Int,
    viewMode: AzkarReaderViewMode,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
    onViewModeChange: (AzkarReaderViewMode) -> Unit,
) {
    val colors = AzkarThemeValues.colors
    AzkarBottomSheet(
        title = "Содержание",
        eyebrow = "Навигация",
        subtitle = if (viewMode == AzkarReaderViewMode.Cards) "По одному" else "Список",
        maxHeightFraction = AzkarDimensions.sheetMaxHeightFraction,
        specificTestTag = AzkarSheetTestTags.Contents,
        onDismiss = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                .padding(
                    start = AzkarSpacing.contentsListHorizontal,
                    end = AzkarSpacing.contentsListHorizontal,
                    bottom = AzkarSpacing.contentsListBottomBase,
                ),
            verticalArrangement = Arrangement.spacedBy(AzkarSpacing.contentsListGap),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AzkarSpacing.styleGridGap),
            ) {
                AzkarChoiceTile(
                    text = "По одному",
                    active = viewMode == AzkarReaderViewMode.Cards,
                    modifier = Modifier
                        .weight(1f)
                        .testTag(AzkarSheetTestTags.ContentsCardsMode),
                    onClick = { onViewModeChange(AzkarReaderViewMode.Cards) },
                )
                AzkarChoiceTile(
                    text = "Список",
                    active = viewMode == AzkarReaderViewMode.List,
                    modifier = Modifier
                        .weight(1f)
                        .testTag(AzkarSheetTestTags.ContentsListMode),
                    onClick = { onViewModeChange(AzkarReaderViewMode.List) },
                )
            }

            entries.forEachIndexed { index, entry ->
                val active = index == activeIndex
                val completed = entry.currentCount >= entry.item.count
                val shape = RoundedCornerShape(AzkarRadius.contentsItem)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = AzkarDimensions.contentsItemMinHeight)
                        .clip(shape)
                        .background(if (active) colors.activeItemBackground else colors.card)
                        .border(
                            AzkarBorders.thin,
                            if (active) colors.primary else colors.border,
                            shape,
                        )
                        .clickable { onSelect(index) }
                        .padding(
                            horizontal = AzkarSpacing.contentsItemHorizontal,
                            vertical = AzkarSpacing.contentsItemVertical,
                        )
                        .testTag(AzkarSheetTestTags.ContentsItemPrefix + index),
                    horizontalArrangement = Arrangement.spacedBy(AzkarSpacing.contentsItemGap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicText(
                        text = (index + 1).toString().padStart(2, '0'),
                        style = AzkarThemeValues.typography.cardNumber.copy(
                            color = if (active) colors.primary else colors.muted,
                        ),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        BasicText(
                            text = entry.item.title,
                            style = AzkarThemeValues.typography.contentsTitle.copy(
                                color = colors.foreground,
                            ),
                        )
                        BasicText(
                            text = "Повторений: ${entry.item.count}",
                            style = AzkarThemeValues.typography.contentsSubtitle.copy(
                                color = colors.muted,
                            ),
                        )
                    }
                    BasicText(
                        text = when {
                            active -> "Сейчас"
                            completed -> "✓"
                            else -> ""
                        },
                        style = AzkarThemeValues.typography.contentsSubtitle.copy(
                            color = if (completed || active) colors.success else colors.muted,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun AzkarExplanationSheet(
    item: AzkarReadingItem,
    onDismiss: () -> Unit,
) {
    val colors = AzkarThemeValues.colors
    AzkarBottomSheet(
        title = item.title,
        eyebrow = "Разъяснение",
        subtitle = null,
        maxHeightFraction = AzkarDimensions.insightSheetMaxHeightFraction,
        specificTestTag = AzkarSheetTestTags.Explanation,
        onDismiss = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                .padding(
                    start = AzkarSpacing.insightBodyHorizontal,
                    end = AzkarSpacing.insightBodyHorizontal,
                    bottom = AzkarSpacing.insightBodyBottomBase,
                ),
            verticalArrangement = Arrangement.spacedBy(AzkarSpacing.insightBodyGap),
        ) {
            AzkarInsightSection("Смысл") {
                BasicText(
                    text = item.translation,
                    style = AzkarThemeValues.typography.insightBody.copy(color = colors.foreground),
                )
            }
            item.note?.let { note ->
                AzkarInsightSection("Примечания учёных") {
                    BasicText(
                        text = note,
                        style = AzkarThemeValues.typography.insightBody.copy(color = colors.foreground),
                    )
                }
            }
            AzkarInsightSection("Источники разбора") {
                BasicText(
                    text = item.source,
                    style = AzkarThemeValues.typography.insightReferences.copy(color = colors.muted),
                )
            }
        }
    }
}

@Composable
private fun AzkarInsightSection(
    title: String,
    content: @Composable () -> Unit,
) {
    val colors = AzkarThemeValues.colors
    val shape = RoundedCornerShape(AzkarRadius.insightSection)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(AzkarBorders.thin, colors.border, shape)
            .padding(AzkarSpacing.insightSectionPadding),
        verticalArrangement = Arrangement.spacedBy(AzkarSpacing.scholarTop),
    ) {
        BasicText(
            text = title,
            style = AzkarThemeValues.typography.sectionHeading.copy(color = colors.foreground),
        )
        content()
    }
}

@Composable
private fun AzkarSettingsSheet(
    settings: AzkarReaderSettings,
    onDismiss: () -> Unit,
    onUpdateSettings: ((AzkarReaderSettings) -> AzkarReaderSettings) -> Unit,
) {
    AzkarBottomSheet(
        title = "Настройки чтения",
        eyebrow = "Настройки",
        subtitle = "Изменения применяются сразу",
        maxHeightFraction = AzkarDimensions.settingsSheetMaxHeightFraction,
        specificTestTag = AzkarSheetTestTags.Settings,
        headerTopPadding = AzkarSpacing.settingsHeaderTop,
        onDismiss = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                .padding(
                    start = AzkarSpacing.settingsBodyHorizontal,
                    end = AzkarSpacing.settingsBodyHorizontal,
                    bottom = AzkarSpacing.settingsBodyBottomBase,
                ),
            verticalArrangement = Arrangement.spacedBy(AzkarSpacing.settingsBodyGap),
        ) {
            AzkarSettingsPreview(settings)
            AzkarFontSettings(settings, onUpdateSettings)
            AzkarSizeSettings(settings, onUpdateSettings)
            AzkarStyleSettings(settings, onUpdateSettings)
            AzkarVisibilitySettings(settings, onUpdateSettings)
            AzkarThemeSettings(settings, onUpdateSettings)
        }
    }
}

@Composable
private fun AzkarSettingsPreview(settings: AzkarReaderSettings) {
    val colors = AzkarThemeValues.colors
    val shape = RoundedCornerShape(AzkarRadius.settingsPreview)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(AzkarBorders.thin, colors.border, shape)
            .padding(
                horizontal = AzkarSpacing.livePreviewHorizontal,
                vertical = AzkarSpacing.livePreviewVertical,
            ),
        verticalArrangement = Arrangement.spacedBy(AzkarSpacing.livePreviewGap),
    ) {
        BasicText(
            text = "بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ",
            modifier = Modifier.fillMaxWidth(),
            style = AzkarThemeValues.typography.arabicBody.copy(
                fontSize = minOf(settings.arabicSizeSp, AzkarDimensions.arabicPreviewMaxSp).sp,
                textAlign = TextAlign.Right,
            ),
        )
        BasicText(
            text = "Предпросмотр русского текста",
            style = AzkarThemeValues.typography.translation.copy(
                fontSize = minOf(settings.russianSizeSp, AzkarDimensions.russianPreviewMaxSp).sp,
            ),
        )
    }
}

@Composable
private fun AzkarFontSettings(
    settings: AzkarReaderSettings,
    update: ((AzkarReaderSettings) -> AzkarReaderSettings) -> Unit,
) {
    AzkarSettingsSection("Русский шрифт") {
        val options = listOf(
            RussianFontFamily.LITERATA to "Книжный",
            RussianFontFamily.PT_SERIF to "Классика",
            RussianFontFamily.INTER to "Современный",
            RussianFontFamily.MANROPE to "Компактный",
            RussianFontFamily.ANDROID_SANS to "Android Sans",
            RussianFontFamily.ANDROID_SERIF to "Android Serif",
        )
        Column(verticalArrangement = Arrangement.spacedBy(AzkarSpacing.fontGridGap)) {
            options.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(AzkarSpacing.fontGridGap)) {
                    row.forEach { (font, label) ->
                        AzkarFontTile(
                            title = label,
                            sample = "Азкар",
                            active = settings.russianFontFamily == font,
                            modifier = Modifier.weight(1f),
                            fontFamily = AzkarFontFamilies.russian(font),
                            onClick = { update { it.copy(russianFontFamily = font) } },
                        )
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }

    AzkarSettingsSection("Арабский шрифт") {
        val options = listOf(
            ArabicFontFamily.NOTO_NASKH_ARABIC to "Чёткий",
            ArabicFontFamily.NOTO_SANS_ARABIC to "Очень читаемый",
            ArabicFontFamily.AMIRI to "Классический",
            ArabicFontFamily.SCHEHERAZADE_NEW to "Мягкий",
        )
        Column(verticalArrangement = Arrangement.spacedBy(AzkarSpacing.fontGridGap)) {
            options.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(AzkarSpacing.fontGridGap)) {
                    row.forEach { (font, label) ->
                        AzkarFontTile(
                            title = label,
                            sample = "الذِّكْر",
                            active = settings.arabicFontFamily == font,
                            modifier = Modifier.weight(1f),
                            fontFamily = AzkarFontFamilies.arabic(font),
                            onClick = { update { it.copy(arabicFontFamily = font) } },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AzkarFontTile(
    title: String,
    sample: String,
    active: Boolean,
    modifier: Modifier,
    fontFamily: FontFamily,
    onClick: () -> Unit,
) {
    val colors = AzkarThemeValues.colors
    val shape = RoundedCornerShape(AzkarRadius.fontTile)
    Column(
        modifier = modifier
            .defaultMinSize(minHeight = AzkarDimensions.settingsFontTileMinHeight)
            .clip(shape)
            .background(if (active) colors.activeItemBackground else colors.card)
            .border(AzkarBorders.thin, if (active) colors.primary else colors.border, shape)
            .clickable(onClick = onClick)
            .padding(
                horizontal = AzkarSpacing.fontTileHorizontal,
                vertical = AzkarSpacing.fontTileVertical,
            ),
        verticalArrangement = Arrangement.spacedBy(AzkarSpacing.fontTileGap),
    ) {
        BasicText(
            text = title,
            style = AzkarThemeValues.typography.fontTileTitle.copy(
                color = colors.foreground,
                fontFamily = fontFamily,
            ),
        )
        BasicText(
            text = sample,
            style = AzkarThemeValues.typography.fontTileSample.copy(
                color = colors.muted,
                fontFamily = fontFamily,
            ),
        )
    }
}

@Composable
private fun AzkarSizeSettings(
    settings: AzkarReaderSettings,
    update: ((AzkarReaderSettings) -> AzkarReaderSettings) -> Unit,
) {
    AzkarSettingsSection("Размер и интервал") {
        Column(verticalArrangement = Arrangement.spacedBy(AzkarSpacing.controlGap)) {
            AzkarValueControl(
                label = "Арабский",
                valueLabel = settings.arabicSizeSp.roundToInt().toString(),
                value = settings.arabicSizeSp,
                min = AzkarDimensions.arabicSizeMinSp,
                max = AzkarDimensions.arabicSizeMaxSp,
                step = AzkarDimensions.arabicSizeStepSp,
                onValueChange = { v -> update { it.copy(arabicSizeSp = v) } },
            )
            AzkarValueControl(
                label = "Русский",
                valueLabel = settings.russianSizeSp.roundToInt().toString(),
                value = settings.russianSizeSp,
                min = AzkarDimensions.russianSizeMinSp,
                max = AzkarDimensions.russianSizeMaxSp,
                step = AzkarDimensions.russianSizeStepSp,
                onValueChange = { v -> update { it.copy(russianSizeSp = v) } },
            )
            AzkarValueControl(
                label = "Межстрочный",
                valueLabel = String.format("%.2f", settings.lineHeight),
                value = settings.lineHeight,
                min = AzkarDimensions.lineHeightMin,
                max = AzkarDimensions.lineHeightMax,
                step = AzkarDimensions.lineHeightStep,
                onValueChange = { v -> update { it.copy(lineHeight = v) } },
            )
        }
    }
}

@Composable
private fun AzkarValueControl(
    label: String,
    valueLabel: String,
    value: Float,
    min: Float,
    max: Float,
    step: Float,
    onValueChange: (Float) -> Unit,
) {
    val colors = AzkarThemeValues.colors
    val shape = RoundedCornerShape(AzkarRadius.control)
    val fraction = ((value - min) / (max - min)).coerceIn(0f, 1f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.card)
            .border(AzkarBorders.thin, colors.border, shape)
            .padding(
                horizontal = AzkarSpacing.controlHorizontal,
                vertical = AzkarSpacing.controlVertical,
            ),
        verticalArrangement = Arrangement.spacedBy(AzkarSpacing.controlGap),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            BasicText(
                text = label,
                modifier = Modifier.weight(1f),
                style = AzkarThemeValues.typography.sliderLabel.copy(color = colors.muted),
            )
            BasicText(
                text = valueLabel,
                style = AzkarThemeValues.typography.sliderValue.copy(color = colors.foreground),
            )
        }
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .pointerInput(min, max, step) {
                    fun valueFor(x: Float): Float {
                        val raw = (x / size.width).coerceIn(0f, 1f)
                        val stepped = ((raw * (max - min)) / step).roundToInt()
                        return (min + stepped * step).coerceIn(min, max)
                    }
                    detectHorizontalDragGestures(
                        onDragStart = { offset -> onValueChange(valueFor(offset.x)) },
                        onHorizontalDrag = { change, _ ->
                            onValueChange(valueFor(change.position.x))
                            change.consume()
                        },
                    )
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(AzkarRadius.pill))
                    .background(colors.surface),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(4.dp)
                    .clip(RoundedCornerShape(AzkarRadius.pill))
                    .background(colors.primary),
            )
            Box(
                modifier = Modifier
                    .offset(x = (maxWidth - 14.dp) * fraction)
                    .size(14.dp)
                    .clip(RoundedCornerShape(AzkarRadius.pill))
                    .background(colors.primary),
            )
        }
    }
}

@Composable
private fun AzkarStyleSettings(
    settings: AzkarReaderSettings,
    update: ((AzkarReaderSettings) -> AzkarReaderSettings) -> Unit,
) {
    AzkarSettingsSection("Стиль чтения") {
        Row(horizontalArrangement = Arrangement.spacedBy(AzkarSpacing.styleGridGap)) {
            AzkarChoiceTile(
                text = "Книжный",
                active = settings.readerStyle == AzkarReaderStyle.Book,
                modifier = Modifier.weight(1f),
                onClick = { update { it.copy(readerStyle = AzkarReaderStyle.Book) } },
            )
            AzkarChoiceTile(
                text = "Компактный",
                active = settings.readerStyle == AzkarReaderStyle.Compact,
                modifier = Modifier.weight(1f),
                onClick = { update { it.copy(readerStyle = AzkarReaderStyle.Compact) } },
            )
        }
    }
}

@Composable
private fun AzkarVisibilitySettings(
    settings: AzkarReaderSettings,
    update: ((AzkarReaderSettings) -> AzkarReaderSettings) -> Unit,
) {
    AzkarSettingsSection("Показывать") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AzkarRadius.toggleGrid))
                .border(
                    AzkarBorders.thin,
                    AzkarThemeValues.colors.border,
                    RoundedCornerShape(AzkarRadius.toggleGrid),
                )
                .padding(AzkarSpacing.toggleGridPadding),
            horizontalArrangement = Arrangement.spacedBy(AzkarSpacing.toggleGridGap),
        ) {
            AzkarToggleTile(
                text = "Перевод",
                checked = settings.showTranslation,
                modifier = Modifier.weight(1f).testTag(AzkarSheetTestTags.SettingTranslation),
                onClick = { update { it.copy(showTranslation = !it.showTranslation) } },
            )
            AzkarToggleTile(
                text = "Источники",
                checked = settings.showSources,
                modifier = Modifier.weight(1f).testTag(AzkarSheetTestTags.SettingSources),
                onClick = { update { it.copy(showSources = !it.showSources) } },
            )
            AzkarToggleTile(
                text = "Примечания",
                checked = settings.showNotes,
                modifier = Modifier.weight(1f).testTag(AzkarSheetTestTags.SettingNotes),
                onClick = { update { it.copy(showNotes = !it.showNotes) } },
            )
        }
    }
}

@Composable
private fun AzkarToggleTile(
    text: String,
    checked: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val colors = AzkarThemeValues.colors
    val shape = RoundedCornerShape(AzkarRadius.toggleItem)
    Column(
        modifier = modifier
            .defaultMinSize(minHeight = AzkarDimensions.toggleItemMinHeight)
            .clip(shape)
            .background(if (checked) colors.activeItemBackground else colors.card)
            .border(AzkarBorders.thin, if (checked) colors.primary else colors.border, shape)
            .clickable(onClick = onClick)
            .padding(
                horizontal = AzkarSpacing.toggleItemHorizontal,
                vertical = AzkarSpacing.toggleItemVertical,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AzkarSpacing.toggleItemGap),
    ) {
        BasicText(
            text = if (checked) "●" else "○",
            style = AzkarThemeValues.typography.toggleText.copy(
                color = if (checked) colors.primary else colors.muted,
            ),
        )
        BasicText(
            text = text,
            style = AzkarThemeValues.typography.toggleText.copy(color = colors.foreground),
        )
    }
}

@Composable
private fun AzkarThemeSettings(
    settings: AzkarReaderSettings,
    update: ((AzkarReaderSettings) -> AzkarReaderSettings) -> Unit,
) {
    AzkarSettingsSection("Тема") {
        Row(horizontalArrangement = Arrangement.spacedBy(AzkarSpacing.themeRowGap)) {
            AzkarChoiceTile(
                text = "Система",
                active = settings.themeMode == AzkarThemeMode.System,
                modifier = Modifier.weight(1f).testTag(AzkarSheetTestTags.ThemeSystem),
                onClick = { update { it.copy(themeMode = AzkarThemeMode.System) } },
            )
            AzkarChoiceTile(
                text = "Светлая",
                active = settings.themeMode == AzkarThemeMode.Light,
                modifier = Modifier.weight(1f).testTag(AzkarSheetTestTags.ThemeLight),
                onClick = { update { it.copy(themeMode = AzkarThemeMode.Light) } },
            )
            AzkarChoiceTile(
                text = "Тёмная",
                active = settings.themeMode == AzkarThemeMode.Dark,
                modifier = Modifier.weight(1f).testTag(AzkarSheetTestTags.ThemeDark),
                onClick = { update { it.copy(themeMode = AzkarThemeMode.Dark) } },
            )
        }
    }
}

@Composable
private fun AzkarSettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    val colors = AzkarThemeValues.colors
    Column(verticalArrangement = Arrangement.spacedBy(AzkarSpacing.settingsSectionGap)) {
        BasicText(
            text = title,
            style = AzkarThemeValues.typography.sectionHeading.copy(color = colors.foreground),
        )
        content()
    }
}

@Composable
private fun AzkarChoiceTile(
    text: String,
    active: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val colors = AzkarThemeValues.colors
    val shape = RoundedCornerShape(AzkarRadius.themeRow)
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 38.dp)
            .clip(shape)
            .background(if (active) colors.activeItemBackground else colors.card)
            .border(AzkarBorders.thin, if (active) colors.primary else colors.border, shape)
            .clickable(onClick = onClick)
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = text,
            style = AzkarThemeValues.typography.themeRowTitle.copy(
                color = if (active) colors.primary else colors.foreground,
                textAlign = TextAlign.Center,
            ),
        )
    }
}
