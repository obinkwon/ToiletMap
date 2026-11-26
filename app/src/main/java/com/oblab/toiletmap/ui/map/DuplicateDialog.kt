package com.oblab.toiletmap.ui.map

import androidx.compose.runtime.Composable
import com.oblab.toiletmap.data.formatDistance

/** 앱 등록 때 30m 안에 이전 장소가 있으면 묻는다. 바깥을 누르면 등록 취소. */
@Composable
fun DuplicateDialog(
    candidate: DuplicateCandidate,
    onAddVisit: () -> Unit,
    onNewPlace: () -> Unit,
    onDismiss: () -> Unit,
) {
    val place = candidate.place
    AppDialog(
        title = "근처에 등록한 장소가 있습니다",
        subtitle = "${place.name ?: place.address ?: "이름 없음"} · ${formatDistance(candidate.distanceM)}",
        onDismissRequest = onDismiss,
        dismissText = "새 장소로 등록",
        onDismissClick = onNewPlace,
        confirmText = "방문 추가",
        onConfirm = onAddVisit,
    ) {
        BodyText("같은 화장실이면 이 장소에 방문을 추가하시겠습니까?")
    }
}
