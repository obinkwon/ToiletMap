package com.oblab.toiletmap.ui.list

import org.junit.Assert.assertEquals
import org.junit.Test

class FilterAndSortTest {
    private val places = listOf(
        PlaceListItem("a", "역삼 센터빌딩", "서울 강남구 테헤란로", distanceMeters = 500, lastVisitedAt = 1, visitCount = 1),
        PlaceListItem("b", null, "서울 강남구 역삼동", distanceMeters = 100, lastVisitedAt = 3, visitCount = 1),
        PlaceListItem("c", "Hongdae Station", null, distanceMeters = 900, lastVisitedAt = 2, visitCount = 1),
    )

    @Test
    fun emptyQuery_keepsAll_sortedByRecent() {
        assertEquals(listOf("b", "c", "a"), filterAndSort(places, "  ", PlaceSort.RECENT).map { it.id })
    }

    @Test
    fun query_matchesNameOrAddress_sortedByDistance() {
        assertEquals(listOf("b", "a"), filterAndSort(places, "역삼", PlaceSort.NEAREST).map { it.id })
    }

    @Test
    fun query_ignoresCase_andNullFields() {
        assertEquals(listOf("c"), filterAndSort(places, "hongdae", PlaceSort.RECENT).map { it.id })
    }
}
