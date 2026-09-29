package app.xalidmuslim.azkar.ui.reading

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import app.xalidmuslim.azkar.ui.designsystem.AzkarTheme
import app.xalidmuslim.azkar.ui.designsystem.AzkarThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AzkarReaderNavigationInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var controller: AzkarReaderNavigationController

    private fun setReader(
        entries: List<AzkarReaderEntry> = listOf(
            AzkarReaderEntry(AzkarGoldenReadingFixtures.Muawwidhat),
            AzkarReaderEntry(AzkarGoldenReadingFixtures.BaqarahLastTwo),
        ),
        initialIndex: Int = 0,
    ) {
        composeRule.setContent {
            AzkarTheme(themeMode = AzkarThemeMode.Light) {
                val rememberedController = remember(entries.size, initialIndex) {
                    AzkarReaderNavigationController(entries.size, initialIndex)
                }
                controller = rememberedController
                AzkarReaderScreen(
                    entries = entries,
                    period = AzkarPeriod.Evening,
                    controller = rememberedController,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(AzkarReadingTestTags.Card).assertIsDisplayed()
    }

    @Test
    fun nextButtonMovesOneToTwo() {
        setReader()
        composeRule.onNodeWithTag(AzkarReadingTestTags.Next).performScrollTo().performClick()
        composeRule.runOnIdle { assertEquals(1, controller.state.activeIndex) }
    }

    @Test
    fun previousButtonMovesTwoToOne() {
        setReader(initialIndex = 1)
        composeRule.onNodeWithTag(AzkarReadingTestTags.Previous).performScrollTo().performClick()
        composeRule.runOnIdle { assertEquals(0, controller.state.activeIndex) }
    }

    @Test
    fun swipeLeftMovesOneToTwo() {
        setReader()
        composeRule.onNodeWithTag(AzkarReadingTestTags.Card).performTouchInput { swipeLeft() }
        composeRule.runOnIdle { assertEquals(1, controller.state.activeIndex) }
    }

    @Test
    fun swipeRightMovesTwoToOne() {
        setReader(initialIndex = 1)
        composeRule.onNodeWithTag(AzkarReadingTestTags.Card).performTouchInput { swipeRight() }
        composeRule.runOnIdle { assertEquals(0, controller.state.activeIndex) }
    }

    @Test
    fun verticalScrollLongDhikrDoesNotPage() {
        setReader(
            entries = listOf(
                AzkarReaderEntry(AzkarGoldenReadingFixtures.BaqarahLastTwo),
                AzkarReaderEntry(AzkarGoldenReadingFixtures.Muawwidhat),
            ),
        )
        composeRule.onNodeWithTag(AzkarReadingTestTags.Card).performTouchInput { swipeUp() }
        composeRule.waitForIdle()
        composeRule.runOnIdle {
            assertEquals(0, controller.state.activeIndex)
            assertTrue(controller.currentScrollY > 0)
        }
    }

    @Test
    fun verticalDominantDiagonalDoesNotPage() {
        setReader()
        composeRule.onNodeWithTag(AzkarReadingTestTags.Card).performTouchInput {
            val start = center
            swipe(
                start = start,
                end = Offset(start.x + 35f, start.y - 150f),
                durationMillis = 300,
            )
        }
        composeRule.runOnIdle { assertEquals(0, controller.state.activeIndex) }
    }

    @Test
    fun subThresholdHorizontalMovementDoesNotPage() {
        setReader()
        composeRule.onNodeWithTag(AzkarReadingTestTags.Card).performTouchInput {
            val start = center
            swipe(
                start = start,
                end = Offset(start.x - 40f, start.y),
                durationMillis = 250,
            )
        }
        composeRule.runOnIdle { assertEquals(0, controller.state.activeIndex) }
    }

    @Test
    fun nextAfterLongScrollStartsNewDhikrAtTop() {
        setReader(
            entries = listOf(
                AzkarReaderEntry(AzkarGoldenReadingFixtures.BaqarahLastTwo),
                AzkarReaderEntry(AzkarGoldenReadingFixtures.Muawwidhat),
            ),
        )
        composeRule.onNodeWithTag(AzkarReadingTestTags.Card).performTouchInput { swipeUp() }
        composeRule.waitForIdle()
        composeRule.runOnIdle { assertTrue(controller.currentScrollY > 0) }

        composeRule.onNodeWithTag(AzkarReadingTestTags.Card).performTouchInput { swipeLeft() }
        composeRule.waitForIdle()
        composeRule.runOnIdle {
            assertEquals(1, controller.state.activeIndex)
            assertEquals(0, controller.currentScrollY)
        }
        composeRule.onNodeWithTag(AzkarReadingTestTags.Arabic).assertIsDisplayed()
    }

    @Test
    fun androidBackUsesReaderHistory() {
        setReader()
        composeRule.onNodeWithTag(AzkarReadingTestTags.Card).performTouchInput { swipeLeft() }
        composeRule.runOnIdle { assertEquals(1, controller.state.activeIndex) }

        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.pressBack()
        composeRule.waitForIdle()
        composeRule.runOnIdle { assertEquals(0, controller.state.activeIndex) }
    }

    @Test
    fun oneSwipeProducesOneNavigationEvent() {
        setReader(
            entries = listOf(
                AzkarReaderEntry(AzkarGoldenReadingFixtures.Muawwidhat),
                AzkarReaderEntry(AzkarGoldenReadingFixtures.BaqarahLastTwo),
                AzkarReaderEntry(AzkarGoldenReadingFixtures.Muawwidhat),
            ),
        )
        composeRule.onNodeWithTag(AzkarReadingTestTags.Card).performTouchInput { swipeLeft() }
        composeRule.runOnIdle {
            assertEquals(1, controller.state.activeIndex)
            assertEquals(1L, controller.state.generation)
        }
    }
}
