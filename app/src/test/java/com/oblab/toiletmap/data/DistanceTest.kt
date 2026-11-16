package com.oblab.toiletmap.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DistanceTest {
    private fun place(id: String, lat: Double, lng: Double) =
        PlaceWithStats(id, null, lat, lng, null, null, null, visitCount = 1, lastVisitedAt = 0)

    @Test
    fun distance_matchesKnownValue() {
        // 위도 0.001도 ≈ 111.2m
        assertEquals(111.2, distanceMeters(37.5, 127.0, 37.501, 127.0), 0.5)
    }

    @Test
    fun nearestWithin_picksClosestInsideRadius() {
        val places = listOf(place("far", 37.5002, 127.0), place("near", 37.5001, 127.0), place("out", 37.6, 127.0))
        assertEquals("near", places.nearestWithin(37.5, 127.0)?.first?.id)
    }

    @Test
    fun nearestWithin_nullWhenNoneInside() {
        assertNull(listOf(place("out", 37.501, 127.0)).nearestWithin(37.5, 127.0))
    }

    @Test
    fun formatDistance_switchesToKm() {
        assertEquals("120m", formatDistance(120.4))
        assertEquals("1.2km", formatDistance(1200.0).replace(',', '.'))
    }
}
