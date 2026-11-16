package com.oblab.toiletmap.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

/**
 * 현재 위치를 한 번 받아오는 함수를 돌려준다.
 * 권한이 없으면 먼저 위치 권한을 묻고, 허용되면 이어서 위치를 받는다.
 */
@Composable
fun rememberMyLocationRequester(onLocation: (Location) -> Unit): () -> Unit {
    val context = LocalContext.current
    val currentOnLocation by rememberUpdatedState(onLocation)
    val client = remember { LocationServices.getFusedLocationProviderClient(context) }

    @SuppressLint("MissingPermission") // 호출 전에 권한 확인함
    fun fetch() {
        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) currentOnLocation(location)
                else Toast.makeText(context, "현재 위치를 찾지 못했어요", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "현재 위치를 찾지 못했어요", Toast.LENGTH_SHORT).show()
            }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        if (result.values.any { it }) fetch()
        else Toast.makeText(context, "위치 권한을 허용해 주세요", Toast.LENGTH_SHORT).show()
    }

    return {
        // 정확한 위치(FINE)를 거절해도 대략적 위치(COARSE)만 있으면 동작
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            fetch()
        } else {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            )
        }
    }
}
