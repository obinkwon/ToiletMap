package com.oblab.toiletmap.widget

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.oblab.toiletmap.ToiletMapApp
import kotlinx.coroutines.launch

// ponytail: 위치를 못 찾을 때 기다리는 시간. 기획서 미정 항목이라 15초로 시작, 실기기 PoC 결과 보고 조정
private const val LOCATION_TIMEOUT_MS = 15_000L

/**
 * 위젯 탭 → 확인 얼럿 → 위치 찾기 → 저장까지 하는 투명 화면. 앱 전체는 열지 않는다.
 * 저장은 중복 확인 없이 새 장소로(dup_checked=N). 합칠지는 다음 앱 실행 때 묻는다.
 */
class WidgetRegisterActivity : ComponentActivity() {

    private enum class Step { NEED_PERMISSION, CONFIRM, FINDING }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var step by rememberSaveable {
                    mutableStateOf(if (hasLocationPermission()) Step.CONFIRM else Step.NEED_PERMISSION)
                }
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions(),
                ) { result ->
                    if (result.values.any { it }) step = Step.CONFIRM
                    else close("위치 권한이 없어 등록할 수 없어요")
                }

                when (step) {
                    Step.NEED_PERMISSION -> AlertDialog(
                        onDismissRequest = ::finish,
                        title = { Text("위치 권한을 먼저 허용해 주세요") },
                        text = { Text("화장실 위치를 저장하려면 '앱 사용 중에만 허용' 이상의 위치 권한이 필요해요.") },
                        confirmButton = {
                            TextButton(onClick = {
                                permissionLauncher.launch(
                                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                                )
                            }) { Text("허용하기") }
                        },
                        dismissButton = { TextButton(onClick = ::finish) { Text("취소") } },
                    )

                    Step.CONFIRM -> AlertDialog(
                        onDismissRequest = ::finish,
                        title = { Text("현재 위치를 등록하시겠습니까?") },
                        confirmButton = {
                            TextButton(onClick = {
                                step = Step.FINDING
                                findAndSave()
                            }) { Text("등록") }
                        },
                        dismissButton = { TextButton(onClick = ::finish) { Text("취소") } },
                    )

                    Step.FINDING -> AlertDialog(
                        onDismissRequest = {}, // 바깥을 눌러 실수로 닫히지 않게. 취소는 버튼으로
                        title = { Text("위치 찾는 중…") },
                        text = {
                            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(Modifier.size(32.dp))
                            }
                        },
                        confirmButton = {},
                        dismissButton = { TextButton(onClick = ::finish) { Text("취소") } },
                    )
                }
            }
        }
    }

    private fun hasLocationPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission") // CONFIRM 단계는 권한이 있을 때만 온다
    private fun findAndSave() {
        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setDurationMillis(LOCATION_TIMEOUT_MS)
            .build()
        LocationServices.getFusedLocationProviderClient(this).getCurrentLocation(request, null)
            .addOnSuccessListener { location ->
                if (location == null) {
                    close("현재 위치를 찾지 못했어요")
                } else {
                    // [취소]로 화면이 이미 닫혔으면 lifecycleScope가 취소돼서 저장하지 않는다
                    lifecycleScope.launch {
                        (application as ToiletMapApp).repository.registerNewPlace(location, fromWidget = true)
                        close("등록했어요")
                    }
                }
            }
            .addOnFailureListener { close("현재 위치를 찾지 못했어요") }
    }

    private fun close(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
        finish()
    }
}
