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
import com.oblab.toiletmap.data.PlaceRepository
import com.oblab.toiletmap.data.PlaceWithStats
import com.oblab.toiletmap.data.distanceMeters
import com.oblab.toiletmap.data.formatDistance
import com.oblab.toiletmap.data.nearestWithin
import com.oblab.toiletmap.ui.list.PlaceListItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 등록하려는 위치 근처(30m)에 이미 있는 장소. */
data class DuplicateCandidate(val place: PlaceWithStats, val distanceM: Double, val location: Location)

class MapViewModel(private val repository: PlaceRepository) : ViewModel() {

    val places = repository.places.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var selectedId by mutableStateOf<String?>(null)
        private set

    /** null이 아니면 "이전 장소에 방문 추가 / 새 장소로 등록"을 묻는 중. */
    var duplicate by mutableStateOf<DuplicateCandidate?>(null)
        private set

    fun select(placeId: String?) {
        selectedId = placeId
    }

    /** [현재 위치 등록]으로 받은 위치. 근처 장소가 있으면 먼저 묻고, 없으면 바로 새 장소로 저장. */
    fun onRegisterLocation(location: Location) {
        val near = places.value.nearestWithin(location.latitude, location.longitude)
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
            initializer { MapViewModel((this[APPLICATION_KEY] as ToiletMapApp).repository) }
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
