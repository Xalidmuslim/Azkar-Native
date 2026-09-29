package app.xalidmuslim.azkar.persistence

import app.xalidmuslim.azkar.ui.reading.AzkarReaderSettings
import app.xalidmuslim.azkar.ui.reading.AzkarReaderViewMode
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

data class AzkarPreferencesSnapshot(
    val settings: AzkarReaderSettings = AzkarReaderSettings(),
    val viewMode: AzkarReaderViewMode = AzkarReaderViewMode.Cards,
    val progressById: Map<String, Int> = emptyMap(),
)

interface AzkarPreferencesRepository {
    fun observeSnapshot(
        date: LocalDate,
        visibleItemIds: Set<String>,
    ): Flow<AzkarPreferencesSnapshot>

    suspend fun saveSettings(settings: AzkarReaderSettings)
    suspend fun saveViewMode(viewMode: AzkarReaderViewMode)

    suspend fun incrementProgress(
        date: LocalDate,
        stableDhikrId: String,
        target: Int,
    ): Int

    suspend fun resetProgress(
        date: LocalDate,
        visibleItemIds: Set<String>,
    )
}
