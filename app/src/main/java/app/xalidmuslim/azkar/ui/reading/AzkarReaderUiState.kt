package app.xalidmuslim.azkar.ui.reading

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.xalidmuslim.azkar.ui.designsystem.ArabicFontFamily
import app.xalidmuslim.azkar.ui.designsystem.AzkarDimensions
import app.xalidmuslim.azkar.ui.designsystem.AzkarThemeMode
import app.xalidmuslim.azkar.ui.designsystem.RussianFontFamily

enum class AzkarReaderSheet {
    None,
    Settings,
    Contents,
    Explanation,
}

enum class AzkarReaderStyle {
    Book,
    Compact,
}

enum class AzkarReaderViewMode {
    Cards,
    List,
}

data class AzkarReaderSettings(
    val russianFontFamily: RussianFontFamily = RussianFontFamily.LITERATA,
    val arabicFontFamily: ArabicFontFamily = ArabicFontFamily.NOTO_NASKH_ARABIC,
    val arabicSizeSp: Float = AzkarDimensions.defaultArabicSizeSp,
    val russianSizeSp: Float = AzkarDimensions.defaultRussianSizeSp,
    val lineHeight: Float = AzkarDimensions.defaultReaderLineHeight,
    val readerStyle: AzkarReaderStyle = AzkarReaderStyle.Book,
    val showTranslation: Boolean = true,
    val showSources: Boolean = true,
    val showNotes: Boolean = true,
    val themeMode: AzkarThemeMode = AzkarThemeMode.System,
)

data class AzkarReaderUiState(
    val activeSheet: AzkarReaderSheet = AzkarReaderSheet.None,
    val selectedExplanationId: String? = null,
    val settings: AzkarReaderSettings = AzkarReaderSettings(),
    val viewMode: AzkarReaderViewMode = AzkarReaderViewMode.Cards,
)

class AzkarReaderUiController(
    initialSettings: AzkarReaderSettings = AzkarReaderSettings(),
    initialViewMode: AzkarReaderViewMode = AzkarReaderViewMode.Cards,
) {
    var state by mutableStateOf(
        AzkarReaderUiState(
            settings = initialSettings,
            viewMode = initialViewMode,
        ),
    )
        private set

    fun openSettings() {
        state = state.copy(
            activeSheet = AzkarReaderSheet.Settings,
            selectedExplanationId = null,
        )
    }

    fun openContents() {
        state = state.copy(
            activeSheet = AzkarReaderSheet.Contents,
            selectedExplanationId = null,
        )
    }

    fun openExplanation(itemId: String) {
        state = state.copy(
            activeSheet = AzkarReaderSheet.Explanation,
            selectedExplanationId = itemId,
        )
    }

    fun closeSheet(): Boolean {
        if (state.activeSheet == AzkarReaderSheet.None) return false
        state = state.copy(
            activeSheet = AzkarReaderSheet.None,
            selectedExplanationId = null,
        )
        return true
    }

    fun updateSettings(transform: (AzkarReaderSettings) -> AzkarReaderSettings) {
        state = state.copy(settings = transform(state.settings))
    }

    fun setViewMode(viewMode: AzkarReaderViewMode): Boolean {
        if (state.viewMode == viewMode) return false
        state = state.copy(viewMode = viewMode)
        return true
    }

    fun handleBack(navigation: AzkarReaderNavigationController): Boolean {
        if (closeSheet()) return true
        return navigation.back()
    }
}
