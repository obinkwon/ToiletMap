package com.oblab.toiletmap

import android.location.Location
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.oblab.toiletmap.ui.map.MapScreen
import com.oblab.toiletmap.ui.map.rememberMyLocationRequester

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var myLocation by remember { mutableStateOf<Location?>(null) }
                val requestMyLocation = rememberMyLocationRequester { myLocation = it }

                // 앱을 열면 바로 현재 위치로 이동
                LaunchedEffect(Unit) { requestMyLocation() }

                MapScreen(
                    selected = null,
                    myLocation = myLocation,
                    onMyLocationClick = requestMyLocation,
                )
            }
        }
    }
}
