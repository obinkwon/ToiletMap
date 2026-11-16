package com.oblab.toiletmap.ui.map

import android.location.Location
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraAnimation
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelOptions
import com.oblab.toiletmap.R

data class MapPin(val id: String, val latitude: Double, val longitude: Double)

/** 카메라 이동 요청. data class가 아니라서 같은 좌표로 다시 만들어도 새 요청으로 본다. */
class CameraTarget(val latitude: Double, val longitude: Double)

/** 카카오맵 MapView를 Compose에 붙인다. 핀을 누르면 [onPinClick]에 장소 id를 넘긴다. */
@Composable
fun KakaoMapView(
    pins: List<MapPin>,
    myLocation: Location?,
    cameraTarget: CameraTarget?,
    onPinClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onMapReady: (KakaoMap) -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentOnPinClick by rememberUpdatedState(onPinClick)
    val currentOnMapReady by rememberUpdatedState(onMapReady)
    var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }

    val mapView = remember {
        MapView(context).apply {
            start(
                object : MapLifeCycleCallback() {
                    override fun onMapDestroy() {}

                    // 인증 실패(앱 키, 키 해시)도 여기로 온다. Logcat에서 "KakaoMap" 확인
                    override fun onMapError(error: Exception?) {
                        Log.e("KakaoMap", "지도 오류", error)
                    }
                },
                object : KakaoMapReadyCallback() {
                    override fun onMapReady(map: KakaoMap) {
                        map.setOnLabelClickListener { _, _, label ->
                            currentOnPinClick(label.labelId)
                            true // 클릭 처리함: 지도 클릭으로 넘기지 않음
                        }
                        kakaoMap = map
                        currentOnMapReady(map)
                    }
                },
            )
        }
    }

    // SDK 문서: resume/pause를 안 부르면 지도가 멈추거나 앱이 죽는다
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.resume()
                Lifecycle.Event.ON_PAUSE -> mapView.pause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(kakaoMap, pins, myLocation) {
        val layer = kakaoMap?.labelManager?.layer ?: return@LaunchedEffect
        val icon = ContextCompat.getDrawable(context, R.drawable.ic_map_pin)!!.toBitmap()
        layer.removeAll()
        pins.forEach {
            layer.addLabel(
                LabelOptions.from(it.id, LatLng.from(it.latitude, it.longitude)).setStyles(icon).setClickable(true),
            )
        }
        if (myLocation != null) {
            val dot = ContextCompat.getDrawable(context, R.drawable.ic_my_location)!!.toBitmap()
            layer.addLabel(LabelOptions.from(LatLng.from(myLocation.latitude, myLocation.longitude)).setStyles(dot))
        }
    }

    // 새 목표가 올 때마다(같은 자리여도) 이동. 지도가 다시 만들어지면 마지막 목표로 다시 이동
    LaunchedEffect(kakaoMap, cameraTarget) {
        val map = kakaoMap ?: return@LaunchedEffect
        val target = cameraTarget ?: return@LaunchedEffect
        map.moveCamera(
            CameraUpdateFactory.newCenterPosition(LatLng.from(target.latitude, target.longitude), 16),
            CameraAnimation.from(300),
        )
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}
