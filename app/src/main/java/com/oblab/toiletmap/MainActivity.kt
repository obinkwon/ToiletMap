package com.oblab.toiletmap

import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kakao.vectormap.KakaoMap
import com.oblab.toiletmap.ui.list.PlaceListScreen
import com.oblab.toiletmap.ui.map.CameraTarget
import com.oblab.toiletmap.ui.map.DeleteConfirmDialog
import com.oblab.toiletmap.ui.map.DuplicateDialog
import com.oblab.toiletmap.ui.map.EditInfoDialog
import com.oblab.toiletmap.ui.map.MapPin
import com.oblab.toiletmap.ui.map.MapScreen
import com.oblab.toiletmap.ui.map.MapViewModel
import com.oblab.toiletmap.ui.map.PlaceSheetActions
import com.oblab.toiletmap.ui.map.rememberMyLocationRequester
import com.oblab.toiletmap.ui.map.toDetail
import com.oblab.toiletmap.ui.map.toListItem

/** 지금 지도 위에 떠 있는 것. 한 번에 하나만. */
private enum class Overlay { NONE, LIST, EDIT_LOCATION, EDIT_INFO, DELETE }

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
                var overlay by rememberSaveable { mutableStateOf(Overlay.NONE) }

                fun moveTo(lat: Double, lng: Double) {
                    cameraTarget = CameraTarget(lat, lng)
                }

                val requestMyLocation = rememberMyLocationRequester {
                    myLocation = it
                    moveTo(it.latitude, it.longitude)
                }
                val requestRegisterLocation = rememberMyLocationRequester {
                    myLocation = it
                    moveTo(it.latitude, it.longitude)
                    vm.onRegisterLocation(it)
                }
                val selected = places.find { it.id == vm.selectedId }
                val requestVisitLocation = rememberMyLocationRequester { location ->
                    myLocation = location
                    selected?.let {
                        vm.addVisit(it.id, location)
                        Toast.makeText(context, "방문을 추가했어요", Toast.LENGTH_SHORT).show()
                    }
                }

                // 앱을 열면 바로 현재 위치로 이동
                LaunchedEffect(Unit) { requestMyLocation() }

                BackHandler(enabled = overlay != Overlay.NONE || selected != null) {
                    if (overlay != Overlay.NONE) overlay = Overlay.NONE else vm.select(null)
                }

                if (overlay == Overlay.LIST) {
                    PlaceListScreen(
                        places = places.map { it.toListItem(myLocation) },
                        onBack = { overlay = Overlay.NONE },
                        onPlaceClick = { item ->
                            overlay = Overlay.NONE
                            vm.select(item.id)
                            places.find { it.id == item.id }?.let { moveTo(it.latitude, it.longitude) }
                        },
                    )
                } else {
                    MapScreen(
                        selected = selected?.toDetail(myLocation),
                        pins = places.map { MapPin(it.id, it.latitude, it.longitude) },
                        myLocation = myLocation,
                        cameraTarget = cameraTarget,
                        editingLocation = overlay == Overlay.EDIT_LOCATION && selected != null,
                        onPinClick = vm::select,
                        onMyLocationClick = requestMyLocation,
                        onRegisterClick = requestRegisterLocation,
                        onListClick = { overlay = Overlay.LIST },
                        sheetActions = PlaceSheetActions(
                            onEditLocation = {
                                selected?.let { moveTo(it.latitude, it.longitude) }
                                overlay = Overlay.EDIT_LOCATION
                            },
                            onAddVisit = requestVisitLocation,
                            onEditInfo = { overlay = Overlay.EDIT_INFO },
                            onDelete = { overlay = Overlay.DELETE },
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
                }

                vm.duplicate?.let { candidate ->
                    DuplicateDialog(
                        candidate = candidate,
                        onAddVisit = vm::addVisitToDuplicate,
                        onNewPlace = { vm.saveNewPlace(candidate.location) },
                        onDismiss = vm::dismissDuplicate,
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
