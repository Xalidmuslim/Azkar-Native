package app.xalidmuslim.azkar.ui.reading

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import app.xalidmuslim.azkar.ui.designsystem.AzkarThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AzkarReaderSheetInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var navigation: AzkarReaderNavigationController
    private lateinit var readerUi: AzkarReaderUiController

    private fun setReader(
        entries: List<AzkarReaderEntry> = listOf(
            AzkarReaderEntry(AzkarGoldenReadingFixtures.Muawwidhat),
            AzkarReaderEntry(AzkarGoldenReadingFixtures.BaqarahLastTwo),
        ),
        initialIndex: Int = 0,
    ) {
        composeRule.setContent {
            val nav = remember(entries.size, initialIndex) {
                AzkarReaderNavigationController(entries.size, initialIndex)
            }
            val ui = remember {
                AzkarReaderUiController(
                    initialSettings = AzkarReaderSettings(themeMode = AzkarThemeMode.Light),
                )
            }
            navigation = nav
            readerUi = ui
            AzkarReaderScreen(
                entries = entries,
                period = AzkarPeriod.Evening,
                controller = nav,
                uiController = ui,
                modifier = Modifier.fillMaxSize(),
            )
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(AzkarReadingTestTags.Card).assertIsDisplayed()
    }

    @Test
    fun settingsSheetOpensAndClosesWithAndroidBack() {
        setReader()
        composeRule.onNodeWithTag(AzkarReadingTestTags.OpenSettingsTop).performClick()
        composeRule.onNodeWithTag(AzkarSheetTestTags.Settings).assertIsDisplayed()

        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(AzkarSheetTestTags.Settings).assertDoesNotExist()
        composeRule.runOnIdle {
            assertEquals(AzkarReaderSheet.None, readerUi.state.activeSheet)
            assertEquals(0, navigation.state.activeIndex)
        }
    }

    @Test
    fun contentsSheetOpensAndDirectNavigationClosesIt() {
        setReader(
            entries = listOf(
                AzkarReaderEntry(AzkarGoldenReadingFixtures.BaqarahLastTwo),
                AzkarReaderEntry(AzkarGoldenReadingFixtures.Muawwidhat),
            ),
        )
        composeRule.onNodeWithTag(AzkarReadingTestTags.Card).performTouchInput { swipeUp() }
        composeRule.waitForIdle()
        composeRule.runOnIdle { assertTrue(navigation.currentScrollY > 0) }

        composeRule.onNodeWithTag(AzkarReadingTestTags.OpenContents)
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeRule.waitForIdle()
        composeRule.runOnIdle {
            assertEquals(AzkarReaderSheet.Contents, readerUi.state.activeSheet)
        }
        composeRule.onNodeWithTag(AzkarSheetTestTags.Contents).assertIsDisplayed()
        composeRule.onNodeWithTag(AzkarSheetTestTags.ContentsItemPrefix + 1).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(AzkarSheetTestTags.Contents).assertDoesNotExist()
        composeRule.runOnIdle {
            assertEquals(1, navigation.state.activeIndex)
            assertEquals(0, navigation.currentScrollY)
        }
    }

    @Test
    fun explanationBackPreservesReaderContext() {
        setReader()
        repeat(3) {
            composeRule.onNodeWithTag(AzkarReadingTestTags.Card)
                .performTouchInput { swipeUp() }
            composeRule.waitForIdle()
        }
        composeRule.onNodeWithTag(AzkarReadingTestTags.Explain)
            .assertIsDisplayed()
            .performClick()
        composeRule.waitForIdle()

        var scrollBefore = 0
        composeRule.runOnIdle {
            scrollBefore = navigation.currentScrollY
            assertEquals(AzkarReaderSheet.Explanation, readerUi.state.activeSheet)
            assertEquals("muawwidhat", readerUi.state.selectedExplanationId)
        }
        composeRule.onNodeWithTag(AzkarSheetTestTags.Explanation).assertIsDisplayed()

        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(AzkarSheetTestTags.Explanation).assertDoesNotExist()
        composeRule.runOnIdle {
            assertEquals(0, navigation.state.activeIndex)
            assertEquals(scrollBefore, navigation.currentScrollY)
            assertEquals(AzkarReaderSheet.None, readerUi.state.activeSheet)
        }
    }

    @Test
    fun backPriorityClosesSheetBeforeReaderHistory() {
        setReader()
        composeRule.onNodeWithTag(AzkarReadingTestTags.Card).performTouchInput { swipeLeft() }
        composeRule.runOnIdle { assertEquals(1, navigation.state.activeIndex) }

        composeRule.onNodeWithTag(AzkarReadingTestTags.OpenSettingsTop).performClick()
        composeRule.onNodeWithTag(AzkarSheetTestTags.Settings).assertIsDisplayed()

        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.pressBack()
        composeRule.waitForIdle()
        composeRule.runOnIdle {
            assertEquals(1, navigation.state.activeIndex)
            assertEquals(AzkarReaderSheet.None, readerUi.state.activeSheet)
        }

        device.pressBack()
        composeRule.waitForIdle()
        composeRule.runOnIdle { assertEquals(0, navigation.state.activeIndex) }
    }

    @Test
    fun settingsStayInMemoryAndApplyImmediately() {
        setReader()
        composeRule.onNodeWithTag(AzkarReadingTestTags.OpenSettingsTop).performClick()
        composeRule.onNodeWithTag(AzkarSheetTestTags.SettingTranslation)
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()
        composeRule.runOnIdle { assertFalse(readerUi.state.settings.showTranslation) }

        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(AzkarReadingTestTags.Translation).assertDoesNotExist()

        composeRule.onNodeWithTag(AzkarReadingTestTags.OpenSettingsTop).performClick()
        composeRule.runOnIdle { assertFalse(readerUi.state.settings.showTranslation) }
        composeRule.onNodeWithTag(AzkarSheetTestTags.Settings).assertIsDisplayed()
    }

    @Test
    fun themeChangeKeepsSettingsSheetAndReaderDestination() {
        setReader(initialIndex = 1)
        composeRule.onNodeWithTag(AzkarReadingTestTags.OpenSettingsTop).performClick()
        composeRule.onNodeWithTag(AzkarSheetTestTags.ThemeDark)
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(AzkarSheetTestTags.Settings).assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(AzkarThemeMode.Dark, readerUi.state.settings.themeMode)
            assertEquals(1, navigation.state.activeIndex)
        }
    }

    @Test
    fun settingsBackPreservesLongDhikrScroll() {
        setReader(
            entries = listOf(
                AzkarReaderEntry(AzkarGoldenReadingFixtures.BaqarahLastTwo),
                AzkarReaderEntry(AzkarGoldenReadingFixtures.Muawwidhat),
            ),
        )
        composeRule.onNodeWithTag(AzkarReadingTestTags.Card).performTouchInput { swipeUp() }
        composeRule.waitForIdle()

        var before = 0
        composeRule.runOnIdle {
            before = navigation.currentScrollY
            assertTrue(before > 0)
        }

        composeRule.onNodeWithTag(AzkarReadingTestTags.OpenSettingsTop).performClick()
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertEquals(0, navigation.state.activeIndex)
            assertEquals(before, navigation.currentScrollY)
        }
    }

    @Test
    fun sheetBlocksHorizontalPagingGesture() {
        setReader()
        composeRule.onNodeWithTag(AzkarReadingTestTags.OpenSettingsTop).performClick()
        composeRule.onNodeWithTag(AzkarSheetTestTags.Settings).assertIsDisplayed()

        composeRule.onNodeWithTag(AzkarReadingTestTags.Card).performTouchInput { swipeLeft() }
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertEquals(0, navigation.state.activeIndex)
            assertEquals(AzkarReaderSheet.Settings, readerUi.state.activeSheet)
        }
    }
}
