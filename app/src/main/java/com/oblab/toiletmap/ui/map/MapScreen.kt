package com.oblab.toiletmap.ui.map

import android.location.Location
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kakao.vectormap.KakaoMap
import com.oblab.toiletmap.R
import com.oblab.toiletmap.ui.theme.DividerColor
import com.oblab.toiletmap.ui.theme.Fill
import com.oblab.toiletmap.ui.theme.Green
import com.oblab.toiletmap.ui.theme.GreenDark
import com.oblab.toiletmap.ui.theme.GreenTint
import com.oblab.toiletmap.ui.theme.Ink
import com.oblab.toiletmap.ui.theme.Line
import com.oblab.toiletmap.ui.theme.MyBlue
import com.oblab.toiletmap.ui.theme.SubText

data class PlaceDetail(
    val name: String,
    val address: String,
    val distance: String,
    val visitCount: Int,
    val lastVisit: String,
    val password: String?,
    val memo: String?,
)

/** 메인 지도 화면. [selected]가 있으면 하단 시트로 장소 상세를 보여준다. */
@Composable
fun MapScreen(
    selected: PlaceDetail?,
    pins: List<MapPin> = emptyList(),
    myLocation: Location? = null,
    cameraTarget: CameraTarget? = null,
    editingLocation: Boolean = false,
    onPinClick: (String) -> Unit = {},
    onMyLocationClick: () -> Unit = {},
    onRegisterClick: () -> Unit = {},
    onListClick: () -> Unit = {},
    sheetActions: PlaceSheetActions = PlaceSheetActions(),
    onConfirmLocation: () -> Unit = {},
    onCancelEditLocation: () -> Unit = {},
    onMapReady: (KakaoMap) -> Unit = {},
) {
    Box(Modifier.fillMaxSize()) {
        // Preview는 네이티브 지도를 못 그려서 그림으로 대신한다
        if (LocalInspectionMode.current) {
            MapPlaceholder(hasSelection = selected != null, modifier = Modifier.fillMaxSize())
        } else {
            KakaoMapView(pins, myLocation, cameraTarget, onPinClick, Modifier.fillMaxSize(), onMapReady)
        }

        if (editingLocation) {
            // 지도 중앙 고정 핀: 핀 끝이 화면 중앙(카메라 위치)에 오도록 반 높이만큼 올린다
            Image(
                painterResource(R.drawable.ic_map_pin),
                contentDescription = "새 위치",
                modifier = Modifier.align(Alignment.Center).offset(y = (-18).dp).size(28.dp, 36.dp),
            )
            Text(
                "지도를 움직여 핀을 화장실 위치에 맞추세요",
                Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(16.dp)
                    .background(Color.White, RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                fontSize = 15.sp,
                color = Ink,
            )
        } else {
            TopBar(onListClick, Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(16.dp))
        }

        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            horizontalAlignment = Alignment.End,
        ) {
            RoundIconButton(Icons.Default.LocationOn, "내 위치로 이동", Modifier.padding(16.dp), tint = MyBlue, onClick = onMyLocationClick)
            when {
                editingLocation -> EditLocationBar(onCancelEditLocation, onConfirmLocation)
                selected == null -> RegisterButton(onRegisterClick)
                else -> PlaceSheet(selected, sheetActions)
            }
        }
    }
}

/** 하단 시트 버튼 동작. */
class PlaceSheetActions(
    val onEditLocation: () -> Unit = {},
    val onAddVisit: () -> Unit = {},
    val onEditInfo: () -> Unit = {},
    val onDelete: () -> Unit = {},
)

@Composable
private fun EditLocationBar(onCancel: () -> Unit, onConfirm: () -> Unit) {
    Row(
        Modifier
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.weight(1f).height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
        ) {
            Text("취소", color = Ink)
        }
        Button(
            onClick = onConfirm,
            modifier = Modifier.weight(1f).height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Green),
        ) {
            Text("이 위치로 저장", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TopBar(onListClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 검색은 목록 화면에서 한다. 여기선 누르면 목록 화면으로 이동
        Surface(
            onClick = onListClick,
            modifier = Modifier.weight(1f).height(48.dp),
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 4.dp,
        ) {
            Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Search, contentDescription = null, tint = SubText)
                Spacer(Modifier.width(8.dp))
                Text("이름·주소 검색", color = SubText, fontSize = 15.sp)
            }
        }
        RoundIconButton(Icons.AutoMirrored.Filled.List, "목록 보기", onClick = onListClick)
        RoundIconButton(Icons.Default.Settings, "설정")
    }
}

@Composable
private fun RoundIconButton(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    tint: Color = Ink,
    onClick: () -> Unit = {},
) {
    Surface(onClick = onClick, modifier = modifier.size(48.dp), shape = CircleShape, color = Color.White, shadowElevation = 4.dp) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = label, tint = tint) }
    }
}

