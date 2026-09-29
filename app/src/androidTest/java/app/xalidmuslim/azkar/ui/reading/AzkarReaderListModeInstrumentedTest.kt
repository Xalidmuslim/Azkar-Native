package app.xalidmuslim.azkar.ui.reading

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
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
class AzkarReaderListModeInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var navigation: AzkarReaderNavigationController
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
        translation = "Пример короткого текста для проверки нативного списка.",
        source = "Источник для UI-теста",
        note = note,
        hasInsight = hasInsight,
    )

    private fun entries() = listOf(
        AzkarReaderEntry(
            item(
                id = "one",
                title = "Первый азкар",
                disputed = true,
                note = "Примечание для проверки отображения.",
            ),
        ),
        AzkarReaderEntry(item(id = "two", title = "Второй азкар")),
        AzkarReaderEntry(item(id = "three", title = "Третий азкар", hasInsight = true)),
        AzkarReaderEntry(item(id = "four", title = "Четвёртый азкар")),
    )

    private fun setReader(
        initialIndex: Int = 0,
        initialViewMode: AzkarReaderViewMode = AzkarReaderViewMode.Cards,
    ) {
        val testEntries = entries()
        composeRule.setContent {
            val nav = remember {
                AzkarReaderNavigationController(testEntries.size, initialIndex)
            }
            val ui = remember {
                AzkarReaderUiController(
                    initialSettings = AzkarReaderSettings(themeMode = AzkarThemeMode.Light),
                    initialViewMode = initialViewMode,
                )
            }
            navigation = nav
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
    }

    private fun openContents() {
        composeRule.onNodeWithTag(AzkarReadingTestTags.OpenContents)
            .assertIsDisplayed()
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(AzkarSheetTestTags.Contents).assertIsDisplayed()
    }

    private fun chooseListMode() {
        composeRule.onNodeWithTag(AzkarSheetTestTags.ContentsListMode)
            .assertIsDisplayed()
            .performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun openContentsChooseListShowsList() {
        setReader()
        openContents()
        chooseListMode()

        composeRule.onNodeWithTag(AzkarReadingTestTags.List).assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(AzkarReaderViewMode.List, readerUi.state.viewMode)
            assertEquals(0, navigation.state.activeIndex)
        }
    }

    @Test
    fun multipleDhikrCardsExistInListSemantics() {
        setReader(initialViewMode = AzkarReaderViewMode.List)

        composeRule.onNodeWithTag(AzkarReadingTestTags.ListCardPrefix + "one").assertExists()
        composeRule.onNodeWithTag(AzkarReadingTestTags.ListCardPrefix + "two").assertExists()
        assertTrue(
            composeRule.onAllNodesWithTag(AzkarReadingTestTags.Card)
                .fetchSemanticsNodes().size >= 2,
        )
    }

    @Test
    fun verticalSwipeScrollsListWithoutReaderHistory() {
        setReader(initialViewMode = AzkarReaderViewMode.List)
        composeRule.onNodeWithTag(AzkarReadingTestTags.ListCardPrefix + "one").assertIsDisplayed()

        repeat(2) {
            composeRule.onNodeWithTag(AzkarReadingTestTags.List)
                .performTouchInput { swipeUp() }
            composeRule.waitForIdle()
        }

        composeRule.onNodeWithTag(AzkarReadingTestTags.ListCardPrefix + "one")
            .assertIsNotDisplayed()
        composeRule.runOnIdle {
            assertEquals(0, navigation.state.activeIndex)
            assertTrue(navigation.state.history.isEmpty())
        }
    }

    @Test
    fun horizontalSwipeInListDoesNotPage() {
        setReader(initialViewMode = AzkarReaderViewMode.List)
        composeRule.onNodeWithTag(AzkarReadingTestTags.List)
            .performTouchInput { swipeLeft() }
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertEquals(0, navigation.state.activeIndex)
            assertTrue(navigation.state.history.isEmpty())
        }
    }

    @Test
    fun cardsToListKeepsCurrentDhikr() {
        setReader()
        composeRule.onNodeWithTag(AzkarReadingTestTags.Card)
            .performTouchInput { swipeLeft() }
        composeRule.waitForIdle()
        composeRule.runOnIdle { assertEquals(1, navigation.state.activeIndex) }

        openContents()
        chooseListMode()

        composeRule.onNodeWithTag(AzkarReadingTestTags.ListCardPrefix + "two")
            .assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(1, navigation.state.activeIndex) }
    }

    @Test
    fun listContentsJumpSmoothScrollsAndUpdatesAnchor() {
        setReader(initialViewMode = AzkarReaderViewMode.List)
        openContents()
        composeRule.onNodeWithTag(AzkarSheetTestTags.ContentsItemPrefix + 2)
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(AzkarReadingTestTags.ListCardPrefix + "three")
            .assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(2, navigation.state.activeIndex)
            assertTrue(navigation.state.history.isEmpty())
        }
    }

    @Test
    fun listToCardsOpensSelectedItemAtTop() {
        setReader(initialViewMode = AzkarReaderViewMode.List)
        openContents()
        composeRule.onNodeWithTag(AzkarSheetTestTags.ContentsItemPrefix + 2)
            .performClick()
        composeRule.waitForIdle()

        openContents()
        composeRule.onNodeWithTag(AzkarSheetTestTags.ContentsCardsMode)
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(AzkarReadingTestTags.Card).assertIsDisplayed()
        composeRule.onNodeWithTag(AzkarReadingTestTags.Arabic).assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(AzkarReaderViewMode.Cards, readerUi.state.viewMode)
            assertEquals(2, navigation.state.activeIndex)
            assertEquals(0, navigation.currentScrollY)
        }
    }

    @Test
    fun settingsInListCloseKeepsScrollPosition() {
        setReader(initialViewMode = AzkarReaderViewMode.List)
        composeRule.onNodeWithTag(AzkarReadingTestTags.ListCardPrefix + "three")
            .performScrollTo()
            .assertIsDisplayed()

        composeRule.onNodeWithTag(AzkarReadingTestTags.OpenSettingsToolbar)
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithTag(AzkarSheetTestTags.Settings).assertIsDisplayed()

        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(AzkarReadingTestTags.ListCardPrefix + "three")
            .assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(AzkarReaderViewMode.List, readerUi.state.viewMode)
        }
    }

    @Test
    fun translationOffInListHidesTranslations() {
        setReader(initialViewMode = AzkarReaderViewMode.List)
        composeRule.onNodeWithTag(AzkarReadingTestTags.OpenSettingsToolbar).performClick()
        composeRule.onNodeWithTag(AzkarSheetTestTags.SettingTranslation)
            .performScrollTo()
            .performClick()
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(AzkarReadingTestTags.Translation).assertDoesNotExist()
        composeRule.runOnIdle {
            assertFalse(readerUi.state.settings.showTranslation)
            assertEquals(AzkarReaderViewMode.List, readerUi.state.viewMode)
        }
    }

    @Test
    fun sourcesOffInListHidesSources() {
        setReader(initialViewMode = AzkarReaderViewMode.List)
        composeRule.onNodeWithTag(AzkarReadingTestTags.OpenSettingsToolbar).performClick()
        composeRule.onNodeWithTag(AzkarSheetTestTags.SettingSources)
            .performScrollTo()
            .performClick()
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(AzkarReadingTestTags.Source).assertDoesNotExist()
        composeRule.runOnIdle { assertFalse(readerUi.state.settings.showSources) }
    }

    @Test
    fun notesOffInListHidesNotesAndDisputedBadge() {
        setReader(initialViewMode = AzkarReaderViewMode.List)
        composeRule.onNodeWithTag(AzkarReadingTestTags.OpenSettingsToolbar).performClick()
        composeRule.onNodeWithTag(AzkarSheetTestTags.SettingNotes)
            .performScrollTo()
            .performClick()
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(AzkarReadingTestTags.Note).assertDoesNotExist()
        composeRule.onNodeWithTag(AzkarReadingTestTags.Disputed).assertDoesNotExist()
        composeRule.runOnIdle { assertFalse(readerUi.state.settings.showNotes) }
    }

    @Test
    fun explanationBackReturnsToSameListPosition() {
        setReader(initialViewMode = AzkarReaderViewMode.List)
        composeRule.onNodeWithTag(AzkarReadingTestTags.ListCardPrefix + "three")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(AzkarReadingTestTags.Explain)
            .assertIsDisplayed()
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(AzkarSheetTestTags.Explanation).assertIsDisplayed()

        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(AzkarReadingTestTags.ListCardPrefix + "three")
            .assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(2, navigation.state.activeIndex)
            assertEquals(AzkarReaderViewMode.List, readerUi.state.viewMode)
            assertEquals(AzkarReaderSheet.None, readerUi.state.activeSheet)
        }
    }

    @Test
    fun androidBackClosesSheetBeforeActualReaderHistory() {
        setReader()
        composeRule.onNodeWithTag(AzkarReadingTestTags.Card)
            .performTouchInput { swipeLeft() }
        composeRule.waitForIdle()
        openContents()
        chooseListMode()
        composeRule.onNodeWithTag(AzkarReadingTestTags.OpenSettingsToolbar).performClick()
        composeRule.onNodeWithTag(AzkarSheetTestTags.Settings).assertIsDisplayed()

        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertEquals(AzkarReaderSheet.None, readerUi.state.activeSheet)
            assertEquals(1, navigation.state.activeIndex)
            assertEquals(listOf(0), navigation.state.history)
        }
    }

    @Test
    fun listScrollDoesNotInventAndroidBackHistory() {
        setReader(initialViewMode = AzkarReaderViewMode.List)
        repeat(2) {
            composeRule.onNodeWithTag(AzkarReadingTestTags.List)
                .performTouchInput { swipeUp() }
            composeRule.waitForIdle()
        }

        composeRule.runOnIdle {
            assertTrue(navigation.state.history.isEmpty())
            assertFalse(readerUi.handleBack(navigation))
            assertEquals(0, navigation.state.activeIndex)
            assertEquals(AzkarReaderViewMode.List, readerUi.state.viewMode)
        }
    }

    @Test
    fun switchingBackToCardsKeepsExistingSwipeNavigation() {
        setReader(initialViewMode = AzkarReaderViewMode.List)
        openContents()
        composeRule.onNodeWithTag(AzkarSheetTestTags.ContentsCardsMode)
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(AzkarReadingTestTags.Card)
            .performTouchInput { swipeLeft() }
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertEquals(AzkarReaderViewMode.Cards, readerUi.state.viewMode)
            assertEquals(1, navigation.state.activeIndex)
            assertEquals(listOf(0), navigation.state.history)
        }
    }
}
