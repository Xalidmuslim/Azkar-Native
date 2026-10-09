package app.xalidmuslim.azkar.ui.reading

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AzkarReaderViewModeTest {
    @Test
    fun defaultViewModeIsCards() {
        val ui = AzkarReaderUiController()
        assertEquals(AzkarReaderViewMode.Cards, ui.state.viewMode)
    }

    @Test
    fun cardsToList() {
        val ui = AzkarReaderUiController()
        assertTrue(ui.setViewMode(AzkarReaderViewMode.List))
        assertEquals(AzkarReaderViewMode.List, ui.state.viewMode)
    }

    @Test
    fun listToCards() {
        val ui = AzkarReaderUiController(initialViewMode = AzkarReaderViewMode.List)
        assertTrue(ui.setViewMode(AzkarReaderViewMode.Cards))
        assertEquals(AzkarReaderViewMode.Cards, ui.state.viewMode)
    }

    @Test
    fun switchModeKeepsSelectedDhikr() {
        val navigation = AzkarReaderNavigationController(itemCount = 4, initialIndex = 2)
        val ui = AzkarReaderUiController()

        ui.setViewMode(AzkarReaderViewMode.List)
        ui.setViewMode(AzkarReaderViewMode.Cards)

        assertEquals(2, navigation.state.activeIndex)
        assertTrue(navigation.state.history.isEmpty())
    }

    @Test
    fun cardsToListKeepsActiveDhikrAnchor() {
        val navigation = AzkarReaderNavigationController(itemCount = 4)
        navigation.navigateTo(2)
        val historyBefore = navigation.state.history
        val ui = AzkarReaderUiController()

        ui.setViewMode(AzkarReaderViewMode.List)

        assertEquals(2, navigation.state.activeIndex)
        assertEquals(historyBefore, navigation.state.history)
    }

    @Test
    fun contentsJumpInListUpdatesSelectedAnchorWithoutHistory() {
        val navigation = AzkarReaderNavigationController(itemCount = 4)
        val ui = AzkarReaderUiController(initialViewMode = AzkarReaderViewMode.List)

        assertTrue(navigation.selectAnchor(3))

        assertEquals(AzkarReaderViewMode.List, ui.state.viewMode)
        assertEquals(3, navigation.state.activeIndex)
        assertTrue(navigation.state.history.isEmpty())
    }

    @Test
    fun listToCardsOpensSelectedAnchor() {
        val navigation = AzkarReaderNavigationController(itemCount = 4)
        val ui = AzkarReaderUiController(initialViewMode = AzkarReaderViewMode.List)
        navigation.selectAnchor(3)

        ui.setViewMode(AzkarReaderViewMode.Cards)

        assertEquals(AzkarReaderViewMode.Cards, ui.state.viewMode)
        assertEquals(3, navigation.state.activeIndex)
    }

    @Test
    fun listScrollDoesNotCreateReaderHistory() {
        val navigation = AzkarReaderNavigationController(itemCount = 4)
        navigation.recordScrollY(640)

        assertEquals(640, navigation.currentScrollY)
        assertTrue(navigation.state.history.isEmpty())
    }

    @Test
    fun modeSwitchDoesNotDuplicateReaderHistory() {
        val navigation = AzkarReaderNavigationController(itemCount = 4)
        navigation.navigateTo(1)
        val ui = AzkarReaderUiController()
        val historyBefore = navigation.state.history

        ui.setViewMode(AzkarReaderViewMode.List)
        ui.setViewMode(AzkarReaderViewMode.Cards)

        assertEquals(historyBefore, navigation.state.history)
    }

    @Test
    fun sheetOpenCloseDoesNotChangeViewMode() {
        val ui = AzkarReaderUiController(initialViewMode = AzkarReaderViewMode.List)
        ui.openContents()
        ui.closeSheet()
        ui.openExplanation("muawwidhat")
        ui.closeSheet()

        assertEquals(AzkarReaderViewMode.List, ui.state.viewMode)
    }

    @Test
    fun settingsUpdateKeepsListMode() {
        val ui = AzkarReaderUiController(initialViewMode = AzkarReaderViewMode.List)
        ui.updateSettings { it.copy(showTranslation = false) }

        assertEquals(AzkarReaderViewMode.List, ui.state.viewMode)
        assertFalse(ui.state.settings.showTranslation)
    }

    @Test
    fun explanationKeepsListMode() {
        val ui = AzkarReaderUiController(initialViewMode = AzkarReaderViewMode.List)
        ui.openExplanation("muawwidhat")
        ui.closeSheet()

        assertEquals(AzkarReaderViewMode.List, ui.state.viewMode)
    }

    @Test
    fun noCardsSwipeNavigationWhileListModeActive() {
        val ui = AzkarReaderUiController(initialViewMode = AzkarReaderViewMode.List)
        assertFalse(ui.state.allowsHorizontalPaging(entryCount = 4))

        ui.setViewMode(AzkarReaderViewMode.Cards)
        assertTrue(ui.state.allowsHorizontalPaging(entryCount = 4))

        ui.openSettings()
        assertFalse(ui.state.allowsHorizontalPaging(entryCount = 4))
    }
}
