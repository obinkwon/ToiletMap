package com.oblab.toiletmap.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.location.Location
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.kakao.vectormap.label.LabelStyle
import com.oblab.toiletmap.R
import com.oblab.toiletmap.ui.theme.Green
import androidx.compose.ui.graphics.toArgb

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
    onMapClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentOnPinClick by rememberUpdatedState(onPinClick)
    val currentOnMapReady by rememberUpdatedState(onMapReady)
    val currentOnMapClick by rememberUpdatedState(onMapClick)
    var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }
    // 확대 단계가 바뀔 때만 핀을 다시 묶는다(이동만 하면 핀끼리 화면 간격은 그대로)
    var zoom by remember { mutableIntStateOf(0) }
    val clusterIcons = remember { HashMap<Int, Bitmap>() }

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
                            val clusterCenter = label.tag as? LatLng
                            if (clusterCenter != null) {
                                // 묶음 핀: 그 자리로 두 단계 확대해서 펼친다
                                val level = (map.cameraPosition?.zoomLevel ?: zoom) + 2
                                map.moveCamera(CameraUpdateFactory.newCenterPosition(clusterCenter, level), CameraAnimation.from(300))
                            } else {
                                currentOnPinClick(label.labelId)
                            }
                            true // 클릭 처리함: 지도 클릭으로 넘기지 않음
                        }
                        map.setOnCameraMoveEndListener { _, position, _ -> zoom = position.zoomLevel }
                        zoom = map.cameraPosition?.zoomLevel ?: 0
                        map.setOnMapClickListener { _, _, _, _ -> currentOnMapClick() }
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

    LaunchedEffect(kakaoMap, pins, myLocation, zoom) {
        val map = kakaoMap ?: return@LaunchedEffect
        val layer = map.labelManager?.layer ?: return@LaunchedEffect
        val icon = ContextCompat.getDrawable(context, R.drawable.ic_map_pin)!!.toBitmap()
        layer.removeAll()
        // 최대로 확대했으면 더 펼칠 수 없으니 묶지 않는다
        val groups = if (zoom >= map.maxZoomLevel) {
            pins.map { listOf(it) }
        } else {
            val radiusPx = CLUSTER_RADIUS_DP * context.resources.displayMetrics.density
            clusterByDistance(pins, radiusPx) { pin ->
                map.toScreenPoint(LatLng.from(pin.latitude, pin.longitude))?.let { it.x.toFloat() to it.y.toFloat() }
                    ?: (Float.NaN to Float.NaN) // 화면 좌표를 못 구하면 어디에도 안 묶인다
            }
        }
        groups.forEach { group ->
            val pin = group.singleOrNull()
            if (pin != null) {
                layer.addLabel(
                    LabelOptions.from(pin.id, LatLng.from(pin.latitude, pin.longitude)).setStyles(icon).setClickable(true),
                )
            } else {
                val center = LatLng.from(group.map { it.latitude }.average(), group.map { it.longitude }.average())
                val bitmap = clusterIcons.getOrPut(group.size) { clusterBitmap(context, group.size) }
                layer.addLabel(
                    LabelOptions.from(center)
                        .setStyles(LabelStyle.from(bitmap).setAnchorPoint(0.5f, 0.5f))
                        .setClickable(true)
                        .setTag(center),
                )
            }
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

/** 화면에서 이 거리(dp) 안에 겹치는 핀은 하나로 묶는다. 핀 아이콘 크기 정도. */
private const val CLUSTER_RADIUS_DP = 40f

/** 묶음 핀: 흰 테두리 초록 원 + 개수. */
private fun clusterBitmap(context: Context, count: Int): Bitmap {
    val density = context.resources.displayMetrics.density
    val size = (40 * density).toInt()
    val half = size / 2f
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(half, half, half, paint)
    paint.color = Green.toArgb()
    canvas.drawCircle(half, half, half - 3 * density, paint)
    paint.color = android.graphics.Color.WHITE
    paint.textSize = 15 * density
    paint.typeface = Typeface.DEFAULT_BOLD
    paint.textAlign = Paint.Align.CENTER
    canvas.drawText(if (count > 99) "99+" else "$count", half, half - (paint.descent() + paint.ascent()) / 2, paint)
    return bitmap
}
