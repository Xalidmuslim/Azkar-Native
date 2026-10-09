package app.xalidmuslim.azkar.ui.reading

import android.graphics.Bitmap
import android.graphics.Color
import android.os.SystemClock
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import app.xalidmuslim.azkar.ui.designsystem.AzkarThemeMode
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AzkarListModeScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var readerUi: AzkarReaderUiController

    private fun item(
        id: String,
        title: String,
        disputed: Boolean = false,
        note: String? = null,
        hasInsight: Boolean = false,
    ) = AzkarReadingItem(
        id = id,
        title = title,
        count = 1,
        disputed = disputed,
        arabic = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ",
        translation = "Слава Аллаху и хвала Ему.",
        source = "Источник для визуальной проверки",
        note = note,
        hasInsight = hasInsight,
    )

    private fun entries() = listOf(
        AzkarReaderEntry(
            item(
                id = "visual-one",
                title = "Первый азкар",
                disputed = true,
                note = "Краткое примечание для проверки карточки.",
            ),
        ),
        AzkarReaderEntry(item(id = "visual-two", title = "Второй азкар")),
        AzkarReaderEntry(item(id = "visual-three", title = "Третий азкар", hasInsight = true)),
        AzkarReaderEntry(item(id = "visual-four", title = "Четвёртый азкар")),
    )

    @Test
    fun capture360LightList() {
        renderList(
            themeMode = AzkarThemeMode.Light,
            fileName = "azkar_360x800_light_list.png",
            expectedWidth = 360,
            expectedHeight = 800,
        )
    }

    @Test
    fun capture393LightList() {
        renderList(
            themeMode = AzkarThemeMode.Light,
            fileName = "azkar_393x873_light_list.png",
            expectedWidth = 393,
            expectedHeight = 873,
        )
    }

    @Test
    fun capture412LightList() {
        renderList(
            themeMode = AzkarThemeMode.Light,
            fileName = "azkar_412x915_light_list.png",
            expectedWidth = 412,
            expectedHeight = 915,
        )
    }

    @Test
    fun capture393DarkList() {
        renderList(
            themeMode = AzkarThemeMode.Dark,
            fileName = "azkar_393x873_dark_list.png",
            expectedWidth = 393,
            expectedHeight = 873,
        )
    }

    @Test
    fun capture393LightListMiddle() {
        renderList(
            themeMode = AzkarThemeMode.Light,
            fileName = "azkar_393x873_light_list_middle.png",
            expectedWidth = 393,
            expectedHeight = 873,
            scrollToId = "visual-three",
        )
    }

    @Test
    fun capture393LightContentsModeSwitch() {
        renderList(
            themeMode = AzkarThemeMode.Light,
            fileName = "azkar_393x873_light_contents_mode_switch.png",
            expectedWidth = 393,
            expectedHeight = 873,
            openContents = true,
        )
    }

    private fun renderList(
        themeMode: AzkarThemeMode,
        fileName: String,
        expectedWidth: Int,
        expectedHeight: Int,
        scrollToId: String? = null,
        openContents: Boolean = false,
    ) {
        val testEntries = entries()
        composeRule.setContent {
            val nav = remember { AzkarReaderNavigationController(testEntries.size) }
            val ui = remember {
                AzkarReaderUiController(
                    initialSettings = AzkarReaderSettings(themeMode = themeMode),
                    initialViewMode = AzkarReaderViewMode.List,
                )
            }
            readerUi = ui
            AzkarReaderScreen(
                entries = testEntries,
                period = AzkarPeriod.Morning,
                controller = nav,
                uiController = ui,
                modifier = Modifier.fillMaxSize(),
            )
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag(AzkarReadingTestTags.List).assertIsDisplayed()

        if (scrollToId != null) {
            composeRule.onNodeWithTag(AzkarReadingTestTags.ListCardPrefix + scrollToId)
                .performScrollTo()
                .assertIsDisplayed()
            composeRule.waitForIdle()
        }

        if (openContents) {
            composeRule.runOnIdle { readerUi.openContents() }
            composeRule.waitForIdle()
            composeRule.onNodeWithTag(AzkarSheetTestTags.Contents).assertIsDisplayed()
            composeRule.onNodeWithTag(AzkarSheetTestTags.ContentsCardsMode).assertIsDisplayed()
            composeRule.onNodeWithTag(AzkarSheetTestTags.ContentsListMode).assertIsDisplayed()
        }

        val screenshot = captureAfterPresentedFrame(expectedWidth, expectedHeight)
        saveScreenshot(screenshot, fileName)
    }

    private fun captureAfterPresentedFrame(expectedWidth: Int, expectedHeight: Int): Bitmap {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val device = UiDevice.getInstance(instrumentation)

        repeat(MAX_SCREENSHOT_ATTEMPTS) { attempt ->
            composeRule.waitForIdle()
            instrumentation.waitForIdleSync()
            device.waitForIdle()
            SystemClock.sleep(if (attempt == 0) INITIAL_PRESENT_WAIT_MS else RETRY_PRESENT_WAIT_MS)

            val screenshot = instrumentation.uiAutomation.takeScreenshot()
            assertEquals(expectedWidth, screenshot.width)
            assertEquals(expectedHeight, screenshot.height)

            if (hasRenderedComposeBody(screenshot)) {
                return screenshot
            }
        }

        error(
            "Physical list screenshot surface stayed visually blank after " +
                "$MAX_SCREENSHOT_ATTEMPTS bounded attempts",
        )
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
                val quantized =
                    ((red ushr 4) shl 8) or ((green ushr 4) shl 4) or (blue ushr 4)
                quantizedColors += quantized

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
