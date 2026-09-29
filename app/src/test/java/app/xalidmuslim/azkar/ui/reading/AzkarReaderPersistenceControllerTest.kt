package app.xalidmuslim.azkar.ui.reading

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.xalidmuslim.azkar.persistence.AzkarDateProvider
import app.xalidmuslim.azkar.persistence.DataStoreAzkarPreferencesRepository
import app.xalidmuslim.azkar.ui.designsystem.AzkarThemeMode
import java.io.File
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AzkarReaderPersistenceControllerTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private class FakeDateProvider(var date: LocalDate) : AzkarDateProvider {
        override fun currentDate(): LocalDate = date
    }

    private fun store(scope: CoroutineScope) =
        PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = {
                File(temporaryFolder.root, "controller-${UUID.randomUUID()}.preferences_pb")
            },
        )

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
        val dataStore = store(backgroundScope)
        val repository = DataStoreAzkarPreferencesRepository(dataStore)
        val dateProvider = FakeDateProvider(LocalDate.of(2026, 9, 29))
        val uiScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val visible = setOf("muawwidhat")

        val first = AzkarReaderUiController(
            repository = repository,
            dateProvider = dateProvider,
            persistenceScope = uiScope,
            visibleItemIds = visible,
        )
        advanceUntilIdle()
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
        advanceUntilIdle()

        assertEquals(1, first.currentCount("muawwidhat"))
        assertEquals(AzkarReaderViewMode.List, first.state.viewMode)

        val second = AzkarReaderUiController(
            repository = DataStoreAzkarPreferencesRepository(dataStore),
            dateProvider = dateProvider,
            persistenceScope = uiScope,
            visibleItemIds = visible,
        )
        advanceUntilIdle()

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
        val dataStore = store(backgroundScope)
        val repository = DataStoreAzkarPreferencesRepository(dataStore)
        val dateProvider = FakeDateProvider(LocalDate.of(2026, 9, 29))
        val uiScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val visible = setOf("muawwidhat")

        val dayOne = AzkarReaderUiController(
            repository = repository,
            dateProvider = dateProvider,
            persistenceScope = uiScope,
            visibleItemIds = visible,
        )
        advanceUntilIdle()
        repeat(3) { dayOne.incrementProgress("muawwidhat", 3) }
        advanceUntilIdle()
        assertEquals(
            3,
            repository.observeSnapshot(dateProvider.date, visible).first()
                .progressById.getValue("muawwidhat"),
        )

        dateProvider.date = LocalDate.of(2026, 9, 30)
        val dayTwo = AzkarReaderUiController(
            repository = repository,
            dateProvider = dateProvider,
            persistenceScope = uiScope,
            visibleItemIds = visible,
        )
        advanceUntilIdle()

        assertEquals(0, dayTwo.currentCount("muawwidhat"))
    }

    @Test
    fun sheetOpenCloseDoesNotChangePersistentReaderState() = runTest {
        val dataStore = store(backgroundScope)
        val repository = DataStoreAzkarPreferencesRepository(dataStore)
        val dateProvider = FakeDateProvider(LocalDate.of(2026, 9, 29))
        val uiScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val ui = AzkarReaderUiController(
            repository = repository,
            dateProvider = dateProvider,
            persistenceScope = uiScope,
            visibleItemIds = setOf("muawwidhat"),
        )
        advanceUntilIdle()

        ui.openSettings()
        ui.closeSheet()
        ui.openContents()
        ui.closeSheet()
        ui.openExplanation("muawwidhat")
        ui.closeSheet()
        advanceUntilIdle()

        val snapshot = repository.observeSnapshot(
            dateProvider.date,
            setOf("muawwidhat"),
        ).first()
        assertEquals(AzkarReaderSettings(), snapshot.settings)
        assertEquals(AzkarReaderViewMode.Cards, snapshot.viewMode)
        assertEquals(0, snapshot.progressById.getValue("muawwidhat"))
    }
}
