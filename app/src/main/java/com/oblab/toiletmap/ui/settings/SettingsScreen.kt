package com.oblab.toiletmap.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.oblab.toiletmap.ToiletMapApp
import com.oblab.toiletmap.data.AppSettings
import com.oblab.toiletmap.ui.map.AppDialog
import com.oblab.toiletmap.ui.map.BodyText
import com.oblab.toiletmap.ui.map.hasLocationPermission
import com.oblab.toiletmap.ui.theme.DividerColor
import com.oblab.toiletmap.ui.theme.Fill
import com.oblab.toiletmap.ui.theme.Green
import com.oblab.toiletmap.ui.theme.Ink
import com.oblab.toiletmap.ui.theme.SubText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 설정 화면 + 권한 상태 확인, 파일 선택, 백업 저장/읽기까지 처리. */
@Composable
fun SettingsRoute(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as ToiletMapApp
    val scope = rememberCoroutineScope()
    var radius by remember { mutableIntStateOf(app.settings.duplicateRadiusM) }
    var permissionGranted by remember { mutableStateOf(hasLocationPermission(context)) }
    var confirmImport by remember { mutableStateOf(false) }

    // 시스템 설정에서 권한을 바꾸고 돌아오면 상태를 다시 읽는다
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) permissionGranted = hasLocationPermission(context)
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_LONG).show()

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val json = app.repository.exportBackup()
                withContext(Dispatchers.IO) {
                    // "wt": 기존 파일에 덮어쓸 때 뒤에 옛 내용이 남지 않게 잘라낸다
                    context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(json.toByteArray()) }
                }
            }.onSuccess { toast("백업 파일을 저장했어요") }
                .onFailure { toast("저장하지 못했어요: ${it.message}") }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val text = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)!!.use { it.readBytes().toString(Charsets.UTF_8) }
                }
                app.repository.importBackup(text)
            }.onSuccess { toast("장소 ${it}곳을 가져왔어요") }
                .onFailure { toast("가져오지 못했어요: ${it.message}") }
        }
    }

    SettingsScreen(
        permissionGranted = permissionGranted,
        radius = radius,
        onBack = onBack,
        onOpenAppSettings = {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
            )
        },
        onRadiusChange = {
            radius = it
            app.settings.duplicateRadiusM = it
        },
        onExport = {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(Date())
            exportLauncher.launch("toiletmap-backup-$date.json")
        },
        onImport = { confirmImport = true },
    )

    if (confirmImport) {
        AppDialog(
            title = "백업을 가져올까요?",
            onDismissRequest = { confirmImport = false },
            dismissText = "취소",
            onDismissClick = { confirmImport = false },
            confirmText = "파일 고르기",
            onConfirm = {
                confirmImport = false
                importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
            },
        ) {
            BodyText("백업에 있는 장소는 지금 앱에 추가되고, 같은 장소는 백업 내용으로 바뀌어요. 걱정되면 먼저 내보내기를 해 두세요.")
        }
    }
}

@Composable
fun SettingsScreen(
    permissionGranted: Boolean,
    radius: Int,
    onBack: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onRadiusChange: (Int) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "지도로 돌아가기", tint = Ink)
            }
            Text("설정", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
        }

        Section("위치 권한", "위젯과 현재 위치 등록에는 '앱 사용 중에만 허용' 이상이 필요해요.") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (permissionGranted) "허용됨" else "허용 안 됨",
                    Modifier.weight(1f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (permissionGranted) Green else Color(0xFFB45309),
                )
                OutlinedButton(onClick = onOpenAppSettings, modifier = Modifier.height(44.dp)) {
                    Text("설정 열기", color = Ink)
                }
            }
        }

        HorizontalDivider(color = DividerColor)

        Section("중복 감지 반경", "등록할 때 이 거리 안에 이전 장소가 있으면 방문 추가할지 물어봐요.") {
            Row(
                Modifier.fillMaxWidth().background(Fill, RoundedCornerShape(12.dp)).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                AppSettings.RADIUS_OPTIONS.forEach { option ->
                    val on = option == radius
                    Surface(
                        selected = on,
                        onClick = { onRadiusChange(option) },
                        modifier = Modifier.weight(1f).height(40.dp),
                        shape = RoundedCornerShape(9.dp),
                        color = if (on) Color.White else Color.Transparent,
                        shadowElevation = if (on) 1.dp else 0.dp,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "${option}m",
                                fontSize = 14.sp,
                                fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                                color = if (on) Ink else Color(0xFF4A514C),
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = DividerColor)

        Section("백업", "모든 장소와 방문 기록을 JSON 파일로 저장하거나 불러와요. 비밀번호도 파일에 그대로 들어가니 보관에 주의하세요.") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onExport, modifier = Modifier.weight(1f).height(48.dp)) { Text("내보내기", color = Ink) }
                OutlinedButton(onClick = onImport, modifier = Modifier.weight(1f).height(48.dp)) { Text("가져오기", color = Ink) }
            }
        }
    }
}

@Composable
private fun Section(title: String, description: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text(description, fontSize = 13.sp, lineHeight = 19.sp, color = SubText)
        }
        content()
    }
}

@Preview(name = "설정", widthDp = 390, heightDp = 844)
@Composable
private fun SettingsPreview() {
    MaterialTheme {
        SettingsScreen(permissionGranted = false, radius = 30, onBack = {}, onOpenAppSettings = {}, onRadiusChange = {}, onExport = {}, onImport = {})
    }
}
