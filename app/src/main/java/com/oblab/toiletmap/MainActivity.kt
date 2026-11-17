package com.oblab.toiletmap

import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kakao.vectormap.KakaoMap
import com.oblab.toiletmap.ui.list.PlaceListScreen
import com.oblab.toiletmap.ui.settings.SettingsRoute
import com.oblab.toiletmap.ui.map.CameraTarget
import com.oblab.toiletmap.ui.map.DeleteConfirmDialog
import com.oblab.toiletmap.ui.map.DuplicateDialog
import com.oblab.toiletmap.ui.map.EditInfoDialog
import com.oblab.toiletmap.ui.map.MapPin
import com.oblab.toiletmap.ui.map.MapScreen
import com.oblab.toiletmap.ui.map.MapViewModel
import com.oblab.toiletmap.ui.map.MergeDialog
import com.oblab.toiletmap.ui.map.PlaceSheetActions
import com.oblab.toiletmap.ui.map.rememberMyLocationRequester
import com.oblab.toiletmap.ui.map.toDetail
import com.oblab.toiletmap.ui.map.toListItem

/** 이 장소를 선택한 채로 앱을 연다(위젯 완료 카드의 [앱에서 보기]). */
const val EXTRA_PLACE_ID = "place_id"

/** 지금 지도 위에 떠 있는 것. 한 번에 하나만. */
private enum class Overlay { NONE, LIST, SETTINGS, EDIT_LOCATION, EDIT_INFO, DELETE }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val context = LocalContext.current
                val vm: MapViewModel = viewModel(factory = MapViewModel.Factory)
                val places by vm.places.collectAsState()
                var myLocation by remember { mutableStateOf<Location?>(null) }
                var cameraTarget by remember { mutableStateOf<CameraTarget?>(null) }
                var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }
                var locating by remember { mutableStateOf(false) }
                val onBusy = { busy: Boolean -> locating = busy }
                var overlay by rememberSaveable { mutableStateOf(Overlay.NONE) }

                fun moveTo(lat: Double, lng: Double) {
                    cameraTarget = CameraTarget(lat, lng)
                }

                val requestMyLocation = rememberMyLocationRequester(onBusy) {
                    myLocation = it
                    moveTo(it.latitude, it.longitude)
                }
                val requestRegisterLocation = rememberMyLocationRequester(onBusy) {
                    myLocation = it
                    moveTo(it.latitude, it.longitude)
                    vm.onRegisterLocation(it)
                }
                val selected = places.find { it.id == vm.selectedId }
                val requestVisitLocation = rememberMyLocationRequester(onBusy) { location ->
                    myLocation = location
                    selected?.let {
                        vm.addVisit(it.id, location)
                        Toast.makeText(context, "방문을 추가했어요", Toast.LENGTH_SHORT).show()
                    }
                }

                // 위젯에서 [앱에서 보기]로 열었으면 그 장소로, 아니면 현재 위치로 이동
                var pendingPlaceId by rememberSaveable { mutableStateOf(intent.getStringExtra(EXTRA_PLACE_ID)) }
                LaunchedEffect(Unit) { if (pendingPlaceId == null) requestMyLocation() }
                LaunchedEffect(places, pendingPlaceId) {
                    val place = places.find { it.id == pendingPlaceId } ?: return@LaunchedEffect // DB 로딩 전이면 다음 places에서 다시
                    vm.select(place.id)
                    moveTo(place.latitude, place.longitude)
                    pendingPlaceId = null
                }

                BackHandler(enabled = overlay != Overlay.NONE || selected != null) {
                    if (overlay != Overlay.NONE) overlay = Overlay.NONE else vm.select(null)
                }

                // 지도는 항상 그려 두고 목록·설정은 그 위에 덮는다.
                // 지도를 빼면 MapView가 새로 만들어져 기본 위치에서 다시 시작하기 때문
                Box(Modifier.fillMaxSize()) {
                    MapScreen(
                        selected = selected?.toDetail(myLocation),
                        pins = places.map { MapPin(it.id, it.latitude, it.longitude) },
                        myLocation = myLocation,
                        locating = locating,
                        cameraTarget = cameraTarget,
                        editingLocation = overlay == Overlay.EDIT_LOCATION && selected != null,
                        onPinClick = vm::select,
                        onMyLocationClick = requestMyLocation,
                        onRegisterClick = requestRegisterLocation,
                        onListClick = { overlay = Overlay.LIST },
                        onSettingsClick = { overlay = Overlay.SETTINGS },
                        sheetActions = PlaceSheetActions(
                            onEditLocation = {
                                selected?.let { moveTo(it.latitude, it.longitude) }
                                overlay = Overlay.EDIT_LOCATION
                            },
                            onAddVisit = requestVisitLocation,
                            onEditInfo = { overlay = Overlay.EDIT_INFO },
                            onDelete = { overlay = Overlay.DELETE },
                            onDismiss = { vm.select(null) },
                        ),
                        onConfirmLocation = {
                            val center = kakaoMap?.cameraPosition?.position
                            if (selected != null && center != null) {
                                vm.updateLocation(selected.id, center.latitude, center.longitude)
                            }
                            overlay = Overlay.NONE
                        },
                        onCancelEditLocation = { overlay = Overlay.NONE },
                        onMapReady = { kakaoMap = it },
                    )

                    if (overlay == Overlay.SETTINGS || overlay == Overlay.LIST) {
                        // Surface가 터치를 막아서 아래 지도로 넘어가지 않는다
                        Surface(Modifier.fillMaxSize()) {
                            if (overlay == Overlay.SETTINGS) {
                                SettingsRoute(onBack = { overlay = Overlay.NONE })
                            } else {
                                PlaceListScreen(
                                    places = places.map { it.toListItem(myLocation) },
                                    onBack = { overlay = Overlay.NONE },
                                    onPlaceClick = { item ->
                                        overlay = Overlay.NONE
                                        vm.select(item.id)
                                        places.find { it.id == item.id }?.let { moveTo(it.latitude, it.longitude) }
                                    },
                                )
                            }
                        }
                    }
                }

                vm.duplicate?.let { candidate ->
                    DuplicateDialog(
                        candidate = candidate,
                        onAddVisit = vm::addVisitToDuplicate,
                        onNewPlace = { vm.saveNewPlace(candidate.location) },
                        onDismiss = vm::dismissDuplicate,
                    )
                }

                vm.mergeQueue.firstOrNull()?.let { candidate ->
                    MergeDialog(
                        candidate = candidate,
                        remaining = vm.mergeQueue.size,
                        onMerge = vm::mergeCurrent,
                        onKeepSeparate = vm::keepSeparateCurrent,
                        onLater = vm::postponeMerges,
                    )
                }

                if (selected != null && overlay == Overlay.EDIT_INFO) {
                    EditInfoDialog(
                        place = selected,
                        onSave = { name, password, memo ->
                            vm.updateInfo(selected.id, name, password, memo)
                            overlay = Overlay.NONE
                        },
                        onDismiss = { overlay = Overlay.NONE },
                    )
                }

                if (selected != null && overlay == Overlay.DELETE) {
                    DeleteConfirmDialog(
                        place = selected,
                        onDelete = {
                            vm.delete(selected.id)
                            overlay = Overlay.NONE
                        },
                        onDismiss = { overlay = Overlay.NONE },
                    )
                }
            }
        }
    }
}
