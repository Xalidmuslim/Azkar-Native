package app.xalidmuslim.azkar.ui.reading

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.datastore.preferences.core.edit
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import app.xalidmuslim.azkar.MainActivity
import app.xalidmuslim.azkar.azkarPreferencesDataStore
import app.xalidmuslim.azkar.content.AzkarCatalog
import app.xalidmuslim.azkar.persistence.AzkarPreferencesSnapshot
import app.xalidmuslim.azkar.persistence.DataStoreAzkarPreferencesRepository
import app.xalidmuslim.azkar.persistence.SystemAzkarDateProvider
import app.xalidmuslim.azkar.ui.designsystem.ArabicFontFamily
import app.xalidmuslim.azkar.ui.designsystem.AzkarThemeMode
import app.xalidmuslim.azkar.ui.designsystem.RussianFontFamily
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AzkarPhase6AppIntegrationInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

    private val repository: DataStoreAzkarPreferencesRepository
        get() = DataStoreAzkarPreferencesRepository(context.azkarPreferencesDataStore)

    @Before
    fun resetProductionStore() {
        runBlocking {
            context.azkarPreferencesDataStore.edit { preferences ->
                preferences.clear()
            }
        }
        composeRule.activityRule.scenario.recreate()
        waitForCards()
        waitForText("1 из 14")
    }

    @Test
    fun mainActivityLaunches() {
        composeRule.onNodeWithTag(AzkarReadingTestTags.Screen)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(AzkarReadingTestTags.Card)
            .assertIsDisplayed()
    }

    @Test
    fun morningDefaultShowsOneOfFourteen() {
        composeRule.onAllNodesWithText("1 из 14")
            .fetchSemanticsNodes()
            .also { assertTrue(it.isNotEmpty()) }
    }

    @Test
    fun switchEveningShowsOneOfThirteen() {
        composeRule.onNodeWithTag(AzkarReadingTestTags.PeriodEvening)
            .assertIsDisplayed()
            .performClick()

        waitForText("1 из 13")
    }

    @Test
    fun contentsOpens() {
        openContents()
        composeRule.onNodeWithTag(AzkarSheetTestTags.Contents)
            .assertIsDisplayed()
    }

    @Test
    fun settingsOpens() {
        openSettings()
        composeRule.onNodeWithTag(AzkarSheetTestTags.Settings)
            .assertIsDisplayed()
    }

    @Test
    fun explanationOpens() {
        openFirstAvailableExplanation()
        composeRule.onNodeWithTag(AzkarSheetTestTags.Explanation)
            .assertIsDisplayed()
    }

    @Test
    fun cardsListSwitchWorks() {
        openContents()
        composeRule.onNodeWithTag(AzkarSheetTestTags.ContentsListMode)
            .assertIsDisplayed()
            .performClick()

        waitForList()
    }

    @Test
    fun countIncrements() {
        val first = AzkarCatalog.firstReadingItem(AzkarPeriod.Morning)
        clickCount(first.id)

        val snapshot = awaitSnapshot {
            it.progressById[first.id] == 1
        }
        assertEquals(1, snapshot.progressById[first.id])
    }

    @Test
    fun persistedSettingSurvivesActivityRecreation() {
        openSettings()
        composeRule.onNodeWithTag(AzkarSheetTestTags.SettingSources)
            .performScrollTo()
            .performClick()

        awaitSnapshot { !it.settings.showSources }

        composeRule.activityRule.scenario.recreate()
        waitForCards()

        composeRule.onNodeWithTag(AzkarReadingTestTags.Source)
            .assertDoesNotExist()
        assertFalse(awaitSnapshot { !it.settings.showSources }.settings.showSources)
    }

    @Test
    fun allReaderSettingsHydrateFromProductionDataStoreAfterActivityRecreation() {
        val expected = AzkarReaderSettings(
            russianFontFamily = RussianFontFamily.INTER,
            arabicFontFamily = ArabicFontFamily.AMIRI,
            arabicSizeSp = 34f,
            russianSizeSp = 19f,
            lineHeight = 1.75f,
            readerStyle = AzkarReaderStyle.Compact,
            showTranslation = false,
            showSources = false,
            showNotes = false,
            themeMode = AzkarThemeMode.Dark,
        )

        runBlocking {
            repository.saveSettings(expected)
        }

        composeRule.activityRule.scenario.recreate()
        waitForCards()

        val restored = awaitSnapshot { it.settings == expected }
        assertEquals(expected.russianFontFamily, restored.settings.russianFontFamily)
        assertEquals(expected.arabicFontFamily, restored.settings.arabicFontFamily)
        assertEquals(expected.arabicSizeSp, restored.settings.arabicSizeSp)
        assertEquals(expected.russianSizeSp, restored.settings.russianSizeSp)
        assertEquals(expected.lineHeight, restored.settings.lineHeight)
        assertEquals(expected.readerStyle, restored.settings.readerStyle)
        assertEquals(expected.showTranslation, restored.settings.showTranslation)
        assertEquals(expected.showSources, restored.settings.showSources)
        assertEquals(expected.showNotes, restored.settings.showNotes)
        assertEquals(expected.themeMode, restored.settings.themeMode)

        composeRule.onNodeWithTag(AzkarReadingTestTags.Translation).assertDoesNotExist()
        composeRule.onNodeWithTag(AzkarReadingTestTags.Source).assertDoesNotExist()
        composeRule.onNodeWithTag(AzkarReadingTestTags.Note).assertDoesNotExist()
    }

    @Test
    fun viewModeSurvivesActivityRecreation() {
        openContents()
        composeRule.onNodeWithTag(AzkarSheetTestTags.ContentsListMode)
            .performClick()
        waitForList()
        awaitSnapshot { it.viewMode == AzkarReaderViewMode.List }

        composeRule.activityRule.scenario.recreate()
        waitForList()

        assertEquals(
            AzkarReaderViewMode.List,
            awaitSnapshot { it.viewMode == AzkarReaderViewMode.List }.viewMode,
        )
    }

    @Test
    fun progressSurvivesActivityRecreation() {
        val first = AzkarCatalog.firstReadingItem(AzkarPeriod.Morning)
        clickCount(first.id)
        awaitSnapshot { it.progressById[first.id] == 1 }

        composeRule.activityRule.scenario.recreate()
        waitForCards()

        val restored = awaitSnapshot { it.progressById[first.id] == 1 }
        assertEquals(1, restored.progressById[first.id])
        if (first.count == 1) {
            waitForText("Выполнено")
        }
    }

    @Test
    fun androidBackClosesSheetBeforeReaderHistory() {
        clickDisplayedTag(AzkarReadingTestTags.Next)
        waitForText("2 из 14")

        openSettings()
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(AzkarSheetTestTags.Settings).assertDoesNotExist()
        composeRule.onAllNodesWithText("2 из 14")
            .fetchSemanticsNodes()
            .also { assertTrue(it.isNotEmpty()) }

        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
        waitForText("1 из 14")
    }

    @Test
    fun standardAndroidBackFinishesActivityAtRoot() {
        val activity = composeRule.activity
        composeRule.runOnIdle {
            activity.onBackPressedDispatcher.onBackPressed()
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        assertTrue(activity.isFinishing || activity.isDestroyed)
    }

    @Test
    fun packageAndLauncherAreProductionCorrect() {
        val packageManager = context.packageManager
        val packageName = context.packageName
        assertEquals("app.xalidmuslim.azkar", packageName)

        val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
        assertEquals("Азкар", packageManager.getApplicationLabel(applicationInfo).toString())

        val component = ComponentName(context, MainActivity::class.java)
        val activityInfo = packageManager.getActivityInfo(component, 0)
        assertTrue(activityInfo.exported)
        assertEquals("Азкар", activityInfo.loadLabel(packageManager).toString())

        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        assertNotNull(launchIntent)
        assertEquals(MainActivity::class.java.name, launchIntent!!.component?.className)
    }

    @Test
    fun internetPermissionIsAbsent() {
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS,
        )
        val permissions = packageInfo.requestedPermissions?.toSet().orEmpty()
        assertFalse(Manifest.permission.INTERNET in permissions)
    }

    private fun openContents() {
        clickDisplayedTag(AzkarReadingTestTags.OpenContents)
    }

    private fun openSettings() {
        clickDisplayedTag(AzkarReadingTestTags.OpenSettingsTop)
    }

    private fun openFirstAvailableExplanation() {
        val items = AzkarCatalog.readingItemsFor(AzkarPeriod.Morning)
        val index = items.indexOfFirst { it.hasInsight }
        assertTrue(index >= 0)

        if (index != 0) {
            openContents()
            awaitComposeSemantics {
                composeRule.onNodeWithTag(AzkarSheetTestTags.ContentsItemPrefix + index)
                    .assertIsDisplayed()
            }
            composeRule.onNodeWithTag(AzkarSheetTestTags.ContentsItemPrefix + index)
                .performClick()
            composeRule.waitForIdle()
        }

        awaitComposeSemantics {
            composeRule.onNodeWithTag(AzkarReadingTestTags.Explain)
                .performScrollTo()
                .assertIsDisplayed()
        }
        composeRule.onNodeWithTag(AzkarReadingTestTags.Explain)
            .performClick()
        composeRule.waitForIdle()
    }

    private fun clickCount(id: String) {
        val tag = AzkarReadingTestTags.CountActionPrefix + id
        awaitComposeSemantics {
            composeRule.onNodeWithTag(tag)
                .performScrollTo()
                .assertIsDisplayed()
        }
        composeRule.onNodeWithTag(tag).performClick()
        composeRule.waitForIdle()
    }

    private fun clickDisplayedTag(tag: String) {
        awaitComposeSemantics {
            composeRule.onNodeWithTag(tag).assertIsDisplayed()
        }
        composeRule.onNodeWithTag(tag).performClick()
        composeRule.waitForIdle()
    }

    private fun waitForCards() {
        awaitComposeSemantics {
            composeRule.onNodeWithTag(AzkarReadingTestTags.Card)
                .assertIsDisplayed()
        }
    }

    private fun waitForList() {
        awaitComposeSemantics {
            composeRule.onNodeWithTag(AzkarReadingTestTags.List)
                .assertIsDisplayed()
        }
    }

    private fun waitForText(text: String) {
        awaitComposeSemantics {
            assertTrue(
                composeRule.onAllNodesWithText(text)
                    .fetchSemanticsNodes()
                    .isNotEmpty(),
            )
        }
    }

    private fun awaitComposeSemantics(
        timeoutMillis: Long = 10_000,
        assertion: () -> Unit,
    ) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val deadline = SystemClock.uptimeMillis() + timeoutMillis
        var lastFailure: Throwable? = null

        do {
            composeRule.waitForIdle()
            val result = runCatching(assertion)
            if (result.isSuccess) {
                composeRule.waitForIdle()
                return
            }
            lastFailure = result.exceptionOrNull()
            instrumentation.waitForIdleSync()
        } while (SystemClock.uptimeMillis() < deadline)

        throw AssertionError(
            "Compose semantics condition was not satisfied within $timeoutMillis ms",
            lastFailure,
        )
    }

    private fun awaitSnapshot(
        predicate: (AzkarPreferencesSnapshot) -> Boolean,
    ): AzkarPreferencesSnapshot = runBlocking {
        withTimeout(10_000) {
            repository.observeSnapshot(
                date = SystemAzkarDateProvider.currentDate(),
                visibleItemIds = AzkarCatalog.stableIds,
            ).first(predicate)
        }
    }
}
