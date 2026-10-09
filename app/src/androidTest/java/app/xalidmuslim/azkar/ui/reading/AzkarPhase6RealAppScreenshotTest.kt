package app.xalidmuslim.azkar.ui.reading

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.SystemClock
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
import app.xalidmuslim.azkar.persistence.DataStoreAzkarPreferencesRepository
import app.xalidmuslim.azkar.ui.designsystem.AzkarThemeMode
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AzkarPhase6RealAppScreenshotTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

    private val repository: DataStoreAzkarPreferencesRepository
        get() = DataStoreAzkarPreferencesRepository(context.azkarPreferencesDataStore)

    @Test
    fun capture360LightMorning() {
        prepare(AzkarThemeMode.Light)
        savePresentedScreenshot("azkar_phase6_360_light_morning.png")
    }

    @Test
    fun capture393LightMorning() {
        prepare(AzkarThemeMode.Light)
        savePresentedScreenshot("azkar_phase6_393_light_morning.png")
    }

    @Test
    fun capture412LightMorning() {
        prepare(AzkarThemeMode.Light)
        savePresentedScreenshot("azkar_phase6_412_light_morning.png")
    }

    @Test
    fun capture393DarkMorning() {
        prepare(AzkarThemeMode.Dark)
        savePresentedScreenshot("azkar_phase6_393_dark_morning.png")
    }

    @Test
    fun capture393LightEvening() {
        prepare(AzkarThemeMode.Light)
        switchEvening()
        savePresentedScreenshot("azkar_phase6_393_light_evening.png")
    }

    @Test
    fun capture393DarkEvening() {
        prepare(AzkarThemeMode.Dark)
        switchEvening()
        savePresentedScreenshot("azkar_phase6_393_dark_evening.png")
    }

    @Test
    fun capture393Contents() {
        prepare(AzkarThemeMode.Light)
        composeRule.onNodeWithTag(AzkarReadingTestTags.OpenContents)
            .assertIsDisplayed()
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(AzkarSheetTestTags.Contents).assertIsDisplayed()
        savePresentedScreenshot("azkar_phase6_393_contents.png")
    }

    @Test
    fun capture393Settings() {
        prepare(AzkarThemeMode.Light)
        composeRule.onNodeWithTag(AzkarReadingTestTags.OpenSettingsTop)
            .assertIsDisplayed()
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(AzkarSheetTestTags.Settings).assertIsDisplayed()
        savePresentedScreenshot("azkar_phase6_393_settings.png")
    }

    @Test
    fun capture393Explanation() {
        prepare(AzkarThemeMode.Light)
        openFirstAvailableExplanation()
        composeRule.onNodeWithTag(AzkarSheetTestTags.Explanation).assertIsDisplayed()
        savePresentedScreenshot("azkar_phase6_393_explanation.png")
    }

    @Test
    fun capture393ListMode() {
        prepare(
            themeMode = AzkarThemeMode.Light,
            viewMode = AzkarReaderViewMode.List,
        )
        composeRule.onNodeWithTag(AzkarReadingTestTags.List).assertIsDisplayed()
        savePresentedScreenshot("azkar_phase6_393_list.png")
    }

    private fun prepare(
        themeMode: AzkarThemeMode,
        viewMode: AzkarReaderViewMode = AzkarReaderViewMode.Cards,
    ) {
        runBlocking {
            context.azkarPreferencesDataStore.edit { preferences ->
                preferences.clear()
            }
            repository.saveSettings(
                AzkarReaderSettings(themeMode = themeMode),
            )
            repository.saveViewMode(viewMode)
        }
        composeRule.activityRule.scenario.recreate()
        if (viewMode == AzkarReaderViewMode.List) {
            waitForTag(AzkarReadingTestTags.List)
        } else {
            waitForTag(AzkarReadingTestTags.Card)
        }
    }

    private fun switchEvening() {
        composeRule.onNodeWithTag(AzkarReadingTestTags.PeriodEvening)
            .assertIsDisplayed()
            .performClick()
        composeRule.waitForIdle()
        waitForTag(AzkarReadingTestTags.Card)
    }

    private fun openFirstAvailableExplanation() {
        val items = AzkarCatalog.readingItemsFor(AzkarPeriod.Morning)
        val index = items.indexOfFirst { it.hasInsight }
        assertTrue(index >= 0)

        if (index != 0) {
            composeRule.onNodeWithTag(AzkarReadingTestTags.OpenContents)
                .performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithTag(AzkarSheetTestTags.ContentsItemPrefix + index)
                .performClick()
            composeRule.waitForIdle()
        }

        composeRule.onNodeWithTag(AzkarReadingTestTags.Explain)
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeRule.waitForIdle()
    }

    private fun waitForTag(tag: String) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            runCatching {
                composeRule.onNodeWithTag(tag).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.waitForIdle()
    }

    private fun savePresentedScreenshot(fileName: String) {
        val screenshot = captureAfterPresentedFrame()
        val directory = File(context.getExternalFilesDir(null), "phase6-evidence")
        check(directory.exists() || directory.mkdirs())
        val file = File(directory, fileName)
        FileOutputStream(file).use { stream ->
            check(screenshot.compress(Bitmap.CompressFormat.PNG, 100, stream))
        }
        check(file.isFile && file.length() > 0L)
    }

    private fun captureAfterPresentedFrame(): Bitmap {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val device = UiDevice.getInstance(instrumentation)
        repeat(MAX_SCREENSHOT_ATTEMPTS) { attempt ->
            composeRule.waitForIdle()
            instrumentation.waitForIdleSync()
            device.waitForIdle()
            SystemClock.sleep(
                if (attempt == 0) INITIAL_PRESENT_WAIT_MS else RETRY_PRESENT_WAIT_MS,
            )
            val screenshot = instrumentation.uiAutomation.takeScreenshot()
            if (hasRenderedComposeBody(screenshot)) return screenshot
        }
        error("Physical Phase 6 MainActivity screenshot surface stayed visually blank")
    }

    private fun hasRenderedComposeBody(bitmap: Bitmap): Boolean {
        val left = bitmap.width / 10
        val right = bitmap.width * 9 / 10
        val top = bitmap.height / 8
        val bottom = bitmap.height * 7 / 8
        val colors = HashSet<Int>()
        var minLuma = 255
        var maxLuma = 0
        var y = top
        while (y < bottom) {
            var x = left
            while (x < right) {
                val color = bitmap.getPixel(x, y)
                val red = Color.red(color)
                val green = Color.green(color)
                val blue = Color.blue(color)
                colors += ((red ushr 4) shl 8) or ((green ushr 4) shl 4) or (blue ushr 4)
                val luma = (red + green + blue) / 3
                minLuma = minOf(minLuma, luma)
                maxLuma = maxOf(maxLuma, luma)
                x += PIXEL_SAMPLE_STEP
            }
            y += PIXEL_SAMPLE_STEP
        }
        return colors.size >= MIN_DISTINCT_COLORS && maxLuma - minLuma >= MIN_LUMA_RANGE
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
