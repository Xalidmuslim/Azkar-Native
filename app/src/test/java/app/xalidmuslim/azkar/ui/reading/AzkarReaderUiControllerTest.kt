package app.xalidmuslim.azkar.ui.reading

import app.xalidmuslim.azkar.ui.designsystem.AzkarThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AzkarReaderUiControllerTest {
    @Test
    fun initialSheetIsNone() {
        val ui = AzkarReaderUiController()
        assertEquals(AzkarReaderSheet.None, ui.state.activeSheet)
        assertNull(ui.state.selectedExplanationId)
    }

    @Test
    fun onlyOneSheetCanBeActive() {
        val ui = AzkarReaderUiController()
        ui.openSettings()
        assertEquals(AzkarReaderSheet.Settings, ui.state.activeSheet)

        ui.openContents()
        assertEquals(AzkarReaderSheet.Contents, ui.state.activeSheet)

        ui.openExplanation("muawwidhat")
        assertEquals(AzkarReaderSheet.Explanation, ui.state.activeSheet)
        assertEquals("muawwidhat", ui.state.selectedExplanationId)
    }

    @Test
    fun closingExplanationClearsSelection() {
        val ui = AzkarReaderUiController()
        ui.openExplanation("muawwidhat")
        assertTrue(ui.closeSheet())
        assertEquals(AzkarReaderSheet.None, ui.state.activeSheet)
        assertNull(ui.state.selectedExplanationId)
        assertFalse(ui.closeSheet())
    }

    @Test
    fun settingsRemainInMemoryAcrossSheetClose() {
        val ui = AzkarReaderUiController()
        ui.openSettings()
        ui.updateSettings {
            it.copy(
                showTranslation = false,
                themeMode = AzkarThemeMode.Dark,
                readerStyle = AzkarReaderStyle.Compact,
            )
        }
        ui.closeSheet()
        ui.openSettings()

        assertFalse(ui.state.settings.showTranslation)
        assertEquals(AzkarThemeMode.Dark, ui.state.settings.themeMode)
        assertEquals(AzkarReaderStyle.Compact, ui.state.settings.readerStyle)
    }

    @Test
    fun backClosesSheetBeforeReaderHistory() {
        val navigation = AzkarReaderNavigationController(itemCount = 4)
        navigation.navigateTo(2)
        val ui = AzkarReaderUiController()
        ui.openContents()

        assertTrue(ui.handleBack(navigation))
        assertEquals(AzkarReaderSheet.None, ui.state.activeSheet)
        assertEquals(2, navigation.state.activeIndex)
        assertEquals(listOf(0), navigation.state.history)

        assertTrue(ui.handleBack(navigation))
        assertEquals(0, navigation.state.activeIndex)
    }

    @Test
    fun backWithoutSheetUsesActualReaderHistory() {
        val navigation = AzkarReaderNavigationController(itemCount = 4)
        navigation.navigateTo(3)
        val ui = AzkarReaderUiController()

        assertTrue(ui.handleBack(navigation))
        assertEquals(0, navigation.state.activeIndex)
    }

    @Test
    fun firstItemWithoutSheetDoesNotConsumeBack() {
        val navigation = AzkarReaderNavigationController(itemCount = 2)
        val ui = AzkarReaderUiController()
        assertFalse(ui.handleBack(navigation))
    }

    @Test
    fun reopeningCurrentItemResetsScrollWithoutHistoryEntry() {
        val navigation = AzkarReaderNavigationController(itemCount = 2)
        navigation.recordScrollY(420)
        navigation.reopenCurrentAtTop()

        assertEquals(0, navigation.currentScrollY)
        assertEquals(0, navigation.state.activeIndex)
        assertTrue(navigation.state.history.isEmpty())
        assertEquals(1L, navigation.state.generation)
        assertEquals(AzkarNavigationDirection.None, navigation.state.direction)
    }
}