@Composable
private fun RegisterButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
            .fillMaxWidth()
            .height(60.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Green),
    ) {
        Icon(Icons.Default.Add, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("현재 위치 등록", fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PlaceSheet(place: PlaceDetail, actions: PlaceSheetActions) {
    var showPassword by remember(place) { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = Color.White,
        shadowElevation = 8.dp,
    ) {
        Column(
            Modifier.navigationBarsPadding().padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(Modifier.align(Alignment.CenterHorizontally).size(36.dp, 4.dp).background(Line, RoundedCornerShape(2.dp)))

            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(place.name, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text(
                        listOf(place.address, place.distance).filter { it.isNotEmpty() }.joinToString(" · "),
                        fontSize = 14.sp,
                        color = SubText,
                    )
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, "더보기 (정보 편집, 삭제)", tint = SubText)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("이름·비밀번호·메모 편집") },
                            onClick = {
                                menuOpen = false
                                actions.onEditInfo()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("삭제", color = Color(0xFFB3261E)) },
                            onClick = {
                                menuOpen = false
                                actions.onDelete()
                            },
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Tag("방문 ${place.visitCount}회", GreenTint, GreenDark)
                Tag("마지막 방문 ${place.lastVisit}", Fill, Color(0xFF4A514C))
            }

            HorizontalDivider(color = DividerColor)

            if (place.password != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("비밀번호", fontSize = 13.sp, color = SubText)
                        // 가릴 때는 길이도 숨기려고 고정 길이 점을 쓴다
                        Text(
                            if (showPassword) place.password else "••••••",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = if (showPassword) 0.sp else 4.sp,
                            color = Ink,
                        )
                    }
                    OutlinedButton(onClick = { showPassword = !showPassword }, modifier = Modifier.height(44.dp)) {
                        Text(if (showPassword) "숨기기" else "보기", color = Ink)
                    }
                }
            }

            if (place.memo != null) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("메모", fontSize = 13.sp, color = SubText)
                    Text(place.memo, fontSize = 15.sp, lineHeight = 22.sp, color = Ink)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = actions.onEditLocation, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(14.dp)) {
                    Text("위치 수정", color = Ink)
                }
                Button(
                    onClick = actions.onAddVisit,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Green),
                ) {
                    Text("방문 추가", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun Tag(text: String, background: Color, color: Color) {
    Text(
        text,
        Modifier.background(background, RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 4.dp),
        color = color,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
    )
}

// Preview 전용 지도 그림 (실제 앱은 KakaoMapView)
@Composable
private fun MapPlaceholder(hasSelection: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier.background(Color(0xFFECEFE8))) {
        // 시안 기준 390x844 좌표를 화면 크기에 맞춰 늘린다
        val sx = size.width / 390f
        val sy = size.height / 844f
        fun p(x: Float, y: Float) = Offset(x * sx, y * sy)

        drawRect(Color(0xFFD3E6CC), p(230f, 190f), Size(140 * sx, 150 * sy))
        drawRect(Color(0xFFD3E6CC), p(20f, 320f), Size(90 * sx, 80 * sy))
        drawPath(
            Path().apply {
                moveTo(0f, 570 * sy)
                cubicTo(120 * sx, 530 * sy, 200 * sx, 620 * sy, 390 * sx, 590 * sy)
                lineTo(390 * sx, 670 * sy)
                cubicTo(210 * sx, 700 * sy, 120 * sx, 610 * sy, 0f, 650 * sy)
                close()
            },
            Color(0xFFC9DEEE),
        )
        listOf(
            Triple(p(-10f, 250f), p(400f, 300f), 18f),
            Triple(p(150f, -10f), p(190f, 860f), 22f),
            Triple(p(-10f, 470f), p(400f, 430f), 12f),
            Triple(p(300f, -10f), p(280f, 560f), 10f),
            Triple(p(60f, -10f), p(90f, 560f), 10f),
            Triple(p(-10f, 770f), p(400f, 750f), 14f),
        ).forEach { (a, b, w) -> drawLine(Color.White, a, b, w * sx, StrokeCap.Round) }

        val dim = if (hasSelection) 0.6f else 1f
        drawPin(p(110f, 200f), sx * if (hasSelection) 1.5f else 1f, if (hasSelection) GreenDark else Green)
        drawPin(p(300f, 256f), sx, Green, dim)
        drawPin(p(230f, 530f), sx, Green, dim)

        // 묶음 핀(숫자는 SDK 클러스터가 그려줌)
        drawCircle(Color.White.copy(alpha = dim), 22 * sx, p(88f, 442f))
        drawCircle(Green.copy(alpha = dim), 19 * sx, p(88f, 442f))

        drawCircle(MyBlue.copy(alpha = 0.15f), 32 * sx, p(173f, 408f))
        drawCircle(Color.White, 9 * sx, p(173f, 408f))
        drawCircle(MyBlue, 6 * sx, p(173f, 408f))
    }
}

/** [tip]은 핀 끝(실제 좌표) 위치. */
private fun DrawScope.drawPin(tip: Offset, scale: Float, color: Color, alpha: Float = 1f) {
    val head = Offset(tip.x, tip.y - 22 * scale)
    drawPath(
        Path().apply {
            moveTo(tip.x - 9 * scale, tip.y - 17 * scale)
            lineTo(tip.x + 9 * scale, tip.y - 17 * scale)
            lineTo(tip.x, tip.y)
            close()
        },
        color,
        alpha,
    )
    drawCircle(Color.White, 13 * scale, head, alpha)
    drawCircle(color, 11.5f * scale, head, alpha)
    drawCircle(Color.White, 4.5f * scale, head, alpha)
}

private val PreviewPlace = PlaceDetail(
    name = "역삼 센터빌딩 2층",
    address = "서울 강남구 테헤란로 [번지]",
    distance = "120m",
    visitCount = 3,
    lastVisit = "2일 전",
    password = "1234*",
    memo = "남자 화장실 오른쪽 끝 칸. 휴지 자주 없음.",
)

@Preview(name = "지도 · 기본", widthDp = 390, heightDp = 844)
@Composable
private fun MapScreenPreview() {
    MaterialTheme { MapScreen(selected = null) }
}

@Preview(name = "지도 · 핀 선택", widthDp = 390, heightDp = 844)
@Composable
private fun MapScreenSelectedPreview() {
    MaterialTheme { MapScreen(selected = PreviewPlace) }
}
