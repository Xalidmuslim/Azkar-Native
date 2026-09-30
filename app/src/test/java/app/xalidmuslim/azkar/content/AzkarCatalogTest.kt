package app.xalidmuslim.azkar.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AzkarCatalogTest {
    private val expectedIds = listOf(
        "sayyid-istighfar",
        "tahlil-ten",
        "tahlil-hundred",
        "tasbih-hundred",
        "kingdom",
        "by-you",
        "bismillah-protection",
        "afiyah",
        "pleased",
        "hayy-qayyum",
        "fatir",
        "muawwidhat",
        "creation-count",
        "fitrah",
        "perfect-words",
        "baqarah-last-two",
    )

    private val commonIds = setOf(
        "sayyid-istighfar",
        "tahlil-ten",
        "tasbih-hundred",
        "kingdom",
        "by-you",
        "bismillah-protection",
        "afiyah",
        "pleased",
        "hayy-qayyum",
        "fatir",
        "muawwidhat",
    )

    @Test
    fun uniqueRecordCountIs16() {
        assertEquals(16, AzkarCatalog.all.size)
    }

    @Test
    fun morningVisibleCountIs14() {
        assertEquals(14, AzkarCatalog.itemsFor(AzkarPeriod.Morning).size)
    }

    @Test
    fun eveningVisibleCountIs13() {
        assertEquals(13, AzkarCatalog.itemsFor(AzkarPeriod.Evening).size)
    }

    @Test
    fun stableIdsAreExactAndUnique() {
        assertEquals(expectedIds, AzkarCatalog.all.map { it.id })
        assertEquals(16, AzkarCatalog.stableIds.size)
    }

    @Test
    fun allTargetCountsAreCanonical() {
        val expected = mapOf(
            "sayyid-istighfar" to 1,
            "tahlil-ten" to 10,
            "tahlil-hundred" to 100,
            "tasbih-hundred" to 100,
            "kingdom" to 1,
            "by-you" to 1,
            "bismillah-protection" to 1,
            "afiyah" to 1,
            "pleased" to 1,
            "hayy-qayyum" to 1,
            "fatir" to 1,
            "muawwidhat" to 3,
            "creation-count" to 3,
            "fitrah" to 1,
            "perfect-words" to 1,
            "baqarah-last-two" to 1,
        )
        assertEquals(expected, AzkarCatalog.all.associate { it.id to it.targetCount })
    }

    @Test
    fun morningOnlyMembershipIsExact() {
        val ids = AzkarCatalog.all
            .filter { it.periods == setOf(AzkarPeriod.Morning) }
            .map { it.id }
            .toSet()
        assertEquals(setOf("tahlil-hundred", "creation-count", "fitrah"), ids)
    }

    @Test
    fun eveningOnlyMembershipIsExact() {
        val ids = AzkarCatalog.all
            .filter { it.periods == setOf(AzkarPeriod.Evening) }
            .map { it.id }
            .toSet()
        assertEquals(setOf("perfect-words", "baqarah-last-two"), ids)
    }

    @Test
    fun commonMembershipIsExact() {
        val ids = AzkarCatalog.all
            .filter { it.periods == setOf(AzkarPeriod.Morning, AzkarPeriod.Evening) }
            .map { it.id }
            .toSet()
        assertEquals(commonIds, ids)
    }

    @Test
    fun kingdomHasDistinctPeriodSpecificVariants() {
        val morning = AzkarCatalog.item("kingdom").resolve(AzkarPeriod.Morning)
        val evening = AzkarCatalog.item("kingdom").resolve(AzkarPeriod.Evening)
        assertNotEquals(morning.arabic, evening.arabic)
        assertNotEquals(morning.translation, evening.translation)
    }

    @Test
    fun byYouHasDistinctPeriodSpecificVariants() {
        val morning = AzkarCatalog.item("by-you").resolve(AzkarPeriod.Morning)
        val evening = AzkarCatalog.item("by-you").resolve(AzkarPeriod.Evening)
        assertNotEquals(morning.arabic, evening.arabic)
        assertNotEquals(morning.translation, evening.translation)
        assertTrue(morning.arabic.endsWith("الْمَصِيرُ"))
        assertTrue(evening.arabic.endsWith("النُّشُورُ"))
    }

    @Test
    fun sharedDhikrUsesSameStableIdAcrossPeriods() {
        val morning = AzkarCatalog.item("muawwidhat").resolve(AzkarPeriod.Morning)
        val evening = AzkarCatalog.item("muawwidhat").resolve(AzkarPeriod.Evening)
        assertEquals("muawwidhat", morning.id)
        assertEquals(morning.id, evening.id)
    }

    @Test
    fun allRecordsProvideExplanationAndSource() {
        AzkarCatalog.all.forEach { item ->
            assertTrue(item.source.isNotBlank())
            assertTrue(item.explanation.meaning.isNotBlank())
            assertTrue(item.explanation.references.isNotEmpty())
        }
    }
}
