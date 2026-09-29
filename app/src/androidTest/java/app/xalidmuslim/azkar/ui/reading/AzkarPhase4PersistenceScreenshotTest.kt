package app.xalidmuslim.azkar.ui.reading

import android.graphics.Bitmap
import android.graphics.Color
import android.os.SystemClock
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import app.xalidmuslim.azkar.persistence.AzkarDateProvider
import app.xalidmuslim.azkar.persistence.DataStoreAzkarPreferencesRepository
import app.xalidmuslim.azkar.ui.designsystem.ArabicFontFamily
import app.xalidmuslim.azkar.ui.designsystem.AzkarThemeMode
import app.xalidmuslim.azkar.ui.designsystem.RussianFontFamily
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AzkarPhase4PersistenceScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    private class FixedDateProvider(
        private val date: LocalDate,
    ) : AzkarDateProvider {
        override fun currentDate(): LocalDate = date
    }

    private lateinit var dataStoreScope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: DataStoreAzkarPreferencesRepository
    private lateinit var dataStoreFile: File
    private lateinit var readerUi: AzkarReaderUiController
    private lateinit var navigation: AzkarReaderNavigationController

    private val date = LocalDate.of(2026, 9, 29)
    private val dateProvider = FixedDateProvider(date)

    private val entries = listOf(
        AzkarReaderEntry(AzkarGoldenReadingFixtures.Muawwidhat),
        AzkarReaderEntry(AzkarGoldenReadingFixtures.BaqarahLastTwo),
    )

    @Before
    fun setUpStorage() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        dataStoreFile = File(
            context.filesDir,
            "phase4-evidence-${UUID.randomUUID()}.preferences_pb",
        )
        dataStoreFile.delete()
        dataStoreScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        dataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { dataStoreFile },
        )
        repository = DataStoreAzkarPreferencesRepository(dataStore)
    }

    @After
    fun tearDownStorage() {
        dataStoreScope.cancel()
        dataStoreFile.delete()
    }

    @Test
    fun capture393LightPartialProgress() {
        runBlocking {
            repository.saveSettings(AzkarReaderSettings(themeMode = AzkarThemeMode.Light))
            repository.incrementProgress(date, "muawwidhat", 3)
        }
        renderAndCapture(
            fileName = "azkar_phase4_393_light_partial_progress.png",
        )
    }

    @Test
    fun capture393LightCompletedDhikr() {
        runBlocking {
            repository.saveSettings(AzkarReaderSettings(themeMode = AzkarThemeMode.Light))
            repeat(3) { repository.incrementProgress(date, "muawwidhat", 3) }
        }
        renderAndCapture(
            fileName = "azkar_phase4_393_light_completed.png",
        )
    }

    @Test
    fun capture393LightListCompletedItem() {
        runBlocking {
            repository.saveSettings(AzkarReaderSettings(themeMode = AzkarThemeMode.Light))
            repository.saveViewMode(AzkarReaderViewMode.List)
            repeat(3) { repository.incrementProgress(date, "muawwidhat", 3) }
        }
        renderAndCapture(
            fileName = "azkar_phase4_393_light_list_completed.png",
            expectList = true,
        )
    }

    @Test
    fun capture393DarkCompletedItem() {
        runBlocking {
            repository.saveSettings(AzkarReaderSettings(themeMode = AzkarThemeMode.Dark))
            repeat(3) { repository.incrementProgress(date, "muawwidhat", 3) }
        }
        renderAndCapture(
            fileName = "azkar_phase4_393_dark_completed.png",
        )
    }

    @Test
    fun capture393LightSettingsRestoredState() {
        runBlocking {
            repository.saveSettings(
                AzkarReaderSettings(
                    russianFontFamily = RussianFontFamily.MANROPE,
                    arabicFontFamily = ArabicFontFamily.AMIRI,
                    arabicSizeSp = 37f,
                    russianSizeSp = 20f,
                    lineHeight = 1.40f,
                    readerStyle = AzkarReaderStyle.Compact,
                    showTranslation = false,
                    showSources = true,
                    showNotes = false,
                    themeMode = AzkarThemeMode.Light,
                ),
            )
        }
        renderAndCapture(
            fileName = "azkar_phase4_393_light_settings_restored.png",
            openSettings = true,
        )
    }

    private fun renderAndCapture(
        fileName: String,
        expectList: Boolean = false,
        openSettings: Boolean = false,
    ) {
        composeRule.setContent {
            val scope = rememberCoroutineScope()
            val nav = remember { AzkarReaderNavigationController(entries.size) }
            navigation = nav
            val ui = remember {
                AzkarReaderUiController(
                    repository = repository,
                    dateProvider = dateProvider,
                    persistenceScope = scope,
                    visibleItemIds = entries.map { it.item.id }.toSet(),
                )
            }
            readerUi = ui
            AzkarReaderScreen(
                entries = entries,
                period = AzkarPeriod.Morning,
                controller = nav,
                uiController = ui,
                modifier = Modifier.fillMaxSize(),
            )
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            ::readerUi.isInitialized && readerUi.state.isHydrated
        }
        composeRule.waitForIdle()

        val listLookup = runCatching {
            composeRule.onNodeWithTag(AzkarReadingTestTags.List).fetchSemanticsNode()
        }
        val cardLookup = runCatching {
            composeRule.onNodeWithTag(AzkarReadingTestTags.Card).fetchSemanticsNode()
        }
        val listNode = listLookup.getOrNull()
        val cardNode = cardLookup.getOrNull()
        val rootNode = composeRule.onRoot(useUnmergedTree = true).fetchSemanticsNode()
        val activeIndex = navigation.state.activeIndex
        val currentItemId = entries.getOrNull(activeIndex)?.item?.id

        val diagnostic =
            "hydrated=${readerUi.state.isHydrated} " +
                "viewMode=${readerUi.state.viewMode} " +
                "activeIndex=$activeIndex " +
                "currentItemId=$currentItemId " +
                "activeSheet=${readerUi.state.activeSheet} " +
                "progress=${readerUi.state.progressById.filterKeys { id -> entries.any { it.item.id == id } }} " +
                "listExists=${listNode != null} " +
                "listLookupError=${listLookup.exceptionOrNull()?.message} " +
                "cardExists=${cardNode != null} " +
                "cardLookupError=${cardLookup.exceptionOrNull()?.message} " +
                "cardBounds=${cardNode?.boundsInRoot} " +
                "scrollY=${navigation.currentScrollY} " +
                "viewport=${rootNode.boundsInRoot}"
        println("PHASE4_DIAG $diagnostic")

        if (expectList) {
            check(listNode != null) { "List semantics node does not exist; $diagnostic" }
            composeRule.onNodeWithTag(AzkarReadingTestTags.List).assertIsDisplayed()
        } else {
            check(cardNode != null) { "Card semantics node does not exist; $diagnostic" }
            composeRule.onNodeWithTag(AzkarReadingTestTags.Card).assertIsDisplayed()
        }

        if (openSettings) {
            composeRule.runOnIdle { readerUi.openSettings() }
            composeRule.waitForIdle()
            composeRule.onNodeWithTag(AzkarSheetTestTags.Settings).assertIsDisplayed()
        }

        val screenshot = captureAfterPresentedFrame()
        saveScreenshot(screenshot, fileName)
    }

    private fun captureAfterPresentedFrame(): Bitmap {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val device = UiDevice.getInstance(instrumentation)

        repeat(MAX_SCREENSHOT_ATTEMPTS) { attempt ->
            composeRule.waitForIdle()
            instrumentation.waitForIdleSync()
            device.waitForIdle()
            SystemClock.sleep(if (attempt == 0) INITIAL_PRESENT_WAIT_MS else RETRY_PRESENT_WAIT_MS)

            val screenshot = instrumentation.uiAutomation.takeScreenshot()
            assertEquals(393, screenshot.width)
            assertEquals(873, screenshot.height)

            if (hasRenderedComposeBody(screenshot)) {
                return screenshot
            }
        }

        error("Physical Phase 4 screenshot surface stayed visually blank")
    }

    private fun hasRenderedComposeBody(bitmap: Bitmap): Boolean {
        val left = (bitmap.width * 10) / 100
        val right = (bitmap.width * 90) / 100
        val top = (bitmap.height * 12) / 100
        val bottom = (bitmap.height * 88) / 100

        val quantizedColors = HashSet<Int>()
        var minLuma = 255
        var maxLuma = 0
        var samples = 0

        var y = top
        while (y < bottom) {
            var x = left
            while (x < right) {
                val color = bitmap.getPixel(x, y)
                val red = Color.red(color)
                val green = Color.green(color)
                val blue = Color.blue(color)
                quantizedColors +=
                    ((red ushr 4) shl 8) or ((green ushr 4) shl 4) or (blue ushr 4)

                val luma = (red + green + blue) / 3
                if (luma < minLuma) minLuma = luma
                if (luma > maxLuma) maxLuma = luma
                samples += 1
                x += PIXEL_SAMPLE_STEP
            }
            y += PIXEL_SAMPLE_STEP
        }

        return samples > 0 &&
            quantizedColors.size >= MIN_DISTINCT_COLORS &&
            (maxLuma - minLuma) >= MIN_LUMA_RANGE
    }

    private fun saveScreenshot(bitmap: Bitmap, fileName: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.getExternalFilesDir(null), "phase2-evidence")
        check(directory.exists() || directory.mkdirs()) {
            "Could not create screenshot evidence directory: $directory"
        }
        val file = File(directory, fileName)
        FileOutputStream(file).use { stream ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                "Could not encode screenshot: $fileName"
            }
        }
        check(file.isFile && file.length() > 0L) {
            "Screenshot evidence was not written: $fileName"
        }
    }

    private companion object {
        const val MAX_SCREENSHOT_ATTEMPTS = 5
        const val INITIAL_PRESENT_WAIT_MS = 350L
        const val RETRY_PRESENT_WAIT_MS = 175L
        const val PIXEL_SAMPLE_STEP = 8
        const val MIN_DISTINCT_COLORS = 6
        const val MIN_LUMA_RANGE = 12
    }
}
