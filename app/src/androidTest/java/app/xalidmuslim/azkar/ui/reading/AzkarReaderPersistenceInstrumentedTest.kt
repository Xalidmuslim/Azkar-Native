package app.xalidmuslim.azkar.ui.reading

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import app.xalidmuslim.azkar.persistence.AzkarDateProvider
import app.xalidmuslim.azkar.persistence.AzkarPreferencesRepository
import app.xalidmuslim.azkar.persistence.AzkarPreferencesSnapshot
import app.xalidmuslim.azkar.persistence.DataStoreAzkarPreferencesRepository
import app.xalidmuslim.azkar.ui.designsystem.AzkarThemeMode
import java.io.File
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AzkarReaderPersistenceInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    private class FakeDateProvider(var date: LocalDate) : AzkarDateProvider {
        override fun currentDate(): LocalDate = date
    }

    private lateinit var dataStoreScope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var backingRepository: DataStoreAzkarPreferencesRepository
    private lateinit var repository: AzkarPreferencesRepository
    private lateinit var dateProvider: FakeDateProvider
    private lateinit var dataStoreFile: File

    private lateinit var navigation: AzkarReaderNavigationController
    private lateinit var readerUi: AzkarReaderUiController
    private lateinit var generation: MutableIntState
    private var renderedGeneration: Int = -1


    private class ResetTracingRepository(
        private val delegate: DataStoreAzkarPreferencesRepository,
        private val snapshotIds: Set<String>,
    ) : AzkarPreferencesRepository {
        val resetCalled = CompletableDeferred<Pair<LocalDate, Set<String>>>()
        val resetWriteCompleted = CompletableDeferred<AzkarPreferencesSnapshot>()

        override fun observeSnapshot(
            date: LocalDate,
            visibleItemIds: Set<String>,
        ): Flow<AzkarPreferencesSnapshot> = delegate.observeSnapshot(date, visibleItemIds)

        override suspend fun saveSettings(settings: AzkarReaderSettings) {
            delegate.saveSettings(settings)
        }

        override suspend fun saveViewMode(viewMode: AzkarReaderViewMode) {
            delegate.saveViewMode(viewMode)
        }

        override suspend fun incrementProgress(
            date: LocalDate,
            stableDhikrId: String,
            target: Int,
        ): Int = delegate.incrementProgress(date, stableDhikrId, target)

        override suspend fun resetProgress(
            date: LocalDate,
            visibleItemIds: Set<String>,
        ) {
            val ids = visibleItemIds.toSet()
            Log.i(
                "AzkarPhase4Reset",
                "repository reset called date=$date visibleIds=$ids",
            )
            resetCalled.complete(date to ids)
            delegate.resetProgress(date, ids)
            val snapshot = delegate.observeSnapshot(date, snapshotIds).first()
            Log.i(
                "AzkarPhase4Reset",
                "repository reset write completed snapshot=${snapshot.progressById} " +
                    "viewMode=${snapshot.viewMode} settings=${snapshot.settings}",
            )
            resetWriteCompleted.complete(snapshot)
        }
    }

    private fun item(
        id: String,
        title: String,
        count: Int,
        hasInsight: Boolean = false,
    ) = AzkarReadingItem(
        id = id,
        title = title,
        count = count,
        disputed = false,
        arabic = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ",
        translation = "Тестовый текст для проверки поведения reader.",
        source = "Источник для UI-теста",
        note = null,
        hasInsight = hasInsight,
    )

    private fun entries() = listOf(
        AzkarReaderEntry(item("one", "Первый азкар", count = 1, hasInsight = true)),
        AzkarReaderEntry(item("three", "Второй азкар", count = 3, hasInsight = true)),
        AzkarReaderEntry(item("hundred", "Третий азкар", count = 100)),
    )

    @Before
    fun setUpStorage() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        dataStoreFile = File(
            context.filesDir,
            "phase4-${UUID.randomUUID()}.preferences_pb",
        )
        dataStoreFile.delete()
        dataStoreScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        dataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { dataStoreFile },
        )
        backingRepository = DataStoreAzkarPreferencesRepository(dataStore)
        repository = backingRepository
        dateProvider = FakeDateProvider(LocalDate.of(2026, 9, 29))
    }

    @After
    fun tearDownStorage() {
        dataStoreScope.cancel()
        dataStoreFile.delete()
    }

    private fun setReader(initialIndex: Int = 0) {
        val testEntries = entries()
        composeRule.setContent {
            val generationState = remember { mutableIntStateOf(0) }
            generation = generationState
            key(generationState.intValue) {
                val nav = remember(generationState.intValue) {
                    AzkarReaderNavigationController(testEntries.size, initialIndex)
                }
                val scope = rememberCoroutineScope()
                val ui = remember(generationState.intValue) {
                    AzkarReaderUiController(
                        repository = repository,
                        dateProvider = dateProvider,
                        persistenceScope = scope,
                        visibleItemIds = testEntries.map { it.item.id }.toSet(),
                    )
                }
                navigation = nav
                readerUi = ui
                renderedGeneration = generationState.intValue
                AzkarReaderScreen(
                    entries = testEntries,
                    period = AzkarPeriod.Morning,
                    controller = nav,
                    uiController = ui,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        waitForHydration(expectedGeneration = 0)
    }

    private fun recreateReader() {
        var expected = 0
        composeRule.runOnIdle {
            expected = generation.intValue + 1
            generation.intValue = expected
        }
        waitForHydration(expected)
    }

    private fun waitForHydration(expectedGeneration: Int) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            renderedGeneration == expectedGeneration &&
                ::readerUi.isInitialized &&
                readerUi.state.isHydrated
        }
        composeRule.waitForIdle()
    }

    private fun openContents() {
        composeRule.onNodeWithTag(AzkarReadingTestTags.OpenContents)
            .assertIsDisplayed()
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(AzkarSheetTestTags.Contents).assertIsDisplayed()
    }

    private fun openSettings() {
        composeRule.onNodeWithTag(AzkarReadingTestTags.OpenSettingsTop)
            .assertIsDisplayed()
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(AzkarSheetTestTags.Settings).assertIsDisplayed()
    }

    private fun chooseListMode() {
        openContents()
        composeRule.onNodeWithTag(AzkarSheetTestTags.ContentsListMode)
            .assertIsDisplayed()
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(AzkarReadingTestTags.List).assertIsDisplayed()
    }

    private fun chooseCardsMode() {
        openContents()
        composeRule.onNodeWithTag(AzkarSheetTestTags.ContentsCardsMode)
            .assertIsDisplayed()
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(AzkarReadingTestTags.Card).assertIsDisplayed()
    }

    private fun clickCount(id: String) {
        composeRule.onNodeWithTag(AzkarReadingTestTags.CountActionPrefix + id)
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeRule.waitForIdle()
    }

    private fun awaitSnapshot(
        label: String,
        ids: Set<String> = entries().map { it.item.id }.toSet(),
        predicate: (AzkarPreferencesSnapshot) -> Boolean,
    ): AzkarPreferencesSnapshot = runBlocking {
        Log.i(
            "AzkarPhase4Reset",
            "await start label=$label date=${dateProvider.date} ids=$ids file=${dataStoreFile.name}",
        )
        try {
            withTimeout(15_000) {
                repository.observeSnapshot(dateProvider.date, ids).first { snapshot ->
                    Log.i(
                        "AzkarPhase4Reset",
                        "await emission label=$label progress=${snapshot.progressById} " +
                            "viewMode=${snapshot.viewMode} settings=${snapshot.settings}",
                    )
                    predicate(snapshot)
                }
            }.also { snapshot ->
                Log.i(
                    "AzkarPhase4Reset",
                    "await satisfied label=$label progress=${snapshot.progressById}",
                )
            }
        } catch (error: Throwable) {
            Log.e(
                "AzkarPhase4Reset",
                "await failed label=$label controller=${readerUi.state} " +
                    "date=${dateProvider.date} file=${dataStoreFile.absolutePath}",
                error,
            )
            throw error
        }
    }

    private fun currentSnapshot(
        label: String,
        ids: Set<String> = entries().map { it.item.id }.toSet(),
    ): AzkarPreferencesSnapshot = runBlocking {
        withTimeout(5_000) {
            repository.observeSnapshot(dateProvider.date, ids).first()
        }.also { snapshot ->
            Log.i(
                "AzkarPhase4Reset",
                "snapshot label=$label date=${dateProvider.date} ids=$ids " +
                    "progress=${snapshot.progressById} viewMode=${snapshot.viewMode} " +
                    "settings=${snapshot.settings}",
            )
        }
    }

    @Test
    fun countZeroToOneInCards() {
        setReader()
        clickCount("one")

        composeRule.runOnIdle {
            assertEquals(1, readerUi.currentCount("one"))
        }
        awaitSnapshot("generic") { it.progressById["one"] == 1 }
    }

    @Test
    fun completedVisualAppears() {
        setReader()
        clickCount("one")

        composeRule.onNodeWithText("Выполнено").assertIsDisplayed()
        composeRule.onNodeWithTag(AzkarReadingTestTags.CountActionPrefix + "one")
            .assertIsNotEnabled()
        composeRule.onNodeWithText("✓ Готово").assertIsDisplayed()
    }

    @Test
    fun countSurvivesReaderRecreation() {
        setReader()
        clickCount("one")
        awaitSnapshot("generic") { it.progressById["one"] == 1 }

        recreateReader()

        composeRule.runOnIdle { assertEquals(1, readerUi.currentCount("one")) }
        composeRule.onNodeWithText("Выполнено").assertIsDisplayed()
    }

    @Test
    fun countVisibleAfterCardsToList() {
        setReader()
        clickCount("one")
        chooseListMode()

        composeRule.onNodeWithTag(AzkarReadingTestTags.ListCardPrefix + "one")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Выполнено").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(1, readerUi.currentCount("one")) }
    }

    @Test
    fun incrementInListVisibleAfterListToCards() {
        setReader()
        chooseListMode()
        clickCount("one")
        chooseCardsMode()

        composeRule.runOnIdle { assertEquals(1, readerUi.currentCount("one")) }
        composeRule.onNodeWithText("Выполнено").assertIsDisplayed()
    }

    @Test
    fun resetClearsCurrentVisibleEntries() {
        val visibleIds = entries().map { it.item.id }.toSet()
        val unrelatedId = "unrelated"
        val snapshotIds = visibleIds + unrelatedId
        val seededSettings = AzkarReaderSettings(showSources = false)

        Log.i(
            "AzkarPhase4Reset",
            "test start date=${dateProvider.date} visibleIds=$visibleIds " +
                "snapshotIds=$snapshotIds file=${dataStoreFile.absolutePath}",
        )

        runBlocking {
            backingRepository.saveSettings(seededSettings)
            backingRepository.incrementProgress(dateProvider.date, "one", 1)
            backingRepository.incrementProgress(dateProvider.date, "three", 3)
            backingRepository.incrementProgress(dateProvider.date, unrelatedId, 1)
        }
        val beforeReset = currentSnapshot("before_reset_seeded", snapshotIds)
        assertEquals(1, beforeReset.progressById.getValue("one"))
        assertEquals(1, beforeReset.progressById.getValue("three"))
        assertEquals(0, beforeReset.progressById.getValue("hundred"))
        assertEquals(1, beforeReset.progressById.getValue(unrelatedId))
        assertEquals(seededSettings, beforeReset.settings)

        val tracingRepository = ResetTracingRepository(
            delegate = backingRepository,
            snapshotIds = snapshotIds,
        )
        repository = tracingRepository
        setReader()

        val settingsBefore = readerUi.state.settings
        val viewModeBefore = readerUi.state.viewMode
        val historyBefore = navigation.state.history.toList()
        composeRule.runOnIdle {
            Log.i(
                "AzkarPhase4Reset",
                "hydrated controller progress=${readerUi.state.progressById} " +
                    "settings=${readerUi.state.settings} viewMode=${readerUi.state.viewMode} " +
                    "history=${navigation.state.history}",
            )
            assertEquals(1, readerUi.currentCount("one"))
            assertEquals(1, readerUi.currentCount("three"))
            assertEquals(0, readerUi.currentCount("hundred"))
        }

        Log.i("AzkarPhase4Reset", "tap Reset begin")
        composeRule.onNodeWithTag(AzkarReadingTestTags.ResetProgress)
            .assertIsDisplayed()
            .performClick()
        composeRule.waitForIdle()
        Log.i("AzkarPhase4Reset", "tap Reset returned")

        val called = runBlocking {
            withTimeout(15_000) { tracingRepository.resetCalled.await() }
        }
        assertEquals(dateProvider.date, called.first)
        assertEquals(visibleIds, called.second)

        val snapshotImmediatelyAfterWrite = runBlocking {
            withTimeout(15_000) { tracingRepository.resetWriteCompleted.await() }
        }
        assertEquals(0, snapshotImmediatelyAfterWrite.progressById.getValue("one"))
        assertEquals(0, snapshotImmediatelyAfterWrite.progressById.getValue("three"))
        assertEquals(0, snapshotImmediatelyAfterWrite.progressById.getValue("hundred"))
        assertEquals(1, snapshotImmediatelyAfterWrite.progressById.getValue(unrelatedId))
        assertEquals(settingsBefore, snapshotImmediatelyAfterWrite.settings)
        assertEquals(viewModeBefore, snapshotImmediatelyAfterWrite.viewMode)

        composeRule.waitUntil(timeoutMillis = 5_000) {
            readerUi.currentCount("one") == 0 &&
                readerUi.currentCount("three") == 0 &&
                readerUi.currentCount("hundred") == 0
        }
        composeRule.runOnIdle {
            Log.i(
                "AzkarPhase4Reset",
                "after Flow/UI update progress=${readerUi.state.progressById} " +
                    "settings=${readerUi.state.settings} viewMode=${readerUi.state.viewMode} " +
                    "history=${navigation.state.history}",
            )
            assertEquals(0, readerUi.currentCount("one"))
            assertEquals(0, readerUi.currentCount("three"))
            assertEquals(0, readerUi.currentCount("hundred"))
            assertEquals(settingsBefore, readerUi.state.settings)
            assertEquals(viewModeBefore, readerUi.state.viewMode)
            assertEquals(historyBefore, navigation.state.history)
        }

        val finalSnapshot = currentSnapshot("after_reset_confirmed", snapshotIds)
        assertEquals(0, finalSnapshot.progressById.getValue("one"))
        assertEquals(0, finalSnapshot.progressById.getValue("three"))
        assertEquals(0, finalSnapshot.progressById.getValue("hundred"))
        assertEquals(1, finalSnapshot.progressById.getValue(unrelatedId))
        assertEquals(settingsBefore, finalSnapshot.settings)
        assertEquals(viewModeBefore, finalSnapshot.viewMode)
        Log.i("AzkarPhase4Reset", "test end")
    }

    @Test
    fun settingsChangeSurvivesReaderRecreation() {
        setReader()
        openSettings()
        composeRule.onNodeWithTag(AzkarSheetTestTags.SettingSources)
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()
        awaitSnapshot("generic") { !it.settings.showSources }

        recreateReader()

        composeRule.runOnIdle { assertFalse(readerUi.state.settings.showSources) }
        composeRule.onNodeWithTag(AzkarReadingTestTags.Source).assertDoesNotExist()
    }

    @Test
    fun themeSurvivesReaderRecreation() {
        setReader()
        openSettings()
        composeRule.onNodeWithTag(AzkarSheetTestTags.ThemeDark)
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()
        awaitSnapshot("generic") { it.settings.themeMode == AzkarThemeMode.Dark }

        recreateReader()

        composeRule.runOnIdle {
            assertEquals(AzkarThemeMode.Dark, readerUi.state.settings.themeMode)
        }
    }

    @Test
    fun translationOffSurvivesReaderRecreation() {
        setReader()
        openSettings()
        composeRule.onNodeWithTag(AzkarSheetTestTags.SettingTranslation)
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()
        awaitSnapshot("generic") { !it.settings.showTranslation }

        recreateReader()

        composeRule.runOnIdle { assertFalse(readerUi.state.settings.showTranslation) }
        composeRule.onNodeWithTag(AzkarReadingTestTags.Translation).assertDoesNotExist()
    }

    @Test
    fun viewModeListSurvivesReaderRecreation() {
        setReader()
        composeRule.runOnIdle {
            assertTrue(readerUi.setViewMode(AzkarReaderViewMode.List))
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(AzkarReadingTestTags.List).assertIsDisplayed()
        awaitSnapshot("generic") { it.viewMode == AzkarReaderViewMode.List }

        recreateReader()

        composeRule.runOnIdle {
            assertEquals(AzkarReaderViewMode.List, readerUi.state.viewMode)
            assertEquals(0, navigation.state.activeIndex)
            assertTrue(navigation.state.history.isEmpty())
        }
        composeRule.onNodeWithTag(AzkarReadingTestTags.List).assertIsDisplayed()
    }

    @Test
    fun settingsSheetClosePreservesCurrentReaderContext() {
        setReader(initialIndex = 1)
        openSettings()

        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(AzkarSheetTestTags.Settings).assertDoesNotExist()
        composeRule.runOnIdle {
            assertEquals(1, navigation.state.activeIndex)
            assertEquals(AzkarReaderSheet.None, readerUi.state.activeSheet)
        }
    }

    @Test
    fun androidBackBehaviorUnchanged() {
        setReader()
        composeRule.onNodeWithTag(AzkarReadingTestTags.Card)
            .performTouchInput { swipeLeft() }
        composeRule.waitForIdle()
        composeRule.runOnIdle { assertEquals(1, navigation.state.activeIndex) }

        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
        composeRule.waitForIdle()

        composeRule.runOnIdle { assertEquals(0, navigation.state.activeIndex) }
    }

    @Test
    fun contentsBehaviorUnchanged() {
        setReader()
        openContents()
        composeRule.onNodeWithTag(AzkarSheetTestTags.ContentsItemPrefix + 2)
            .performClick()
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertEquals(2, navigation.state.activeIndex)
            assertEquals(AzkarReaderSheet.None, readerUi.state.activeSheet)
        }
    }

    @Test
    fun explanationBehaviorUnchanged() {
        setReader()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithTag(AzkarReadingTestTags.Explain).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithTag(AzkarReadingTestTags.Explain)
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(AzkarSheetTestTags.Explanation).assertIsDisplayed()

        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(AzkarSheetTestTags.Explanation).assertDoesNotExist()
        composeRule.runOnIdle {
            assertEquals(0, navigation.state.activeIndex)
            assertEquals(AzkarReaderSheet.None, readerUi.state.activeSheet)
        }
    }

    @Test
    fun dailyDateIsolationUsesInjectedProvider() {
        setReader()
        clickCount("one")
        awaitSnapshot("generic") { it.progressById["one"] == 1 }

        dateProvider.date = LocalDate.of(2026, 9, 30)
        recreateReader()

        composeRule.runOnIdle { assertEquals(0, readerUi.currentCount("one")) }
        composeRule.onNodeWithText("0 / 1").assertIsDisplayed()
    }
}
