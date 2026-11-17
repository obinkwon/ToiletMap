package com.oblab.toiletmap.ui.map

import org.junit.Assert.assertEquals
import org.junit.Test

class ClusterTest {
    private fun cluster(vararg xs: Float) = clusterByDistance(xs.toList(), 10f) { it to 0f }

    @Test
    fun nearPointsGroupTogether() {
        assertEquals(listOf(listOf(0f, 5f), listOf(100f)), cluster(0f, 5f, 100f))
    }

    @Test
    fun eachPointBelongsToOneGroup() {
        // 8은 0과 15 둘 다에 가깝지만 먼저 잡힌 0 묶음에만 들어간다
        assertEquals(listOf(listOf(0f, 8f), listOf(15f)), cluster(0f, 8f, 15f))
    }

    @Test
    fun unknownPositionStaysAlone() {
        assertEquals(listOf(listOf(Float.NaN), listOf(0f)), cluster(Float.NaN, 0f))
    }

    @Test
    fun emptyInput() {
        assertEquals(emptyList<List<Float>>(), cluster())
    }
}
