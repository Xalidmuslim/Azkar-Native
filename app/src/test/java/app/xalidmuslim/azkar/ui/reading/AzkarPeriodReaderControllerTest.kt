package app.xalidmuslim.azkar.ui.reading

import app.xalidmuslim.azkar.content.AzkarCatalog
import app.xalidmuslim.azkar.ui.designsystem.AzkarThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AzkarPeriodReaderControllerTest {
    @Test
    fun defaultPeriodIsDeterministicMorning() {
        val controller = AzkarPeriodReaderController()
        assertEquals(AzkarPeriod.Morning, controller.period)
        assertEquals(0, controller.navigation.state.activeIndex)
    }

    @Test
    fun morningToEveningSelectsFirstItem() {
        val controller = AzkarPeriodReaderController(AzkarPeriod.Morning)
        controller.navigation.navigateTo(8)
        assertTrue(controller.switchTo(AzkarPeriod.Evening))
        assertEquals(AzkarPeriod.Evening, controller.period)
        assertEquals(0, controller.navigation.state.activeIndex)
        assertEquals("sayyid-istighfar", AzkarCatalog.firstReadingItem(controller.period).id)
    }

    @Test
    fun eveningToMorningSelectsFirstItem() {
        val controller = AzkarPeriodReaderController(AzkarPeriod.Evening)
        controller.navigation.navigateTo(7)
        assertTrue(controller.switchTo(AzkarPeriod.Morning))
        assertEquals(AzkarPeriod.Morning, controller.period)
        assertEquals(0, controller.navigation.state.activeIndex)
        assertEquals("sayyid-istighfar", AzkarCatalog.firstReadingItem(controller.period).id)
    }

    @Test
    fun periodSwitchResetsReadingScrollAndActiveIndex() {
        val controller = AzkarPeriodReaderController()
        controller.navigation.navigateTo(5)
        controller.navigation.recordScrollY(640)
        assertEquals(640, controller.navigation.currentScrollY)

        controller.switchTo(AzkarPeriod.Evening)

        assertEquals(0, controller.navigation.state.activeIndex)
        assertEquals(0, controller.navigation.currentScrollY)
    }

    @Test
    fun periodSwitchInvalidatesOldNavigationHistory() {
        val controller = AzkarPeriodReaderController()
        controller.navigation.next()
        controller.navigation.next()
        assertTrue(controller.navigation.state.history.isNotEmpty())

        controller.switchTo(AzkarPeriod.Evening)

        assertTrue(controller.navigation.state.history.isEmpty())
        assertFalse(controller.navigation.back())
    }

    @Test
    fun periodSwitchPreservesReaderSettings() {
        val period = AzkarPeriodReaderController()
        val ui = AzkarReaderUiController()
        ui.updateSettings {
            it.copy(
                showTranslation = false,
                themeMode = AzkarThemeMode.Dark,
                readerStyle = AzkarReaderStyle.Compact,
            )
        }
        val before = ui.state.settings

        period.switchTo(AzkarPeriod.Evening)

        assertEquals(before, ui.state.settings)
    }

    @Test
    fun periodSwitchPreservesViewMode() {
        val period = AzkarPeriodReaderController()
        val ui = AzkarReaderUiController()
        ui.setViewMode(AzkarReaderViewMode.List)

        period.switchTo(AzkarPeriod.Evening)

        assertEquals(AzkarReaderViewMode.List, ui.state.viewMode)
    }

    @Test
    fun periodSwitchPreservesSharedProgress() {
        val period = AzkarPeriodReaderController()
        val ui = AzkarReaderUiController()
        ui.incrementProgress("muawwidhat", 3)

        period.switchTo(AzkarPeriod.Evening)

        assertEquals(1, ui.currentCount("muawwidhat"))
    }

    @Test
    fun morningResetDoesNotResetEveningOnlyProgress() {
        val ui = AzkarReaderUiController()
        ui.incrementProgress("sayyid-istighfar", 1)
        ui.incrementProgress("perfect-words", 1)
        ui.incrementProgress("baqarah-last-two", 1)

        ui.resetProgress(AzkarCatalog.itemsFor(AzkarPeriod.Morning).map { it.id })

        assertEquals(0, ui.currentCount("sayyid-istighfar"))
        assertEquals(1, ui.currentCount("perfect-words"))
        assertEquals(1, ui.currentCount("baqarah-last-two"))
    }

    @Test
    fun eveningResetDoesNotResetMorningOnlyProgress() {
        val ui = AzkarReaderUiController()
        ui.incrementProgress("sayyid-istighfar", 1)
        ui.incrementProgress("tahlil-hundred", 100)
        ui.incrementProgress("creation-count", 3)

        ui.resetProgress(AzkarCatalog.itemsFor(AzkarPeriod.Evening).map { it.id })

        assertEquals(0, ui.currentCount("sayyid-istighfar"))
        assertEquals(1, ui.currentCount("tahlil-hundred"))
        assertEquals(1, ui.currentCount("creation-count"))
    }

    @Test
    fun morningHeaderDenominatorIs14() {
        assertEquals(14, AzkarCatalog.readingItemsFor(AzkarPeriod.Morning).size)
    }

    @Test
    fun eveningHeaderDenominatorIs13() {
        assertEquals(13, AzkarCatalog.readingItemsFor(AzkarPeriod.Evening).size)
    }

    @Test
    fun morningContentsCountIs14() {
        val rows = AzkarCatalog.readingItemsFor(AzkarPeriod.Morning)
        assertEquals((1..14).toList(), rows.indices.map { it + 1 })
    }

    @Test
    fun eveningContentsCountIs13() {
        val rows = AzkarCatalog.readingItemsFor(AzkarPeriod.Evening)
        assertEquals((1..13).toList(), rows.indices.map { it + 1 })
    }

    @Test
    fun cardsNavigationUsesMorningFilteredBounds() {
        val controller = AzkarPeriodReaderController(AzkarPeriod.Morning)
        repeat(20) { controller.navigation.next() }
        assertEquals(13, controller.navigation.state.activeIndex)
        assertFalse(controller.navigation.next())
    }

    @Test
    fun cardsNavigationUsesEveningFilteredBounds() {
        val controller = AzkarPeriodReaderController(AzkarPeriod.Evening)
        repeat(20) { controller.navigation.next() }
        assertEquals(12, controller.navigation.state.activeIndex)
        assertFalse(controller.navigation.next())
    }
}
