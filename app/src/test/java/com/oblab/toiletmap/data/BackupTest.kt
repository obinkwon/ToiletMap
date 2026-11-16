package com.oblab.toiletmap.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupTest {
    private val place = PlaceEntity(
        id = "p1", name = null, latitude = 37.5, longitude = 127.0, address = "서울", password = "12*#",
        memo = "메모\n줄바꿈", createdAt = 1, updatedAt = 2, deleted = "N", dupChecked = "Y",
    )
    private val visit = VisitEntity("v1", "p1", visitedAt = 3, rawLatitude = 37.5, rawLongitude = 127.0, accuracyM = null)

    @Test
    fun roundTrip_keepsEveryField() {
        val back = parseBackup(toBackupJson(listOf(place), listOf(visit), exportedAt = 9))
        assertEquals(listOf(place), back.places)
        assertEquals(listOf(visit), back.visits)
    }

    @Test
    fun rejectsOtherApp() {
        assertThrows(IllegalArgumentException::class.java) {
            parseBackup("""{"app":"other","version":1,"places":[],"visits":[]}""")
        }
    }

    @Test
    fun rejectsBadLatitude() {
        val bad = toBackupJson(listOf(place.copy(latitude = 123.0)), emptyList(), 0)
        assertThrows(IllegalArgumentException::class.java) { parseBackup(bad) }
    }
}
