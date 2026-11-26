package com.oblab.toiletmap.widget

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.oblab.toiletmap.EXTRA_PLACE_ID
import com.oblab.toiletmap.MainActivity
import com.oblab.toiletmap.ToiletMapApp
import com.oblab.toiletmap.ui.map.hasLocationPermission
import kotlinx.coroutines.launch

private const val LOCATION_TIMEOUT_MS = 15_000L
private const val AUTO_CLOSE_SECONDS = 3

/**
 * 위젯 탭 → 확인 카드 → 위치 찾기 → 저장 → 완료까지 하는 투명 화면. 앱 전체는 열지 않는다.
 * 저장은 중복 확인 없이 새 장소로(dup_checked=N). 합칠지는 다음 앱 실행 때 묻는다.
 */
class WidgetRegisterActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var step by rememberSaveable {
                    mutableStateOf(if (hasLocationPermission(this)) RegisterStep.CONFIRM else RegisterStep.NEED_PERMISSION)
                }
                var savedPlaceId by rememberSaveable { mutableStateOf<String?>(null) }
                var secondsLeft by rememberSaveable { mutableIntStateOf(AUTO_CLOSE_SECONDS) }
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions(),
                ) { result ->
                    if (result.values.any { it }) step = RegisterStep.CONFIRM
                    else close("위치 권한이 없어 등록할 수 없습니다")
                }

                RegisterSheet(
                    step = step,
                    secondsLeft = secondsLeft,
                    onDismiss = ::finish,
                    onPrimary = {
                        when (step) {
                            RegisterStep.NEED_PERMISSION -> permissionLauncher.launch(
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                            )
                            RegisterStep.CONFIRM -> {
                                step = RegisterStep.FINDING
                                findAndSave { placeId ->
                                    savedPlaceId = placeId
                                    step = RegisterStep.DONE
                                    startAutoClose { secondsLeft = it }
                                }
                            }
                            RegisterStep.DONE -> openInApp(savedPlaceId)
                            RegisterStep.FINDING -> Unit
                        }
                    },
                )
            }
        }
    }

    @SuppressLint("MissingPermission") // CONFIRM 단계는 권한이 있을 때만 온다
    private fun findAndSave(onSaved: (placeId: String) -> Unit) {
        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setDurationMillis(LOCATION_TIMEOUT_MS)
            .build()
        LocationServices.getFusedLocationProviderClient(this).getCurrentLocation(request, null)
            .addOnSuccessListener { location ->
                if (location == null) {
                    close("현재 위치를 찾지 못했습니다")
                } else {
                    // [취소]로 화면이 이미 닫혔으면 lifecycleScope가 취소돼서 저장하지 않는다
                    lifecycleScope.launch {
                        onSaved((application as ToiletMapApp).repository.registerNewPlace(location, fromWidget = true))
                    }
                }
            }
            .addOnFailureListener { close("현재 위치를 찾지 못했습니다") }
    }

    /**
     * 1초마다 [onTick]으로 남은 초를 알리고 0이 되면 닫는다.
     * Compose 타이머 대신 화면(View)에 직접 예약한다. 화면이 이미 닫혔으면 예약도 함께 사라진다.
     */
    private fun startAutoClose(onTick: (secondsLeft: Int) -> Unit) {
        fun tick(left: Int) {
            if (isFinishing) return
            if (left == 0) {
                finish()
                return
            }
            onTick(left)
            window.decorView.postDelayed({ tick(left - 1) }, 1_000)
        }
        tick(AUTO_CLOSE_SECONDS)
    }

    /** 앱을 새로 열어 방금 등록한 장소를 지도에서 보여준다. */
    private fun openInApp(placeId: String?) {
        startActivity(
            Intent(this, MainActivity::class.java)
                .putExtra(EXTRA_PLACE_ID, placeId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
        )
        finish()
    }

    private fun close(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
        finish()
    }
}
