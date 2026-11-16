package com.oblab.toiletmap.data

import android.content.Context
import android.location.Geocoder
import android.location.Location
import androidx.glance.appwidget.updateAll
import com.oblab.toiletmap.widget.RegisterWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * [appScope]: 화면이 닫혀도 끝까지 돌아야 하는 후속 작업용(주소 채우기, 위젯 갱신).
 * 위젯 등록 화면은 저장 직후 바로 닫히기 때문.
 */
class PlaceRepository(
    private val dao: PlaceDao,
    private val context: Context,
    private val appScope: CoroutineScope,
) {

    val places = dao.observePlaces()

    suspend fun lastVisitedAt(): Long? = dao.lastVisitedAt()

    /**
     * 새 장소로 등록하고 id를 돌려준다. 주소는 저장 후 따로 채운다.
     * 앱 등록은 중복 확인을 거쳐서 dup_checked=Y, 위젯 등록은 확인 없이 저장해서 N.
     */
    suspend fun registerNewPlace(location: Location, fromWidget: Boolean = false): String {
        val now = System.currentTimeMillis()
        val place = PlaceEntity(
            latitude = location.latitude,
            longitude = location.longitude,
            createdAt = now,
            updatedAt = now,
            dupChecked = if (fromWidget) "N" else "Y",
        )
        dao.insertPlaceWithVisit(place, location.toVisit(place.id, now))
        appScope.launch { fillAddress(place.id, location.latitude, location.longitude) }
        refreshWidget()
        return place.id
    }

    /** 이전 장소에 방문만 추가. */
    suspend fun addVisit(placeId: String, location: Location) {
        dao.insertVisit(location.toVisit(placeId, System.currentTimeMillis()))
        refreshWidget()
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
        appScope.launch { fillAddress(placeId, lat, lng) }
    }

    suspend fun delete(placeId: String) {
        dao.softDelete(placeId, System.currentTimeMillis())
        refreshWidget()
    }

    suspend fun uncheckedIds(): List<String> = dao.uncheckedIds()

    /** 합칠 필요 없음(또는 사용자가 따로 두기로 함). 다시 묻지 않는다. */
    suspend fun markDupChecked(placeId: String) = dao.markDupChecked(placeId, System.currentTimeMillis())

    /** 위젯으로 등록한 [fromId]를 이전 장소 [toId]로 합친다. */
    suspend fun merge(fromId: String, toId: String) = dao.merge(fromId, toId, System.currentTimeMillis())

    suspend fun exportBackup(): String =
        toBackupJson(dao.allPlaces(), dao.allVisits(), System.currentTimeMillis())

    /** 백업을 가져오고 가져온 장소 수를 돌려준다. 파일이 잘못되면 예외(메시지는 사용자용). */
    suspend fun importBackup(json: String): Int {
        val data = parseBackup(json)
        dao.importBackup(data.places, data.visits)
        refreshWidget()
        return data.places.size
    }

    /** 홈 화면 위젯의 "마지막 등록" 시각을 다시 그린다. */
    private fun refreshWidget() {
        appScope.launch { RegisterWidget().updateAll(context) }
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
