package app.xalidmuslim.azkar.ui.reading

import app.xalidmuslim.azkar.persistence.AzkarDateProvider
import app.xalidmuslim.azkar.persistence.AzkarPreferencesRepository
import app.xalidmuslim.azkar.persistence.AzkarPreferencesSnapshot
import app.xalidmuslim.azkar.ui.designsystem.AzkarThemeMode
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AzkarReaderPersistenceControllerTest {
    private class FakeDateProvider(var date: LocalDate) : AzkarDateProvider {
        override fun currentDate(): LocalDate = date
    }

    private class FakeRepository : AzkarPreferencesRepository {
        private var settings = AzkarReaderSettings()
        private var viewMode = AzkarReaderViewMode.Cards
        private val progress = mutableMapOf<Pair<LocalDate, String>, Int>()
        private val version = MutableStateFlow(0)

        override fun observeSnapshot(
            date: LocalDate,
            visibleItemIds: Set<String>,
        ): Flow<AzkarPreferencesSnapshot> = version.map {
            AzkarPreferencesSnapshot(
                settings = settings,
                viewMode = viewMode,
                progressById = visibleItemIds.associateWith { id ->
                    progress[date to id] ?: 0
                },
            )
        }

        override suspend fun saveSettings(settings: AzkarReaderSettings) {
            this.settings = settings
            version.value += 1
        }

        override suspend fun saveViewMode(viewMode: AzkarReaderViewMode) {
            this.viewMode = viewMode
            version.value += 1
        }

        override suspend fun incrementProgress(
            date: LocalDate,
            stableDhikrId: String,
            target: Int,
        ): Int {
            val key = date to stableDhikrId
            val updated = ((progress[key] ?: 0) + 1).coerceAtMost(target)
            progress[key] = updated
            version.value += 1
            return updated
        }

        override suspend fun resetProgress(
            date: LocalDate,
            visibleItemIds: Set<String>,
        ) {
            visibleItemIds.forEach { progress[date to it] = 0 }
            version.value += 1
        }
    }

    @Test
    fun progressHeaderCountsCompletedItemsNotRepetitions() {
        val entries = listOf(
            AzkarReaderEntry(AzkarGoldenReadingFixtures.BaqarahLastTwo, currentCount = 1),
            AzkarReaderEntry(AzkarGoldenReadingFixtures.Muawwidhat, currentCount = 2),
            AzkarReaderEntry(
                AzkarGoldenReadingFixtures.Muawwidhat.copy(id = "hundred", count = 100),
                currentCount = 50,
            ),
        )
        assertEquals(1, countCompletedItems(entries))
    }

    @Test
    fun cardsAndListShareOneProgressStateAndSettingsPersist() = runTest {
        val repository = FakeRepository()
        val dateProvider = FakeDateProvider(LocalDate.of(2026, 9, 29))
        val uiScope = backgroundScope
        val visible = setOf("muawwidhat")

        val first = AzkarReaderUiController(
            repository = repository,
            dateProvider = dateProvider,
            persistenceScope = uiScope,
            visibleItemIds = visible,
        )
        testScheduler.runCurrent()
        assertTrue(first.state.isHydrated)

        first.incrementProgress("muawwidhat", 3)
        first.setViewMode(AzkarReaderViewMode.List)
        first.updateSettings {
            it.copy(
                arabicSizeSp = 37f,
                russianSizeSp = 21f,
                lineHeight = 1.4f,
                readerStyle = AzkarReaderStyle.Compact,
                showTranslation = false,
                showSources = false,
                showNotes = false,
                themeMode = AzkarThemeMode.Dark,
            )
        }
        testScheduler.runCurrent()

        assertEquals(1, first.currentCount("muawwidhat"))
        assertEquals(AzkarReaderViewMode.List, first.state.viewMode)

        val second = AzkarReaderUiController(
            repository = repository,
            dateProvider = dateProvider,
            persistenceScope = uiScope,
            visibleItemIds = visible,
        )
        testScheduler.runCurrent()

        assertEquals(1, second.currentCount("muawwidhat"))
        assertEquals(AzkarReaderViewMode.List, second.state.viewMode)
        assertEquals(37f, second.state.settings.arabicSizeSp)
        assertEquals(21f, second.state.settings.russianSizeSp)
        assertEquals(1.4f, second.state.settings.lineHeight)
        assertEquals(AzkarReaderStyle.Compact, second.state.settings.readerStyle)
        assertFalse(second.state.settings.showTranslation)
        assertFalse(second.state.settings.showSources)
        assertFalse(second.state.settings.showNotes)
        assertEquals(AzkarThemeMode.Dark, second.state.settings.themeMode)
    }

    @Test
    fun newCalendarDateStartsAtZeroForNewSession() = runTest {
        val repository = FakeRepository()
        val dateProvider = FakeDateProvider(LocalDate.of(2026, 9, 29))
        val uiScope = backgroundScope
        val visible = setOf("muawwidhat")

        val dayOne = AzkarReaderUiController(
            repository = repository,
            dateProvider = dateProvider,
            persistenceScope = uiScope,
            visibleItemIds = visible,
        )
        testScheduler.runCurrent()
        repeat(3) { dayOne.incrementProgress("muawwidhat", 3) }
        testScheduler.runCurrent()
        assertEquals(3, dayOne.currentCount("muawwidhat"))

        dateProvider.date = LocalDate.of(2026, 9, 30)
        val dayTwo = AzkarReaderUiController(
            repository = repository,
            dateProvider = dateProvider,
            persistenceScope = uiScope,
            visibleItemIds = visible,
        )
        testScheduler.runCurrent()

        assertEquals(0, dayTwo.currentCount("muawwidhat"))
    }

    @Test
    fun sheetOpenCloseDoesNotChangePersistentReaderState() = runTest {
        val repository = FakeRepository()
        val dateProvider = FakeDateProvider(LocalDate.of(2026, 9, 29))
        val ui = AzkarReaderUiController(
            repository = repository,
            dateProvider = dateProvider,
            persistenceScope = backgroundScope,
            visibleItemIds = setOf("muawwidhat"),
        )
        testScheduler.runCurrent()

        ui.openSettings()
        ui.closeSheet()
        ui.openContents()
        ui.closeSheet()
        ui.openExplanation("muawwidhat")
        ui.closeSheet()
        testScheduler.runCurrent()

        assertEquals(AzkarReaderSettings(), ui.state.settings)
        assertEquals(AzkarReaderViewMode.Cards, ui.state.viewMode)
        assertEquals(0, ui.currentCount("muawwidhat"))
    }
}
