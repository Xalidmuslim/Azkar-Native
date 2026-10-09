package app.xalidmuslim.azkar.ui.reading

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AzkarReaderNavigationControllerTest {
    @Test
    fun initialIndexIsZero() {
        val controller = AzkarReaderNavigationController(itemCount = 4)
        assertEquals(0, controller.state.activeIndex)
    }

    @Test
    fun nextMovesExactlyOneItem() {
        val controller = AzkarReaderNavigationController(itemCount = 4)
        assertTrue(controller.next())
        assertEquals(1, controller.state.activeIndex)
        assertEquals(1L, controller.state.generation)
    }

    @Test
    fun previousMovesExactlyOneItem() {
        val controller = AzkarReaderNavigationController(itemCount = 4, initialIndex = 2)
        assertTrue(controller.previous())
        assertEquals(1, controller.state.activeIndex)
    }

    @Test
    fun boundsDoNotNavigate() {
        val first = AzkarReaderNavigationController(itemCount = 3)
        assertFalse(first.previous())
        assertEquals(0, first.state.activeIndex)

        val last = AzkarReaderNavigationController(itemCount = 3, initialIndex = 2)
        assertFalse(last.next())
        assertEquals(2, last.state.activeIndex)
    }

    @Test
    fun navigationBuildsHistory() {
        val controller = AzkarReaderNavigationController(itemCount = 4)
        controller.next()
        controller.next()
        assertEquals(listOf(0, 1), controller.state.history)
    }

    @Test
    fun backUsesHistory() {
        val controller = AzkarReaderNavigationController(itemCount = 4)
        controller.next()
        controller.next()
        assertTrue(controller.back())
        assertEquals(1, controller.state.activeIndex)
        assertTrue(controller.back())
        assertEquals(0, controller.state.activeIndex)
        assertFalse(controller.back())
    }

    @Test
    fun directNavigationBackReturnsToActualOrigin() {
        val controller = AzkarReaderNavigationController(itemCount = 4)
        assertTrue(controller.navigateTo(3))
        assertEquals(3, controller.state.activeIndex)
        assertTrue(controller.back())
        assertEquals(0, controller.state.activeIndex)
    }

    @Test
    fun newItemResetsScrollPosition() {
        val controller = AzkarReaderNavigationController(itemCount = 3)
        controller.recordScrollY(480)
        assertEquals(480, controller.currentScrollY)
        controller.next()
        assertEquals(0, controller.currentScrollY)
    }

    @Test
    fun sameDestinationDoesNotCreateDuplicateEvent() {
        val controller = AzkarReaderNavigationController(itemCount = 3)
        assertFalse(controller.navigateTo(0))
        assertEquals(0L, controller.state.generation)
        assertTrue(controller.next())
        assertFalse(controller.navigateTo(1))
        assertEquals(1L, controller.state.generation)
        assertEquals(listOf(0), controller.state.history)
    }
}

class AzkarSwipeSessionTest {
    @Test
    fun horizontalBelowThresholdDoesNotPage() {
        val session = AzkarSwipeSession(thresholdPx = 55f, dominanceRatio = 1.15f)
        assertNull(session.update(dx = -54f, dy = 0f, pointerCount = 1))
    }

    @Test
    fun swipeLeftPagesNextOnce() {
        val session = AzkarSwipeSession(thresholdPx = 55f, dominanceRatio = 1.15f)
        assertEquals(AzkarSwipeDecision.Next, session.update(-80f, 10f, 1))
        assertNull(session.update(-120f, 10f, 1))
    }

    @Test
    fun swipeRightPagesPrevious() {
        val session = AzkarSwipeSession(thresholdPx = 55f, dominanceRatio = 1.15f)
        assertEquals(AzkarSwipeDecision.Previous, session.update(80f, 10f, 1))
    }

    @Test
    fun verticalDominantGestureNeverPages() {
        val session = AzkarSwipeSession(thresholdPx = 55f, dominanceRatio = 1.15f)
        assertNull(session.update(30f, -80f, 1))
        assertNull(session.update(120f, -90f, 1))
    }

    @Test
    fun multiTouchCancelsPaging() {
        val session = AzkarSwipeSession(thresholdPx = 55f, dominanceRatio = 1.15f)
        assertNull(session.update(-20f, 0f, 2))
        assertNull(session.update(-100f, 0f, 1))
    }

    @Test
    fun cancellationPreventsPaging() {
        val session = AzkarSwipeSession(thresholdPx = 55f, dominanceRatio = 1.15f)
        session.cancel()
        assertNull(session.update(-100f, 0f, 1))
    }
}
