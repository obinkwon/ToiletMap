package com.oblab.toiletmap.ui.map

import android.location.Location
import android.text.format.DateUtils
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.oblab.toiletmap.ToiletMapApp
import com.oblab.toiletmap.data.AppSettings
import com.oblab.toiletmap.data.PlaceRepository
import com.oblab.toiletmap.data.PlaceWithStats
import com.oblab.toiletmap.data.distanceMeters
import com.oblab.toiletmap.data.formatDistance
import com.oblab.toiletmap.data.nearestWithin
import com.oblab.toiletmap.ui.list.PlaceListItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 등록하려는 위치 근처(설정의 중복 반경 안)에 이미 있는 장소. */
data class DuplicateCandidate(val place: PlaceWithStats, val distanceM: Double, val location: Location)

/** 위젯으로 등록한 [newPlace]와 그 근처(설정의 중복 반경 안)의 이전 장소 [target]. */
data class MergeCandidate(val newPlace: PlaceWithStats, val target: PlaceWithStats, val distanceM: Double)

class MapViewModel(
    private val repository: PlaceRepository,
    private val settings: AppSettings,
) : ViewModel() {

    val places = repository.places.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var selectedId by mutableStateOf<String?>(null)
        private set

    /** 지도에 원으로 보여줄 중복 감지 반경(m). */
    val duplicateRadiusM: Int get() = settings.duplicateRadiusM

    /** null이 아니면 "이전 장소에 방문 추가 / 새 장소로 등록"을 묻는 중. */
    var duplicate by mutableStateOf<DuplicateCandidate?>(null)
        private set

    /** 위젯으로 등록한 장소 중 근처에 이전 장소가 있는 것. 앞에서부터 하나씩 묻는다. */
    var mergeQueue by mutableStateOf<List<MergeCandidate>>(emptyList())
        private set

    init {
        // 앱을 열 때 한 번: 위젯 등록(dup_checked=N) 장소를 근처 이전 장소와 비교
        viewModelScope.launch {
            val unchecked = repository.uncheckedIds()
            if (unchecked.isEmpty()) return@launch
            val all = repository.places.first()
            val queue = mutableListOf<MergeCandidate>()
            for (id in unchecked) {
                val place = all.find { it.id == id } ?: continue
                val near = all.nearestWithin(place.latitude, place.longitude, settings.duplicateRadiusM.toDouble(), excludeId = id)
                if (near == null) repository.markDupChecked(id) // 근처에 없으면 물을 것도 없음
                else queue += MergeCandidate(place, near.first, near.second)
            }
            mergeQueue = queue
        }
    }

    /** 합치기: 위젯 장소의 방문을 이전 장소로 옮긴다. */
    fun mergeCurrent() {
        val candidate = mergeQueue.firstOrNull() ?: return
        val fromId = candidate.newPlace.id
        // 합쳐져서 사라지는 장소가 걸린 남은 질문은 뺀다(위젯 장소끼리 서로 가까운 경우). 다음 실행 때 다시 계산
        mergeQueue = mergeQueue.drop(1).filter { it.newPlace.id != fromId && it.target.id != fromId }
        if (selectedId == candidate.newPlace.id) selectedId = candidate.target.id
        viewModelScope.launch { repository.merge(candidate.newPlace.id, candidate.target.id) }
    }

    /** 따로 두기: 별도 장소로 유지하고 다시 묻지 않는다. */
    fun keepSeparateCurrent() {
        val candidate = mergeQueue.firstOrNull() ?: return
        mergeQueue = mergeQueue.drop(1)
        viewModelScope.launch { repository.markDupChecked(candidate.newPlace.id) }
    }

    /** 나중에: 이번엔 넘기고 다음 앱 실행 때 다시 묻는다. */
    fun postponeMerges() {
        mergeQueue = emptyList()
    }

    fun select(placeId: String?) {
        selectedId = placeId
    }

    /** [현재 위치 등록]으로 받은 위치. 근처 장소가 있으면 먼저 묻고, 없으면 바로 새 장소로 저장. */
    fun onRegisterLocation(location: Location) {
        val near = places.value.nearestWithin(location.latitude, location.longitude, settings.duplicateRadiusM.toDouble())
        if (near != null) duplicate = DuplicateCandidate(near.first, near.second, location)
        else saveNewPlace(location)
    }

    fun saveNewPlace(location: Location) {
        duplicate = null
        viewModelScope.launch { selectedId = repository.registerNewPlace(location) }
    }

    fun addVisitToDuplicate() {
        val candidate = duplicate ?: return
        duplicate = null
        viewModelScope.launch {
            repository.addVisit(candidate.place.id, candidate.location)
            selectedId = candidate.place.id
        }
    }

    fun dismissDuplicate() {
        duplicate = null
    }

    fun addVisit(placeId: String, location: Location) {
        viewModelScope.launch { repository.addVisit(placeId, location) }
    }

    fun updateInfo(placeId: String, name: String, password: String, memo: String) {
        viewModelScope.launch { repository.updateInfo(placeId, name, password, memo) }
    }

    fun updateLocation(placeId: String, lat: Double, lng: Double) {
        viewModelScope.launch { repository.updateLocation(placeId, lat, lng) }
    }

    fun delete(placeId: String) {
        if (selectedId == placeId) selectedId = null
        viewModelScope.launch { repository.delete(placeId) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ToiletMapApp
                MapViewModel(app.repository, app.settings)
            }
        }
    }
}

/** 목록 화면에 보여줄 형태로 바꾼다. 내 위치를 모르면 거리는 null. */
fun PlaceWithStats.toListItem(myLocation: Location?) = PlaceListItem(
    id = id,
    name = name,
    address = address,
    distanceMeters = myLocation?.let { distanceMeters(it.latitude, it.longitude, latitude, longitude).toInt() },
    lastVisitedAt = lastVisitedAt ?: 0L,
    visitCount = visitCount,
)

/** 하단 시트에 보여줄 형태로 바꾼다. 내 위치를 모르면 거리는 빈 문자열. */
fun PlaceWithStats.toDetail(myLocation: Location?) = PlaceDetail(
    name = name ?: "이름 없음",
    address = address ?: "주소 확인 중",
    distance = myLocation?.let { formatDistance(distanceMeters(it.latitude, it.longitude, latitude, longitude)) } ?: "",
    visitCount = visitCount,
    lastVisit = lastVisitedAt?.let { DateUtils.getRelativeTimeSpanString(it).toString() } ?: "-",
    password = password,
    memo = memo,
)
