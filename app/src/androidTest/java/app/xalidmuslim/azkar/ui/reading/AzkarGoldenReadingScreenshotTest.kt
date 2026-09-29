package app.xalidmuslim.azkar.ui.reading

import android.graphics.Bitmap
import android.graphics.Color
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import app.xalidmuslim.azkar.ui.designsystem.AzkarTheme
import app.xalidmuslim.azkar.ui.designsystem.AzkarThemeMode
import app.xalidmuslim.azkar.ui.designsystem.AzkarThemeValues
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AzkarGoldenReadingScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun capture360Light() {
        renderAndCapture(
            themeMode = AzkarThemeMode.Light,
            state = AzkarGoldenReadingFixtures.GoldenMorning,
            fileName = "azkar_360x800_light.png",
            expectedWidth = 360,
            expectedHeight = 800,
            expectDisputed = true,
        )
    }

    @Test
    fun capture393Light() {
        renderAndCapture(
            themeMode = AzkarThemeMode.Light,
            state = AzkarGoldenReadingFixtures.GoldenMorning,
            fileName = "azkar_393x873_light.png",
            expectedWidth = 393,
            expectedHeight = 873,
            expectDisputed = true,
        )
    }

    @Test
    fun capture412Light() {
        renderAndCapture(
            themeMode = AzkarThemeMode.Light,
            state = AzkarGoldenReadingFixtures.GoldenMorning,
            fileName = "azkar_412x915_light.png",
            expectedWidth = 412,
            expectedHeight = 915,
            expectDisputed = true,
        )
    }

    @Test
    fun capture393Dark() {
        renderAndCapture(
            themeMode = AzkarThemeMode.Dark,
            state = AzkarGoldenReadingFixtures.GoldenMorning,
            fileName = "azkar_393x873_dark.png",
            expectedWidth = 393,
            expectedHeight = 873,
            expectDisputed = true,
        )
    }

    @Test
    fun capture393LongExistingDhikr() {
        renderAndCapture(
            themeMode = AzkarThemeMode.Light,
            state = AzkarGoldenReadingFixtures.LongEvening,
            fileName = "azkar_393x873_long_baqarah.png",
            expectedWidth = 393,
            expectedHeight = 873,
            expectDisputed = false,
        )
    }

    private fun renderAndCapture(
        themeMode: AzkarThemeMode,
        state: AzkarGoldenReadingUiState,
        fileName: String,
        expectedWidth: Int,
        expectedHeight: Int,
        expectDisputed: Boolean,
    ) {
        composeRule.setContent {
            AzkarTheme(themeMode = themeMode) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AzkarThemeValues.colors.background)
                        .verticalScroll(rememberScrollState()),
                ) {
                    AzkarGoldenReadingScreen(state = state)
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("azkar-reading-screen").assertIsDisplayed()
        composeRule.onNodeWithTag("azkar-arabic").assertIsDisplayed()
        composeRule.onNodeWithTag("azkar-translation").fetchSemanticsNode()
        composeRule.onNodeWithTag("azkar-source").fetchSemanticsNode()
        composeRule.onNodeWithTag("azkar-note").fetchSemanticsNode()
        if (expectDisputed) {
            composeRule.onNodeWithTag("azkar-disputed").fetchSemanticsNode()
        }

        val screenshot = captureAfterPresentedFrame(expectedWidth, expectedHeight)
        saveScreenshot(screenshot, fileName)

        composeRule.onNodeWithTag("azkar-arabic").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("azkar-translation").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("azkar-source").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("azkar-note").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("azkar-explain").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("azkar-counter").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("azkar-pager").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("azkar-footer").performScrollTo().assertIsDisplayed()
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
            "Physical screenshot surface stayed visually blank after " +
                "$MAX_SCREENSHOT_ATTEMPTS bounded attempts",
        )
    }

    private fun hasRenderedComposeBody(bitmap: Bitmap): Boolean {
        val left = (bitmap.width * 10) / 100
        val right = (bitmap.width * 90) / 100
        val top = (bitmap.height * 20) / 100
        val bottom = (bitmap.height * 82) / 100

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
