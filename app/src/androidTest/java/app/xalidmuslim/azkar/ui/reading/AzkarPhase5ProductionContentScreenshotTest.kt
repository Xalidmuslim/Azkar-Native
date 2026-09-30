package app.xalidmuslim.azkar.ui.reading

import android.graphics.Bitmap
import android.graphics.Color
import android.os.SystemClock
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import app.xalidmuslim.azkar.content.AzkarCatalog
import app.xalidmuslim.azkar.ui.designsystem.AzkarThemeMode
import java.io.File
import java.io.FileOutputStream
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AzkarPhase5ProductionContentScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var periodController: AzkarPeriodReaderController
    private lateinit var uiController: AzkarReaderUiController

    @Test
    fun capture393LightMorningFirst() = renderAndCapture(
        initialPeriod = AzkarPeriod.Morning,
        fileName = "azkar_phase5_393_light_morning_first.png",
        focusText = "1 из 14",
    )

    @Test
    fun capture393LightEveningFirst() = renderAndCapture(
        initialPeriod = AzkarPeriod.Evening,
        fileName = "azkar_phase5_393_light_evening_first.png",
        focusText = "1 из 13",
    )

    @Test
    fun capture393LightMorningContents() = renderAndCapture(
        initialPeriod = AzkarPeriod.Morning,
        fileName = "azkar_phase5_393_light_morning_contents.png",
        configure = { uiController.openContents() },
        focusText = "Содержание",
    )

    @Test
    fun capture393LightEveningContents() = renderAndCapture(
        initialPeriod = AzkarPeriod.Evening,
        fileName = "azkar_phase5_393_light_evening_contents.png",
        configure = { uiController.openContents() },
        focusText = "Содержание",
    )

    @Test
    fun capture393LightEveningKingdomVariant() = renderAndCapture(
        initialPeriod = AzkarPeriod.Evening,
        fileName = "azkar_phase5_393_light_evening_kingdom.png",
        configure = {
            periodController.navigation.navigateTo(
                AzkarCatalog.itemsFor(AzkarPeriod.Evening).indexOfFirst { it.id == "kingdom" },
            )
        },
        focusText = "благе этой ночи",
        substring = true,
    )

    @Test
    fun capture393LightMorningKingdomVariant() = renderAndCapture(
        initialPeriod = AzkarPeriod.Morning,
        fileName = "azkar_phase5_393_light_morning_kingdom.png",
        configure = {
            periodController.navigation.navigateTo(
                AzkarCatalog.itemsFor(AzkarPeriod.Morning).indexOfFirst { it.id == "kingdom" },
            )
        },
        focusText = "благе этого дня",
        substring = true,
    )

    @Test
    fun capture393LightEveningList() = renderAndCapture(
        initialPeriod = AzkarPeriod.Evening,
        fileName = "azkar_phase5_393_light_evening_list.png",
        configure = { uiController.setViewMode(AzkarReaderViewMode.List) },
        focusText = "Список",
    )

    @Test
    fun capture393DarkEvening() = renderAndCapture(
        initialPeriod = AzkarPeriod.Evening,
        fileName = "azkar_phase5_393_dark_evening.png",
        configure = {
            uiController.updateSettings { it.copy(themeMode = AzkarThemeMode.Dark) }
        },
        focusText = "1 из 13",
    )

    @Test
    fun capture360LightMorning() = renderAndCapture(
        initialPeriod = AzkarPeriod.Morning,
        fileName = "azkar_phase5_360_light_morning.png",
        focusText = "1 из 14",
    )

    @Test
    fun capture412LightEvening() = renderAndCapture(
        initialPeriod = AzkarPeriod.Evening,
        fileName = "azkar_phase5_412_light_evening.png",
        focusText = "1 из 13",
    )

    @Test
    fun captureLongEveningBaqarahLastTwo() = renderAndCapture(
        initialPeriod = AzkarPeriod.Evening,
        fileName = "azkar_phase5_393_long_evening_baqarah.png",
        configure = {
            periodController.navigation.navigateTo(
                AzkarCatalog.itemsFor(AzkarPeriod.Evening)
                    .indexOfFirst { it.id == "baqarah-last-two" },
            )
        },
        focusText = "13 из 13",
    )

    @Test
    fun captureCompletedProgressAfterPeriodSwitch() = renderAndCapture(
        initialPeriod = AzkarPeriod.Morning,
        fileName = "azkar_phase5_393_completed_after_switch.png",
        configure = {
            uiController.incrementProgress("sayyid-istighfar", 1)
            periodController.switchTo(AzkarPeriod.Evening)
        },
        focusText = "Выполнено",
    )

    private fun renderAndCapture(
        initialPeriod: AzkarPeriod,
        fileName: String,
        configure: () -> Unit = {},
        focusText: String,
        substring: Boolean = false,
    ) {
        composeRule.setContent {
            val period = remember { AzkarPeriodReaderController(initialPeriod) }
            val ui = remember {
                AzkarReaderUiController(
                    initialSettings = AzkarReaderSettings(themeMode = AzkarThemeMode.Light),
                )
            }
            periodController = period
            uiController = ui
            AzkarProductionReaderScreen(
                periodController = period,
                uiController = ui,
                modifier = Modifier.fillMaxSize(),
            )
        }
        composeRule.waitForIdle()
        composeRule.runOnIdle(configure)
        composeRule.waitForIdle()
        composeRule.onNodeWithText(focusText, substring = substring).assertExists()
        saveScreenshot(captureAfterPresentedFrame(), fileName)
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
            if (hasRenderedComposeBody(screenshot)) return screenshot
        }
        error("Physical Phase 5 screenshot surface stayed visually blank")
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

    private fun saveScreenshot(bitmap: Bitmap, fileName: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.getExternalFilesDir(null), "phase2-evidence")
        check(directory.exists() || directory.mkdirs())
        val file = File(directory, fileName)
        FileOutputStream(file).use { stream ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
        }
        check(file.isFile && file.length() > 0L)
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
