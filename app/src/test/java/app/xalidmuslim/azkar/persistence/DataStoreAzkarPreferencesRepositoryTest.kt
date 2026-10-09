package app.xalidmuslim.azkar.persistence

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import app.xalidmuslim.azkar.ui.designsystem.ArabicFontFamily
import app.xalidmuslim.azkar.ui.designsystem.AzkarThemeMode
import app.xalidmuslim.azkar.ui.designsystem.RussianFontFamily
import app.xalidmuslim.azkar.ui.reading.AzkarReaderSettings
import app.xalidmuslim.azkar.ui.reading.AzkarReaderStyle
import app.xalidmuslim.azkar.ui.reading.AzkarReaderViewMode
import java.io.File
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreAzkarPreferencesRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val day1 = LocalDate.of(2026, 9, 29)
    private val day2 = LocalDate.of(2026, 9, 30)

    private fun repository(
        scope: CoroutineScope,
    ): Pair<DataStore<Preferences>, DataStoreAzkarPreferencesRepository> {
        val file = File(
            temporaryFolder.root,
            "azkar-${UUID.randomUUID()}.preferences_pb",
        )
        val store = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { file },
        )
        return store to DataStoreAzkarPreferencesRepository(store)
    }

    private suspend fun DataStoreAzkarPreferencesRepository.snapshot(
        date: LocalDate = day1,
        ids: Set<String> = setOf("muawwidhat"),
    ) = observeSnapshot(date, ids).first()

    @Test
    fun defaultSettingsCorrect() = runTest {
        val (_, repo) = repository(backgroundScope)
        val snapshot = repo.snapshot()
        val settings = snapshot.settings

        assertEquals(RussianFontFamily.LITERATA, settings.russianFontFamily)
        assertEquals(ArabicFontFamily.NOTO_NASKH_ARABIC, settings.arabicFontFamily)
        assertEquals(32f, settings.arabicSizeSp)
        assertEquals(17f, settings.russianSizeSp)
        assertEquals(1.65f, settings.lineHeight)
        assertEquals(AzkarReaderStyle.Book, settings.readerStyle)
        assertTrue(settings.showTranslation)
        assertTrue(settings.showSources)
        assertTrue(settings.showNotes)
        assertEquals(AzkarThemeMode.System, settings.themeMode)
        assertEquals(AzkarReaderViewMode.Cards, snapshot.viewMode)
    }

    @Test
    fun saveReadArabicSize() = runTest {
        val (_, repo) = repository(backgroundScope)
        repo.saveSettings(AzkarReaderSettings(arabicSizeSp = 37f))
        assertEquals(37f, repo.snapshot().settings.arabicSizeSp)
    }

    @Test
    fun saveReadRussianSize() = runTest {
        val (_, repo) = repository(backgroundScope)
        repo.saveSettings(AzkarReaderSettings(russianSizeSp = 21f))
        assertEquals(21f, repo.snapshot().settings.russianSizeSp)
    }

    @Test
    fun saveReadLineHeight() = runTest {
        val (_, repo) = repository(backgroundScope)
        repo.saveSettings(AzkarReaderSettings(lineHeight = 1.4f))
        assertEquals(1.4f, repo.snapshot().settings.lineHeight)
    }

    @Test
    fun saveReadArabicFont() = runTest {
        val (_, repo) = repository(backgroundScope)
        repo.saveSettings(AzkarReaderSettings(arabicFontFamily = ArabicFontFamily.AMIRI))
        assertEquals(ArabicFontFamily.AMIRI, repo.snapshot().settings.arabicFontFamily)
    }

    @Test
    fun saveReadRussianFont() = runTest {
        val (_, repo) = repository(backgroundScope)
        repo.saveSettings(AzkarReaderSettings(russianFontFamily = RussianFontFamily.MANROPE))
        assertEquals(RussianFontFamily.MANROPE, repo.snapshot().settings.russianFontFamily)
    }

    @Test
    fun saveReadReaderStyle() = runTest {
        val (_, repo) = repository(backgroundScope)
        repo.saveSettings(AzkarReaderSettings(readerStyle = AzkarReaderStyle.Compact))
        assertEquals(AzkarReaderStyle.Compact, repo.snapshot().settings.readerStyle)
    }

    @Test
    fun saveReadTheme() = runTest {
        val (_, repo) = repository(backgroundScope)
        repo.saveSettings(AzkarReaderSettings(themeMode = AzkarThemeMode.Dark))
        assertEquals(AzkarThemeMode.Dark, repo.snapshot().settings.themeMode)
    }

    @Test
    fun saveReadTranslationFlag() = runTest {
        val (_, repo) = repository(backgroundScope)
        repo.saveSettings(AzkarReaderSettings(showTranslation = false))
        assertFalse(repo.snapshot().settings.showTranslation)
    }

    @Test
    fun saveReadSourcesFlag() = runTest {
        val (_, repo) = repository(backgroundScope)
        repo.saveSettings(AzkarReaderSettings(showSources = false))
        assertFalse(repo.snapshot().settings.showSources)
    }

    @Test
    fun saveReadNotesFlag() = runTest {
        val (_, repo) = repository(backgroundScope)
        repo.saveSettings(AzkarReaderSettings(showNotes = false))
        assertFalse(repo.snapshot().settings.showNotes)
    }

    @Test
    fun saveReadViewModeCardsList() = runTest {
        val (_, repo) = repository(backgroundScope)
        repo.saveViewMode(AzkarReaderViewMode.List)
        assertEquals(AzkarReaderViewMode.List, repo.snapshot().viewMode)
        repo.saveViewMode(AzkarReaderViewMode.Cards)
        assertEquals(AzkarReaderViewMode.Cards, repo.snapshot().viewMode)
    }

    @Test
    fun invalidNumericValuesClampOrFallback() = runTest {
        val (store, repo) = repository(backgroundScope)
        store.edit { preferences ->
            preferences[AzkarPreferenceKeys.ArabicSize] = 500f
            preferences[AzkarPreferenceKeys.RussianSize] = -100f
            preferences[AzkarPreferenceKeys.LineHeight] = Float.NaN
        }

        val settings = repo.snapshot().settings
        assertEquals(44f, settings.arabicSizeSp)
        assertEquals(13f, settings.russianSizeSp)
        assertEquals(1.65f, settings.lineHeight)
    }

    @Test
    fun invalidEnumAndFontValuesFallback() = runTest {
        val (store, repo) = repository(backgroundScope)
        store.edit { preferences ->
            preferences[AzkarPreferenceKeys.ArabicFont] = "UNKNOWN_ARABIC"
            preferences[AzkarPreferenceKeys.RussianFont] = "UNKNOWN_RUSSIAN"
            preferences[AzkarPreferenceKeys.Theme] = "UNKNOWN_THEME"
            preferences[AzkarPreferenceKeys.ReaderStyle] = "UNKNOWN_STYLE"
            preferences[AzkarPreferenceKeys.ViewMode] = "UNKNOWN_MODE"
        }

        val snapshot = repo.snapshot()
        assertEquals(ArabicFontFamily.NOTO_NASKH_ARABIC, snapshot.settings.arabicFontFamily)
        assertEquals(RussianFontFamily.LITERATA, snapshot.settings.russianFontFamily)
        assertEquals(AzkarThemeMode.System, snapshot.settings.themeMode)
        assertEquals(AzkarReaderStyle.Book, snapshot.settings.readerStyle)
        assertEquals(AzkarReaderViewMode.Cards, snapshot.viewMode)
    }

    @Test
    fun progressInitiallyZero() = runTest {
        val (_, repo) = repository(backgroundScope)
        assertEquals(0, repo.snapshot().progressById.getValue("muawwidhat"))
    }

    @Test
    fun incrementCount() = runTest {
        val (_, repo) = repository(backgroundScope)
        assertEquals(1, repo.incrementProgress(day1, "muawwidhat", 3))
        assertEquals(1, repo.snapshot().progressById.getValue("muawwidhat"))
    }

    @Test
    fun countClampsAtTarget() = runTest {
        val (_, repo) = repository(backgroundScope)
        repeat(6) { repo.incrementProgress(day1, "muawwidhat", 3) }
        assertEquals(3, repo.snapshot().progressById.getValue("muawwidhat"))
    }

    @Test
    fun progressPersistsAcrossRepositoryRecreation() = runTest {
        val (store, repo1) = repository(backgroundScope)
        repeat(3) { repo1.incrementProgress(day1, "muawwidhat", 3) }
        val repo2 = DataStoreAzkarPreferencesRepository(store)
        assertEquals(3, repo2.snapshot().progressById.getValue("muawwidhat"))
    }

    @Test
    fun differentDhikrIdsHaveIndependentProgress() = runTest {
        val (_, repo) = repository(backgroundScope)
        repeat(2) { repo.incrementProgress(day1, "muawwidhat", 3) }
        repo.incrementProgress(day1, "kingdom", 1)

        val snapshot = repo.snapshot(ids = setOf("muawwidhat", "kingdom"))
        assertEquals(2, snapshot.progressById.getValue("muawwidhat"))
        assertEquals(1, snapshot.progressById.getValue("kingdom"))
    }

    @Test
    fun differentCalendarDatesHaveIndependentProgress() = runTest {
        val (_, repo) = repository(backgroundScope)
        repeat(3) { repo.incrementProgress(day1, "muawwidhat", 3) }

        assertEquals(3, repo.snapshot(day1).progressById.getValue("muawwidhat"))
        assertEquals(0, repo.snapshot(day2).progressById.getValue("muawwidhat"))
    }

    @Test
    fun resetVisibleEntries() = runTest {
        val (_, repo) = repository(backgroundScope)
        repo.incrementProgress(day1, "one", 3)
        repo.incrementProgress(day1, "two", 3)
        repo.resetProgress(day1, setOf("one", "two"))

        val snapshot = repo.snapshot(ids = setOf("one", "two"))
        assertEquals(0, snapshot.progressById.getValue("one"))
        assertEquals(0, snapshot.progressById.getValue("two"))
    }

    @Test
    fun resetDoesNotDeleteUnrelatedEntry() = runTest {
        val (_, repo) = repository(backgroundScope)
        repo.incrementProgress(day1, "visible", 1)
        repo.incrementProgress(day1, "unrelated", 1)
        repo.resetProgress(day1, setOf("visible"))

        val snapshot = repo.snapshot(ids = setOf("visible", "unrelated"))
        assertEquals(0, snapshot.progressById.getValue("visible"))
        assertEquals(1, snapshot.progressById.getValue("unrelated"))
    }

    @Test
    fun oneIncrementActionProducesOneIncrement() = runTest {
        val (_, repo) = repository(backgroundScope)
        repo.incrementProgress(day1, "muawwidhat", 100)
        assertEquals(1, repo.snapshot().progressById.getValue("muawwidhat"))
    }
}
