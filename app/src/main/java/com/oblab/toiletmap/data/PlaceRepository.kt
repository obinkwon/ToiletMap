package com.oblab.toiletmap.data

import android.content.Context
import android.location.Geocoder
import android.location.Location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class PlaceRepository(private val dao: PlaceDao, private val context: Context) {

    val places = dao.observePlaces()

    /** 앱에서 새 장소로 등록. 중복 확인을 거친 뒤라 dup_checked=Y. 새 장소 id를 돌려준다. */
    suspend fun registerNewPlace(location: Location): String {
        val now = System.currentTimeMillis()
        val place = PlaceEntity(
            latitude = location.latitude,
            longitude = location.longitude,
            createdAt = now,
            updatedAt = now,
            dupChecked = "Y",
        )
        dao.insertPlaceWithVisit(place, location.toVisit(place.id, now))
        fillAddress(place.id, location.latitude, location.longitude)
        return place.id
    }

    /** 이전 장소에 방문만 추가. */
    suspend fun addVisit(placeId: String, location: Location) {
        dao.insertVisit(location.toVisit(placeId, System.currentTimeMillis()))
    }

    /** 이름·비밀번호·메모 저장. 빈 칸은 null로 저장한다. */
    suspend fun updateInfo(placeId: String, name: String, password: String, memo: String) {
        dao.updateInfo(
            placeId,
            name.trim().ifEmpty { null },
            password.ifEmpty { null },
            memo.trim().ifEmpty { null },
            System.currentTimeMillis(),
        )
    }

    /** 핀 위치 보정. 주소도 새 위치로 다시 찾는다. */
    suspend fun updateLocation(placeId: String, lat: Double, lng: Double) {
        dao.updateLocation(placeId, lat, lng, System.currentTimeMillis())
        fillAddress(placeId, lat, lng)
    }

    suspend fun delete(placeId: String) {
        dao.softDelete(placeId, System.currentTimeMillis())
    }

    // ponytail: 오프라인이면 주소 없이 끝난다. 연결되면 채우는 WorkManager 작업은 아직 없음
    @Suppress("DEPRECATION") // 비동기 getFromLocation은 API 33+ 전용이라 minSdk 28에선 동기 버전을 IO 스레드에서 쓴다
    private suspend fun fillAddress(placeId: String, lat: Double, lng: Double) = withContext(Dispatchers.IO) {
        val line = runCatching {
            Geocoder(context, Locale.KOREA).getFromLocation(lat, lng, 1)?.firstOrNull()?.getAddressLine(0)
        }.getOrNull() ?: return@withContext
        dao.updateAddress(placeId, line.removePrefix("대한민국 "), System.currentTimeMillis())
    }

    private fun Location.toVisit(placeId: String, now: Long) = VisitEntity(
        placeId = placeId,
        visitedAt = now,
        rawLatitude = latitude,
        rawLongitude = longitude,
        accuracyM = if (hasAccuracy()) accuracy.toDouble() else null,
    )
}
