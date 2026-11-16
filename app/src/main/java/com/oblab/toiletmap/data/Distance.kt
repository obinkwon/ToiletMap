package com.oblab.toiletmap.data

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** 기획서: 중복 감지 반경 약 30m (설정에서 바꿀 수 있게 할 예정) */
const val DUPLICATE_RADIUS_M = 30.0

/** 두 좌표 사이 거리(m). 지구를 구로 보는 haversine 근사. 수십 m~수십 km에선 충분히 정확. */
fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
    return 2 * 6_371_000.0 * asin(sqrt(a))
}

/** [radiusM] 안에서 가장 가까운 장소와 거리. [excludeId] 장소(자기 자신)는 뺀다. 없으면 null. */
fun List<PlaceWithStats>.nearestWithin(
    lat: Double,
    lng: Double,
    radiusM: Double = DUPLICATE_RADIUS_M,
    excludeId: String? = null,
): Pair<PlaceWithStats, Double>? =
    filter { it.id != excludeId }
        .map { it to distanceMeters(lat, lng, it.latitude, it.longitude) }
        .filter { it.second <= radiusM }
        .minByOrNull { it.second }

fun formatDistance(meters: Double): String =
    if (meters < 1000) "${meters.toInt()}m" else "%.1fkm".format(meters / 1000)
