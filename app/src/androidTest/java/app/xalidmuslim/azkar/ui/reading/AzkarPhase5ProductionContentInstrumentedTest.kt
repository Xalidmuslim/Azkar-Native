package app.xalidmuslim.azkar.ui.reading

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import app.xalidmuslim.azkar.content.AzkarCatalog
import app.xalidmuslim.azkar.ui.designsystem.AzkarThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AzkarPhase5ProductionContentInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var periodController: AzkarPeriodReaderController
    private lateinit var uiController: AzkarReaderUiController

    @Test
    fun defaultMorningRendersOneOf14() {
        setReader()
        composeRule.onNodeWithText("1 из 14").assertIsDisplayed()
        assertEquals(AzkarPeriod.Morning, periodController.period)
    }

    @Test
    fun morningContentsHas14Rows() {
        setReader()
        openContents()
        composeRule.onNodeWithTag(AzkarSheetTestTags.ContentsItemPrefix + 13).fetchSemanticsNode()
        assertTrue(
            composeRule.onAllNodesWithTag(AzkarSheetTestTags.ContentsItemPrefix + 14)
                .fetchSemanticsNodes()
                .isEmpty(),
        )
    }

    @Test
    fun switchToEveningRendersOneOf13() {
        setReader()
        switchEvening()
        composeRule.onNodeWithText("1 из 13").assertIsDisplayed()
        assertEquals(AzkarPeriod.Evening, periodController.period)
    }

    @Test
    fun eveningContentsHas13Rows() {
        setReader(AzkarPeriod.Evening)
        openContents()
        composeRule.onNodeWithTag(AzkarSheetTestTags.ContentsItemPrefix + 12).fetchSemanticsNode()
        assertTrue(
            composeRule.onAllNodesWithTag(AzkarSheetTestTags.ContentsItemPrefix + 13)
                .fetchSemanticsNodes()
                .isEmpty(),
        )
    }

    @Test
    fun eveningContainsBaqarahLastTwo() {
        setReader(AzkarPeriod.Evening)
        openContents()
        composeRule.onNodeWithText("Последние два аята Аль-Бакара").fetchSemanticsNode()
    }

    @Test
    fun morningDoesNotContainBaqarahLastTwo() {
        setReader()
        openContents()
        assertTrue(
            composeRule.onAllNodesWithText("Последние два аята Аль-Бакара")
                .fetchSemanticsNodes()
                .isEmpty(),
        )
    }

    @Test
    fun eveningDoesNotContainTahlilHundred() {
        setReader(AzkarPeriod.Evening)
        openContents()
        assertTrue(
            composeRule.onAllNodesWithText("Тахлиль — 100 раз в начале дня")
                .fetchSemanticsNodes()
                .isEmpty(),
        )
    }

    @Test
    fun kingdomTextChangesByPeriod() {
        setReader()
        composeRule.runOnIdle {
            periodController.navigation.navigateTo(
                AzkarCatalog.itemsFor(AzkarPeriod.Morning).indexOfFirst { it.id == "kingdom" },
            )
        }
        composeRule.onNodeWithText("благе этого дня", substring = true).fetchSemanticsNode()

        switchEvening()
        composeRule.runOnIdle {
            periodController.navigation.navigateTo(
                AzkarCatalog.itemsFor(AzkarPeriod.Evening).indexOfFirst { it.id == "kingdom" },
            )
        }
        composeRule.onNodeWithText("благе этой ночи", substring = true).fetchSemanticsNode()
    }

    @Test
    fun byYouTextChangesByPeriod() {
        setReader()
        composeRule.runOnIdle {
            periodController.navigation.navigateTo(
                AzkarCatalog.itemsFor(AzkarPeriod.Morning).indexOfFirst { it.id == "by-you" },
            )
        }
        composeRule.onNodeWithText("к Тебе — возвращение", substring = true).fetchSemanticsNode()

        switchEvening()
        composeRule.runOnIdle {
            periodController.navigation.navigateTo(
                AzkarCatalog.itemsFor(AzkarPeriod.Evening).indexOfFirst { it.id == "by-you" },
            )
        }
        composeRule.onNodeWithText("к Тебе — воскресение", substring = true).fetchSemanticsNode()
    }

    @Test
    fun periodSwitchOpensFirstItemAtTop() {
        setReader()
        composeRule.runOnIdle {
            periodController.navigation.navigateTo(5)
            periodController.navigation.recordScrollY(420)
        }
        switchEvening()
        composeRule.runOnIdle {
            assertEquals(0, periodController.navigation.state.activeIndex)
            assertEquals(0, periodController.navigation.currentScrollY)
            assertTrue(periodController.navigation.state.history.isEmpty())
        }
        composeRule.onNodeWithText("1 из 13").assertIsDisplayed()
    }

    @Test
    fun settingsPreservedAfterPeriodSwitch() {
        setReader()
        composeRule.runOnIdle {
            uiController.updateSettings { it.copy(showTranslation = false) }
        }
        switchEvening()
        composeRule.runOnIdle { assertFalse(uiController.state.settings.showTranslation) }
        assertTrue(
            composeRule.onAllNodesWithTag(AzkarReadingTestTags.Translation)
                .fetchSemanticsNodes()
                .isEmpty(),
        )
    }

    @Test
    fun listModePreservedAfterPeriodSwitch() {
        setReader()
        composeRule.runOnIdle { uiController.setViewMode(AzkarReaderViewMode.List) }
        composeRule.waitForIdle()
        switchEvening()
        composeRule.runOnIdle { assertEquals(AzkarReaderViewMode.List, uiController.state.viewMode) }
        composeRule.onNodeWithTag(AzkarReadingTestTags.List).assertIsDisplayed()
        composeRule.onNodeWithTag(AzkarReadingTestTags.ListCardPrefix + "sayyid-istighfar")
            .fetchSemanticsNode()
    }

    @Test
    fun sharedProgressPreservedAfterPeriodSwitch() {
        setReader()
        composeRule.runOnIdle { uiController.incrementProgress("sayyid-istighfar", 1) }
        composeRule.waitForIdle()
        switchEvening()
        composeRule.runOnIdle { assertEquals(1, uiController.currentCount("sayyid-istighfar")) }
        composeRule.onNodeWithText("Выполнено").fetchSemanticsNode()
    }

    @Test
    fun morningResetAffectsOnlyMorningVisibleIds() {
        setReader()
        composeRule.runOnIdle {
            uiController.incrementProgress("sayyid-istighfar", 1)
            uiController.incrementProgress("perfect-words", 1)
        }
        composeRule.onNodeWithTag(AzkarReadingTestTags.ResetProgress)
            .performScrollTo()
            .performClick()
        composeRule.runOnIdle {
            assertEquals(0, uiController.currentCount("sayyid-istighfar"))
            assertEquals(1, uiController.currentCount("perfect-words"))
        }
    }

    @Test
    fun eveningResetAffectsOnlyEveningVisibleIds() {
        setReader(AzkarPeriod.Evening)
        composeRule.runOnIdle {
            uiController.incrementProgress("sayyid-istighfar", 1)
            uiController.incrementProgress("tahlil-hundred", 100)
        }
        composeRule.onNodeWithTag(AzkarReadingTestTags.ResetProgress)
            .performScrollTo()
            .performClick()
        composeRule.runOnIdle {
            assertEquals(0, uiController.currentCount("sayyid-istighfar"))
            assertEquals(1, uiController.currentCount("tahlil-hundred"))
        }
    }

    @Test
    fun cardsSwipeRespectsFilteredEveningBounds() {
        setReader(AzkarPeriod.Evening)
        composeRule.runOnIdle { periodController.navigation.navigateTo(12) }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(AzkarReadingTestTags.Card)
            .performTouchInput { swipeLeft() }
        composeRule.runOnIdle { assertEquals(12, periodController.navigation.state.activeIndex) }
    }

    @Test
    fun androidBackClosesSheetBeforeReaderHistory() {
        setReader()
        composeRule.runOnIdle { periodController.navigation.navigateTo(2) }
        openContents()

        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.pressBack()
        composeRule.waitForIdle()
        assertTrue(
            composeRule.onAllNodesWithTag(AzkarSheetTestTags.Contents)
                .fetchSemanticsNodes()
                .isEmpty(),
        )
        composeRule.runOnIdle { assertEquals(2, periodController.navigation.state.activeIndex) }

        device.pressBack()
        composeRule.waitForIdle()
        composeRule.runOnIdle { assertEquals(0, periodController.navigation.state.activeIndex) }
    }

    @Test
    fun explanationOpensForCurrentEveningItem() {
        val expectedId = "perfect-words"
        val eveningItems = AzkarCatalog.itemsFor(AzkarPeriod.Evening)
        val requestedIndex = eveningItems.indexOfFirst { it.id == expectedId }
        assertTrue(requestedIndex >= 0)
        val expectedTitle = eveningItems[requestedIndex].title

        setReader(AzkarPeriod.Evening)
        composeRule.runOnIdle {
            periodController.navigation.navigateTo(requestedIndex)
        }

        composeRule.waitForIdle()

        composeRule.runOnIdle {
            val activeIndex = periodController.navigation.state.activeIndex
            assertEquals(requestedIndex, activeIndex)
            assertEquals(expectedId, eveningItems[activeIndex].id)
            assertEquals(AzkarReaderSheet.None, uiController.state.activeSheet)
            assertEquals(null, uiController.state.selectedExplanationId)
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            val cardNode = runCatching {
                composeRule
                    .onNodeWithTag(AzkarReadingTestTags.Card, useUnmergedTree = true)
                    .fetchSemanticsNode()
            }.getOrNull() ?: return@waitUntil false
            val renderedTitleNodes = composeRule
                .onAllNodesWithText(expectedTitle, useUnmergedTree = true)
                .fetchSemanticsNodes()
            val explainNodes = composeRule
                .onAllNodesWithTag(AzkarReadingTestTags.Explain, useUnmergedTree = true)
                .fetchSemanticsNodes()

            renderedTitleNodes.count { isDescendantOf(it, cardNode.id) } == 1 &&
                explainNodes.count { isDescendantOf(it, cardNode.id) } == 1
        }

        val cardNode = composeRule
            .onNodeWithTag(AzkarReadingTestTags.Card, useUnmergedTree = true)
            .fetchSemanticsNode()
        val renderedTitleNodes = composeRule
            .onAllNodesWithText(expectedTitle, useUnmergedTree = true)
            .fetchSemanticsNodes()
        val explainNodes = composeRule
            .onAllNodesWithTag(AzkarReadingTestTags.Explain, useUnmergedTree = true)
            .fetchSemanticsNodes()

        assertEquals(1, renderedTitleNodes.count { isDescendantOf(it, cardNode.id) })
        assertEquals(1, explainNodes.count { isDescendantOf(it, cardNode.id) })

        composeRule.onNodeWithTag(AzkarReadingTestTags.Card, useUnmergedTree = true)
            .performScrollTo()
        composeRule.onNodeWithTag(AzkarReadingTestTags.Explain, useUnmergedTree = true)
            .performScrollTo()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule
                    .onNodeWithTag(AzkarReadingTestTags.Explain, useUnmergedTree = true)
                    .assertIsDisplayed()
            }.isSuccess
        }

        composeRule.onNodeWithTag(AzkarReadingTestTags.Explain, useUnmergedTree = true)
            .assertIsDisplayed()
            .performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            uiController.state.activeSheet == AzkarReaderSheet.Explanation &&
                uiController.state.selectedExplanationId == expectedId
        }
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertEquals(AzkarReaderSheet.Explanation, uiController.state.activeSheet)
            assertEquals(expectedId, uiController.state.selectedExplanationId)
        }

        val sheetNode = composeRule
            .onNodeWithTag(AzkarSheetTestTags.Explanation, useUnmergedTree = true)
            .fetchSemanticsNode()
        val sheetTitleNodes = composeRule
            .onAllNodesWithText(expectedTitle, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .filter { isDescendantOf(it, sheetNode.id) }
        val relatedSectionNodes = composeRule
            .onAllNodesWithText("Связанный случай", useUnmergedTree = true)
            .fetchSemanticsNodes()
            .filter { isDescendantOf(it, sheetNode.id) }

        assertEquals(1, sheetTitleNodes.size)
        assertEquals(1, relatedSectionNodes.size)
    }

    @Test
    fun darkThemeSurvivesPeriodSwitch() {
        setReader()
        composeRule.runOnIdle {
            uiController.updateSettings { it.copy(themeMode = AzkarThemeMode.Dark) }
        }
        switchEvening()
        composeRule.runOnIdle { assertEquals(AzkarThemeMode.Dark, uiController.state.settings.themeMode) }
    }

    @Test
    fun cardsListSwitchingWorksAfterPeriodSwitch() {
        setReader()
        switchEvening()
        openContents()
        composeRule.onNodeWithTag(AzkarSheetTestTags.ContentsListMode).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(AzkarReadingTestTags.List).assertIsDisplayed()

        composeRule.onNodeWithTag(AzkarReadingTestTags.OpenContents).performClick()
        composeRule.onNodeWithTag(AzkarSheetTestTags.ContentsCardsMode).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(AzkarReadingTestTags.Card).assertIsDisplayed()
        composeRule.onNodeWithText("1 из 13").assertIsDisplayed()
    }

    private fun isDescendantOf(node: SemanticsNode, ancestorId: Int): Boolean {
        var parent = node.parent
        while (parent != null) {
            if (parent.id == ancestorId) return true
            parent = parent.parent
        }
        return false
    }

    private fun setReader(initialPeriod: AzkarPeriod = AzkarPeriod.Morning) {
        composeRule.setContent {
            val period = remember { AzkarPeriodReaderController(initialPeriod) }
            val ui = remember { AzkarReaderUiController() }
            periodController = period
            uiController = ui
            AzkarProductionReaderScreen(
                periodController = period,
                uiController = ui,
                modifier = Modifier.fillMaxSize(),
            )
        }
        composeRule.waitForIdle()
    }

    private fun switchEvening() {
        composeRule.onNodeWithTag(AzkarReadingTestTags.PeriodEvening)
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()
    }

    private fun openContents() {
        composeRule.onNodeWithTag(AzkarReadingTestTags.OpenContents)
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(AzkarSheetTestTags.Contents).assertIsDisplayed()
    }
}
