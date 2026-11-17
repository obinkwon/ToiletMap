package com.oblab.toiletmap.ui.map

/**
 * 화면 좌표가 [radius] 안에 모인 것끼리 묶는다. 앞에서부터 하나를 잡고 그 주변을 한 묶음으로 가져가는 단순한 방식.
 * 핀이 수백 개 정도라 O(n²)로 충분하다.
 */
fun <T> clusterByDistance(items: List<T>, radius: Float, position: (T) -> Pair<Float, Float>): List<List<T>> {
    val pos = items.map(position)
    val used = BooleanArray(items.size)
    val r2 = radius * radius
    return items.indices.mapNotNull { i ->
        if (used[i]) return@mapNotNull null
        val (x, y) = pos[i]
        items.indices
            .filter { j ->
                val dx = pos[j].first - x
                val dy = pos[j].second - y
                // j == i: 좌표가 NaN이어도 자기 자신은 꼭 넣어 빈 묶음이 안 생기게 한다
                j == i || (!used[j] && dx * dx + dy * dy <= r2)
            }
            .onEach { used[it] = true }
            .map { items[it] }
    }
}
