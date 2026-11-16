package com.oblab.toiletmap.ui.map

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.oblab.toiletmap.data.formatDistance

/** 앱 등록 때 30m 안에 이전 장소가 있으면 묻는다. 바깥을 누르면 등록 취소. */
// ponytail: 가장 가까운 장소 하나만 보여준다. 여러 곳 중 고르게 하려면 목록으로 확장
@Composable
fun DuplicateDialog(
    candidate: DuplicateCandidate,
    onAddVisit: () -> Unit,
    onNewPlace: () -> Unit,
    onDismiss: () -> Unit,
) {
    val place = candidate.place
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("근처에 등록한 장소가 있어요") },
        text = {
            Text("${place.name ?: place.address ?: "이름 없음"} · ${formatDistance(candidate.distanceM)}\n이 장소에 방문을 추가할까요?")
        },
        confirmButton = { TextButton(onClick = onAddVisit) { Text("이전 장소에 방문 추가") } },
        dismissButton = { TextButton(onClick = onNewPlace) { Text("새 장소로 등록") } },
    )
}
